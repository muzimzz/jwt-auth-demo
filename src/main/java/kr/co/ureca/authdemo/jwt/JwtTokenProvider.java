package kr.co.ureca.authdemo.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import kr.co.ureca.authdemo.entity.UserRole;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

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
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role.name())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + accessTokenValidityMs))
                .signWith(secretKey)
                .compact();
    }

    public String createRefreshToken(Long userId) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + refreshTokenValidityMs))
                .signWith(secretKey)
                .compact();
    }

    // 서명/만료 검증 후 클레임 반환. 실패 시 예외 던짐 (호출부에서 catch)
    public Claims parseClaims(String token) {
        return Jwts.parser().verifyWith(secretKey).build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // 필터에서 쓸 boolean 버전 (parseClaims 감싸서 예외를 false로 변환)
    public boolean validateToken(String token) {
        return Jwts.parser().verifyWith(secretKey).build()
                .parseSignedClaims(token)
                .getPayload()
                .getExpiration().before(new Date());
    }

    public Long getUserId(String token) {
        return Jwts.parser().verifyWith(secretKey).build()
                .parseSignedClaims(token)
                .getPayload()
                .get("userId", Long.class);
    }

    // access token에서만 유효. refresh token엔 role 클레임 없음
    public UserRole getRole(String token) {
        return Jwts.parser().verifyWith(secretKey).build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", UserRole.class);
    }
}
