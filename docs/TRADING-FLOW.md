# 트레이딩 일일 흐름

> **쉬운 설명** → [USAGE-GUIDE.md](./USAGE-GUIDE.md)  
> 이 문서는 API·설정·DB를 조금 더 자세히 적은 **기술용** 보조 자료입니다.

---

## 핵심 원칙 (한 줄)

**장중 “지금 살까?”** → AI API 호출 **금지**, DB에 저장된 AI + 규칙 + 리스크만 사용.

---

## 하루 타임라인

| 시간 | 하는 일 |
|------|--------|
| 08:10~08:55 | 장전: 관심종목 → 뉴스·DART 공시 → AI 분석 저장 |
| 09:00~15:30 | 장중: 뉴스 들어오면 전략 시그널 생성 (빠른 경로, API 없음) + 3분마다 AI 배치(비동기) |
| 14:30~15:10 | 종가 후보 AI |
| 15:10~15:20 | 종가베팅: 캐시 없으면 Fast AI (최대 3초) |
| 15:40~ | 장마감: 복기·전략 제안·익일 관심종목 |

---

## 뉴스 → 시그널 흐름

```
뉴스 수집 (네이버)
  → (선택) OpenAI 기사 분석
  → NewsArticleIngestPipeline (단일 진입)
  → 전략별 StrategyAnalysisService
  → strategy_signal 저장
  → 프론트 /strategy-signals
```

레거시 `trading_signal` 은 `legacy-trading-signal-on-ingest: true` 일 때만.

---

## 수동 실행 API

```http
POST /api/trading-flow/pre-market/run
POST /api/trading-flow/post-market/run
GET  /api/trading-flow/ai-provider-performance?days=7
```

```http
GET  /api/strategy/analysis/signals?status=CANDIDATE
GET  /api/strategy/analysis/signals/{id}
POST /api/strategy/analysis/article/{articleId}
```

```http
GET  /api/disclosures?stockCode=005930&days=7
POST /api/disclosures/collect?stockCode=005930
```

```http
GET  /api/strategies/enabled-now
GET  /api/ai/company-analysis/today
```

---

## 주요 설정 (`application-local.yml`)

```yaml
app:
  trading:
    flow:
      pre-market-pipeline-enabled: true
      post-market-pipeline-enabled: true
      ingest-triggers-strategy-analysis: true   # 뉴스 → strategy_signal
      legacy-trading-signal-on-ingest: false    # 예전 trading_signal
      pre-market-disclosure-collect-enabled: true
  ai:
    platform:
      batch-enabled: true
      rule-score-weight: 0.80
      ai-score-weight: 0.20
      closing-bet-fast-ai-enabled: true
      closing-bet-fast-ai-timeout-ms: 3000
  dart:
    api-key: ${DART_API_KEY:}
```

---

## 점수 공식

```
finalBuyScore ≈ ruleBased × 0.80 + aiOverall × 0.20 − riskPenalty
```

AI `RISK` / `AVOID` → 매수 후보 제외.

---

## 주요 DB 테이블

| 테이블 | 용도 |
|--------|------|
| `strategy_signal` | 전략별 시그널 (화면 **전략 시그널**) |
| `trading_signal` | 레거시 매매 시그널 (화면 **매매 시그널**) |
| `ai_company_analysis` | AI 기업 분석 캐시 |
| `stock_disclosure` | DART 공시 |
| `user_watchlist` | 관심종목 |
| `ai_trade_review` | 장마감 복기 |

---

## 백엔드 패키지 (개발자용)

| 패키지 | 역할 |
|--------|------|
| `tradingflow` | 장전/장마감 파이프라인 |
| `ai.platform` | AI 배치·캐시 ([AI-PLATFORM.md](./AI-PLATFORM.md)) |
| `strategy` | 전략 판단·시그널 |
| `disclosure` | DART 공시 |
| `news` | 뉴스 수집 |

AI 상세: [AI-PLATFORM.md](./AI-PLATFORM.md)
