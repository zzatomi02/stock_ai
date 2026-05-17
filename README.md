# stock — 개발 스택 한눈에 보기

이 폴더는 **주식 트레이딩** 관련 서비스를 묶어 둡니다.

| 프로젝트 | 역할 | 문서 |
|----------|------|------|
| `stock-trading-frontend-v4` | Next.js 15 UI | [README](./stock-trading-frontend-v4/README.md) |
| `stock-trading-backend-v4` | Spring Boot API, KIS·DB·Swagger·Scala | [README](./stock-trading-backend-v4/README.md) |
| `stock-trading-ai-service-v4` | FastAPI AI 서비스 | [README](./stock-trading-ai-service-v4/README.md) |

## 사용 기술 요약

- **프론트**: Node 20, **pnpm**, Next.js 15, React 19, TypeScript, lightweight-charts 등  
- **백엔드**: Java 17, Spring Boot 3, JPA + MyBatis, MySQL, Flyway, springdoc **Swagger UI**, **Scala 2.13**(Maven)  
- **AI**: Python 3.12, uv, FastAPI, Uvicorn  
- **인프라(개발)**: Docker Compose — MySQL 8, 위 세 서비스 동시 기동  

## 한 번에 올리기 (Docker Compose 개발 모드)

`stock` 디렉터리에서:

```bash
docker compose -f docker-compose.dev.yml up
```

- MySQL: `localhost:3306`, DB `stock_ai_db`  
- 백엔드: `http://localhost:8080`  
- AI: `http://localhost:8001`  
- 프론트: `http://localhost:3000`  

중지: `Ctrl+C` 또는 `docker compose -f docker-compose.dev.yml down`  
(볼륨까지 지우려면 `down -v` — DB 데이터 삭제됨)

### 비밀 설정 (선택)

`stock/.env` 에 한국투자·네이버 키 등을 두고 Compose가 읽도록 할 수 있습니다. Git에는 올리지 마세요.

## 로컬에서 나눠서 실행할 때 순서

1. **MySQL** 기동(또는 Compose에서 mysql만 실행)  
2. **AI** (선택): `stock-trading-ai-service-v4` 에서 `uv sync` → `uv run uvicorn ... --port 8001`  
3. **백엔드**: `stock-trading-backend-v4` 에서 `mvn spring-boot:run`  
4. **프론트**: `stock-trading-frontend-v4` 에서 **`pnpm install`** → **`pnpm dev`**

## Swagger (백엔드 API 문서)

백엔드가 떠 있는 상태에서 브라우저로:

- **http://localhost:8080/swagger-ui.html**

OpenAPI JSON: **http://localhost:8080/v3/api-docs**

## Scala (백엔드)

소스는 `stock-trading-backend-v4/src/main/scala` 에 두고, `mvn compile` 로 Java와 함께 빌드합니다. 자세한 설명은 [backend README](./stock-trading-backend-v4/README.md) 의 Scala 절을 참고하세요.

## 프론트에서 백엔드 주소 (Docker)

Compose의 `frontend` 서비스는 `RUNNING_IN_DOCKER=true`, `INTERNAL_API_BASE_URL=http://backend:8080/api` 를 사용합니다.  
호스트에서만 `pnpm dev` 할 때는 보통 `127.0.0.1:8080` 으로 연결됩니다.
