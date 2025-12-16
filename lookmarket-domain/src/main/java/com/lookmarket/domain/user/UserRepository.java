package com.lookmarket.domain.user;

import java.util.Optional;

/**
 * User 도메인의 Repository 인터페이스 (포트)
 *
 * Hexagonal Architecture의 Domain Layer에 정의된 포트(Port)로,
 * Infrastructure Layer에서 구현됩니다 (어댑터 패턴).
 *
 * 중요: Spring Data JPA의 JpaRepository를 확장하지 않습니다.
 * 순수한 도메인 인터페이스를 유지하여 프레임워크 의존성을 제거합니다.
 */
public interface UserRepository {

    /**
     * 사용자 저장 (생성 또는 수정)
     *
     * @param user 저장할 사용자 엔티티
     * @return 저장된 사용자 엔티티 (ID 포함)
     */
    User save(User user);

    /**
     * ID로 사용자 조회
     *
     * @param id 사용자 ID
     * @return 조회된 사용자 (Optional)
     */
    Optional<User> findById(Long id);

    /**
     * 이메일로 사용자 조회
     *
     * @param email 사용자 이메일
     * @return 조회된 사용자 (Optional)
     */
    Optional<User> findByEmail(String email);

    /**
     * 이메일 중복 확인
     *
     * @param email 확인할 이메일
     * @return 존재 여부
     */
    boolean existsByEmail(String email);

    /**
     * 사용자 삭제
     *
     * @param user 삭제할 사용자 엔티티
     */
    void delete(User user);

    /**
     * ID로 사용자 삭제
     *
     * @param id 삭제할 사용자 ID
     */
    void deleteById(Long id);
}
