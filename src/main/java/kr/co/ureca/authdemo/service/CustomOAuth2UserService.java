package kr.co.ureca.authdemo.service;

import kr.co.ureca.authdemo.entity.User;
import kr.co.ureca.authdemo.oauth.CustomOAuth2User;
import kr.co.ureca.authdemo.oauth.OAuthAttributes;
import kr.co.ureca.authdemo.oauth.SocialType;
import kr.co.ureca.authdemo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 네이버(순수 OAuth2, openid 없음) 로그인 경로.
 * 카카오/구글은 scope에 openid가 있어서 여기가 아니라 CustomOidcUserService를 탄다.
 *
 * SocialUserProcessor 같은 공용 헬퍼는 안 만들기로 했으므로, DB 조회/저장 로직은
 * CustomOidcUserService에도 그대로 중복해서 작성한다.
 */
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(userRequest); // 원본 attributes 확보 (userinfo 호출은 Security가 수행)

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        SocialType socialType = SocialType.valueOf(registrationId.toUpperCase());
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        OAuthAttributes attrs = OAuthAttributes.of(socialType, userNameAttributeName, oAuth2User.getAttributes());

        User user = userRepository.findBySocialTypeAndSocialTypeId(attrs.socialType(), attrs.socialTypeId())
                .map(existingUser -> existingUser.update(attrs.name(), attrs.email()))
                // 정보가 바뀐채로 로그인될경우 update, 컬럼이 많아지면 dto 고려. 실제로 값이 바뀌지 않았을 경우에는 update쿼리 x (dirty check)
                .orElseGet(() -> userRepository.save(attrs.toEntity()));

        return new CustomOAuth2User(user, List.of(new SimpleGrantedAuthority(user.getRole().name())), attrs.attributes(), attrs.nameAttributeKey());
    }
}
