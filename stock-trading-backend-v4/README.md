# stock-trading-backend-v4 (백엔드 API)

Spring Boot 3 · Java 17 · MySQL

---

## 먼저 읽을 문서

| 문서 | 내용 |
|------|------|
| **[../docs/USAGE-GUIDE.md](../docs/USAGE-GUIDE.md)** | **쉬운 사용 설명서 (추천)** |
| [../docs/GETTING-STARTED.md](../docs/GETTING-STARTED.md) | 실행·Docker·환경변수 |
| [../docs/TRADING-FLOW.md](../docs/TRADING-FLOW.md) | 장전/장중/장마감 |
| [../docs/AI-PLATFORM.md](../docs/AI-PLATFORM.md) | AI 배치·캐시 |

---

## 하는 일 (요약)

- REST API (`/api/...`)
- 장전/장마감 자동 파이프라인 (뉴스·DART·AI)
- 장중 **전략 시그널** (Rule + Risk + AI 캐시)
- KIS·네이버·DART 연동
- Swagger: http://localhost:8080/swagger-ui.html

---

## 실행

```bash
cd stock-trading-backend-v4
mvn spring-boot:run
```

Docker 전체 스택: 상위 `stock` 폴더에서 `docker compose -f docker-compose.dev.yml up`

---

## API 키

- `stock/.env` 또는 `src/main/resources/application-secrets.local.yml` (Git 제외)
- `OPENAI_API_KEY`, `NAVER_*`, `DART_API_KEY`, `KIS_*`

---

## 테스트

```bash
mvn test
```

---

## 기술 스택

Java 17, Spring Boot 3.3, JPA, MyBatis, MySQL, Flyway, springdoc(Swagger), Scala(선택)
