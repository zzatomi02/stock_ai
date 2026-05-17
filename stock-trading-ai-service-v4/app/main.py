from fastapi import FastAPI, Request
from fastapi.responses import JSONResponse

from app.api import router
from app.config import settings

app = FastAPI(title=settings.app_name, version=settings.version)


@app.middleware("http")
async def reject_large_requests(request: Request, call_next):
    content_length = request.headers.get("content-length")
    if content_length and int(content_length) > settings.max_request_bytes:
        return JSONResponse(
            status_code=413,
            content={
                "success": False,
                "message": f"요청 본문은 최대 {settings.max_request_bytes} bytes 까지 허용됩니다.",
            },
        )
    return await call_next(request)


app.include_router(router)
