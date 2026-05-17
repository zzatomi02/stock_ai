import base64
import binascii
import io

import numpy as np
from PIL import Image, UnidentifiedImageError

from app.schemas import CombinedRequest, DataRequest, ImageRequest, PredictionResponse, ScoreBreakdownLine

NEUTRAL_NEWS = 50
NEUTRAL_KW = 50


def label_for(score: int) -> str:
    if score >= 65:
        return "buy_watch"
    if score >= 45:
        return "neutral"
    return "avoid"


def clamp_score(value: float) -> int:
    return int(max(0, min(100, value)))


def _clip(x: float, lo: float, hi: float) -> float:
    return float(max(lo, min(hi, x)))


def rsi14_wilder(closes: np.ndarray) -> float:
    """Wilder RSI(14). 캔들이 부족하면 50(중립)."""
    c = np.asarray(closes, dtype=float)
    if c.size < 2:
        return 50.0
    if c.size < 15:
        return 50.0
    deltas = np.diff(c)
    period = 14
    gains = np.where(deltas > 0, deltas, 0.0)
    losses_ = np.where(deltas < 0, -deltas, 0.0)
    avg_gain = float(np.mean(gains[:period]))
    avg_loss = float(np.mean(losses_[:period]))
    for i in range(period, len(gains)):
        g = float(gains[i])
        l = float(losses_[i])
        avg_gain = (avg_gain * (period - 1) + g) / period
        avg_loss = (avg_loss * (period - 1) + l) / period
    if avg_loss < 1e-12:
        return 100.0 if avg_gain > 0 else 50.0
    if avg_gain < 1e-12:
        return 0.0
    rs = avg_gain / avg_loss
    return 100.0 - (100.0 / (1.0 + rs))


def bollinger_pctb(closes: np.ndarray, n: int = 20, k: float = 2.0) -> tuple[float, float, float, float, float] | None:
    """(pct_b 0~1, mid, upper, lower, width). n봉 미만이면 None."""
    c = np.asarray(closes, dtype=float)
    m = int(min(n, c.size))
    if m < 2:
        return None
    w = c[-m:]
    mid = float(w.mean())
    std = float(w.std(ddof=0))
    upper = mid + k * std
    lower = mid - k * std
    width = upper - lower
    if width < 1e-12 * max(abs(mid), 1.0):
        return None
    last = float(c[-1])
    pct_b = (last - lower) / width
    pct_b = float(_clip(pct_b, 0.0, 1.0))
    return pct_b, mid, upper, lower, width


