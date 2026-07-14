# 소셜 로그인 전체 플로우 — Security 담당 vs 직접 구현

Spring Boot 3.x / Spring Security 6.x 기준. 구글/카카오/네이버 3사 `oauth2Login()` 연동.

**이번 단계 범위**: 로그인 완료 + DB 저장까지만. 컬럼 확장(친구, 닉네임 등 비즈니스 로직)과 JWT 토큰 발급은 다음 단계로 미룸. `DefaultOAuth2User`/`DefaultOidcUser`를 그대로 안 쓰고 Custom으로 감싸서, 나중에 `User`가 커져도 principal 클래스는 안 바뀌게 설계함. `SocialUserProcessor` 같은 공용 헬퍼는 만들지 않기로 하고, DB 조회/저장 로직은 OAuth2/OIDC 두 서비스에 각각 중복 작성함.

각 단계 앞에 `[Security 담당]` / `[직접 구현]` 을 표시했다. 등록·설정처럼 코드는 아니지만 개발자가 직접 해야 하는 작업은 `[사전 설정]`으로 별도 표시했다.

---

## 클래스 목록과 용도

| 클래스 | 경로 | 용도 |
|---|---|---|
| `OAuthAttributes` (record) | `oauth` | 소셜 타입별 원본 응답을 `attributes`/`nameAttributeKey`/`socialType`/`socialTypeId`/`name`/`email`로 통일. `of()`가 socialType으로 분기, `toEntity()`로 신규 `User` 생성 |
| `SocialType` (enum) | `oauth` | `KAKAO`/`NAVER`/`GOOGLE`. registrationId(String) 매직 스트링 방지용 |
| `User` (엔티티) | `entity` | DB 저장 대상. 지금은 `id`/`socialType`/`socialTypeId`/`name`/`email`/`role`만 |
| `UserRole` (enum) | `entity` | `ROLE_USER` |
| `UserRepository` | `repository` | `findBySocialTypeAndSocialTypeId(...)` |
| `CustomOAuth2UserService extends DefaultOAuth2UserService` | `service` | 네이버(openid 없음) 경로. `super.loadUser()`로 원본 확보 → 정규화·DB 저장 → `CustomOAuth2User`로 감싸 리턴 |
| `CustomOAuth2User extends DefaultOAuth2User` | `oauth` | `User` 보유 + `getUser()`. `Serializable`/`equals` 상속받으려고 `implements` 대신 `extends` |
| `CustomOidcUserService extends OidcUserService` | `service` | 카카오/구글(openid 있음) 경로. 위와 동일한 패턴, 리턴 타입만 `OidcUser` |
| `CustomOidcUser extends DefaultOidcUser` | `oauth` | `User` 보유 + `getUser()`. `OidcUser` 계약이라 id_token/userInfo도 같이 들고 있음 |
| `SecurityConfig` | `config` | `.userInfoEndpoint()`에 `userService(customOAuth2UserService)`와 `oidcUserService(customOidcUserService)` 둘 다 등록 |

---

## 1. 로그인 버튼 클릭 → `/oauth2/authorization/{provider}` 요청

**[Security 담당]** `OAuth2AuthorizationRequestRedirectFilter`가 이 URL 패턴을 가로챈다. `application.yaml`의 registration 정보(client-id, scope, authorization-uri)로 인가 요청을 만들고, CSRF 방지용 `state` 값과 PKCE `code_challenge`를 생성해 세션에 저장한 뒤 소셜 로그인 화면으로 리다이렉트한다.

프론트엔드는 이 URL로 링크만 걸면 되고 별도 구현이 필요 없다.

## 2. 사용자가 소셜 사이트에서 로그인 + 동의

완전히 소셜사 서버에서 벌어지는 일이라 Spring Security 영역 밖이다.

**[사전 설정]** 각 사 콘솔에 redirect-uri 등록, 동의항목 설정, (카카오) Client Secret 활성화, (카카오) OpenID Connect 활성화 등은 코드는 아니지만 개발자가 직접 해야 하는 작업이다.

## 3. 콜백 수신 — `GET /login/oauth2/code/{provider}?code=...&state=...`

**[Security 담당]** `OAuth2LoginAuthenticationFilter`가 이 경로를 가로채서 `state`를 검증(CSRF 방지)하고, 받은 인가 코드로 프로바이더 토큰 엔드포인트에 access_token(OIDC면 id_token도)을 요청·교환한다.

## 4. openid scope 유무로 경로가 갈림

**[Security 담당]** `scope`에 `openid`가 있으면(카카오/구글) `OidcAuthorizationCodeAuthenticationProvider` → `CustomOidcUserService`, 없으면(네이버) `OAuth2LoginAuthenticationProvider` → `CustomOAuth2UserService`로 라우팅된다.

## 5. 사용자 정보 조회 + 정규화

**[Security 담당]** 각 서비스의 `super.loadUser()`가 실제 userinfo HTTP 호출(또는 id_token 클레임 추출)을 수행한다.

**[직접 구현]** 프로바이더마다 응답 모양이 다르므로(카카오 플랫, 네이버 `response` 중첩) `OAuthAttributes.of(socialType, userNameAttributeName, attributes)`로 정규화한다.

## 6. DB 조회/저장

**[직접 구현]** `UserRepository.findBySocialTypeAndSocialTypeId(...)`로 기존 회원 조회, 없으면 `attrs.toEntity()`로 신규 저장. 이 결과를 `CustomOAuth2User`/`CustomOidcUser`로 감싸서 리턴해야 Security가 인증 완료 처리를 이어갈 수 있다.

## 7. 인증 완료 처리 — SecurityContext/세션에 저장

**[Security 담당]** `loadUser()`가 리턴한 객체를 `OAuth2AuthenticationToken`으로 감싸 `SecurityContextHolder`에 넣고, 세션(`HttpSession`)에 저장한다.

## 8. (다음 단계) JWT 발급, 로그아웃, 컬럼 확장

이번 단계 범위 밖. 로그인 + DB 저장까지 끝나면 다음으로 진행.

---

## 만들어야 할 것 체크리스트

- [x] `OAuthAttributes` (구조 완성, `ofGoogle`/`ofKakao`/`ofNaver`/`toEntity`는 TODO 스켈레톤)
- [x] `SocialType`, `User`, `UserRole`, `UserRepository`
- [x] `CustomOAuth2User`, `CustomOAuth2UserService` (스켈레톤)
- [x] `CustomOidcUser`, `CustomOidcUserService` (스켈레톤)
- [x] `SecurityConfig`에 `oidcUserService(...)` 등록
- [ ] `OAuthAttributes.ofGoogle` 구현
- [ ] `OAuthAttributes.ofKakao` 구현
- [ ] `OAuthAttributes.ofNaver` 구현 (response 봉투 풀기 포함)
- [ ] `OAuthAttributes.toEntity` 구현
- [ ] 실제 로그인 테스트 (3사 전부)
