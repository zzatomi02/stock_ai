# stock — 사용·개발 설명서

이 폴더는 **stock 모노레포 전체** (백엔드·프론트·AI·Docker) 운영·개발 문서입니다.  
GitHub: [zzatomi02/stock_ai](https://github.com/zzatomi02/stock_ai)

## 문서 목록

| 문서 | 내용 |
|------|------|
| [GETTING-STARTED.md](./GETTING-STARTED.md) | Docker / 로컬 실행, Swagger, 환경 변수 |
| [TRADING-FLOW.md](./TRADING-FLOW.md) | 장전·장중·장마감 파이프라인, API, DB, 설정 |
| [AI-PLATFORM.md](./AI-PLATFORM.md) | AI 배치, 캐시, 점수 블렌딩, 운영 체크리스트 |
| [GIT.md](./GIT.md) | GitHub 계정 분리 (`zzatomi02` vs `Noono0`) |

## 프로젝트별 README

| 경로 | 역할 |
|------|------|
| [../stock-trading-backend-v4/README.md](../stock-trading-backend-v4/README.md) | Spring Boot API |
| [../stock-trading-frontend-v4/README.md](../stock-trading-frontend-v4/README.md) | Next.js UI |
| [../stock-trading-ai-service-v4/README.md](../stock-trading-ai-service-v4/README.md) | FastAPI AI |

## 문서 보는 방법

1. **IDE** — `docs/TRADING-FLOW.md` 열기 → `Ctrl+Shift+V` (미리보기)
2. **Cursor** — `@docs/TRADING-FLOW.md` 첨부 후 질문
3. **GitHub** — 저장소 웹에서 `docs/` 폴더 탐색

## 핵심 원칙 (한 줄)

> 장중 **빠른 매수 판단**에는 AI API 호출 없음 → Rule + Risk + **DB 캐시**만 사용.  
> AI API는 **장전·장중 배치(비동기)·종가·장마감** 에만.