def predict_data(request: DataRequest) -> PredictionResponse:
    lines: list[ScoreBreakdownLine] = []
    closes = np.array([c.close for c in request.candles], dtype=float)
    volumes = np.array([c.volume for c in request.candles], dtype=float)
    if len(closes) < 2 or closes[0] <= 0:
        part_trend = 0.0
        part_ma = 0.0
        part_vol = 0.0
        part_rsi = 0.0
        part_bb = 0.0
        trend_score = 50.0
        lines.append(
            ScoreBreakdownLine(
                key="tech_baseline",
                label="기술·중립 기준(가중 60%의 기준 50)",
                category="technical",
                contribution=int(round(0.6 * 50.0)),
                detail="봉이 2개 미만이면 추세/이평/거래량 항목은 0, 기준 30점만 유지(0.6×50).",
            )
        )
        lines.append(
            ScoreBreakdownLine(
                key="candles_short",
                label="기술(봉 부족) — 가격·이평·거래량",
                category="technical",
                contribution=0,
                detail="봉이 2개 미만이면 세부 항목은 0(중립)으로 둡니다.",
            )
        )
    else:
        trend_pct = ((closes[-1] - closes[0]) / closes[0]) * 100.0
        ma_short = float(closes[-5:].mean() if len(closes) >= 5 else closes.mean())
        ma_long = float(closes[-20:].mean() if len(closes) >= 20 else closes.mean())
        vol_ratio = float(volumes[-1] / max(volumes.mean(), 1.0))
        part_trend = trend_pct * 1.5
        part_ma = (ma_short - ma_long) / max(ma_long, 1.0) * 200.0
        part_vol = (vol_ratio - 1.0) * 8.0
        rsi = rsi14_wilder(closes)
        part_rsi = _clip((rsi - 50.0) * 0.45, -12.0, 12.0)
        bb = bollinger_pctb(closes, n=20, k=2.0)
        if bb is not None:
            pb, bmid, up, lo, width = bb
            part_bb = _clip((pb - 0.5) * 22.0, -10.0, 10.0)
        else:
            pb, bmid, up, lo, width = 0.5, 0.0, 0.0, 0.0, 0.0
            part_bb = 0.0
        trend_score = 50.0 + part_trend + part_ma + part_vol + part_rsi + part_bb
        c_base = int(round(0.6 * 50.0))
        lines.append(
            ScoreBreakdownLine(
                key="tech_baseline",
                label="기술·중립 기준(가중 60%의 기준 50)",
                category="technical",
                contribution=c_base,
                detail="0.6×50. 가격·거래량 휴리스틱의 기준에 해당합니다.",
            )
        )
        lines.append(
            ScoreBreakdownLine(
                key="trend_window",
                label="기간 수익률(첫·마지막 종가)",
                category="technical",
                contribution=int(round(0.6 * part_trend)),
                detail=f"기간 수익률 약 {trend_pct:.2f}%. 0.6×(수익률×1.5) 반영.",
            )
        )
        lines.append(
            ScoreBreakdownLine(
                key="ma5_ma20",
                label="단기(5봉)·장기(20봉) 이평 괴리",
                category="technical",
                contribution=int(round(0.6 * part_ma)),
                detail=f"MA5 {ma_short:.0f} · MA20 {ma_long:.0f}. 0.6×(괴리×200) 반영. 단기>장기면 가산",
            )
        )
        lines.append(
            ScoreBreakdownLine(
                key="volume_ratio",
                label="최근 봉 거래량 / 평균 대비",
                category="technical",
                contribution=int(round(0.6 * part_vol)),
                detail=f"거래량 비 약 {vol_ratio:.2f}. 0.6×((비율-1)×8) 반영.",
            )
        )
        lines.append(
            ScoreBreakdownLine(
                key="rsi14",
                label="RSI(14) — Wilder",
                category="technical",
                contribution=int(round(0.6 * part_rsi)),
                detail=f"RSI 약 {rsi:.1f}. 50(중립)에서 벗어날수록 휴리스틱 가감(대략 ±12 cap).",
            )
        )
        if bb is not None and width > 0:
            lines.append(
                ScoreBreakdownLine(
                    key="bollinger20_2",
                    label="볼린저(20,2) — %B (종가 대역)",
                    category="technical",
                    contribution=int(round(0.6 * part_bb)),
                    detail=f"중심 {bmid:.0f} / 상단 {up:.0f} / 하단 {lo:.0f}, %B≈{pb:.2f}. 0.5에 가깝게 중립.",
                )
            )
        else:
            lines.append(
                ScoreBreakdownLine(
                    key="bollinger20_2",
                    label="볼린저(20,2)",
                    category="technical",
                    contribution=0,
                    detail="봉이 부족하거나 밴드 폭이 0에 가까워 %B는 생략.",
                )
            )

    raw = 0.6 * trend_score + 0.2 * request.newsScore + 0.2 * request.keywordScore
    c_news = int(round(0.2 * request.newsScore))
    c_kw = int(round(0.2 * request.keywordScore))
    lines.append(
        ScoreBreakdownLine(
            key="news_weighted",
            label="뉴스 점수(가중 20%)",
            category="news",
            contribution=c_news,
            detail=f"입력 {request.newsScore}/100, 중립(50) 대비 {'+' if request.newsScore - NEUTRAL_NEWS >= 0 else ''}"
            f"{request.newsScore - NEUTRAL_NEWS} (가감: {int(round(0.2 * (request.newsScore - NEUTRAL_NEWS)))}pt)",
        )
    )
    lines.append(
        ScoreBreakdownLine(
            key="keyword_weighted",
            label="키워드 점수(가중 20%)",
            category="keyword",
            contribution=c_kw,
            detail=f"입력 {request.keywordScore}/100, 중립(50) 대비 {request.keywordScore - NEUTRAL_KW} (가감: "
            f"{int(round(0.2 * (request.keywordScore - NEUTRAL_KW)))}pt)",
        )
    )
    final = clamp_score(raw)
    return PredictionResponse(
        stockCode=request.stockCode,
        model="heuristic-data",
        score=final,
        label=label_for(final),
        reason="캔들·이평·RSI·볼린저·거래량(가중 60%) + 뉴스(20%) + 키워드(20%) — breakdown 참고",
        components={
            "trendScoreRaw": round(float(trend_score), 2),
            "news": request.newsScore,
            "keyword": request.keywordScore,
        },
        rawBeforeClamp=raw,
        breakdown=lines,
    )


