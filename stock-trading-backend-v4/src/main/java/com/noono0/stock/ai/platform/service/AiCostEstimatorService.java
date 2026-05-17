package com.noono0.stock.ai.platform.service;

import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
public class AiCostEstimatorService {

    public BigDecimal estimate(String modelName, int inputTokens, int outputTokens) {
        double inRate = 0.00015;
        double outRate = 0.0006;
        if (modelName != null && modelName.contains("gpt-4o-mini")) {
            inRate = 0.00015;
            outRate = 0.0006;
        }
        double cost = (inputTokens / 1000.0) * inRate + (outputTokens / 1000.0) * outRate;
        return BigDecimal.valueOf(cost).setScale(4, RoundingMode.HALF_UP);
    }
}
