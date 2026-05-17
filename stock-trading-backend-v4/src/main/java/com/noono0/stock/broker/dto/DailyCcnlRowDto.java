package com.noono0.stock.broker.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

/** 국내주식 일별체결(조회) output1 한 줄을 UI용으로 정리 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record DailyCcnlRowDto(
        String orderDate,
        String orderTime,
        String stockCode,
        String stockName,
        String sideLabel,
        String sellBuyCode,
        long quantity,
        long price,
        String rawJsonSubset
) {}