def predict_image(request: ImageRequest) -> PredictionResponse:
    try:
        raw = base64.b64decode(request.imageBase64, validate=True)
        img = Image.open(io.BytesIO(raw)).convert("L").resize((64, 64))
    except (binascii.Error, UnidentifiedImageError, OSError) as exc:
        raise ValueError("imageBase64 must contain a valid image") from exc

    arr = np.array(img, dtype=float)
    mean_px = float(arr.mean() / 255.0 * 100.0)
    score = clamp_score(mean_px)
    lines = [
        ScoreBreakdownLine(
            key="image_luminance",
            label="이미지 휴리스틱(64×64 흑백 평균 밝기)",
            category="image",
            contribution=score,
            detail="딥러닝이 아닌, 밝기 기반 단순 점수입니다. combined 시 20%에만 쓰입니다.",
        )
    ]
    return PredictionResponse(
        stockCode=request.stockCode,
        model="heuristic-image",
        score=score,
        label=label_for(score),
        reason="차트 이미지(밝기) 휴리스틱 — breakdown 참고",
        rawBeforeClamp=float(mean_px),
        breakdown=lines,
    )


def predict_combined(request: CombinedRequest) -> PredictionResponse:
    data_result = predict_data(
        DataRequest(
            stockCode=request.stockCode,
            candles=request.candles,
            newsScore=request.newsScore,
            keywordScore=request.keywordScore,
        )
    )
    image_score = 50
    img_lines: list[ScoreBreakdownLine] = []
    if request.imageBase64:
        ir = predict_image(ImageRequest(stockCode=request.stockCode, imageBase64=request.imageBase64))
        image_score = ir.score
        img_lines = ir.breakdown

    raw = 0.8 * float(data_result.score) + 0.2 * float(image_score)
    final = clamp_score(raw)
    blend_lines: list[ScoreBreakdownLine] = [
        ScoreBreakdownLine(
            key="blend_data",
            label="데이터·뉴스·키워드(80%)",
            category="blend",
            contribution=int(round(0.8 * float(data_result.score))),
            detail="predict/data 최종 score(0~100) × 0.8",
        ),
        ScoreBreakdownLine(
            key="blend_image",
            label="이미지 휴리스틱(20%)",
            category="blend",
            contribution=int(round(0.2 * float(image_score))),
            detail="이미지 점수 × 0.2",
        ),
    ]
    sub_lines: list[ScoreBreakdownLine] = []
    for b in data_result.breakdown:
        sub_lines.append(
            ScoreBreakdownLine(
                key="data:" + b.key,
                label="[데이터] " + b.label,
                category=b.category,
                contribution=int(round(0.8 * float(b.contribution))),
                detail=f"(80%) {b.detail}",
            )
        )
    for b in img_lines:
        sub_lines.append(
            ScoreBreakdownLine(
                key="img:" + b.key,
                label="[이미지] " + b.label,
                category="image",
                contribution=int(round(0.2 * float(b.contribution))),
                detail=f"(20%) {b.detail}",
            )
        )
    return PredictionResponse(
        stockCode=request.stockCode,
        model="heuristic-combined",
        score=final,
        label=label_for(final),
        reason="데이터(80%)+이미지(20%) — 상세는 blend·하위 breakdown",
        dataScore=data_result.score,
        imageScore=image_score,
        rawBeforeClamp=raw,
        breakdown=blend_lines + sub_lines,
    )
