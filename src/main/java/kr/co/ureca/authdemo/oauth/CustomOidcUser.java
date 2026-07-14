package kr.co.ureca.authdemo.oauth;

import kr.co.ureca.authdemo.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;

import java.util.Collection;

/**
 * 카카오/구글(OIDC, openid 있음) 로그인 경로에서 쓰는 principal.
 * DefaultOidcUser를 상속해서 Serializable/equals/hashCode + id_token/userInfo 접근을 그대로 물려받고,
 * 도메인 User를 같이 들고 다닐 수 있게 getUser()만 추가한다.
 */
public class CustomOidcUser extends DefaultOidcUser {

    private final User user;

    public CustomOidcUser(User user,
                           Collection<? extends GrantedAuthority> authorities,
                           OidcIdToken idToken,
                           OidcUserInfo userInfo,
                           String nameAttributeKey) {
        super(authorities, idToken, userInfo, nameAttributeKey);
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    @Override   // super.getName(): DB(user)가 아닌 외부api 호출의 응답에서 바로 가져옴
    public String getName() {
        return String.valueOf(user.getId());
    }
}
