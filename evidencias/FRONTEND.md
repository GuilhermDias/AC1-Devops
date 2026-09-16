# Evidência — Front-end VueJS (US1–US4)

Front-end em `src/main/resources/static/index.html`: Vue 3 via CDN (sem build step, mesmo
padrão de um único arquivo autocontido), servido automaticamente pelo Spring Boot na raiz da
aplicação e empacotado dentro do jar — nenhuma mudança em `Dockerfile`/`docker-compose.yml` foi
necessária.

## Verificação executada

Aplicação rodando localmente (perfil `h2`, `./mvnw spring-boot:run -Dspring-boot.run.profiles=h2`),
front-end testado em navegador real (Chrome) navegando para `http://localhost:8080/`.

| Passo | Ação na UI | Requisição disparada | Resultado na tela |
|---|---|---|---|
| 1 | Preencher "Nome do aluno" com `Aluno Vue` e clicar em **Criar aluno** | `POST /students` → `201` | Mensagem "Aluno criado"; tabela de Alunos passa a listar `1 - Aluno Vue - 0 XP - BRONZE - —` |
| 2 | Selecionar o aluno `1`, motivo `ATIVIDADE_CONCLUIDA`, quantidade `120`, clicar em **Dar XP** | `POST /students/1/xp` → `200` | Mensagem "Aluno Vue agora tem 120 XP (PRATA)"; linha da tabela atualizada para `120 XP`, nível `PRATA`, histórico `BRONZE → PRATA` |
| 3 | Digitar `999` em "Id do aluno" e clicar em **Consultar** | `GET /students/999` → `404` | Mensagem de erro em vermelho: "Aluno não encontrado: 999" (corpo do erro do `ApiExceptionHandler` repassado direto pra tela) |

Esse fluxo exercita as 4 User Stories através da própria interface:
- **US1** (sobe de nível cruzando faixa) — passo 2, `BRONZE → PRATA` visível no histórico.
- **US2** (acumula sem mudar de nível) — reproduzível dando XP menor que a faixa seguinte; a
  tabela reflete o XP somado sem novo evento no histórico.
- **US3** (sem novo evento no nível máximo) — reproduzível dando XP a um aluno já em Diamante;
  o histórico não ganha linha nova, só o XP total sobe.
- **US4** (consulta de perfil completo) — a tabela de Alunos e a busca por id expõem XP total,
  nível atual e histórico de level-ups de uma vez, direto do `GET /students` e `GET /students/{id}`.

## Cobertura automatizada

Não há testes automatizados de front-end (Selenium/Cypress/Playwright) nesta entrega — a
verificação acima foi manual, em navegador real. A garantia de comportamento correto continua
vindo dos testes de domínio/service/controller já existentes (100% de cobertura), que exercitam
exatamente os mesmos endpoints que o front-end consome.

## Bancos de dados: H2 (testes) e Postgres (runtime)

- **H2**: usado só internamente pela suíte de testes (`src/test/resources/application.properties`
  força o perfil `h2`); `./mvnw test` roda 100% offline, sem Docker. Ver
  [`cobertura-jacoco.md`](cobertura-jacoco.md).
- **Postgres**: banco de execução real, via `docker compose up` — ver
  [`DOCKER.md`](DOCKER.md)/[`docker-output.txt`](docker-output.txt) para a evidência completa de
  schema, persistência entre `down`/`up` e endpoints funcionando sobre o container.
