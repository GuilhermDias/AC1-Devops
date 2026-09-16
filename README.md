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
- Testes: JUnit 5, AssertJ, Mockito, JaCoCo (0.8.13)

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
| Postgres | `localhost:5432` (banco `ac1devops_db`) | `postgres` / `postgres` |

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
├── domain/      Student, Level, LevelUpEvent — regra de negocio pura (POJOs, sem framework)
├── entity/      StudentEntity, LevelUpEventEntity — persistencia JPA (mapeiam o estado final)
├── repository/  StudentRepository (Spring Data JPA)
├── dto/         records de entrada/saida da API
├── service/     StudentService — ponte entre entity (persistencia) e domain (regra de negocio)
├── web/         StudentController + ApiExceptionHandler (REST)
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
