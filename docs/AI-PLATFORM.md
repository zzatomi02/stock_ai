# AI 플랫폼

> **쉬운 설명** → [USAGE-GUIDE.md](./USAGE-GUIDE.md) §1, §6  
> 구현 코드: `stock-trading-backend-v4/.../ai/platform/`

---

## 기억할 것

| 상황 | AI API |
|------|--------|
| 장중 “지금 살까?” (시그널 생성) | **안 부름** — DB 캐시만 |
| 장전·장마감 배치 | **부름** — 결과를 DB에 저장 |
| 종가베팅 15:10~15:20, 캐시 없음 | **최대 3초** 짧게 시도 |
| 수동 분석 (장외) | **부름** |

---

## 시간대별 AI

| 이름 | 시간 | 설명 |
|------|------|------|
| PRE_MARKET | 08:10~08:55 | 장전 기업 분석 |
| INTRADAY_ASYNC | 09:00~15:30 | 배치만 (몇 분 간격) |
| CLOSING_CANDIDATE | 14:30~15:10 | 종가 후보 |
| CLOSING_BET_FAST | 15:10~15:20 | 종가베팅, 3초 제한 |
| POST_MARKET | 15:40~ | 장마감 심층 |
| MANUAL | 장외 | Swagger/화면에서 수동 |

---

## 배치가 하는 일 (4단계)

1. 분석할 종목 목록 수집  
2. Job 생성 (중복 방지)  
3. OpenAI 등 API 호출  
4. `ai_company_analysis` 에 저장 → 시그널 점수에 반영  

장전/장마감은 `PreMarketPipelineService` / `PostMarketPipelineService` 가 뉴스·공시와 함께 실행.

---

## 자주 쓰는 API

| 용도 | API |
|------|-----|
| 오늘 분석 목록 | `GET /api/ai/company-analysis/today` |
| 종목별 | `GET /api/ai/company-analysis/{stockCode}` |
| 장중 캐시만 | `GET /api/ai/cached/company/{stockCode}` |
| 수동 요청 | `POST /api/ai/analysis/request` |
| Provider 설정 | `GET/PUT /api/ai/providers` |

---

## 설정

```yaml
app:
  ai:
    platform:
      batch-enabled: true
      rule-score-weight: 0.80
      ai-score-weight: 0.20
      closing-bet-fast-ai-enabled: true
      closing-bet-fast-ai-timeout-ms: 3000
  openai:
    api-key: ${OPENAI_API_KEY:}   # 또는 application-secrets.local.yml
```

키는 DB에 저장하지 않습니다.

---

## 수동 분석 예시

```json
POST /api/ai/analysis/request
{
  "analysisType": "COMPANY_ANALYSIS",
  "stockCode": "005930",
  "providerTypes": ["OPENAI"],
  "executionTiming": "MANUAL"
}
```

---

## 운영 기능 (요약)

- 분석 **유효기간** 만료 처리  
- 같은 입력 **중복 Job** 방지  
- **토큰·비용** 로그  
- API 실패 시 **이전 캐시 fallback**  
- 전략 설정 변경 **승인** 후 반영  
