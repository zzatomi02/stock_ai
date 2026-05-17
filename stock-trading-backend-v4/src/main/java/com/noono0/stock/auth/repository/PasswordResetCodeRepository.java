package com.noono0.stock.auth.repository;

import com.noono0.stock.auth.domain.PasswordResetCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.Optional;

public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {

    Optional<PasswordResetCode> findFirstByUserIdAndUsedAtIsNullOrderByCreatedAtDesc(Long userId);

    Optional<PasswordResetCode> findFirstByUserIdOrderByCreatedAtDesc(Long userId);

    @Modifying
    @Query(
            "UPDATE PasswordResetCode c SET c.usedAt = :now WHERE c.userId = :userId AND c.usedAt IS NULL")
    int invalidatePending(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
