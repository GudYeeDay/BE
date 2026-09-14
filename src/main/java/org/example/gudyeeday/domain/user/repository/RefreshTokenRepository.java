package org.example.gudyeeday.domain.user.repository;

import org.example.gudyeeday.domain.user.entity.RefreshToken;
import org.example.gudyeeday.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    Optional<RefreshToken> findByUser(User user);

    void deleteByUser(User user);
}
