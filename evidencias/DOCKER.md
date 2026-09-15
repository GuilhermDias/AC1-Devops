# Evidência — Containerização e migração para PostgreSQL

Etapa que o README listava como "próximo passo": sair do H2 em memória e passar a rodar a
aplicação e o banco em containers. Saída bruta da execução em
[`docker-output.txt`](docker-output.txt).

## O que foi entregue

| Arquivo | Papel |
|---|---|
| `Dockerfile` | Build multi-stage: etapa Maven/JDK 21 compila o jar, imagem final leva só JRE 21 + `app.jar` |
| `.dockerignore` | Mantém `target/`, `.git`, `.idea` e `evidencias/` fora do contexto de build |
| `docker-compose.yml` | Orquestra `postgres` + `pgadmin` + `app` na rede `app_net` |
| `application-postgres.properties` | Datasource lido de `SPRING_DATASOURCE_*` (perfil padrão) |
| `application-h2.properties` | Banco em memória para dev local sem Docker |
| `src/test/resources/application.properties` | Fixa o perfil `h2` nos testes |

## Verificações executadas

1. **Aplicação sobe no perfil `postgres` e conecta no container do banco**
   — `The following 1 profile is active: "postgres"`, `Database JDBC URL
   [jdbc:postgresql://postgres:5432/ac1devops_db]`, `Database version: 16.15`.

2. **Ordem de inicialização respeitada** — o `depends_on` com `condition: service_healthy`
   segurou a aplicação até o `pg_isready` do Postgres passar (`Container ac1devops-postgres
   Healthy` antes de `Container ac1devops-app Starting`).

3. **Schema gerado a partir das entities** — tabelas `students` e `level_up_events`, com os
   nomes de coluna explícitos (`xp_total`, `from_level`, `to_level`, `student_id`), FK de
   `level_up_events` para `students` e check constraint do enum de nível.

4. **Regras de negócio funcionando sobre Postgres** — as US1–US4 exercitadas via API:

   | Requisição | Resultado |
   |---|---|
   | `POST /students` | `{"id":1,...,"xpTotal":0,"level":"BRONZE","levelUpEvents":[]}` |
   | `POST /students/1/xp` (120) | `level: PRATA`, 1 evento `BRONZE→PRATA` |
   | `POST /students/1/xp` (+200 = 320) | `level: OURO`, 2 eventos `BRONZE→PRATA`, `PRATA→OURO` |
   | `GET /students/1` | mesmo estado, relido do banco |
   | `GET /students/999` | `HTTP 404` |

5. **Persistência real (o ponto da migração)** — após `docker compose down` e novo
   `docker compose up -d`, `GET /students` devolveu o aluno com 320 XP, nível OURO e os dois
   eventos de level-up. Com H2 em memória esse estado se perdia a cada restart.

6. **Swagger e pgAdmin no ar** — `/swagger-ui/index.html` → `HTTP 200`,
   `/v3/api-docs` → `HTTP 200`, pgAdmin → `HTTP 302` (redirect para a tela de login).

7. **Suíte de testes sem Docker** — `./mvnw test` segue verde (26/26) rodando em H2, sem
   depender de container nenhum.

## Observação sobre a captura

Nesta captura as portas publicadas foram remapeadas (app `18080`, postgres `15432`, pgadmin
`15050`) porque outra stack Docker ocupava `8080`, `5432` e `5050` na máquina. O
`docker-compose.yml` versionado usa as portas padrão `8080`, `5432` e `5050`.
