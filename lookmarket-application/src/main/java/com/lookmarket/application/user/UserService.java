package com.lookmarket.application.user;

import com.lookmarket.domain.user.User;
import com.lookmarket.domain.user.UserRepository;
import com.lookmarket.domain.user.UserRole;
import com.lookmarket.domain.user.UserStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * User 애플리케이션 서비스
 *
 * 책임:
 * - 유즈케이스 오케스트레이션
 * - 트랜잭션 경계 관리
 * - 도메인 이벤트 발행 (Spring Event 사용)
 */
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * 회원가입
     *
     * @param email 이메일
     * @param password 비밀번호 (평문)
     * @param name 이름
     * @param phoneNumber 전화번호 (선택)
     * @param role 역할
     * @return 생성된 사용자
     * @throws IllegalArgumentException 이메일이 이미 존재하는 경우
     */
    @Transactional
    public User register(String email, String password, String name, String phoneNumber, UserRole role) {
        // 1. 이메일 중복 체크
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists: " + email);
        }

        // 2. 비밀번호 암호화
        String encodedPassword = passwordEncoder.encode(password);

        // 3. 도메인 객체 생성 (비즈니스 로직은 도메인에서)
        User user = User.create(email, encodedPassword, name, phoneNumber, role);

        // 4. 저장
        User savedUser = userRepository.save(user);

        // TODO: Phase 4에서 Kafka로 전환
        // 현재는 Spring Event 사용하지 않음 (이벤트 처리 구현 시 추가)
        // applicationEventPublisher.publishEvent(new UserCreatedEvent(savedUser.getId()));

        return savedUser;
    }

    /**
     * 사용자 조회 (ID)
     *
     * @param userId 사용자 ID
     * @return 사용자 (Optional)
     */
    public Optional<User> getUserById(Long userId) {
        return userRepository.findById(userId);
    }

    /**
     * 사용자 조회 (이메일)
     *
     * @param email 이메일
     * @return 사용자 (Optional)
     */
    public Optional<User> getUserByEmail(String email) {
        return userRepository.findByEmail(email);
    }

    /**
     * 이메일 변경
     *
     * @param userId 사용자 ID
     * @param newEmail 새 이메일
     * @return 업데이트된 사용자
     * @throws IllegalArgumentException 사용자가 없거나 이메일이 이미 존재하는 경우
     */
    @Transactional
    public User changeEmail(Long userId, String newEmail) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // 2. 새 이메일 중복 체크
        if (userRepository.existsByEmail(newEmail)) {
            throw new IllegalArgumentException("Email already exists: " + newEmail);
        }

        // 3. 도메인 로직 실행 (도메인에서 검증)
        user.changeEmail(newEmail);

        // 4. 저장
        return userRepository.save(user);
    }

    /**
     * 비밀번호 변경
     *
     * @param userId 사용자 ID
     * @param currentPassword 현재 비밀번호 (평문)
     * @param newPassword 새 비밀번호 (평문)
     * @return 업데이트된 사용자
     * @throws IllegalArgumentException 사용자가 없거나 현재 비밀번호가 일치하지 않는 경우
     */
    @Transactional
    public User changePassword(Long userId, String currentPassword, String newPassword) {
        // 1. 사용자 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        // 2. 현재 비밀번호 검증
        if (!passwordEncoder.matches(currentPassword, user.getPassword())) {
            throw new IllegalArgumentException("Current password does not match");
        }

        // 3. 새 비밀번호 암호화
        String encodedNewPassword = passwordEncoder.encode(newPassword);

        // 4. 도메인 로직 실행
        user.changePassword(encodedNewPassword);

        // 5. 저장
        return userRepository.save(user);
    }

    /**
     * 계정 활성화
     *
     * @param userId 사용자 ID
     * @return 업데이트된 사용자
     * @throws IllegalArgumentException 사용자가 없는 경우
     */
    @Transactional
    public User activateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.activate();
        return userRepository.save(user);
    }

    /**
     * 계정 정지
     *
     * @param userId 사용자 ID
     * @return 업데이트된 사용자
     * @throws IllegalArgumentException 사용자가 없는 경우
     */
    @Transactional
    public User suspendUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.suspend();
        return userRepository.save(user);
    }

    /**
     * 계정 비활성화
     *
     * @param userId 사용자 ID
     * @return 업데이트된 사용자
     * @throws IllegalArgumentException 사용자가 없는 경우
     */
    @Transactional
    public User deactivateUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found: " + userId));

        user.deactivate();
        return userRepository.save(user);
    }
}
