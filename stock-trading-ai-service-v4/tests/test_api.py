from fastapi.testclient import TestClient

from app.main import app


client = TestClient(app)


def test_health_ok():
    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok"}


def test_predict_data_returns_score():
    response = client.post(
        "/predict/data",
        json={
            "stockCode": "005930",
            "candles": [
                {"time": "2026-04-24", "open": 70000, "high": 72000, "low": 69000, "close": 71000, "volume": 1000},
                {"time": "2026-04-25", "open": 71000, "high": 73500, "low": 70500, "close": 73000, "volume": 1500},
            ],
            "newsScore": 60,
            "keywordScore": 70,
        },
    )

    assert response.status_code == 200
    body = response.json()
    assert body["stockCode"] == "005930"
    assert body["model"] == "heuristic-data"
    assert 0 <= body["score"] <= 100
    assert "breakdown" in body
    assert isinstance(body["breakdown"], list)
    assert len(body["breakdown"]) >= 2
    for row in body["breakdown"]:
        assert "label" in row
        assert "contribution" in row
        assert "category" in row


def test_invalid_image_is_rejected():
    response = client.post("/predict/image", json={"stockCode": "005930", "imageBase64": "not-base64"})

    assert response.status_code == 400
