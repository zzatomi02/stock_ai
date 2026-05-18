# stock-trading-frontend-v4 (웹 화면)

Next.js 15 · React · pnpm

---

## 먼저 읽을 문서

**[../docs/USAGE-GUIDE.md](../docs/USAGE-GUIDE.md)** — 화면별 설명, 실행 방법, 문제 해결  
**[../docs/API-AUDIT.md](../docs/API-AUDIT.md)** — 프론트·백엔드 API 대조 (불일치·미연결 목록)

---

## 실행

```bash
cd stock-trading-frontend-v4
pnpm install
pnpm dev
```

주소: http://localhost:3000  
(백엔드가 http://localhost:8080 에 떠 있어야 API가 동작합니다)

Docker 전체: 상위 `stock` 폴더에서 `docker compose -f docker-compose.dev.yml up`

---

## 주요 화면 (메뉴)

| 경로 | 설명 |
|------|------|
| `/` | 대시보드 |
| `/market` | 시장 Top100 |
| `/strategy-signals` | **전략 시그널** (전략별·AI·리스크) |
| `/signals` | 레거시 매매 시그널 |
| `/strategies` | 전략 관리·ON/OFF |
| `/strategy-settings` | 전략 설정 |
| `/market-strategy-settings` | 시장별 전략 |
| `/strategy-time-windows` | 시간대별 전략 |
| `/enabled-strategies-now` | 지금 활성 전략 |
| `/news`, `/news/ingest` | 뉴스 |
| `/ai/company-analysis` | AI 기업 분석 |
| `/watchlist` | 관심종목 |

---

## 환경 변수

| 변수 | 설명 |
|------|------|
| `INTERNAL_API_BASE_URL` | Docker 시 `http://backend:8080/api` |
| `BACKEND_BASIC_USERNAME` / `PASSWORD` | 운영 Basic 인증 |
| `PROXY_ALLOWED_PREFIXES` | 프록시 허용 API prefix (compose에 기본값 있음) |

브라우저 요청은 `/api/proxy/...` → Next.js가 백엔드로 전달 (GET/POST/PUT/PATCH/DELETE).

---

## 빌드

```bash
pnpm build
pnpm start
```

---

## Swagger (백엔드)

http://localhost:8080/swagger-ui.html
