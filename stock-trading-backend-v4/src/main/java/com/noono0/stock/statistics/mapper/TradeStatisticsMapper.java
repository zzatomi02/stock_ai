package com.noono0.stock.statistics.mapper;

import com.noono0.stock.statistics.dto.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface TradeStatisticsMapper {
    TradeSummaryRow findTradeSummary(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, @Param("keyword") String keyword);
    List<TradeTimelineRow> findTradeTimeline(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);
    List<KeywordPerformanceRow> findKeywordPerformance(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    long countTradeExecutions();
}
