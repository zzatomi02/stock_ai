# 설명서 안내

## 어떤 문서를 읽을까요?

| 상황 | 읽을 문서 |
|------|-----------|
| **처음 써보는데 뭐가 뭔지 모르겠다** | **[USAGE-GUIDE.md](./USAGE-GUIDE.md)** ← 여기 |
| Docker로 실행만 하고 싶다 | [GETTING-STARTED.md](./GETTING-STARTED.md) |
| 하루에 뭐가 자동으로 도는지 알고 싶다 | [USAGE-GUIDE.md §6](./USAGE-GUIDE.md#6-하루에-자동으로-돌아가는-것) |
| AI를 언제 쓰는지 / 안 쓰는지 | [USAGE-GUIDE.md §1](./USAGE-GUIDE.md#1-이게-뭔가요) · [AI-PLATFORM.md](./AI-PLATFORM.md) |
| API·DB·클래스 이름이 필요하다 | [TRADING-FLOW.md](./TRADING-FLOW.md) · [AI-PLATFORM.md](./AI-PLATFORM.md) |
| 프론트·백엔드 API가 맞는지 확인 | [API-AUDIT.md](./API-AUDIT.md) |
| GitHub에 올리기 | [GIT.md](./GIT.md) |

---

## 문서 목록

| 파일 | 내용 |
|------|------|
| **[USAGE-GUIDE.md](./USAGE-GUIDE.md)** | **쉬운 사용 설명서 (화면·흐름·문제 해결)** |
| [GETTING-STARTED.md](./GETTING-STARTED.md) | 설치, Docker, Swagger, 환경 변수 |
| [TRADING-FLOW.md](./TRADING-FLOW.md) | 트레이딩 파이프라인 (기술 상세) |
| [AI-PLATFORM.md](./AI-PLATFORM.md) | AI 배치·캐시 (기술 상세) |
| [GIT.md](./GIT.md) | Git 계정·원격 저장소 |
| [API-AUDIT.md](./API-AUDIT.md) | 프론트 ↔ 백엔드 API 대조·미사용 목록 |

---

## 핵심만 기억하기

1. **화면** → http://localhost:3000  
2. **매매 판단용 시그널** → 메뉴 **「전략 시그널」** (`/strategy-signals`)  
3. **장중에는 AI API를 바로 안 부름** → 아침·배치에 저장한 AI만 사용  
4. **키** → `stock/.env` (Git 제외)

프로젝트별 README:

- [백엔드](../stock-trading-backend-v4/README.md)
- [프론트](../stock-trading-frontend-v4/README.md)
- [AI 서비스](../stock-trading-ai-service-v4/README.md)
