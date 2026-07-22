# 정리형(확인형) 질문 모음

대화 중 "내가 이해한 걸 정리하면 ~ 맞아?" / "~라는거지?" / "~아니야?" / "~이라고 하면 돼?" 처럼,
본인이 이해한 내용을 스스로 요약·재구성한 뒤 맞는지 확인받는 형식으로 물어본 질문들만 모았습니다.
단순 "이게 뭐야?" 류의 정보 질문은 제외했습니다.

---

## 1. 응답 정규화 / OAuthAttributes 설계

- "지금 상태는 `response.get(...)`을 써야 하는데, response가 Security에 의존적인 객체니까 DTO로 변환하는 과정을 추가하자는 거지?"
  → 맞음. 이게 `OAuthAttributes`를 만든 이유.
- "`OAuthAttributes`에서 응답을 바로 저장하고, 서비스에서 쓸 클래스를 또 만들라는 게 `CustomOAuth2UserService` 같은 거야?"
  → 맞음.
- "`OAuthAttributes`는 외부 API 응답 형식을 그대로 따라가야 한다는 건가?"
  → 맞음 (그래서 provider마다 `ofGoogle`/`ofKakao`/`ofNaver`로 분기).
- "이 record는 사용자 요청이 아니라 API 응답이라 `@Valid`를 안 쓰는 거야? nullable 정도는 명시하는 게 좋지 않나?"
  → 맞음. 대신 필드 주석으로 nullable 여부를 표기.
- "`of` 메서드는 static인 거 이해하는데, `ofGoogle`/`ofKakao`도 다 static이어야 해? private만 있으면 안 돼?"
  → static이어야 함 (인스턴스 생성 전에 호출되는 팩토리 메서드라서).
- "`ofKakao`의 `.nameAttributeKey(userNameAttributeName)`이랑 `.socialTypeId(...)` // `"sub"` 은 같은 값 아니야?"
  → 값은 같을 수 있지만 의미(키 이름 vs 실제 id 값)가 달라서 구분해서 저장.
- "그럼 일단 이렇게 하면 돼?" (`ofGoogle` 구현 시도 코드 첨부)
  → 리뷰 후 `"nickname"` → `"name"` 키 오타 지적, 본인이 수정.
- "unchecked cast 경고 뜨는 건 어쩔 수 없어?"
  → 맞음, `@SuppressWarnings("unchecked")`로 처리.
- "그냥 attrs를 통째로 넘기면 좋지 않냐고 물어보는 거야, 컬럼 많아졌을 때"
  → 트레이드오프 있음(식별자 필드 실수로 덮어쓸 위험) — `UserUpdateInfo` 같은 절충안 제시.

## 2. CustomOAuth2User / CustomOidcUser / loadUser

- "이 코드는 원래 Security가 OIDC를 처리하면 `OidcUser`를 반환받지만, 네이버는 OIDC가 안 되니까 `OAuth2User`를 받아서 처리하는 코드인 건가?"
  → 맞음.
- "`registration.name`으로 할 거면 `AuthProvider` enum은 안 만들어도 되는 건가?"
  → enum은 필요함 (registrationId는 String이라 타입 안전성이 없어서).
- "registrationId를 String으로 쓰는 건 yaml에 있는 값이 그대로 들어가는 거 아님?"
  → 맞음.
- "`getName()`을 재정의 안 하면 Security가 모르고 쓰다가 에러 나?"
  → 정확히는 세션 직렬화/재구성 시점에 문제가 될 수 있음.
- "`return new CustomOAuth2User(user, oAuth2User.getAuthorities(), ...)` 이대로 쓰면 authorities에 `ROLE_USER`가 아니라 `OIDC_USER`/`OAUTH2_USER`가 저장되는 거 아니야? 그러면 안 되잖아"
  → 맞음, 실제 버그였음. `user.getRole()` 기반으로 수정.
- "loadUser는 원래 OIDC가 아니라 일반 세션 로그인에도 쓰는 거 아니야? 리턴 타입이 OIDC 전용이면 안 되는 거 아니야?"
  → 아님. 일반 로그인은 `UserDetailsService`, OAuth2/OIDC는 `OAuth2UserService`로 완전히 별개.
- "loadUser에서 반환만 하면 최종 로그인 처리(SecurityContext 저장)는 알아서 해주는 거지?"
  → 맞음, Security가 자동 처리.

## 3. 카카오/제공자별 응답 구조

- "카카오 디벨로퍼 문서 응답엔 providerId 같은 게 없지 않나?"
  → 맞음, `sub`(OIDC) 또는 `id`(REST API)가 그 역할.
- "카카오는 왜 `user-info-uri`가 있는 거? OIDC면 id_token만으로 충분한 거 아니야?"
  → 카카오 동의항목 중 일부(닉네임 등)는 userinfo 호출로 추가 조회해야 함.
- "`OAuth2User.loadUser`의 반환값이 id_token이라는 거지?"
  → 아님, id_token은 클레임 소스 중 하나일 뿐이고 반환값은 정규화된 `OAuth2User`/`OidcUser` 객체.

## 4. yaml ↔ registrationId ↔ ClientRegistration 바인딩

- "결국 userinfoendpoint로 따로 안 빼주면 그냥 알아서 `DefaultOAuth2User` 타입으로 반환된다는 건가?"
  → 절반만 맞음. OIDC 경로는 `DefaultOidcUser`, 순수 OAuth2 경로만 `DefaultOAuth2User`.
- "yaml의 kakao/naver가 key, 나머지가 value인 Map이 `ClientRegistration`이라는 이름으로 저장됨 — 이렇게 말하면 돼?"
  → 거의 맞음. 정확히는 Map의 각 entry 하나하나가 `ClientRegistration` 객체 1개가 되고, 그걸 모은 게 `ClientRegistrationRepository`.

## 5. DB 저장/업데이트 로직

- "userRepository에 저장하는 서비스 로직 보면, 기존 회원 정보가 바뀐 채로 로그인됐을 때 update하는 로직이 없는데 괜찮은 거야?"
  → 실제 누락이었음, `existing.update(...)` 추가 필요.
- "update가 필요하다고 생각은 하는데, 실제로는 값이 안 바뀐 경우가 대부분일 텐데 매번 update 쿼리 날리는 거 괜찮나?"
  → 괜찮음, JPA dirty checking이 값 안 바뀌면 UPDATE 자체를 생략함.
- "google/kakao/naver 로그인 결과가 `[username] 2/3/1 [role] ROLE_USER`로 나오는데 이게 맞는 거야?"
  → 맞음 — username은 DB PK, 세 프로바이더가 별개 row로 저장된 결과.

## 6. 전체 흐름/단계 전환 확인

- "그러면 지금 상태에서 딱 로그인까지는 전부 구현된 거야?"
  → 큰 흐름은 맞으나 update 로직, 시큐리티 죽은 규칙, 시크릿 노출 등 잔여 항목 존재.
- "그러면 이제 해야 할 거 JWT로 넘어가면 돼?"
  → 맞음, 2단계로 확인하고 md에 반영.
- "여기까지가 우리가 지금 개발한 거고 이제 토큰 처리 하면 되는 거고"
  → 맞음.
