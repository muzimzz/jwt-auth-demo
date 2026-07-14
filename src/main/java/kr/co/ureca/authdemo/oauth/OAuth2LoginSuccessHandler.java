package kr.co.ureca.authdemo.oauth;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import kr.co.ureca.authdemo.jwt.JwtTokenProvider;
import kr.co.ureca.authdemo.jwt.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

/**
 * oauth2Login 성공 시 세션 대신(또는 병행해서) JWT를 발급하는 핸들러.
 * SecurityConfig의 .oauth2Login(oauth2 -> oauth2.successHandler(this))에 등록해서 사용.
 */
@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication)
            throws IOException, ServletException {
        // TODO:
        // 1) authentication에서 CustomOAuth2User/CustomOidcUser 꺼내서 User(id, role) 확보
        //    (OAuth2User와 OidcUser 둘 다 올 수 있으므로 타입 분기 필요)
        // 2) jwtTokenProvider로 access/refresh 토큰 발급
        // 3) refreshTokenRepository에 refresh token 저장 (TTL = jwt.refresh-token-validity-ms)
        // 4) 클라이언트에 토큰 전달 방식 결정: 리다이렉트+쿼리파라미터 / httpOnly 쿠키 / JSON 응답 중 택 1
        throw new UnsupportedOperationException("TODO: 로그인 성공 시 토큰 발급 로직 구현 필요");
    }
}
