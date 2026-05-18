# 프론트 ↔ 백엔드 API 대조 (2026-05)

프론트(`stock-trading-frontend-v4`)가 호출하는 경로와 백엔드(`stock-trading-backend-v4`) REST를 비교한 결과입니다.  
브라우저에서는 `/api/proxy/{path}` → 백엔드 `http://…:8080/api/{path}` 로 전달됩니다.

---

## 요약

| 구분 | 결과 |
|------|------|
| **경로·메서드 불일치** | **없음** (호출 중인 경로는 모두 백엔드에 존재) |
| **응답 필드 혼동** | 1건 — 실행 단계 API 중복 (아래 §3) |
| **프론트 미연결 (백엔드만 있음)** | 약 70개 엔드포인트 |
| **백엔드 미구현 (프론트만 호출)** | **없음** |

---

## 1. 프론트에서 실제 호출하는 API (일치 ✅)

모두 백엔드에 동일 경로·메서드로 존재합니다.

| 영역 | 메서드 | 경로 |
|------|--------|------|
| auth | GET/POST | `/auth/check-username`, `/signup`, `/login`, `/me`, `/find-username`, `/password-reset/*` |
| dashboard | GET | `/dashboard` |
| market | GET | `/market/top100`, `/market/stocks/{code}`, `…/candles` |
| strategies | GET | `/strategies`, `/strategies/enabled-now` |
| orchestration | GET/PATCH/PUT | `/strategies/orchestration/snapshot`, `…/{code}/enabled`, `…/{code}/today` |
| strategy analysis | GET/POST | `/strategies/analysis/signals`, `…/signals/{id}`, `…/article/{id}`, `…/scan-recent` |
| strategy settings | GET/PUT/POST | `/strategy-settings`, `/strategy-runtime-settings` |
| time / market rules | GET/PUT | `/strategy-time-windows`, `/market-condition-strategies` |
| signals (레거시) | GET | `/signals/platform`, `/signals`, `/signals/rejected`, `/signals/grades/{grade}` |
| news | GET/POST/PUT/DELETE | `/news/rules`, `/news/score/preview`, `/news/keywords`, `/news/sources`, `…/run`, `…/run-all` |
| news articles | GET | `/news/articles` |
| ai | GET/POST | `/ai/prompts`, `/ai/cached/company/{code}/bundle`, `/ai/platform/jobs/company-analysis` |
| broker | GET/POST | `/broker/kis/credentials/*`, `/balance`, `/executions/daily/view`, `/orders/recent`, `/order/buy|sell` |
| ops | GET/POST | `/ops/emergency-stop` |
| watchlist | GET/POST/DELETE | `/watchlist` |
| statistics | GET | `/statistics/trades` |
| llm | GET/POST/PUT/DELETE | `/llm/question-templates` |

**참고:** `PUT /strategy-settings/{strategyType}/enabled` — 프론트 변수명은 `editType`이지만 값은 `strategyType`과 동일합니다.

---

## 2. 프론트에 없는 화면·기능 (백엔드만 있음)

Swagger·스케줄·향후 UI용. **삭제하지 않았습니다.**

### 운영·리스크·공시

| 메서드 | 경로 | 용도 |
|--------|------|------|
| GET | `/ops/execution-phase` | 실행 단계 (`/signals/platform`과 유사) |
| GET | `/ops/dashboard` | 운영 대시보드 |
| GET | `/ops/realtime-ws` | KIS 실시간 WS 상태 |
| GET | `/ops/schedules` | 스케줄 스냅샷 |
| GET/POST | `/risk/*` | 리스크 로그·PnL·API 헬스 |
| GET/POST | `/disclosures`, `/disclosures/collect` | DART 공시 |
| POST | `/trading-flow/pre-market/run`, `post-market/run` | 장전·장후 파이프라인 수동 실행 |
| GET | `/trading-flow/ai-provider-performance` | AI 제공자 성능 |

### 전략·설정 (고급)

| 메서드 | 경로 |
|--------|------|
| GET | `/strategies/decision-context` |
| GET | `/strategies/analysis/article/{articleId}/signals` |
| PUT | `/strategies/orchestration/{code}/market-rules`, `time-rules` |
| GET | `/strategies/orchestration/{code}/rules` |
| GET/POST/PUT | `/strategy-config-changes/*` |
| GET | `/strategy-performance/summary` |

