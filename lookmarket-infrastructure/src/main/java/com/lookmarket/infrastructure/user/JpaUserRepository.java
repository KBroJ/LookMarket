package com.lookmarket.infrastructure.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

/**
 * User JPA Repository (Spring Data JPA)
 *
 * Spring Data JPA의 JpaRepository를 확장하여
 * 기본 CRUD 및 커스텀 쿼리 메서드를 제공합니다.
 */
public interface JpaUserRepository extends JpaRepository<UserEntity, Long> {

    /**
     * 이메일로 사용자 조회
     *
     * @param email 사용자 이메일
     * @return 조회된 사용자 엔티티 (Optional)
     */
    Optional<UserEntity> findByEmail(String email);

    /**
     * 이메일 존재 여부 확인
     *
     * @param email 확인할 이메일
     * @return 존재 여부
     */
    boolean existsByEmail(String email);
}
