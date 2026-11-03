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
- Spring Data JPA + **PostgreSQL 16 em container Docker** (perfil padrão `postgres`)
- Docker: build multi-stage da aplicação + `docker compose` com Postgres, pgAdmin e a API
- Front-end: Vue 3 (via CDN, arquivo único sem build step), servido como recurso estático do
  próprio Spring Boot
- Springdoc OpenAPI / Swagger UI
- Testes: JUnit 5, AssertJ, Mockito, ArchUnit (1.4.1), JaCoCo (0.8.13)
- CI: GitHub Actions (`.github/workflows/ci.yml`) e Jenkins ([`Jenkinsfile`](Jenkinsfile))
- Observabilidade: Actuator, Micrometer, Prometheus e Grafana ([`observabilidade/`](observabilidade/))

## Como rodar

### Com Docker (forma padrão — sobe API + Postgres + pgAdmin)

```bash
docker compose up -d --build
```

| Serviço | Endereço | Credenciais |
|---|---|---|
| Front-end | `http://localhost:8080/` | — |
| API | `http://localhost:8080` | — |
| Swagger UI | `http://localhost:8080/swagger-ui/index.html` | — |
| pgAdmin | `http://localhost:5050` | `admin@admin.com` / `admin` |
| Postgres | `localhost:5433` (banco `ac1devops_db`) | `postgres` / `postgres` |
| Serviço de IA | `http://localhost:8000` (`/health`, `/docs`) | — |
| Broker MQTT (Mosquitto) | `localhost:1883` | Sem autenticação: só para o laboratório |
| Métricas do Core | `http://localhost:8080/actuator/prometheus` | — |
| Prometheus e Grafana (`docker compose -f observabilidade/docker-compose.yml up -d`) | `http://localhost:9091` e `http://localhost:3000/d/ac1devops` | Grafana: leitura sem login |

Para conectar no banco pelo pgAdmin, cadastre um servidor apontando para o host **`postgres`**
(nome do serviço na rede do compose), porta `5432` — não `localhost`, que dentro do container
do pgAdmin se refere a ele mesmo.

Encerrar a stack:

```bash
docker compose down          # para os containers, preserva os dados
docker compose down -v       # remove também os volumes (zera o banco)
```

## Como testar

```bash
./mvnw test
```

Os testes **não dependem de Docker nem de um Postgres no ar**: rodam sempre contra um banco em
memória, configurado automaticamente no classpath de teste. Relatório de cobertura JaCoCo gerado
em `target/site/jacoco/index.html` a cada execução.

## Arquitetura

```
src/main/java/org/example/ac1devops/
├── progression/                 modulo de progressao (Monolito Modular, ver ADR-001)
│   ├── api/                     PUBLICO: StudentProgressionApi (fachada), DTOs, Level,
│   │                            StudentNotFoundException
│   └── internal/                PRIVADO ao modulo (verificado por ArchUnit)
│       ├── StudentService       implementa StudentProgressionApi; ponte entity <-> domain
│       ├── domain/              Student, LevelUpEvent — regra de negocio pura (POJOs)
│       └── persistence/         StudentEntity, LevelUpEventEntity, StudentRepository (JPA)
├── web/         StudentController + ApiExceptionHandler (REST) — usam so progression.api
└── config/      OpenApiConfig (Swagger/OpenAPI)

src/main/resources/static/
└── index.html   front-end Vue 3 (CDN, arquivo único) — consome os endpoints de /students

Infraestrutura (raiz do projeto):
├── Dockerfile          build multi-stage: Maven/JDK 21 compila, imagem final so com JRE + jar
├── .dockerignore       mantem target/, .git e evidencias fora do contexto de build
└── docker-compose.yml  orquestra postgres + pgadmin + app na rede 'app_net'

Perfis (src/main/resources):
├── application.properties           config comum + perfil padrao (postgres)
└── application-postgres.properties  datasource do container, via variaveis de ambiente
```

**Por que domínio e entity são classes separadas:** `Student`/`Level`/`LevelUpEvent` são POJOs
puros, testados isoladamente desde o ciclo RED→GREEN→BLUE (ver abaixo), sem nenhuma anotação de
framework. `StudentEntity`/`LevelUpEventEntity` só guardam o estado final (xpTotal, level,
histórico) para persistência — quem *calcula* uma transição de nível continua sendo
exclusivamente o domínio. O `StudentService` reconstrói um `Student` de domínio a partir do XP
total já salvo (um único `receiveXp` com o total acumulado recria a mesma sequência de eventos,
já que a regra depende só do total de XP, não do caminho percorrido), aplica a nova operação, e
persiste de volta só os eventos novos.

