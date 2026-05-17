package com.noono0.stock.disclosure.repository;

import com.noono0.stock.disclosure.domain.StockDisclosure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;

public interface StockDisclosureRepository extends JpaRepository<StockDisclosure, Long> {
    boolean existsByRceptNo(String rceptNo);

    List<StockDisclosure> findByStockCodeAndRceptDtGreaterThanEqualOrderByRceptDtDesc(
            String stockCode, LocalDate since);
}
