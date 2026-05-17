# stock-trading-backend-v4

Spring Boot 기반 주식 트레이딩 백엔드 API입니다.

## 사용 기술

| 구분 | 내용 |
|------|------|
| 런타임 | Java 17 |
| 프레임워크 | Spring Boot 3.3, Spring Web / WebFlux, Spring Security |
| 데이터 | Spring Data JPA, MyBatis 3, MySQL 8, Flyway(운영 스키마 이력), `ddl-auto: update`(로컬 전용) |
| 기타 | Redis·메일·캐시·AOP(감사 로그), p6spy(SQL 로깅), Actuator |
| API 문서 | springdoc-openapi → **Swagger UI** |
| 외부 연동 | 한국투자증권 OpenAPI(KIS OAuth·REST), 네이버 검색(뉴스) |
| JVM 외 언어 | **Scala 2.13** (`scala-maven-plugin`, `src/main/scala`) |

## 문서 (사용 설명서)

운영·개발 설명은 **`docs/`** 폴더에 Markdown으로 정리되어 있습니다.

| 문서 | 설명 |
|------|------|
| [docs/README.md](docs/README.md) | 문서 목록·보는 방법 |
| [docs/TRADING-FLOW.md](docs/TRADING-FLOW.md) | 장전·장중·장마감 파이프라인, API, 로컬 실행 |
| [docs/AI-PLATFORM.md](docs/AI-PLATFORM.md) | AI 배치, 캐시, 정책, 운영 체크리스트 |

IDE에서 `docs/TRADING-FLOW.md` 를 연 뒤 Markdown 미리보기(`Ctrl+Shift+V`)를 쓰거나, Cursor 채팅에서 `@docs/TRADING-FLOW.md` 로 첨부해 질문하면 됩니다.

## 주요 기능

- **전략·뉴스·거래 통계·대시보드** 등 REST API (`/api/...`)
- **일일 트레이딩 흐름**: 장전 AI 분석 → 장중 시그널(Rule+Risk+AI캐시) → 장마감 복기 ([docs/TRADING-FLOW.md](docs/TRADING-FLOW.md))
- **시장**: 거래량/거래금액 Top100(한국투자 순위 API 연동, 실패 시 샘플 데이터)
- **KIS**: OAuth `POST …/oauth2/tokenP` → 현재가·잔고·주문·체결 등(모의/실전 모드·헤더 `X-Trading-Mode`)
- **AI 서비스** 프록시, 백테스트 스텁, 자동매매 엔진(옵션), 감사 로그 등

## 사전 요구

- JDK 17, Maven 3.9+
- 로컬 DB: MySQL 8 (또는 Docker Compose 상위 폴더 `stock` 참고)

## 실행 방법

### 1) IDE / 터미널에서만 실행

```bash
cd stock-trading-backend-v4
mvn spring-boot:run
```

기본 프로파일은 `local`(`application-local.yml`). MySQL이 `localhost:3306`, DB `stock_ai_db`, 사용자 `stock_user` / 비밀번호 `1234` 등에 맞춰 두거나 `SPRING_DATASOURCE_*` 로 덮어씁니다.

### 2) 상위 폴더 Docker Compose로 전체 스택

`stock` 디렉터리에서:

```bash
docker compose -f docker-compose.dev.yml up
```

백엔드 컨테이너는 Maven으로 `spring-boot:run` 하며, MySQL·AI·프론트와 함께 기동합니다. 환경 변수는 `stock/.env` 에 두고 Compose가 주입할 수 있습니다.

## 운영 보안

- `local` 프로파일은 개발 편의를 위해 `app.security.permit-all=true` 입니다.
- 운영은 `SPRING_PROFILES_ACTIVE=prod` 와 함께 `APP_SECURITY_USERNAME`, `APP_SECURITY_PASSWORD`, `APP_CORS_ALLOWED_ORIGINS` 를 반드시 지정합니다.
- 운영 프로파일은 `ddl-auto=validate`, Flyway `validate-on-migrate=true`, Swagger 기본 비활성화로 동작합니다.

## Swagger (OpenAPI)

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON**: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

Swagger는 로컬에서 기본 허용됩니다. 운영에서는 `APP_SWAGGER_ENABLED=true` 를 명시한 경우에만 노출하세요.

## Scala 사용 방법

- 소스 위치: `src/main/scala` (예: `com.noono0.stock.scala.ScalaBridge` object)
- 빌드: `mvn compile` 시 Java와 함께 `scala-maven-plugin`으로 컴파일됩니다.
- Java에서 `object` 를 부를 때(Scala 2.13): 컴파일 후 생성되는 심볼을 사용합니다. 예: `com.noono0.stock.scala.ScalaBridge$.MODULE$.RuntimeVersion()`  
  호출을 단순히 하려면 Scala 쪽에 `@JvmStatic def ...` 를 두거나, Java 전용 래퍼 클래스를 두는 방식을 권장합니다.

새 Scala 파일을 추가한 뒤 `mvn clean compile` 로 확인하세요.

## 기타 설정 메모

- **한국투자**: `application-local.yml` 의 `app.kis.*` 및 환경 변수 `KIS_PAPER_APPKEY`, `KIS_PAPER_SECRET`, `KIS_ACCOUNT_NO` 등
- **프론트 연동 URL**: `app.front-origin`, Docker 시 `NEXT_ORIGIN`
- **토큰 발급 로그**: 로그에 `【KIS-TOKEN】` 접두어로 남깁니다(발급 성공/실패·캐시 등).

## 테스트

```bash
mvn test
```
