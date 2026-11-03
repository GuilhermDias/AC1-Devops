"""Carga no fluxo de conclusao: mede a resposta e quanto a entrega demora depois.

Uso: python carga_conclusoes.py [alunos] [threads] [url-do-core]
"""
import concurrent.futures as cf
import json
import statistics
import sys
import time
import urllib.error
import urllib.request

STUDENTS = int(sys.argv[1]) if len(sys.argv) > 1 else 100
THREADS = int(sys.argv[2]) if len(sys.argv) > 2 else 20
BASE = sys.argv[3] if len(sys.argv) > 3 else "http://localhost:8080"
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
print(f"status: {statuses}")
print(f"resposta: mediana {statistics.median(times):.0f} ms, p95 {times[int(len(times) * 0.95)]:.0f} ms")

while True:
    pending = [e for e in call("GET", "/cursos/eventos")[1] if e["status"] == "PENDING"]
    if not pending:
        break
    time.sleep(0.2)
print(f"ultimos 100 eventos entregues {time.perf_counter() - start:.1f} s depois do inicio")
delivered = sum(len(call("GET", f"/notificacoes/students/{s}")[1]) for s in students)
print(f"notificacoes: {delivered} de {len(jobs)}")
