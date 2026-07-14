package kr.co.ureca.authdemo.controller;

import kr.co.ureca.authdemo.jwt.JwtTokenProvider;
import kr.co.ureca.authdemo.jwt.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 토큰 재발급 / 로그아웃 API.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;

    public record ReissueRequest(String refreshToken) {}
    public record TokenResponse(String accessToken, String refreshToken) {}

    @PostMapping("/reissue")
    public ResponseEntity<TokenResponse> reissue(@RequestBody ReissueRequest request) {
        // TODO:
        // 1) jwtTokenProvider.validateToken(request.refreshToken()) 검증
        // 2) userId 추출 후 refreshTokenRepository.findByUserId(userId)로 저장된 값과 일치하는지 확인
        // 3) 일치하면 새 access token(+선택적으로 refresh token 회전) 발급해서 반환
        // 4) 불일치/만료면 401
        throw new UnsupportedOperationException("TODO: 재발급 로직 구현 필요");
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        // TODO:
        // 1) 현재 인증된 사용자(userId) 확인 (SecurityContext에서)
        // 2) refreshTokenRepository.deleteByUserId(userId)
        throw new UnsupportedOperationException("TODO: 로그아웃 로직 구현 필요");
    }
}
