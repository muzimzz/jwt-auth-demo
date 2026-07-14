# 소셜 로그인 전체 플로우 — Security 담당 vs 직접 구현

Spring Boot 3.x / Spring Security 6.x 기준. 구글/카카오/네이버 3사 `oauth2Login()` 연동.

**1단계(로그인 + DB 저장)는 완료**. 구글/카카오/네이버 3사 실제 로그인 테스트까지 확인함. `DefaultOAuth2User`/`DefaultOidcUser`를 그대로 안 쓰고 Custom으로 감싸서, 나중에 `User`가 커져도 principal 클래스는 안 바뀌게 설계함. `SocialUserProcessor` 같은 공용 헬퍼는 만들지 않기로 하고, DB 조회/저장 로직은 OAuth2/OIDC 두 서비스에 각각 중복 작성함.

**2단계(지금부터)**: JWT 토큰 발급 + 세션 대신 토큰 기반 인증으로 전환. 컬럼 확장(친구, 닉네임 등 비즈니스 로직)은 그 다음.

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

### 2단계(JWT)에서 새로 만들 클래스 (예정)

| 클래스 | 경로 | 용도 |
|---|---|---|
| `JwtTokenProvider` | `jwt` | Access/Refresh 토큰 생성, 파싱, 서명 검증, 만료 검증 담당 |
| `OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler` | `oauth` 또는 `security` | `.oauth2Login().successHandler(...)`로 등록. 로그인 성공 시 `CustomOAuth2User`/`CustomOidcUser`에서 `user.getId()`/`role` 꺼내 토큰 발급 후 클라이언트로 전달 |
| `JwtAuthenticationFilter extends OncePerRequestFilter` | `jwt` | 매 요청마다 `Authorization: Bearer ...` 검증해서 `SecurityContextHolder`에 인증 정보 세팅. `UsernamePasswordAuthenticationFilter` 앞에 등록 |
| `RefreshTokenRepository` (Redis 기반) | `repository` 또는 `jwt` | Refresh Token을 `user:{id}:refreshToken` 형태로 Redis에 저장 (TTL = 만료시간) |
| `AuthController` | `controller` | 토큰 재발급(`/api/auth/reissue`), 로그아웃(Redis에서 refresh token 삭제) API |

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





## 8. (2단계) JWT 발급 — 세션 기반 인증에서 토큰 기반 인증으로 전환

지금은 7번까지 끝나면 `Authentication`이 `HttpSession`에 저장되는 Security 기본 동작으로 로그인이 마무리됨. JWT로 넘어간다는 건 "로그인 성공 후 세션 대신 토큰을 발급하고, 이후 요청은 세션이 아니라 그 토큰으로 인증한다"로 바꾸는 것.


### 8-1. 토큰 설계 [직접 구현]

- Access Token: 짧은 만료(예: 30분~1시간). 매 요청 인증에 사용. 클레임은 최소 `sub`(user id)와 `role` 정도로 충분.
- Refresh Token: 긴 만료(예: 2주). Access Token 재발급 전용이고 매 요청에는 안 쓰임.
- 서명 키는 `application.yaml`에 넣지 말고 시크릿 값 자체를 환경변수로 분리 (client-secret과 같은 이유).


### 8-2. 로그인 성공 시 토큰 발급 [직접 구현]

지금 `SecurityConfig`의 `.oauth2Login(...)`에는 성공 핸들러가 없어서 기본 동작(리다이렉트)만 일어남. `OAuth2LoginSuccessHandler`를 만들어 `.successHandler(...)`로 등록하고, `Authentication`에서 `CustomOAuth2User`/`CustomOidcUser`를 꺼내 `user.getId()`/`user.getRole()`로 Access/Refresh 토큰을 발급.

토큰을 클라이언트로 어떻게 넘길지 결정 필요: (a) 프론트 URL로 리다이렉트하며 쿼리파라미터/fragment에 담기, (b) httpOnly Secure 쿠키로 내려주기, (c) JSON으로 바로 반환. 프론트가 SPA라면 보통 (a) 또는 (b).


### 8-3. Refresh Token 저장 [직접 구현]

