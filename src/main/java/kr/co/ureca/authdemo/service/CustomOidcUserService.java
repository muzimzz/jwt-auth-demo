package kr.co.ureca.authdemo.service;

import kr.co.ureca.authdemo.entity.User;
import kr.co.ureca.authdemo.oauth.CustomOidcUser;
import kr.co.ureca.authdemo.oauth.OAuthAttributes;
import kr.co.ureca.authdemo.oauth.SocialType;
import kr.co.ureca.authdemo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 카카오/구글(OIDC, openid 있음) 로그인 경로.
 * 네이버는 scope에 openid가 없어서 여기가 아니라 CustomOAuth2UserService를 탄다.
 *
 * SocialUserProcessor 같은 공용 헬퍼는 안 만들기로 했으므로, DB 조회/저장 로직은
 * CustomOAuth2UserService와 동일한 코드를 여기도 그대로 중복해서 작성한다.
 */
@Service
@RequiredArgsConstructor
public class CustomOidcUserService extends OidcUserService {

    private final UserRepository userRepository;

    @Override
    @Transactional
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = super.loadUser(userRequest); // id_token 검증 + (설정된 경우) userinfo 병합까지 끝난 상태

        String registrationId = userRequest.getClientRegistration().getRegistrationId();
        SocialType socialType = SocialType.valueOf(registrationId.toUpperCase());
        String userNameAttributeName = userRequest.getClientRegistration()
                .getProviderDetails().getUserInfoEndpoint().getUserNameAttributeName();

        OAuthAttributes attrs = OAuthAttributes.of(socialType, userNameAttributeName, oidcUser.getAttributes());

        User user = userRepository.findBySocialTypeAndSocialTypeId(attrs.socialType(), attrs.socialTypeId())
                .map(existingUser -> existingUser.update(attrs.name(), attrs.email()))
                // 정보가 바뀐채로 로그인될경우 update, 컬럼이 많아지면 dto 고려. 실제로 값이 바뀌지 않았을 경우에는 update쿼리 x (dirty check)
                .orElseGet(() -> userRepository.save(attrs.toEntity()));

        return new CustomOidcUser(user, List.of(new SimpleGrantedAuthority(user.getRole().name())), oidcUser.getIdToken(), oidcUser.getUserInfo(), attrs.nameAttributeKey());
    }
}
