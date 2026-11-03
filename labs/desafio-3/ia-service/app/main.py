"""Servico de IA: fronteira HTTP/JSON entre o Core (Java) e os modelos (Python)."""
from fastapi import FastAPI

from app import assistente, recomendacoes

app = FastAPI(title="Servico de IA", version="1.0.0")
app.include_router(recomendacoes.router)
app.include_router(assistente.router)


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok", "modelVersion": recomendacoes.MODEL_VERSION}
