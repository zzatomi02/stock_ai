package com.noono0.stock.dashboard.controller;

import com.noono0.stock.common.api.ApiResponse;
import com.noono0.stock.dashboard.service.DashboardService;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DashboardControllerTest {

    @Test
    void dashboardReturnsStableSummaryShape() {
        DashboardService dashboardService = mock(DashboardService.class);
        when(dashboardService.buildSummary(any())).thenReturn(Map.of(
                "evaluationAmount", 0,
                "todayProfitAmount", 0,
                "todayProfitRate", 0.0,
                "autoTrading", false,
                "newsCollectionStatus", "UNAVAILABLE",
                "topKeywords", new String[0],
                "dataStatus", "UNAVAILABLE",
                "dataReason", "테스트"
        ));
        ApiResponse<?> response = new DashboardController(dashboardService).dashboard(null);

        assertThat(response.success()).isTrue();
        assertThat(response.data()).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> data = new LinkedHashMap<>((Map<String, Object>) response.data());
        assertThat(data).containsKeys(
                "evaluationAmount",
                "todayProfitAmount",
                "todayProfitRate",
                "autoTrading",
                "newsCollectionStatus",
                "topKeywords",
                "dataStatus",
                "dataReason");
    }
}
