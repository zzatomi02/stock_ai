# 사용 설명서 (쉬운 버전)

> **이 문서만 읽어도** 프로그램이 무엇을 하는지, 화면에서 무엇을 누르면 되는지 알 수 있습니다.  
> 개발·클래스 이름이 필요하면 맨 아래 [개발자용 문서](#개발자용-문서) 로 가세요.

---

## 1. 이게 뭔가요?

**주식 매매를 돕는 웹 프로그램**입니다.

| 하는 일 | 설명 |
|--------|------|
| 뉴스·공시 수집 | 네이버 뉴스, DART 전자공시 |
| 전략별 점수 | 시가베팅·수급단타·종가베팅 등 전략마다 “살까 말까” 점수 |
| AI 기업 분석 | OpenAI로 종목 분석 (미리 돌려 두고 DB에 저장) |
| (선택) 자동매매 | KIS API 연동 — 키 넣었을 때만 |

**중요:** 장중에 “지금 당장 살까?” 판단할 때는 **AI API를 바로 부르지 않습니다.**  
아침에·배치로 미리 저장해 둔 AI 결과만 읽고, 규칙(Rule)과 리스크(Risk)와 합칩니다.

---

## 2. 프로그램 구성 (4개)

```
브라우저 (localhost:3000)  ←  화면 (Next.js)
        ↓
백엔드 API (localhost:8080)  ←  로직·DB·스케줄 (Spring Boot)
        ↓
MySQL (localhost:3306)       ←  데이터 저장
        ↓
(선택) AI 서비스 (8001)      ←  FastAPI — 대부분 백엔드가 OpenAI 직접 호출
```

| 폴더 | 역할 |
|------|------|
| `stock-trading-frontend-v4` | 웹 화면 |
| `stock-trading-backend-v4` | API·자동 스케줄·DB |
| `stock-trading-ai-service-v4` | 선택 AI 프록시 |
| `docs/` | **지금 보고 있는 설명서** |

---

## 3. 처음 실행 (Docker, 권장)

PowerShell에서 **stock 폴더**로 이동:

```powershell
cd "C:\dev\2026 new prjt\stock"
copy .env.example .env
# .env 파일을 열어 NAVER, OPENAI, DART 등 키 입력 (없어도 화면은 뜸)
docker compose -f docker-compose.dev.yml up
```

| 주소 | 용도 |
|------|------|
| http://localhost:3000 | **웹 화면** |
| http://localhost:8080/swagger-ui.html | API 테스트 |

백엔드는 처음 **2~5분** 걸릴 수 있습니다. 터미널에 `Started StockTradingApplication` 이 보이면 준비 완료입니다.

중지: `Ctrl+C` 후 `docker compose -f docker-compose.dev.yml down`

---

## 4. API 키는 어디에?

| 키 | 용도 | 넣는 곳 |
|----|------|--------|
| `OPENAI_API_KEY` | AI 기업 분석 | `stock/.env` 또는 `stock-trading-backend-v4/.../application-secrets.local.yml` |
| `NAVER_CLIENT_ID` / `SECRET` | 뉴스 검색 | `stock/.env` |
| `DART_API_KEY` | 공시 수집 | 위와 동일 |
| `KIS_PAPER_*` | 모의투자 주문 | `stock/.env` |

**Git에 올리지 마세요.** `application-secrets.local.yml` 은 이미 `.gitignore` 됩니다.

---

## 5. 화면별로 뭐 하는 곳인가요?

왼쪽 메뉴 기준입니다.

### 시장·주문

| 메뉴 | 하는 일 |
|------|--------|
| 대시보드 | 요약 |
| 시장 · Top100 | 거래량·거래금액 상위 종목 |
| 보유 잔고 / 주문 내역 | KIS 연동 시 |

### 시그널 (매매 후보)

| 메뉴 | 하는 일 |
|------|--------|
| **매매 시그널** | 예전 방식 (`trading_signal`) — 뉴스 점수 기반 단순 후보 |
| **전략 시그널** | **지금 쓰는 방식** (`strategy_signal`) — 전략별·AI·리스크 반영, 행 클릭 시 상세 |
| 관심종목 | 내가 보는 종목 목록 |

### 전략 설정

| 메뉴 | 하는 일 |
|------|--------|
| 전략 관리 | 전략 ON/OFF, 오늘만 임시 설정 |
| 전략 설정 | 전략 마스터 ON/OFF |
| 시장별 전략 | 강세/약세일 때 전략 가중치 |
| 시간대별 전략 | 장전·장중·종가 시간대별 ON/OFF |
| 활성 전략 | **지금 이 시각**에 켜진 전략 한눈에 |

### 뉴스·AI

| 메뉴 | 하는 일 |
|------|--------|
| 뉴스 분석 / 뉴스 소스 관리 | 키워드·네이버 수집 |
| AI 기업 분석 | 저장된 AI 분석 목록 |
| GPT 프롬프트 | 프롬프트 템플릿 |

---

## 6. 하루에 자동으로 돌아가는 것

| 대략 시간 | 자동으로 하는 일 |
|----------|------------------|
| **08:10~08:55** | 장전: 관심종목 → 뉴스·공시 수집 → AI 분석 저장 |
| **09:00~15:30** | 장중: 뉴스 들어오면 **전략 시그널** 생성 (AI API 안 부름) + 가끔 AI 배치 |
| **14:30~15:10** | 종가 후보 종목 AI |
| **15:10~15:20** | 종가베팅: AI 캐시 없으면 **최대 3초** 짧은 AI 시도 |
| **15:40~** | 장마감: 복기·전략 개선 제안·익일 관심종목 |

수동으로 돌리고 싶을 때 (Swagger 또는 curl):

```http
POST http://localhost:8080/api/trading-flow/pre-market/run
POST http://localhost:8080/api/trading-flow/post-market/run
```

---

## 7. 뉴스가 들어오면 어떻게 되나요?

```
네이버 뉴스 수집
    → (선택) OpenAI로 기사 요약·점수
    → 전략별 분석 (시가베팅, 종가베팅 등)
    → strategy_signal 테이블에 저장
    → 화면 "전략 시그널" 에서 확인
```

예전처럼 `매매 시그널` 과 **동시에 두 번** 만들지 않습니다. (기본 설정)

---

## 8. 점수는 어떻게 나오나요?

매수 후보 점수 (간단):

```
최종 점수 ≈ 규칙 점수 × 80% + AI 점수 × 20% − 리스크 감점
```

- AI가 **RISK** / **AVOID** 이면 → 매수 후보에서 제외
- 실제 주문 직전에도 리스크를 다시 검사

---

## 9. 자주 나는 문제

| 증상 | 해결 |
|------|------|
| `API HTTP 400` … `-parameters` | 백엔드 **재시작** (코드 수정 반영). `docker compose restart backend` |
| `API HTTP 403` … 프록시 | 프론트 재시작. `.env` 의 `PROXY_ALLOWED_PREFIXES` 가 너무 짧지 않은지 확인 |
| `PATCH` 405 | 프록시가 PATCH 지원해야 함 — 최신 `route.ts` 반영 후 프론트 재시작 |
| 백엔드 2~5분 안 뜸 | Docker에서 `mvn clean spring-boot:run` 이라 첫 기동이 느림. 로그에 `Started` 확인 |
| AI 분석 안 됨 | `OPENAI_API_KEY` 설정 여부 확인 |
| DART 공시 없음 | `DART_API_KEY` 설정 후 장전 파이프라인 또는 `POST /api/disclosures/collect?stockCode=005930` |

---

## 10. 처음 데이터 쌓기 (추천 순서)

1. 브라우저 http://localhost:3000 접속
2. **관심종목**에 종목 추가 (예: 삼성전자 `005930`)
3. Swagger에서 `POST /api/trading-flow/pre-market/run` 실행  
   또는 장전 시간대에 스케줄러 대기
4. **전략 시그널** 메뉴에서 CANDIDATE 확인
5. **AI 기업 분석** 메뉴에서 오늘 분석 확인
6. **뉴스 소스 관리**에서 `query`, `stockCode` 넣고 수집

---

## 개발자용 문서

| 문서 | 내용 |
|------|------|
| [GETTING-STARTED.md](./GETTING-STARTED.md) | 실행·환경변수·Swagger |
| [TRADING-FLOW.md](./TRADING-FLOW.md) | 파이프라인·API·DB (조금 더 기술적) |
| [AI-PLATFORM.md](./AI-PLATFORM.md) | AI 배치·캐시·타이밍 |
| [GIT.md](./GIT.md) | GitHub 계정 |

---

## 한 줄 요약

> **아침에 AI·뉴스·공시를 쌓고, 장중에는 규칙+저장된 AI로 전략 시그널을 만들고, 화면 `/strategy-signals`에서 본다.**
