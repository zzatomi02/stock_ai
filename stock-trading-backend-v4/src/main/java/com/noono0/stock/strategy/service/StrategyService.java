package com.noono0.stock.strategy.service;

import com.noono0.stock.strategy.dto.StrategyDto;
import com.noono0.stock.strategy.mapper.StrategyMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StrategyService {
    private final StrategyMapper strategyMapper;

    /**
     * readOnly 트랜잭션이면 감사 AOP가 같은 트랜잭션에서 audit_log 를 INSERT 할 수 없어 rollback-only 가 된다.
     */
    @Transactional
    public List<StrategyDto> findAll() {
        return strategyMapper.selectAll().stream().map(StrategyDto::from).toList();
    }
}
