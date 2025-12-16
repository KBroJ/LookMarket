package com.lookmarket.infrastructure.user;

import com.lookmarket.domain.user.User;
import com.lookmarket.domain.user.UserRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * UserRepository 구현체 (어댑터)
 *
 * Domain의 UserRepository 인터페이스를 구현하며,
 * JpaUserRepository를 사용하여 실제 데이터베이스 작업을 수행합니다.
 *
 * Hexagonal Architecture의 Adapter 역할을 합니다.
 */
@Component
public class UserAdapter implements UserRepository {

    private final JpaUserRepository jpaUserRepository;

    public UserAdapter(JpaUserRepository jpaUserRepository) {
        this.jpaUserRepository = jpaUserRepository;
    }

    @Override
    public User save(User user) {
        UserEntity entity = UserEntity.fromDomain(user);
        UserEntity saved = jpaUserRepository.save(entity);
        return saved.toDomain();
    }

    @Override
    public Optional<User> findById(Long id) {
        return jpaUserRepository.findById(id)
                .map(UserEntity::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return jpaUserRepository.findByEmail(email)
                .map(UserEntity::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        return jpaUserRepository.existsByEmail(email);
    }

    @Override
    public void delete(User user) {
        UserEntity entity = UserEntity.fromDomain(user);
        jpaUserRepository.delete(entity);
    }

    @Override
    public void deleteById(Long id) {
        jpaUserRepository.deleteById(id);
    }
}
