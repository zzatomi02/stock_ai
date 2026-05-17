package com.noono0.stock.risk.repository;

import com.noono0.stock.risk.domain.OrderValidationLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderValidationLogRepository extends JpaRepository<OrderValidationLog, Long> {}
