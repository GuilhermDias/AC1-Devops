"""RF04: assistente educacional. Resposta simulada no lugar de RAG/LLM, sem chave de API."""
from fastapi import APIRouter
from pydantic import BaseModel, Field

router = APIRouter()


class AssistantRequest(BaseModel):
    question: str = Field(min_length=1)


class AssistantResponse(BaseModel):
    answer: str
    simulated: bool
    sources: list[str]


@router.post("/assistente")
def assistente(request: AssistantRequest) -> AssistantResponse:
    return AssistantResponse(
        answer=f"Resposta simulada para: {request.question}",
        simulated=True,
        sources=[],
    )
