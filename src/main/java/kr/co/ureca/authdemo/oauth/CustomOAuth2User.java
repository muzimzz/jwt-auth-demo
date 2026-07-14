package kr.co.ureca.authdemo.oauth;

import kr.co.ureca.authdemo.entity.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;

import java.util.Collection;
import java.util.Map;

/**
 * 네이버(순수 OAuth2, openid 없음) 로그인 경로에서 쓰는 principal.
 * DefaultOAuth2User를 상속해서 Serializable/equals/hashCode를 그대로 물려받고,
 * 도메인 User를 같이 들고 다닐 수 있게 getUser()만 추가한다.
 */
public class CustomOAuth2User extends DefaultOAuth2User {

    private final User user;

    public CustomOAuth2User(User user,
                             Collection<? extends GrantedAuthority> authorities,
                             Map<String, Object> attributes,
                             String nameAttributeKey) {
        super(authorities, attributes, nameAttributeKey);
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