**Arquitetura — [AC2, Exercício B1: Arquitetura de Software e Tomada de Decisão Tech Lead](docs/arquitetura/README.md)** (DevOps e QA, Facens, Grupo 10, entrega em 04/11/2026): os 6 desafios com RF, RNF, ASR, RPC, trade-offs,
ADRs (em [`docs/arquitetura/adr/`](docs/arquitetura/adr/)), C4 e evidências, além da matriz de ASRs e da síntese para a PoC da AF. O CI
([`.github/workflows/ci.yml`](.github/workflows/ci.yml)) roda `./mvnw verify` (testes, ArchUnit e
piso de cobertura JaCoCo) em todo push e Pull Request.

## Endpoints

| Método | Caminho | Descrição | Respostas |
|---|---|---|---|
| `POST` | `/students` | Cria um aluno (`{"name": "..."}`) | `201` |
| `POST` | `/students/{id}/xp` | Concede XP (`{"reason": "...", "amount": N}`) | `200`, `404`, `409` |
| `GET` | `/students/{id}` | Consulta um aluno por id | `200`, `404` |
| `GET` | `/students` | Lista todos os alunos | `200` |
| `GET` | `/ranking?page&size` | Ranking paginado, por XP | `200`, `400` |
| `GET` | `/ranking/top?n` | Os `n` primeiros colocados | `200`, `400` |
| `GET` | `/ranking/students/{id}` | Posição de um aluno | `200`, `404` |
| `GET` | `/conquistas/students/{id}` | Badges do aluno | `200`, `404` |
| `POST` | `/cursos/{courseId}/alunos/{studentId}/conclusao` | Conclui o curso e concede a recompensa | `201`, `200` se já concluído, `404`, `409` |
| `GET` | `/cursos/eventos` | Estado da entrega dos eventos de conclusão mais recentes | `200` |
| `POST` | `/cursos/eventos/{id}/reenvio` | Devolve para a fila um evento com tentativas esgotadas | `202`, `404` |
| `GET` | `/notificacoes/students/{id}` | Notificações enviadas ao aluno | `200`, `404` |
| `GET` | `/iot/eventos` | Eventos recebidos dos dispositivos e o resultado do processamento | `200` |
| `GET` | `/recomendacoes/students/{id}` | Recomendações de cursos; `degraded: true` indica a lista de populares, usada quando o serviço de IA não responde | `200`, `404` |

Serviço de IA, na porta 8000 ([`labs/desafio-3/ia-service`](labs/desafio-3/ia-service/)):

| Método | Caminho | Descrição |
|---|---|---|
| `GET` | `/health` | Saúde do serviço |
| `POST` | `/recomendacoes` | Recomendação por regra determinística |
| `POST` | `/assistente` | Assistente com resposta simulada |

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

Evidência da containerização e da migração para Postgres (fora do ciclo TDD, é etapa de
infraestrutura): [`evidencias/DOCKER.md`](evidencias/DOCKER.md) e
[`evidencias/docker-output.txt`](evidencias/docker-output.txt).

Evidência do front-end exercitando as US1–US4 pela interface (criar aluno, dar XP cruzando
nível, consultar aluno e tratamento de 404): [`evidencias/FRONTEND.md`](evidencias/FRONTEND.md).

## Cobertura final

100% de instruções, branches, linhas e métodos em todas as classes do projeto (domínio, entity,
repository, service, controller, DTOs, config) — `Ac1DevOpsApplication` (bootstrap do Spring
Boot) é excluído da métrica por não ser código de negócio. Detalhes em
[`evidencias/cobertura-jacoco.md`](evidencias/cobertura-jacoco.md).

## Créditos

Grupo 10 — AC1-ATDD. Contribuições via PRs de `viniciusalegre20` (GREEN das US1–US4 e
containerização com Docker + migração para PostgreSQL), `bruno-cO70` (BLUE do domínio),
`LeovLuz` (Swagger/OpenAPI) e `GuilhermDias` (sincronização, camadas REST, front-end e
consolidação da documentação).
