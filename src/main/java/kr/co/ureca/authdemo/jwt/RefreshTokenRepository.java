package kr.co.ureca.authdemo.jwt;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Repository;

import java.time.Duration;
import java.util.Optional;

/**
 * Refresh Token을 Redis에 저장. key = "refreshToken:{userId}", TTL = 만료시간.
 * JPA가 아니라 Redis를 쓰는 이유: 만료 처리를 TTL로 자동화하고, 재발급/로그아웃 때만 조회하면 되기 때문.
 */
@Repository
@RequiredArgsConstructor
public class RefreshTokenRepository {

    private static final String KEY_PREFIX = "refreshToken:";

    private final StringRedisTemplate redisTemplate;

    public void save(Long userId, String refreshToken, Duration ttl) {
        throw new UnsupportedOperationException("TODO: Redis에 저장 구현 필요");
    }

    public Optional<String> findByUserId(Long userId) {
        throw new UnsupportedOperationException("TODO: Redis에서 조회 구현 필요");
    }

    public void deleteByUserId(Long userId) {
        throw new UnsupportedOperationException("TODO: Redis에서 삭제 구현 필요 (로그아웃 시 사용)");
    }
}
