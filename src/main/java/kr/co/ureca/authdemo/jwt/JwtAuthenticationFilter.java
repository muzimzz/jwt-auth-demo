package kr.co.ureca.authdemo.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * 매 요청마다 Authorization 헤더의 토큰을 검증해서 SecurityContext에 인증 정보를 세팅.
 * SecurityConfig에서 UsernamePasswordAuthenticationFilter 앞에 등록해서 사용.
 */
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        // TODO:
        // 1) resolveToken(request)로 "Authorization: Bearer ..." 에서 토큰 추출
        // 2) 토큰 없으면 그냥 filterChain.doFilter(request, response) 하고 리턴 (인증 없이 통과 -> 이후 authorizeHttpRequests가 막음)
        // 3) jwtTokenProvider.validateToken(token) 확인
        // 4) 유효하면 userId/role로 Authentication 만들어서 SecurityContextHolder.getContext().setAuthentication(...)
        // 5) filterChain.doFilter(request, response) 호출 잊지 말 것
        throw new UnsupportedOperationException("TODO: 토큰 검증 후 SecurityContext 세팅 구현 필요");
    }

    // "Bearer {token}" 형태에서 순수 토큰 문자열만 추출
    private String resolveToken(HttpServletRequest request) {
        throw new UnsupportedOperationException("TODO: Authorization 헤더 파싱 구현 필요");
    }
}
