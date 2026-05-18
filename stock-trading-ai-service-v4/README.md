# stock-trading-ai-service-v4

FastAPI 기반 AI 보조 서비스입니다. 백엔드에서 `AI_BASE_URL`(기본 `http://localhost:8001`)로 프록시 호출합니다.

**전체 사용 설명** → [../docs/USAGE-GUIDE.md](../docs/USAGE-GUIDE.md)  
(대부분의 AI 기업 분석은 백엔드가 OpenAI를 직접 호출합니다. 이 서비스는 선택 사항입니다.)

## 사용 기술

- Python 3.12+
- [uv](https://docs.astral.sh/uv/) (의존성·가상환경)
- FastAPI, Uvicorn, Pydantic v2, NumPy, Pillow 등 (`pyproject.toml` 참고)

## 주요 기능

- 백엔드에서 호출하는 `/predict/data`, `/predict/image`, `/predict/combined`
- 현재 구현은 실제 학습 모델이 아니라 OHLCV/뉴스/키워드/이미지 기반 **휴리스틱 보조 점수**입니다.
- `app/api.py`, `app/schemas.py`, `app/services.py`, `app/config.py` 로 API, 입력 스키마, 예측 로직, 설정을 분리했습니다.

## 사전 요구

- Python 3.12 이상
- uv 설치(또는 `pip install uv`)

## 실행 방법 (로컬)

```bash
cd stock-trading-ai-service-v4
uv sync
uv run uvicorn app.main:app --reload --host 0.0.0.0 --port 8001
```

브라우저에서 문서: [http://localhost:8001/docs](http://localhost:8001/docs) (FastAPI Swagger)

## 테스트

```bash
uv sync
uv run pytest
```

## 운영 설정

| 변수 | 기본값 | 설명 |
|------|--------|------|
| `MAX_REQUEST_BYTES` | `1048576` | HTTP 요청 본문 최대 크기 |
| `MAX_IMAGE_BASE64_LENGTH` | `750000` | 이미지 base64 문자열 최대 길이 |

## Docker Compose

상위 폴더 `stock` 의 `docker-compose.dev.yml` 에서 `ai-service` 가 같은 포트(8001)로 기동되며, healthcheck로 `/health` 를 확인합니다. 서비스 Dockerfile은 `pyproject.toml` 기준으로 의존성을 설치합니다.

## 백엔드와 연결

- 로컬: 백엔드 `application-local.yml` 의 `app.ai.base-url: http://localhost:8001`
- Docker: Compose에서 `AI_BASE_URL=http://ai-service:8001` 로 백엔드에 주입

## Scala / Java Swagger

이 저장소는 Python 전용입니다. **Scala·Spring Swagger**는 `stock-trading-backend-v4/README.md` 를 참고하세요.
