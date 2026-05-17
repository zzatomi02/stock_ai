# AI 플랫폼 — 사용·개발 설명서

> 구현 위치: `stock-trading-backend-v4/src/main/java/com/noono0/stock/ai/platform/`

## 1. 실행 시점 정책 (`AiExecutionTiming`)

| Timing | API 호출 | 유효기간(예) |
|--------|----------|--------------|
| PRE_MARKET | 08:10~08:55 허용 | 당일 15:30 |
| INTRADAY_ASYNC | 09:00~15:30 배치만 | +6시간 |
| CLOSING_CANDIDATE | 14:30~15:10 | 당일 15:30 |
| POST_MARKET | 15:40+ | ~7일 |
| MANUAL | 장외만 | 유형별 |
| INTRADAY_BLOCKED | **항상 금지** | — |

**빠른 판단**: `assertFastPathNoLiveApi()` — 장중 동기 경로에서 API 호출 시 예외.

## 2. 배치 4단계

`AiScheduledBatchService` → `AiBatchOrchestratorService.runPhase(phase)`

1. `AiAnalysisTargetCollector` — 종목 후보
2. `AiAnalysisJobService.enqueueCompanyAnalysis` — Job 생성 (dedup)
3. sync 또는 `@Async` 로 `AiAnalysisExecutorService.processJob`
4. 성공 시 `AiConsensusService.buildCompanyConsensus`

장전/장마감은 `PreMarketPipelineService` / `PostMarketPipelineService` 가 E2E 포함.

## 3. 장중 캐시 조회

`CachedAiAnalysisQueryService.getBestForTrading(stockCode)`

- `valid_from <= now <= valid_until` 인 `ai_company_analysis` 만
- `AiScoreBlendService` / `StrategySignalEnrichmentService` 에서 사용

## 4. REST API

| 메서드 | 경로 |
|--------|------|
| GET/PUT | `/api/ai/providers` |
| GET/PUT | `/api/ai/models/{id}` |
| GET/POST/PUT | `/api/ai/prompt-templates` |
| POST | `/api/ai/analysis/request` |
| GET | `/api/ai/company-analysis/today` |
| GET | `/api/ai/company-analysis/{stockCode}` |
| GET | `/api/ai/cached/{stockCode}` |

레거시: `/api/ai/platform/*`

## 5. 운영 기능 체크리스트

| # | 기능 | 구현 |
|---|------|------|
| 1 | 유효기간 | `AiValidityService` |
| 2 | input_hash 중복 | `AiJobDedupService` |
| 3 | 토큰·비용 | `AiUsageLog` + `AiCostEstimatorService` |
| 4 | 실패 fallback | `AiAnalysisFallbackService` |
| 5 | Provider 성과 | `GET /api/trading-flow/ai-provider-performance` |
| 6 | 템플릿 버전 | PUT 시 prompt 변경 → version bump |
| 7 | 설정 승인 | `strategy_config_change_request` |
| 8 | API Key 보안 | 응답에 env 이름만 |
| 9 | 장중 API 제한 | `AiExecutionPolicyService` |
| 10 | signal에 AI 저장 | `strategy_signal` AI 컬럼 |

## 6. 수동 분석 요청 예시

```json
POST /api/ai/analysis/request
{
  "analysisType": "COMPANY_ANALYSIS",
  "stockCode": "005930",
  "providerTypes": ["OPENAI"],
  "executionTiming": "MANUAL"
}
```

## 7. Provider 설정

- DB: `ai_provider_config`, `ai_model_config`
- 시드: `AiPlatformSeedRunner` (기동 시)
- OpenAI: `OpenAiProviderClient` — `OPENAI_API_KEY`
- 기타: Stub 클라이언트 (확장 가능)
