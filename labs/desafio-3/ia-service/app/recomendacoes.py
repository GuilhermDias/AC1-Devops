"""RF03: recomendacao de cursos. Regra deterministica no lugar de um modelo treinado."""
import asyncio
import os
from enum import Enum

from fastapi import APIRouter
from pydantic import BaseModel, Field

MODEL_VERSION = "regras-v1"
MAX_COURSES = 3

router = APIRouter()


class Level(str, Enum):
    BRONZE = "BRONZE"
    PRATA = "PRATA"
    OURO = "OURO"
    DIAMANTE = "DIAMANTE"


LEVEL_ORDER = list(Level)

CATALOG = [
    {"courseId": "fundamentos-git", "title": "Fundamentos de Git", "level": Level.BRONZE},
    {"courseId": "logica-programacao", "title": "Logica de Programacao", "level": Level.BRONZE},
    {"courseId": "testes-unitarios", "title": "Testes Unitarios", "level": Level.PRATA},
    {"courseId": "docker-essencial", "title": "Docker Essencial", "level": Level.PRATA},
    {"courseId": "ci-cd", "title": "Integracao e Entrega Continuas", "level": Level.OURO},
    {"courseId": "arquitetura-software", "title": "Arquitetura de Software", "level": Level.OURO},
    {"courseId": "observabilidade", "title": "Observabilidade", "level": Level.DIAMANTE},
    {"courseId": "sistemas-distribuidos", "title": "Sistemas Distribuidos", "level": Level.DIAMANTE},
]


class RecommendationRequest(BaseModel):
    studentId: int
    level: Level
    completedCourses: list[str] = Field(default_factory=list)


class Course(BaseModel):
    courseId: str
    title: str
    reason: str


class RecommendationResponse(BaseModel):
    studentId: int
    courses: list[Course]
    modelVersion: str


def recommend(level: Level, completed: list[str]) -> list[Course]:
    """Cursos do nivel do aluno e depois dos niveis seguintes, sem repetir os ja concluidos."""
    courses = []
    for target in LEVEL_ORDER[LEVEL_ORDER.index(level):]:
        for item in CATALOG:
            if item["level"] == target and item["courseId"] not in completed:
                reason = "Indicado para o seu nivel" if target == level else "Prepara para o proximo nivel"
                courses.append(Course(courseId=item["courseId"], title=item["title"], reason=reason))
    return courses[:MAX_COURSES]


@router.post("/recomendacoes")
async def recomendacoes(request: RecommendationRequest) -> RecommendationResponse:
    # IA_DELAY_MS simula um modelo lento no roteiro de evidencia; o padrao e nao atrasar.
    await asyncio.sleep(int(os.getenv("IA_DELAY_MS", "0")) / 1000)
    return RecommendationResponse(
        studentId=request.studentId,
        courses=recommend(request.level, request.completedCourses),
        modelVersion=MODEL_VERSION,
    )
