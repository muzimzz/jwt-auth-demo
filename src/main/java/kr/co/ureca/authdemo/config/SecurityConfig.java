package kr.co.ureca.authdemo.config;

import kr.co.ureca.authdemo.jwt.JwtAuthenticationFilter;
import kr.co.ureca.authdemo.jwt.JwtTokenProvider;
import kr.co.ureca.authdemo.oauth.OAuth2LoginSuccessHandler;
import kr.co.ureca.authdemo.service.CustomOAuth2UserService;
import kr.co.ureca.authdemo.service.CustomOidcUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
// @EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomOAuth2UserService customOAuth2UserService;
    private final CustomOidcUserService customOidcUserService;
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler; // 추가
    private final JwtTokenProvider jwtTokenProvider;                   // 추가 — 필터 생성용

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) {

        http
                .csrf(csrf -> csrf.disable())

                .formLogin(login -> login.disable())

                .httpBasic(basic -> basic.disable())

                // ① 세션 정책 — 지금 단계는 IF_REQUIRED(기본값) 유지 권장
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))

                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                // 네이버(openid 없음)는 여기로 옴
                                .userService(customOAuth2UserService)
                                // 카카오/구글(openid 있음)은 여기로 옴
                                .oidcUserService(customOidcUserService)
                        )
                        .successHandler(oAuth2LoginSuccessHandler) // ② handler 등록
                )

                // ③ 매 요청 토큰 검증 필터 추가
                .addFilterBefore(new JwtAuthenticationFilter(jwtTokenProvider), UsernamePasswordAuthenticationFilter.class)

                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/login/**", "/oauth2/**", "/api/auth/reissue").permitAll()
                        .anyRequest().authenticated())

                ;

        return http.build();

    }
}
