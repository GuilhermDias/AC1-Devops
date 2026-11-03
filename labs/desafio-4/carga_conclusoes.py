"""Carga no fluxo de conclusao: mede a resposta e quanto a entrega demora depois.

Uso: python carga_conclusoes.py [alunos] [threads] [--sem-espera]
Com --sem-espera, nao aguarda a entrega: serve para medir a resposta com o canal fora do ar.
"""
import concurrent.futures as cf
import json
import statistics
import sys
import time
import urllib.error
import urllib.request

ARGS = [arg for arg in sys.argv[1:] if not arg.startswith("--")]
STUDENTS = int(ARGS[0]) if len(ARGS) > 0 else 100
THREADS = int(ARGS[1]) if len(ARGS) > 1 else 20
WAIT = "--sem-espera" not in sys.argv
BASE = "http://localhost:8080"
COURSES = ["fundamentos-git", "logica-programacao", "testes-unitarios", "docker-essencial",
           "ci-cd", "arquitetura-software", "observabilidade", "sistemas-distribuidos"]


def call(method, path, body=None):
    request = urllib.request.Request(BASE + path, method=method, data=body,
                                     headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(request, timeout=60) as response:
            return response.status, json.loads(response.read() or "null")
    except urllib.error.HTTPError as error:
        return error.code, None
    except (TimeoutError, urllib.error.URLError):
        return 0, None


def complete(job):
    student, course = job
    start = time.perf_counter()
    status, _ = call("POST", f"/cursos/{course}/alunos/{student}/conclusao")
    return status, (time.perf_counter() - start) * 1000


students = [call("POST", "/students", json.dumps({"name": f"Carga {i}"}).encode())[1]["id"] for i in range(STUDENTS)]
jobs = [(student, course) for student in students for course in COURSES]

start = time.perf_counter()
with cf.ThreadPoolExecutor(THREADS) as pool:
    results = list(pool.map(complete, jobs))
elapsed = time.perf_counter() - start
times = sorted(ms for _, ms in results)
statuses = {}
for status, _ in results:
    statuses[status] = statuses.get(status, 0) + 1

print(f"{len(jobs)} conclusoes em {elapsed:.1f} s ({len(jobs) / elapsed:.0f}/s), {THREADS} threads")
print(f"status (0 = sem resposta em 60 s): {statuses}")
print(f"resposta: mediana {statistics.median(times):.0f} ms, p95 {times[int(len(times) * 0.95)]:.0f} ms")

if WAIT:
    deadline = time.perf_counter() + 180
    delivered = 0
    while delivered < len(jobs) and time.perf_counter() < deadline:
        delivered = sum(len(call("GET", f"/notificacoes/students/{s}")[1]) for s in students)
    print(f"notificacoes: {delivered} de {len(jobs)}, {time.perf_counter() - start:.1f} s depois do inicio")
