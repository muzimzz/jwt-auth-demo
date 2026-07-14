package kr.co.ureca.authdemo.oauth;

import kr.co.ureca.authdemo.entity.User;
import kr.co.ureca.authdemo.entity.UserRole;
import lombok.Builder;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;

import java.util.Map;

@Builder
public record OAuthAttributes(
        Map<String, Object> attributes,   // 원본 응답 전체. 지금 필드로 안 뽑은 값이 나중에 필요하면 여기서 꺼냄
        String nameAttributeKey,          // OAuth2User/OidcUser 재구성할 때 필요한 식별자 키 (예: "id", "sub")
        SocialType socialType,            // KAKAO | NAVER | GOOGLE
        String socialTypeId,              // 소셜 타입 안에서의 고유 ID. 항상 String
        String name,                      // 표시 이름/닉네임. null일 수 있음
        String email                      // 이메일. 동의 안 하면 null (가짜 값 합성 금지)
) {

    public static OAuthAttributes of(SocialType socialType, String userNameAttributeName, Map<String, Object> attributes) {
        return switch (socialType) {
            case GOOGLE -> ofGoogle(socialType, userNameAttributeName, attributes);
            case KAKAO -> ofKakao(socialType, userNameAttributeName, attributes);
            case NAVER -> ofNaver(socialType, attributes);
        };
    }

    private static OAuthAttributes ofGoogle(SocialType socialType, String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .socialType(socialType)
                .socialTypeId(String.valueOf(attributes.get(userNameAttributeName)))
                // .socialTypeId(attributes.get(userNameAttributeName).toString())
                .name((String) attributes.get("name"))
                .email((String) attributes.get("email"))
                .build();
    }

    private static OAuthAttributes ofKakao(SocialType socialType, String userNameAttributeName, Map<String, Object> attributes) {
        return OAuthAttributes.builder()
                .attributes(attributes)
                .nameAttributeKey(userNameAttributeName)
                .socialType(socialType)
                .socialTypeId(String.valueOf(attributes.get(userNameAttributeName)))
                // .socialTypeId(attributes.get(userNameAttributeName).toString())
                .name((String) attributes.get("nickname"))
                .email((String) attributes.get("email"))
                .build();
    }

    @SuppressWarnings("unchecked")
    private static OAuthAttributes ofNaver(SocialType socialType, Map<String, Object> attributes) {
        Map<String, Object> response = (Map<String, Object>) attributes.get("response");
        if (response == null) {
            throw new OAuth2AuthenticationException("네이버 응답에서 response 속성을 찾을 수 없습니다.");
        }

        String userNameAttributeName = "id";

        return OAuthAttributes.builder()
                .attributes(response)
                .nameAttributeKey(userNameAttributeName)
                .socialType(socialType)
                .socialTypeId(String.valueOf(response.get(userNameAttributeName)))
                .name((String) response.get("nickname"))
                .email((String) response.get("email"))
                .build();
    }

    public User toEntity() {
        return User.builder()
                .socialType(socialType)
                .socialTypeId(socialTypeId)
                .name(name)
                .email(email)
                .role(UserRole.ROLE_USER)
                .build();
    }
}
