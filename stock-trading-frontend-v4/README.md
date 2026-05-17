# stock-trading-frontend-v4

Next.js 15(App Router) 기반 프론트엔드입니다. 백엔드 `stock-trading-backend-v4` 의 `/api` 와 연동합니다.

## 사용 기술

- **런타임**: Node.js 20 권장
- **패키지 매니저**: **pnpm**
- **프레임워크**: Next.js 15, React 19(RC), TypeScript
- **UI/차트**: Radix UI, Framer Motion, lightweight-charts v5, Recharts, Sass, Tailwind v4 계열
- **상태/데이터**: TanStack Query, Zustand, next-auth(beta)

## 주요 기능

- 대시보드, 시장 Top100, 종목 상세·캔들 차트, 전략·통계·뉴스 등 화면
- 브라우저에서는 `/api/proxy/...` Route Handler를 통해 백엔드로 요청(동일 출처)
- 서버 컴포넌트(RSC)에서는 `INTERNAL_API_BASE_URL` 로 백엔드에 직접 fetch

## 사전 요구

- Node.js 20+
- [pnpm](https://pnpm.io/) (`corepack enable` 후 사용 가능)

## 실행 방법 (로컬)

```bash
cd stock-trading-frontend-v4
pnpm install
pnpm dev
```

기본 주소: [http://localhost:3000](http://localhost:3000)

백엔드가 **같은 머신**에서 `http://127.0.0.1:8080` 으로 떠 있어야 합니다.  
프론트만 Docker로 띄우는 경우 Compose에서 `RUNNING_IN_DOCKER=true`, `INTERNAL_API_BASE_URL=http://backend:8080/api` 를 설정합니다(호스트에서 `pnpm dev` 할 때는 보통 생략).

## 빌드 / 프로덕션

```bash
pnpm build
pnpm start
```

## 환경 변수 (요약)

| 변수 | 설명 |
|------|------|
| `NEXT_PUBLIC_API_BASE_URL` | 브라우저가 알 수 있는 API 베이스(선택) |
| `INTERNAL_API_BASE_URL` | 서버(RSC) 전용 백엔드 베이스, Docker 시 `http://backend:8080/api` |
| `RUNNING_IN_DOCKER` | `true` 이면 서버 쪽에서 `backend` 호스트명 처리 등에 사용 |
| `AUTH_SECRET` | NextAuth 등에 필요(길이 충분한 문자열) |
| `BACKEND_BASIC_USERNAME` / `BACKEND_BASIC_PASSWORD` | 운영 백엔드 Basic 인증을 서버/프록시에서 호출할 때 사용 |
| `PROXY_ALLOWED_PREFIXES` | `/api/proxy/...` 로 전달할 수 있는 백엔드 API prefix 목록 |

## 프록시 / 운영 보안

`/api/proxy/...` 는 기본적으로 `market,dashboard,strategies,strategy,news,statistics,backtest,ai,broker,ops` prefix만 백엔드로 전달합니다. 운영에서 백엔드 인증을 켠 경우 프론트 서버 환경 변수에 `BACKEND_BASIC_USERNAME`, `BACKEND_BASIC_PASSWORD` 를 설정합니다.

Dockerfile은 `next.config.ts`의 `output: 'standalone'` 기준으로 `pnpm build` 후 `node server.js`를 실행합니다.

## 린트 / 테스트

```bash
pnpm lint
pnpm test
```

## Swagger (백엔드 API 확인)

프론트가 아니라 **백엔드**에서 제공합니다. 백엔드 기동 후:

- [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

## Scala

Scala는 **백엔드 Maven 프로젝트**에 포함되어 있습니다. 프론트 README가 아니라 `stock-trading-backend-v4/README.md` 를 참고하세요.
