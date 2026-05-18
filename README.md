# stock — 주식 트레이딩 (모노레포)

GitHub: [zzatomi02/stock_ai](https://github.com/zzatomi02/stock_ai)

---

## 처음이신가요? → 여기부터

**[docs/USAGE-GUIDE.md](./docs/USAGE-GUIDE.md)** — 화면 설명, 하루 흐름, API 키, 자주 나는 오류 (쉬운 말)

5분 안에 띄우기:

```powershell
cd "C:\dev\2026 new prjt\stock"
copy .env.example .env
docker compose -f docker-compose.dev.yml up
```

- 화면: http://localhost:3000  
- API 문서: http://localhost:8080/swagger-ui.html  

---

## 프로젝트 구성

| 폴더 | 설명 |
|------|------|
| [stock-trading-frontend-v4](./stock-trading-frontend-v4/) | Next.js 웹 UI |
| [stock-trading-backend-v4](./stock-trading-backend-v4/) | Spring Boot API·스케줄·DB |
| [stock-trading-ai-service-v4](./stock-trading-ai-service-v4/) | (선택) FastAPI |
| [docs/](./docs/) | **사용 설명서** |

---

## 문서 목록

| 문서 | 누가 읽나요 |
|------|------------|
| **[USAGE-GUIDE.md](./docs/USAGE-GUIDE.md)** | **모든 사용자 (필수 추천)** |
| [GETTING-STARTED.md](./docs/GETTING-STARTED.md) | 설치·Docker·환경변수 |
| [TRADING-FLOW.md](./docs/TRADING-FLOW.md) | 장전/장중/장마감·API |
| [AI-PLATFORM.md](./docs/AI-PLATFORM.md) | AI 배치·캐시 |
| [GIT.md](./docs/GIT.md) | GitHub 푸시 |

---

## 지금 구현된 핵심 기능

- 장전/장마감 자동 파이프라인 (뉴스·DART 공시·AI 분석)
- 장중 전략 시그널 (Rule + Risk + AI 캐시, API 실시간 호출 없음)
- 종가베팅 Fast AI (15:10~15:20, 캐시 없을 때 3초 제한)
- 프론트 **전략 시그널** 화면 (`/strategy-signals`)
- DART 공시 수집·KIS·네이버 뉴스 연동

---

## API 키 (요약)

`stock/.env` 에 설정 (예시는 `.env.example`):

- `OPENAI_API_KEY` — AI 분석  
- `NAVER_CLIENT_ID` / `NAVER_CLIENT_SECRET` — 뉴스  
- `DART_API_KEY` — 공시  
- `KIS_PAPER_*` — 모의투자 (선택)  

민감 정보는 **커밋하지 마세요.**
