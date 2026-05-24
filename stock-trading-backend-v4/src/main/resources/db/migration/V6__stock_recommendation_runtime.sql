CREATE TABLE IF NOT EXISTS platform_runtime_config (
    config_key   VARCHAR(64)  NOT NULL PRIMARY KEY,
    config_json  JSON         NOT NULL,
    updated_at   DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS stock_recommendation (
    id                          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    trade_date                  DATE         NOT NULL,
    stock_code                  VARCHAR(20)  NOT NULL,
    stock_name                  VARCHAR(120) NULL,
    status                      VARCHAR(32)  NOT NULL,
    best_adjusted_score         DECIMAL(5,2) NULL,
    signal_grade                VARCHAR(2)   NULL,
    strategy_codes              VARCHAR(500) NULL,
    primary_strategy_signal_id  BIGINT       NULL,
    reason_summary              VARCHAR(2000) NULL,
    notification_sent           TINYINT(1)   NOT NULL DEFAULT 0,
    notified_at                 DATETIME(6)  NULL,
    approved_at                 DATETIME(6)  NULL,
    approved_by                 VARCHAR(64)  NULL,
    rejected_at                 DATETIME(6)  NULL,
    rejected_by                 VARCHAR(64)  NULL,
    reject_reason               VARCHAR(500) NULL,
    broker_order_attempt_id     BIGINT       NULL,
    created_at                  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at                  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_stock_rec_date_code (trade_date, stock_code),
    KEY idx_stock_rec_status (trade_date, status),
    KEY idx_stock_rec_score (trade_date, best_adjusted_score DESC)
);
