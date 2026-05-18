# 실행 가이드

쉬운 전체 설명은 **[USAGE-GUIDE.md](./USAGE-GUIDE.md)** 를 먼저 보세요.

---

## 1. 필요한 것

- Docker Desktop (권장)  
  또는 JDK 17 + Maven + Node 20 + pnpm + MySQL 8

---

## 2. Docker로 한 번에 실행 (권장)

```powershell
cd "C:\dev\2026 new prjt\stock"
copy .env.example .env
# 메모장으로 .env 열어 키 입력 (없어도 기동은 됨)
docker compose -f docker-compose.dev.yml up
```

| 서비스 | URL |
|--------|-----|
| **프론트 (화면)** | http://localhost:3000 |
| 백엔드 API | http://localhost:8080 |
| Swagger | http://localhost:8080/swagger-ui.html |
| AI (선택) | http://localhost:8001 |
| MySQL | localhost:3306 / DB `stock_ai_db` |

### 기동이 느릴 때

백엔드 컨테이너는 `mvn clean spring-boot:run` 이라 **처음 2~5분** 걸릴 수 있습니다.  
터미널에 아래가 보이면 성공입니다.

```text
Started StockTradingApplication
```

### 중지

```powershell
docker compose -f docker-compose.dev.yml down
```

DB까지 지우고 처음부터: `down -v`

---

## 3. 환경 변수 (.env)

`stock/.env` 파일 (`.env.example` 복사):

| 변수 | 필수? | 용도 |
|------|-------|------|
| `OPENAI_API_KEY` | AI 쓸 때 | 기업 분석 |
| `NAVER_CLIENT_ID` / `NAVER_CLIENT_SECRET` | 뉴스 쓸 때 | 네이버 뉴스 |
| `DART_API_KEY` | 공시 쓸 때 | DART 전자공시 |
| `KIS_PAPER_APPKEY` 등 | 주문 쓸 때 | 한국투자 모의투자 |

로컬만 쓸 때 OpenAI 키는 아래에 둬도 됩니다 (Git 제외):

`stock-trading-backend-v4/src/main/resources/application-secrets.local.yml`

```yaml
app:
  openai:
    api-key: "sk-..."
  dart:
    api-key: "..."
```

---

## 4. 로컬에서 각각 실행

1. MySQL 실행 (`stock_ai_db`, 사용자 `stock_user` / `1234` 또는 환경변수로 변경)
2. (선택) AI: `stock-trading-ai-service-v4` → `uv run uvicorn app.main:app --reload --port 8001`
3. 백엔드: `cd stock-trading-backend-v4` → `mvn spring-boot:run`
4. 프론트: `cd stock-trading-frontend-v4` → `pnpm install` → `pnpm dev`

프론트만 Docker, 백엔드는 PC에서 직접 실행해도 됩니다. 이때 프론트는 `http://127.0.0.1:8080/api` 로 백엔드를 봅니다.

---

## 5. 프론트 ↔ 백엔드 연결

| 실행 방식 | 프론트가 백엔드를 부르는 주소 |
|-----------|------------------------------|
| Docker Compose 전체 | `http://backend:8080/api` (자동) |
| PC에서 `pnpm dev` 만 | `http://127.0.0.1:8080/api` |

브라우저는 항상 `http://localhost:3000/api/proxy/...` 로 요청하고, Next.js가 백엔드로 넘깁니다.

---

## 6. 문제 해결

| 증상 | 확인 |
|------|------|
| 400 … `-parameters` | 백엔드 최신 코드로 **재시작** |
| 403 … 프록시 | `PROXY_ALLOWED_PREFIXES` — `.env` 에 옛 값이 있으면 삭제하거나 compose 기본값 사용 |
| 405 PATCH | 프론트 `src/app/api/proxy/[...path]/route.ts` 에 PUT/PATCH 있는지 확인 후 프론트 재시작 |
| 백엔드 exited | 로그에서 YAML 오류·DB 연결 실패 확인 |
| 프론트만 안 됨 | `pnpm install` 후 `pnpm dev` |

더 많은 설명: [USAGE-GUIDE.md §9](./USAGE-GUIDE.md#9-자주-나는-문제)

---

## 7. 다음 단계

- 화면·메뉴 설명 → [USAGE-GUIDE.md](./USAGE-GUIDE.md)
- 하루 자동 흐름 → [TRADING-FLOW.md](./TRADING-FLOW.md)
