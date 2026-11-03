"""Requisicoes concorrentes de XP no mesmo aluno. Uso: python concorrencia_xp.py [badges|nobadges]"""
import collections
import concurrent.futures as cf
import json
import sys
import urllib.error
import urllib.request

BASE = "http://localhost:18081"
HAS_BADGES = len(sys.argv) < 2 or sys.argv[1] == "badges"


def call(method, path, body=None):
    req = urllib.request.Request(BASE + path, method=method,
                                 data=json.dumps(body).encode() if body else None,
                                 headers={"Content-Type": "application/json"})
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return r.status, json.loads(r.read() or "null")
    except urllib.error.HTTPError as e:
        return e.code, None


totals = collections.Counter()
for round_no in range(10):
    sid = call("POST", "/students", {"name": f"Concorrente {round_no}"})[1]["id"]
    with cf.ThreadPoolExecutor(20) as pool:
        results = list(pool.map(lambda _: call("POST", f"/students/{sid}/xp", {"reason": "C", "amount": 50}), range(40)))
    totals.update(r[0] for r in results)
    final = call("GET", f"/students/{sid}")[1]
    line = f"rodada {round_no}: xp={final['xpTotal']}/2000 level_ups={len(final['levelUpEvents'])}"
    if HAS_BADGES:
        codes = [b["code"] for b in call("GET", f"/conquistas/students/{sid}")[1]]
        line += f" badges={len(codes)} duplicados={len(codes) - len(set(codes))}"
    print(line)
print("status de 400 POST:", dict(totals))
