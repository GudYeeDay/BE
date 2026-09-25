package org.example.gudyeeday.domain.user.repository;

import org.example.gudyeeday.domain.user.entity.User;
import org.example.gudyeeday.domain.user.enums.Provider;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByProviderAndProviderId(Provider provider, String providerId);

    Optional<User> findByProviderId(String providerId);

    // 사용자 단위 동시 요청 직렬화용 (SELECT ... FOR UPDATE)
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from User u where u.email = :email")
    Optional<User> findByEmailForUpdate(@Param("email") String email);
}
