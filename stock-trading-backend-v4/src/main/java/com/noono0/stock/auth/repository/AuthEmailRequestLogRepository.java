package com.noono0.stock.auth.repository;

import com.noono0.stock.auth.domain.AuthEmailRequestLog;
import com.noono0.stock.auth.domain.AuthEmailRequestType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AuthEmailRequestLogRepository extends JpaRepository<AuthEmailRequestLog, Long> {

    Optional<AuthEmailRequestLog> findFirstByEmailAndRequestTypeOrderByCreatedAtDesc(
            String email, AuthEmailRequestType requestType);
}