`build.gradle`에 이미 있는 `spring-boot-starter-data-redis`를 이 용도로 사용. TTL 기반 만료가 자연스러워서 Refresh Token 저장에 적합. `user:{id}:refreshToken` 형태 키로 저장하고, 재발급 요청 때 클라이언트가 보낸 값과 비교해서 검증.


### 8-4. 요청마다 토큰 검증 [직접 구현]

`JwtAuthenticationFilter`(`OncePerRequestFilter`)가 `Authorization: Bearer ...` 헤더에서 토큰을 꺼내 서명·만료를 검증하고, 유효하면 `Authentication`을 만들어 `SecurityContextHolder`에 세팅. `SecurityConfig`에서 이 필터를 `UsernamePasswordAuthenticationFilter` 앞에 등록.


### 8-5. 세션 끄기 [직접 구현]

`.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))`로 전환. 단, OAuth2 인가 코드 교환 "과정 중"에 Security가 `state`/PKCE 값을 잠깐 저장하는 데는 여전히 저장소가 필요하므로, 세션을 완전히 끄면 `authorization-request-repository`를 쿠키 기반으로 바꿔야 할 수도 있음 (여기는 로그인 진행 중에만 쓰이는 임시 저장소라 STATELESS와는 별개 이슈).


### 8-6. 재발급 API [직접 구현]

`POST /api/auth/reissue`: 클라이언트가 보낸 refresh token 검증 → Redis에 저장된 값과 일치하면 새 Access Token 발급(+ 필요하면 Refresh Token도 회전).


### 8-7. 로그아웃 [직접 구현]

Redis에서 해당 유저의 refresh token 삭제. 카카오/네이버 자체 로그아웃(토큰 revoke) 연동은 이번에도 범위에 넣을지 별도 결정.


### 8-8. (3단계) 컬럼 확장, 친구 시스템

JWT까지 끝나면 진행. 범위 밖.

---

## 만들어야 할 것 체크리스트

### 1단계 — 로그인 + DB 저장 (완료)

- [x] `OAuthAttributes` (`ofGoogle`/`ofKakao`/`ofNaver`/`toEntity` 구현 완료)
- [x] `SocialType`, `User`, `UserRole`, `UserRepository`
- [x] `CustomOAuth2User`, `CustomOAuth2UserService`
- [x] `CustomOidcUser`, `CustomOidcUserService`
- [x] `SecurityConfig`에 `userService(...)` / `oidcUserService(...)` 등록
- [x] 실제 로그인 테스트 (구글/카카오/네이버 3사 전부, DB 저장 확인)

### 1단계 마무리 전 남은 정리 항목

- [ ] 기존 회원 재로그인 시 `name`/`email` 갱신 로직 (`User.update()` + 서비스에서 `.map(existing -> existing.update(...))`) — 아직 미반영
- [ ] `SecurityConfig`의 `authorizeHttpRequests`에서 `.requestMatchers("/**").permitAll()` 뒤에 오는 `.anyRequest().authenticated()`가 죽은 규칙 — 의도 확인 후 정리
- [ ] `application.yaml`의 카카오/네이버/구글 client-secret 평문 노출 → 환경변수로 치환
- [ ] 카카오 `scope`의 `friends` 제거 (친구 기능 붙이기 전까지는 불필요, 심사 안 된 앱이면 로그인 실패 위험)

### 2단계 — JWT 발급 (진행 예정)

- [ ] 토큰 설계 (Access/Refresh 만료시간, 클레임, 서명 키 환경변수 분리)
- [ ] `JwtTokenProvider` (생성/파싱/검증)
- [ ] `OAuth2LoginSuccessHandler` (로그인 성공 시 토큰 발급) + `SecurityConfig`에 등록
- [ ] Refresh Token Redis 저장 (`RefreshTokenRepository`)
- [ ] `JwtAuthenticationFilter` + `SecurityConfig`에 필터 등록
- [ ] `SecurityConfig` 세션 정책 STATELESS로 전환 (필요 시 authorization-request-repository 쿠키 기반으로 변경)
- [ ] 재발급 API (`AuthController`)
- [ ] 로그아웃 API (Redis refresh token 삭제)

### 3단계 — 컬럼 확장, 친구 시스템 (범위 밖, 추후)
