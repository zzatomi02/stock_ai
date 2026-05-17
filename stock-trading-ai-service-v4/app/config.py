import os
from dataclasses import dataclass


@dataclass(frozen=True)
class Settings:
    app_name: str = os.getenv("APP_NAME", "Stock Trading AI Service")
    version: str = os.getenv("APP_VERSION", "0.4.0")
    max_request_bytes: int = int(os.getenv("MAX_REQUEST_BYTES", "1048576"))
    max_image_base64_length: int = int(os.getenv("MAX_IMAGE_BASE64_LENGTH", "750000"))


settings = Settings()
