package com.noono0.stock.strategy.dto;

/**
 * 구성 점수 (더미·실데이터 혼용 가능).
 *
 * <p>최종 raw = 뉴스·키워드·수급·기술 가중 합산 − 리스크 페널티 (0~100)
 */
public record StrategyComponentScores(
        int newsScore,
        int supplyScore,
        int technicalScore,
        int riskPenalty,
        int compositeRawScore,
        String gradeHint) {

    public static StrategyComponentScores of(
            int news, int supply, int technical, int riskPenalty, int composite, String grade) {
        return new StrategyComponentScores(news, supply, technical, riskPenalty, composite, grade);
    }
}
