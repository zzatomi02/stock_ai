# 트레이딩 일일 흐름 — 사용·개발 설명서

> **대상**: stock 모노레포 (`stock-trading-backend-v4` 중심)  
> **핵심 원칙**: 장중 빠른 매수 판단에는 AI API 호출 없음 → Rule + Risk + DB 캐시만 사용

---

## 1. 전체 구조

| 프로젝트 | 경로 | 역할 |
|---------|------|------|
| 백엔드 | `stock-trading-backend-v4/` | 전략·리스크·뉴스·AI·스케줄·DB |
| 프론트 | `stock-trading-frontend-v4/` | UI (AI 분석·시장·전략 등) |
| AI 서비스 | `stock-trading-ai-service-v4/` | (선택) 외부 AI 프록시 — `app.ai.base-url` |

```
[배치] 장전/장중비동기/종가/장마감 → AI API → ai_company_analysis
[장중] 뉴스·신호 → Rule → Risk → AI 캐시 조회 → strategy_signal
[주문] RiskManagementEngine (실제 매수 직전)
```

---

## 2. 하루 타임라인

| 시간 | 자동 스케줄 | 하는 일 |
|------|-------------|---------|
| 08:10~08:55 | `AiScheduledBatchService.preMarketBatch` | 관심종목 → 뉴스 수집 → AI 장전 분석 |
| 09:00~15:30 | `intradayAsyncBatch` (3분) | 신규 뉴스·미분석 시그널 AI (비동기) |
| 14:30~15:10 | `closingCandidateBatch` | 종가 후보 AI |
| 15:40+ | `postMarketBatch` | 심층 AI → 복기 → 전략 추천 → 익일 관심종목 |

뉴스 ingest 시 **즉시** `IntradaySignalPipelineService` → `strategy_signal` (API 호출 없음).

---

## 3. 패키지 맵 (백엔드)

### `com.noono0.stock.tradingflow`

| 클래스 | 역할 |
|--------|------|
| `PreMarketPipelineService` | 장전 E2E |
| `PostMarketPipelineService` | 장마감 E2E |
| `IntradaySignalPipelineService` | 뉴스 → 전략 분석 |
| `WatchlistPreparationService` | 시그널 → `user_watchlist` (system) |
| `AiTradeReviewService` | `ai_trade_review` |
| `StrategyImprovementService` | `strategy_improvement_recommendation` |
| `TradingFlowController` | 수동 실행 API |

### `com.noono0.stock.ai.platform`

배치·Job·Provider·캐시·유효기간·중복방지·fallback·비용 로그.  
상세는 [AI-PLATFORM.md](./AI-PLATFORM.md).

### `com.noono0.stock.strategy`

| 클래스 | 역할 |
|--------|------|
| `StrategyDecisionEngine` | 전략 활성·점수·최종 신호 |
| `StrategyAnalysisService` | 기사별 분석 → DB 저장 |
| `StrategySignalEnrichmentService` | Rule+Risk+AI → signal 컬럼 |
| `StrategySignalRiskGateService` | 시그널 단계 리스크 |

---

## 4. 수동 실행 API

```http
POST /api/trading-flow/pre-market/run
POST /api/trading-flow/post-market/run
GET  /api/trading-flow/ai-provider-performance?days=7
```

```http
GET  /api/ai/company-analysis/today
GET  /api/ai/company-analysis/{stockCode}
POST /api/ai/analysis/request
```

```http
POST /api/strategy/analysis/article/{articleId}
GET  /api/strategy/analysis/signals?tradeDate=YYYY-MM-DD&status=CANDIDATE
```

```http
GET/POST/DELETE /api/watchlist
Header: X-User-Id: system
```

전략 설정 **승인 후 반영**:

```http
POST /api/strategy-runtime-settings?requireApproval=true
GET  /api/strategy-config-changes/pending
PUT  /api/strategy-config-changes/{id}/approve
PUT  /api/strategy-config-changes/{id}/reject
```

---

## 5. 설정

백엔드 `stock-trading-backend-v4/src/main/resources/application-local.yml`:

```yaml
app:
  trading:
    flow:
      system-user-id: system
      pre-market-pipeline-enabled: true
      post-market-pipeline-enabled: true
      ingest-triggers-strategy-analysis: true
      pre-market-news-collect-enabled: true
  ai:
    platform:
      batch-enabled: true
      rule-score-weight: 0.80
      ai-score-weight: 0.20
      intraday-interval-ms: 180000
```

API Key는 DB에 저장하지 않음 → 환경 변수 (`OPENAI_API_KEY` 등).

---

## 6. 로컬 첫 실행 체크리스트

1. MySQL + 백엔드 `mvn spring-boot:run` (profile `local`)
2. `OPENAI_API_KEY` (실제 AI 호출 시)
3. `POST /api/watchlist` + `X-User-Id: system`
4. `POST /api/trading-flow/pre-market/run`
5. `GET /api/ai/company-analysis/today`
6. 뉴스 수집 또는 `POST /api/strategy/analysis/article/{id}`
7. `GET /api/strategy/analysis/signals`
8. `POST /api/trading-flow/post-market/run`

Swagger: http://localhost:8080/swagger-ui.html

---

## 7. 주요 DB 테이블

| 테이블 | 용도 |
|--------|------|
| `strategy_signal` | 전략 분석 + AI·리스크 스냅샷 |
| `ai_company_analysis` | 기업 분석 (`valid_from` / `valid_until`) |
| `ai_analysis_job` | 배치 작업 (`input_hash` 중복 방지) |
| `ai_usage_log` | 토큰·비용 |
| `ai_trade_review` | 장마감 복기 (영구) |
| `strategy_improvement_recommendation` | 전략 개선 제안 |
| `strategy_config_change_request` | 설정 변경 승인 |
| `user_watchlist` | 관심종목 |

---

## 8. DART 공시 수집

- **설정**: `DART_API_KEY` 또는 `application-secrets.local.yml` → `app.dart.api-key`
- **장전 파이프라인**: 관심종목별 최근 7일 `list.json` → `stock_disclosure` 저장
- **AI 컨텍스트**: `AiAnalysisContextBuilder`의 `recentDisclosures` (최근 3일, 최대 8건)
- **API**: `GET /api/disclosures?stockCode=005930&days=7`, `POST /api/disclosures/collect?stockCode=005930`

---

## 9. 미구현·스텁

- 종가베팅 Fast AI 3초 timeout
- ingest `SignalEngine` vs `StrategyAnalysisService` 이중 경로
- 프론트 시그널 상세 UI

---

## 10. 점수 공식 (BUY)

```
finalBuyScore ≈ ruleBased × 0.80 + aiOverall × 0.20 − riskPenalty
```

AI `RISK`/`AVOID` → 매수 후보 제외. Risk Engine은 **주문 직전**에도 재검증.

전략별 fallback: `StrategyAiPolicyService` (백엔드).
