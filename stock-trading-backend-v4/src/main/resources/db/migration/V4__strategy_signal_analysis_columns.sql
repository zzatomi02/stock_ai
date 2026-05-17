-- strategy_signal 분석 컬럼: StrategySignal 엔티티 + ddl-auto(로컬) / 운영 시 아래 DDL 수동 적용
--
-- ALTER TABLE strategy_signal
--     ADD COLUMN market_condition VARCHAR(50) NULL,
--     ADD COLUMN market_time_window VARCHAR(50) NULL,
--     ADD COLUMN market_weight_multiplier DECIMAL(5, 2) NULL,
--     ADD COLUMN time_weight_multiplier DECIMAL(5, 2) NULL,
--     ADD COLUMN strategy_selected_reason VARCHAR(1000) NULL,
--     ADD COLUMN strategy_excluded_reason VARCHAR(1000) NULL;
-- ALTER TABLE strategy_signal
--     MODIFY COLUMN raw_final_score DECIMAL(5, 2) NULL,
--     MODIFY COLUMN adjusted_final_score DECIMAL(5, 2) NULL,
--     MODIFY COLUMN final_weight_multiplier DECIMAL(5, 2) NULL;

SELECT 1;
