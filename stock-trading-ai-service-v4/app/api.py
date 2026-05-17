from fastapi import APIRouter, HTTPException

from app.schemas import CombinedRequest, DataRequest, ImageRequest, PredictionResponse
from app.services import predict_combined, predict_data, predict_image

router = APIRouter()


@router.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}


@router.post("/predict/data", response_model=PredictionResponse)
def predict_data_endpoint(request: DataRequest) -> PredictionResponse:
    return predict_data(request)


@router.post("/predict/image", response_model=PredictionResponse)
def predict_image_endpoint(request: ImageRequest) -> PredictionResponse:
    try:
        return predict_image(request)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc


@router.post("/predict/combined", response_model=PredictionResponse)
def predict_combined_endpoint(request: CombinedRequest) -> PredictionResponse:
    try:
        return predict_combined(request)
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