### 시그널 (레거시 `trading_signal`)

| 메서드 | 경로 | 비고 |
|--------|------|------|
| POST | `/signals/generate/{articleId}` | 구 `SignalEngine` |
| POST | `/signals/scan-recent` | 구 스캔 — 신규는 `/strategies/analysis/scan-recent` |

### 뉴스·시장 (부분)

| 메서드 | 경로 |
|--------|------|
| POST | `/news/score`, `/news/ingest` |
| PUT | `/news/keywords/{id}` |
| POST | `/news/keywords/match/preview` |
| GET | `/news/keywords/match/history/{articleId}` |
| GET | `/market/mood` |
| GET/PUT/DELETE | `/market-warnings/*` |
| GET/POST | `/stock-map` |

### AI (중복·관리 API)

| 메서드 | 경로 | 비고 |
|--------|------|------|
| GET/POST/PUT | `/ai/prompt-templates` | `/ai/prompts`와 역할 겹침 |
| GET/PUT | `/ai/providers`, `/ai/models` |
| GET | `/ai/company-analysis/today`, `/{stockCode}` |
| POST | `/ai/analysis/request` |
| POST/GET | `/ai/forward` | FastAPI 프록시 |
| GET/POST | `/ai/platform/providers`, `models`, `jobs/{jobId}/run` |

### 브로커·기타

| 메서드 | 경로 |
|--------|------|
| GET | `/broker/kis/oauth/ping`, `/price/{iscd}`, `/executions/daily` |
| POST | `/broker/kis/order/cancel` |
| DELETE | `/broker/kis/credentials` |
| GET | `/orders/reasons/signal/{signalId}` |
| GET | `/statistics/health` |
| GET | `/backtest/replay` |

---

## 3. 중복·혼동 가능 API

| 항목 | 설명 | 권장 |
|------|------|------|
| 실행 단계 | `GET /signals/platform` → `{ executionPhase, observeOnly }`<br>`GET /ops/execution-phase` → `{ phase, observeOnly }` | 프론트는 **`/signals/platform`** 사용 중 (필드명 일치). ops 쪽은 운영 전용으로 유지 |
| 전략 시그널 vs 매매 시그널 | **전략** `strategy_signal` → `/strategies/analysis/signals` (메뉴 **전략 시그널**)<br>**레거시** `trading_signal` → `/signals` (메뉴 **매매 시그널**) | 일상 점검은 **전략 시그널** 우선 |
| AI 프롬프트 | `/ai/prompts` (UI 사용) vs `/ai/prompt-templates` (관리 API) | UI는 prompts만 사용 |

---

## 4. 프록시 허용 prefix

`src/app/api/proxy/[...path]/route.ts` 의 `DEFAULT_ALLOWED_PREFIXES`에 아래가 포함되어 있습니다.  
프론트가 아직 호출하지 않아도 **403 방지**를 위해 등록된 항목: `disclosures`, `trading-flow`, `risk`, `strategy-config-changes`, `backtest` 등.

---

## 5. 코드 정리 (이번에 반영)

| 항목 | 조치 |
|------|------|
| `StrategyOrchestrationPanel` 인라인 `fetch` | `apiPatch` / `apiPut` 공통 클라이언트로 통합 |
| `news/keywords/page.tsx` | 미사용 `apiPut` import 제거 |
| `lib/api.ts` | `apiPatch` 추가 |

---

## 6. 향후 UI 연결 후보 (우선순위)

1. **공시** — `/disclosures` (장전 수집 결과 조회)
2. **장전·장후 수동 실행** — `/trading-flow/pre-market/run`, `post-market/run`
3. **리스크 상태** — `/risk/status`, `/risk/events`
4. **레거시 시그널 생성** — `/signals/generate/{articleId}` (전략 분석으로 대체 권장)

---

*자동 점검 기준: 프론트 `src` 내 `apiGet|apiPost|apiPut|apiDelete|apiPatch` 및 `/api/proxy` fetch, 백엔드 `*Controller.java` 매핑.*
