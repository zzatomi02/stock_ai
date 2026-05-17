# 시작 가이드 — stock 모노레포

## 프로젝트 구성

| 폴더 | 기술 | 포트(로컬) |
|------|------|------------|
| `stock-trading-backend-v4` | Java 17, Spring Boot 3 | 8080 |
| `stock-trading-frontend-v4` | Next.js 15, pnpm | 3000 |
| `stock-trading-ai-service-v4` | Python FastAPI | 8001 |

## Docker Compose (한 번에 기동)

`stock` 디렉터리에서:

```bash
docker compose -f docker-compose.dev.yml up
```

| 서비스 | URL |
|--------|-----|
| 프론트 | http://localhost:3000 |
| 백엔드 | http://localhost:8080 |
| AI | http://localhost:8001 |
| MySQL | localhost:3306 (`stock_ai_db`) |

중지: `docker compose -f docker-compose.dev.yml down`  
(DB 초기화: `down -v`)

### 환경 변수

`stock/.env` 에 KIS·네이버·OpenAI 키 등을 둘 수 있습니다. **Git에 커밋하지 마세요.**  
예시는 `.env.example` 참고.

## 로컬에서 개별 실행

1. **MySQL** 기동
2. **AI** (선택): `stock-trading-ai-service-v4` → `uv sync` → `uv run uvicorn app.main:app --reload --port 8001`
3. **백엔드**: `stock-trading-backend-v4` → `mvn spring-boot:run`
4. **프론트**: `stock-trading-frontend-v4` → `pnpm install` → `pnpm dev`

## Swagger

백엔드 기동 후:

- http://localhost:8080/swagger-ui.html
- http://localhost:8080/v3/api-docs

## 프론트 API 주소

- Docker Compose: `INTERNAL_API_BASE_URL=http://backend:8080/api`
- 호스트에서 `pnpm dev`만: 보통 `http://127.0.0.1:8080/api`

## 다음에 읽을 문서

- 일일 운영 흐름 → [TRADING-FLOW.md](./TRADING-FLOW.md)
- AI 설정·배치 → [AI-PLATFORM.md](./AI-PLATFORM.md)
