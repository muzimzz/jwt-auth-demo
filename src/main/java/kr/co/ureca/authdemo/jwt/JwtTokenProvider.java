package kr.co.ureca.authdemo.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.security.Keys;
import kr.co.ureca.authdemo.entity.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;

/**
 * Access/Refresh 토큰 생성·파싱·검증 담당.
 * 클레임은 최소한만: sub(userId), role(access token에만).
 */
@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final long accessTokenValidityMs;
    private final long refreshTokenValidityMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-validity-ms}") long accessTokenValidityMs,
            @Value("${jwt.refresh-token-validity-ms}") long refreshTokenValidityMs
    ) {
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
        this.accessTokenValidityMs = accessTokenValidityMs;
        this.refreshTokenValidityMs = refreshTokenValidityMs;
    }

    // userId + role을 클레임에 담아 access token 발급
    public String createAccessToken(Long userId, UserRole role) {
        throw new UnsupportedOperationException("TODO: access token 생성 구현 필요");
    }

    // userId만 담아 refresh token 발급 (role은 재발급 시 DB에서 다시 조회)
    public String createRefreshToken(Long userId) {
        throw new UnsupportedOperationException("TODO: refresh token 생성 구현 필요");
    }

    // 서명/만료 검증 후 클레임 반환. 실패 시 예외 던짐 (호출부에서 catch)
    public Claims parseClaims(String token) {
        throw new UnsupportedOperationException("TODO: 토큰 파싱/검증 구현 필요");
    }

    // 필터에서 쓸 boolean 버전 (parseClaims 감싸서 예외를 false로 변환)
    public boolean validateToken(String token) {
        throw new UnsupportedOperationException("TODO: 토큰 유효성 검사 구현 필요");
    }

    public Long getUserId(String token) {
        throw new UnsupportedOperationException("TODO: 클레임에서 userId 추출 구현 필요");
    }

    // access token에서만 유효. refresh token엔 role 클레임 없음
    public UserRole getRole(String token) {
        throw new UnsupportedOperationException("TODO: 클레임에서 role 추출 구현 필요");
    }
}
