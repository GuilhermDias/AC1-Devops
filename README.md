# AC1-DevOps — Educação Continuada Gamificada (Grupo 10)

Case: plataforma de educação continuada gamificada, onde alunos acumulam XP ao concluir
atividades e sobem de nível conforme faixas de XP pré-definidas.

## User Stories (US1–US4)

| US | Descrição |
|---|---|
| US1 | Aluno sobe de nível quando o XP acumulado cruza a faixa do próximo nível. |
| US2 | Aluno acumula XP sem mudar de nível quando permanece na mesma faixa. |
| US3 | Aluno no nível máximo (Diamante) continua acumulando XP sem gerar novos eventos de level-up. |
| US4 | Consulta de perfil retorna XP total, nível atual e histórico de level-ups do aluno. |

Tabela de faixas de XP:

| Nível | XP necessário |
|---|---|
| BRONZE | 0 – 99 |
| PRATA | 100 – 299 |
| OURO | 300 – 699 |
| DIAMANTE | 700+ |

## Stack

- Java 21, Spring Boot 4.1.1 (Maven, artifactId `Grupo10_ATDD`)
- Spring Data JPA + H2 (banco em memória — migração para Postgres via Docker é a próxima etapa,
  fora deste escopo)
- Springdoc OpenAPI / Swagger UI
- Testes: JUnit 5, AssertJ, Mockito, JaCoCo (0.8.13)

## Como rodar

```bash
./mvnw spring-boot:run
```

Aplicação sobe em `http://localhost:8080`. Documentação interativa da API em
`http://localhost:8080/swagger-ui/index.html` (spec OpenAPI em `/v3/api-docs`).

## Como testar

```bash
./mvnw test
```

Relatório de cobertura JaCoCo gerado em `target/site/jacoco/index.html` a cada execução.

## Arquitetura

```
src/main/java/org/example/ac1devops/
├── domain/      Student, Level, LevelUpEvent — regra de negocio pura (POJOs, sem framework)
├── entity/      StudentEntity, LevelUpEventEntity — persistencia JPA (mapeiam o estado final)
├── repository/  StudentRepository (Spring Data JPA)
├── dto/         records de entrada/saida da API
├── service/     StudentService — ponte entre entity (persistencia) e domain (regra de negocio)
├── web/         StudentController + ApiExceptionHandler (REST)
└── config/      OpenApiConfig (Swagger/OpenAPI)
```

**Por que domínio e entity são classes separadas:** `Student`/`Level`/`LevelUpEvent` são POJOs
puros, testados isoladamente desde o ciclo RED→GREEN→BLUE (ver abaixo), sem nenhuma anotação de
framework. `StudentEntity`/`LevelUpEventEntity` só guardam o estado final (xpTotal, level,
histórico) para persistência — quem *calcula* uma transição de nível continua sendo
exclusivamente o domínio. O `StudentService` reconstrói um `Student` de domínio a partir do XP
total já salvo (um único `receiveXp` com o total acumulado recria a mesma sequência de eventos,
já que a regra depende só do total de XP, não do caminho percorrido), aplica a nova operação, e
persiste de volta só os eventos novos.

## Endpoints

| Método | Caminho | Descrição | Respostas |
|---|---|---|---|
| `POST` | `/students` | Cria um aluno (`{"name": "..."}`) | `201` |
| `POST` | `/students/{id}/xp` | Concede XP (`{"reason": "...", "amount": N}`) | `200`, `404` |
| `GET` | `/students/{id}` | Consulta um aluno por id | `200`, `404` |
| `GET` | `/students` | Lista todos os alunos | `200` |

XP inválido (`amount <= 0`) é ignorado silenciosamente pelo domínio (comportamento testado no
BLUE) — a API não rejeita com `400`, apenas repassa o estado inalterado.

## Ciclo RED → GREEN → BLUE

O domínio (US1–US4) foi desenvolvido em TDD completo, documentado em
[`FEATURE_US3_RED_GREEN_BLUE.md`](FEATURE_US3_RED_GREEN_BLUE.md) e nas evidências em
[`evidencias/`](evidencias/):

- **RED**: [`evidencias/red-output.txt`](evidencias/red-output.txt)
- **GREEN**: [`evidencias/GREEN.md`](evidencias/GREEN.md), [`evidencias/green-output.txt`](evidencias/green-output.txt)
- **BLUE**: [`evidencias/BLUE.md`](evidencias/BLUE.md), [`evidencias/blue-output.txt`](evidencias/blue-output.txt)
- **Cobertura completa**: [`evidencias/cobertura-jacoco.md`](evidencias/cobertura-jacoco.md),
  relatório navegável em [`evidencias/jacoco-report/index.html`](evidencias/jacoco-report/index.html)

## Cobertura final

100% de instruções, branches, linhas e métodos em todas as classes do projeto (domínio, entity,
repository, service, controller, DTOs, config) — `Ac1DevOpsApplication` (bootstrap do Spring
Boot) é excluído da métrica por não ser código de negócio. Detalhes em
[`evidencias/cobertura-jacoco.md`](evidencias/cobertura-jacoco.md).

## Próximos passos (fora deste escopo)

Migração do H2 em memória para Postgres via Docker — a ser feita em uma próxima etapa por outro
integrante do grupo.

## Créditos

Grupo 10 — AC1-ATDD. Contribuições via PRs de `viniciusalegre20` (GREEN das US1–US4),
`bruno-cO70` (BLUE do domínio) e `GuilhermDias` (sincronização, Swagger, camadas REST e
consolidação da documentação).
