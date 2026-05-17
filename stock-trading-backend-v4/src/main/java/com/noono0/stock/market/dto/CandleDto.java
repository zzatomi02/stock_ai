package com.noono0.stock.market.dto;

public record CandleDto(String time, long open, long high, long low, long close, long volume) {}
