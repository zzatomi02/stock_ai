package com.noono0.stock.order.repository;

import com.noono0.stock.order.domain.OrderReason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderReasonRepository extends JpaRepository<OrderReason, Long> {
    Optional<OrderReason> findBySignalId(Long signalId);
}
