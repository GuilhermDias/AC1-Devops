# Cobertura JaCoCo — fase GREEN

| Classe | Instruções | Branches | Linhas não cobertas | Status |
|---|---|---|---|---|
| `Ac1DevOpsApplication` | 38% | n/a | 2 de 3 | VERMELHO |
| `Student` | 96% | 100% | 1 de 18 | VERDE |
| `Level` | 96% | 100% | 1 de 14 | VERDE |
| `LevelUpEvent` | 80% | n/a | 1 de 6 | AMARELO |
| **TOTAL** | **91%** | **100%** | **5 de 41** | |

## O que está amarelo/vermelho e por quê

| Ponto não coberto | Motivo |
|---|---|
| `Ac1DevOpsApplication.main()` (2 linhas) | O `@SpringBootTest` sobe o contexto, mas nunca chama `main()`. Fora do escopo da US3 (não é código de domínio). |
| `LevelUpEvent.getFromLevel()` (1 linha) | Os testes RED só assertam `getToLevel()`. O getter existe para o histórico completo, mas ainda não é exercido. |
| `Student.getName()` (1 linha) | Nenhum cenário das US1–US4 consulta o nome do aluno. |
| `Level.getMinXp()` (1 linha) | A faixa é lida internamente por `fromXp()`; o getter público não é chamado pelos testes. |

Branches: **100%** — todas as ramificações de `Level.fromXp()` e do laço de promoção de
`Student.receiveXp()` são exercidas pelos 4 cenários.

Relatório HTML navegável (para print): `evidencias/jacoco-report/index.html`
