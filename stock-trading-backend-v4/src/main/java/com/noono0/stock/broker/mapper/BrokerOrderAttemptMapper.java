package com.noono0.stock.broker.mapper;

import com.noono0.stock.broker.domain.BrokerOrderAttempt;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface BrokerOrderAttemptMapper {

    int insert(BrokerOrderAttempt row);

    List<BrokerOrderAttempt> findRecent(@Param("limit") int limit);

    long count();

    int countByClientOrderKey(@Param("clientOrderKey") String clientOrderKey);

    int countByStockCodeAndCreatedAtAfter(@Param("stockCode") String stockCode, @Param("after") LocalDateTime after);

    int countBySideAndCreatedAtBetween(
            @Param("side") String side,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to);

    int countByStockCodeAndSideAndCreatedAtAfter(
            @Param("stockCode") String stockCode,
            @Param("side") String side,
            @Param("after") LocalDateTime after);

    int countBySignalId(@Param("signalId") long signalId);
}
