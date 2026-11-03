import json
from pathlib import Path

import pytest
from fastapi.testclient import TestClient

from app.main import app

CONTRACT = Path(__file__).parent.parent / "contract"
client = TestClient(app)


def course_ids(body):
    return [course["courseId"] for course in body["courses"]]


def test_health():
    response = client.get("/health")

    assert response.status_code == 200
    assert response.json() == {"status": "ok", "modelVersion": "regras-v1"}


def test_resposta_segue_o_contrato_usado_pelos_testes_do_core():
    request = json.loads((CONTRACT / "recomendacoes-requisicao.json").read_text(encoding="utf-8"))
    expected = json.loads((CONTRACT / "recomendacoes-resposta.json").read_text(encoding="utf-8"))

    response = client.post("/recomendacoes", json=request)

    assert response.status_code == 200
    assert response.json() == expected


def test_recomendacao_e_deterministica():
    request = {"studentId": 1, "level": "OURO"}

    first = client.post("/recomendacoes", json=request).json()
    second = client.post("/recomendacoes", json=request).json()

    assert first == second


def test_comeca_pelo_nivel_do_aluno():
    body = client.post("/recomendacoes", json={"studentId": 1, "level": "BRONZE"}).json()

    assert course_ids(body) == ["fundamentos-git", "logica-programacao", "testes-unitarios"]


def test_nao_repete_curso_concluido():
    body = client.post("/recomendacoes", json={
        "studentId": 1,
        "level": "BRONZE",
        "completedCourses": ["fundamentos-git"],
    }).json()

    assert "fundamentos-git" not in course_ids(body)
    assert course_ids(body) == ["logica-programacao", "testes-unitarios", "docker-essencial"]


def test_devolve_lista_vazia_sem_cursos_disponiveis():
    body = client.post("/recomendacoes", json={
        "studentId": 1,
        "level": "DIAMANTE",
        "completedCourses": ["observabilidade", "sistemas-distribuidos"],
    }).json()

    assert body["courses"] == []


@pytest.mark.parametrize("payload", [
    {"level": "PRATA"},
    {"studentId": 1},
    {"studentId": 1, "level": "PLATINA"},
])
def test_recusa_requisicao_invalida(payload):
    assert client.post("/recomendacoes", json=payload).status_code == 422


def test_atraso_simulado_e_aplicado(monkeypatch):
    monkeypatch.setenv("IA_DELAY_MS", "1")

    assert client.post("/recomendacoes", json={"studentId": 1, "level": "PRATA"}).status_code == 200


def test_assistente_responde_de_forma_simulada():
    response = client.post("/assistente", json={"question": "O que e CI?"})

    assert response.status_code == 200
    assert response.json() == {
        "answer": "Resposta simulada para: O que e CI?",
        "simulated": True,
        "sources": [],
    }


def test_assistente_recusa_pergunta_vazia():
    assert client.post("/assistente", json={"question": ""}).status_code == 422
