"""Carga no ranking: mistura top 100, paginas aleatorias e posicao de alunos, e mede a latencia no cliente.

Uso: py carga_ranking.py [segundos] [threads]
"""
import concurrent.futures as cf
import json
import random
import statistics
import sys
import time
import urllib.error
import urllib.request

SEGUNDOS = int(sys.argv[1]) if len(sys.argv) > 1 else 30
THREADS = int(sys.argv[2]) if len(sys.argv) > 2 else 10
BASE = "http://localhost:8080"


def chamar(caminho):
    inicio = time.perf_counter()
    try:
        with urllib.request.urlopen(BASE + caminho, timeout=30) as resposta:
            corpo = resposta.read()
            return resposta.status, (time.perf_counter() - inicio) * 1000, corpo
    except urllib.error.HTTPError as erro:
        return erro.code, (time.perf_counter() - inicio) * 1000, b""


def p95(valores):
    ordenados = sorted(valores)
    return ordenados[int(len(ordenados) * 0.95)]


status, _, corpo = chamar("/ranking?page=0&size=100")
dados = json.loads(corpo)
total_paginas = max(1, dados["totalElements"] // 20)
ids = [entrada["studentId"] for entrada in dados["entries"]]
print(f"alunos no ranking: {dados['totalElements']}")


def trabalhador(_):
    top, pagina, posicao, erros = [], [], [], 0
    limite = time.monotonic() + SEGUNDOS
    while time.monotonic() < limite:
        s0, ms0, _ = chamar("/ranking?page=0&size=100")
        s1, ms1, _ = chamar(f"/ranking?page={random.randrange(total_paginas)}&size=20")
        s2, ms2, _ = chamar(f"/ranking/students/{random.choice(ids)}")
        top.append(ms0)
        pagina.append(ms1)
        posicao.append(ms2)
        erros += (s0 >= 500) + (s1 >= 500) + (s2 >= 500)
    return top, pagina, posicao, erros


with cf.ThreadPoolExecutor(THREADS) as pool:
    resultados = list(pool.map(trabalhador, range(THREADS)))

tops = [ms for r in resultados for ms in r[0]]
paginas = [ms for r in resultados for ms in r[1]]
posicoes = [ms for r in resultados for ms in r[2]]
erros = sum(r[3] for r in resultados)
print(f"{len(tops) + len(paginas) + len(posicoes)} requisicoes em {SEGUNDOS} s, {THREADS} threads, {erros} erros 5xx")
print(f"top 100: mediana {statistics.median(tops):.0f} ms, p95 {p95(tops):.0f} ms")
print(f"pagina aleatoria: mediana {statistics.median(paginas):.0f} ms, p95 {p95(paginas):.0f} ms")
print(f"posicao: mediana {statistics.median(posicoes):.0f} ms, p95 {p95(posicoes):.0f} ms")
