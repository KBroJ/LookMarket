package com.lookmarket.application.auth;

import com.lookmarket.domain.user.User;
import com.lookmarket.domain.user.UserRepository;
import com.lookmarket.domain.user.UserStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증 관련 Application Service
 *
 * 책임:
 * - 로그인 시 사용자 검증 (이메일/비밀번호 확인)
 * - 토큰 갱신 시 사용자 유효성 검증
 *
 * 토큰 생성/검증은 API 레이어의 JwtTokenProvider가 담당합니다.
 * 이 서비스는 순수 비즈니스 로직(사용자 인증)만 처리합니다.
 */
@Service
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * 로그인 인증
     *
     * @param email 이메일
     * @param rawPassword 평문 비밀번호
     * @return 인증된 사용자
     * @throws AuthenticationException 인증 실패 시
     */
    public User authenticate(String email, String rawPassword) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AuthenticationException("이메일 또는 비밀번호가 올바르지 않습니다."));

        // 계정 상태 확인
        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new AuthenticationException("비활성화된 계정입니다.");
        }

        if (user.getStatus() == UserStatus.SUSPENDED) {
            throw new AuthenticationException("정지된 계정입니다.");
        }

        // 비밀번호 검증
        if (!passwordEncoder.matches(rawPassword, user.getPassword())) {
            throw new AuthenticationException("이메일 또는 비밀번호가 올바르지 않습니다.");
        }

        return user;
    }

    /**
     * 토큰 갱신을 위한 사용자 검증
     *
     * @param userId 사용자 ID
     * @return 검증된 사용자
     * @throws AuthenticationException 사용자를 찾을 수 없거나 비활성 상태인 경우
     */
    public User validateUserForRefresh(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AuthenticationException("사용자를 찾을 수 없습니다."));

        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new AuthenticationException("비활성 상태의 계정입니다.");
        }

        return user;
    }
}
