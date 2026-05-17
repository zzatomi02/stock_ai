package com.noono0.stock.market.dto;

import java.util.List;

public record Top100SnapshotDto(
        String type,
        List<TopStockDto> rows,
        String lastUpdatedAt,
        boolean stale,
        int refreshIntervalSec
) {
}
