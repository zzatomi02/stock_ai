from typing import Literal

from pydantic import BaseModel, Field, field_validator

from app.config import settings


class ScoreBreakdownLine(BaseModel):
    """최종 점수(0~100)에 합산·가감된 한 줄 근거. contribution 합 ≈ (클램프 전) 원시 합에 대응."""

    key: str = Field(min_length=1, max_length=64)
    label: str = Field(min_length=1, max_length=200)
    category: Literal["technical", "news", "keyword", "image", "blend"] = "technical"
    contribution: int = Field(description="최종 점수 쪽에 반영된 가산(음수면 감산)")
    detail: str = Field(default="")


class Candle(BaseModel):
    time: str = Field(min_length=1, max_length=64)
    open: float = Field(ge=0)
    high: float = Field(ge=0)
    low: float = Field(ge=0)
    close: float = Field(ge=0)
    volume: float = Field(ge=0)


class DataRequest(BaseModel):
    stockCode: str = Field(min_length=1, max_length=20)
    candles: list[Candle] = Field(min_length=1, max_length=300)
    newsScore: int = Field(default=50, ge=0, le=100)
    keywordScore: int = Field(default=50, ge=0, le=100)


class ImageRequest(BaseModel):
    stockCode: str = Field(min_length=1, max_length=20)
    imageBase64: str = Field(min_length=1)

    @field_validator("imageBase64")
    @classmethod
    def image_must_be_small_enough(cls, value: str) -> str:
        if len(value) > settings.max_image_base64_length:
            raise ValueError("imageBase64 is too large")
        return value


class CombinedRequest(DataRequest):
    imageBase64: str | None = None

    @field_validator("imageBase64")
    @classmethod
    def optional_image_must_be_small_enough(cls, value: str | None) -> str | None:
        if value is not None and len(value) > settings.max_image_base64_length:
            raise ValueError("imageBase64 is too large")
        return value


class PredictionResponse(BaseModel):
    stockCode: str
    model: Literal["heuristic-data", "heuristic-image", "heuristic-combined"]
    score: int = Field(ge=0, le=100)
    label: Literal["buy_watch", "neutral", "avoid"]
    reason: str
    components: dict[str, float | int] | None = None
    dataScore: int | None = None
    imageScore: int | None = None
    rawBeforeClamp: float | None = Field(
        default=None, description="클램프(0~100) 이전 원시 합. 항목 contribution 합과 대조."
    )
    breakdown: list[ScoreBreakdownLine] = Field(
        default_factory=list, description="기술·뉴스·키워드 등 항목별 가산/감산 근거."
    )
