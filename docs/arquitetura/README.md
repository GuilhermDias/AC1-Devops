# Estudo de Caso: Educação Continuada Gamificada

Registro único e cumulativo do **Exercício B1 — Architecture Lab (DevOps e QA)**, sobre o Core da
plataforma de Educação Continuada Gamificada (AC1, Grupo 10).

## Contexto e case-base

**Cenário:** o MVP cresceu de **1.000 para até 100.000 usuários**; o **Ranking** cresce mais que
as outras funcionalidades; cursos, conquistas e recompensas continuam evoluindo.

**Base existente:** Java 21, Spring Boot 4.1, PostgreSQL 16, front-end Vue 3 servido pelo próprio
Core, Docker Compose e CI no GitHub Actions. Detalhes e instruções de execução no
[README do projeto](../../README.md).

> **Legenda de estado:** **Existe** = implementado e testado nesta branch. **Planejado** = desenho-alvo
> das próximas etapas; nenhum código correspondente existe ainda.

## Padrão de decisão

Cadeia de rastreabilidade seguida neste documento:

```
RF → RNF → ASR → RPC (trade-offs) → ADR → C4 → evidência no código
```

Cada desafio segue exatamente esta sequência:

1. **RF**
2. **RNF**, classificado por FURPS+
3. **ASR**, no máximo 3, cada um com uma frase dizendo como altera estrutura, comunicação, dados,
   implantação ou operação
4. **RPC**
5. **Decisão do Tech Lead**, respondendo às perguntas do desafio
6. **Alternativas** (mínimo 2) e trade-offs em tabela
7. **ADR**, em [`adr/`](adr/)
8. **C4**, atualizando só o nível afetado, em Mermaid
9. **Evidência**: mini-lab, teste, métrica ou falha simulada, com como reproduzir e o resultado obtido

Critérios aplicados em todas as decisões:

- Toda tecnologia responde a um problema do case; "é moderno" ou "o mercado usa" não são justificativa.
- Nem todo RNF é ASR.
- RPC é restrição, não solução.
- A microsolução testa uma hipótese arquitetural, não apenas o funcionamento da ferramenta.
- Prefere-se a solução mais simples que atenda aos ASRs.

Microsoluções ficam em `/labs/desafio-N/` ou no próprio código do Core, sempre linkadas aqui. Prints
ficam em [`evidencias/img/`](evidencias/img/), com o prefixo do desafio (`d1-`, `d2-`...).

## Matriz cumulativa de ASRs

A numeração é global: novos desafios continuam a partir da ASR12.

| ASR | Desafio de origem | RNF / FURPS+ | Decisão / ADR | Evidência | Status |
|---|---|---|---|---|---|
| **ASR1** — Evoluir funcionalidades com baixo acoplamento | Desafio 1 | Manutenibilidade / S | Monólito Modular, [ADR-001](adr/ADR-001-estilo-arquitetural-core.md) | [`ModularArchitectureTest`](../../src/test/java/org/example/ac1devops/architecture/ModularArchitectureTest.java); mini-lab (item 9.8) | **Atendido** |
| **ASR2** — Ranking com p95 ≤ 300 ms para 100.000 usuários | Desafio 1 | Desempenho e Escalabilidade / P | Índice, paginação, cache e gatilhos G1–G5, [ADR-001](adr/ADR-001-estilo-arquitetural-core.md) | [`RankingPerformanceTest`](../../src/test/java/org/example/ac1devops/ranking/internal/RankingPerformanceTest.java) (item 9.4) | **Parcial:** medido só em H2, sem carga e sem p95; observabilidade não implementada |
| **ASR3** — Fronteiras verificáveis automaticamente | Desafio 1 | Testabilidade / S | ArchUnit no `verify` e no CI, [ADR-001](adr/ADR-001-estilo-arquitetural-core.md) | Falha simulada em [`archunit-violacao.txt`](evidencias/archunit-violacao.txt); [`ci.yml`](../../.github/workflows/ci.yml); prints do CI: [runs](evidencias/img/d1-ci-verde.png), [resumo](evidencias/img/d1-ci-resumo.png), [etapas](evidencias/img/d1-ci-etapas.png) | **Atendido (CI no GitHub)** |
| **ASR4** — Mudar regra de gamificação sem tocar em persistência ou framework | Desafio 2 | Modificabilidade / S | Ports & Adapters só em `conquistas`, [ADR-002](adr/ADR-002-ports-and-adapters-em-conquistas.md) | [`BadgeAwardContract`](../../src/test/java/org/example/ac1devops/conquistas/internal/domain/BadgeAwardContract.java) nos dois adapters; [mutação da regra](evidencias/d2-mutacao-regra.txt); CI no GitHub, run #2: [lista de runs](evidencias/img/d2-ci-verde.png), [etapas do job](evidencias/img/d2-ci-etapas.png) | **Atendido** |
| **ASR5** — Regra de gamificação testável sem Spring nem banco | Desafio 2 | Testabilidade / S | Domínio puro com adapter em memória; ArchUnit na fronteira, [ADR-002](adr/ADR-002-ports-and-adapters-em-conquistas.md) | [`BadgeAwardInMemoryTest`](../../src/test/java/org/example/ac1devops/conquistas/internal/domain/BadgeAwardInMemoryTest.java); [violação barrada](evidencias/d2-archunit-violacao.txt); CI no GitHub, run #2: [lista de runs](evidencias/img/d2-ci-verde.png), [etapas do job](evidencias/img/d2-ci-etapas.png) | **Atendido** |
| **ASR6** — Indisponibilidade da IA não bloqueia o Core | Desafio 3 | Tolerância a falhas e Desempenho / R, P | Timeout, circuit breaker e fallback, [ADR-003](adr/ADR-003-servico-python-de-ia.md) | [`RecomendacaoEndpointIntegrationTest`](../../src/test/java/org/example/ac1devops/web/RecomendacaoEndpointIntegrationTest.java); [roteiro](evidencias/d3-roteiro.txt) e prints ([no ar](evidencias/img/d3-02-core-com-ia.png), [fora do ar](evidencias/img/d3-03-core-ia-fora.png), [fluxo essencial](evidencias/img/d3-04-fluxo-essencial.png)); [falhas simuladas](evidencias/d3-falhas-simuladas.txt) | **Atendido** |
| **ASR7** — IA publicada e escalada sem o Core | Desafio 3 | Deploy independente e Escalabilidade / +, P | Serviço Python independente, [ADR-003](adr/ADR-003-servico-python-de-ia.md) | [`Dockerfile`](../../labs/desafio-3/ia-service/Dockerfile); job `ia-service` no [`ci.yml`](../../.github/workflows/ci.yml) | **Parcial:** imagem própria demonstrada e serviço parado sem parar o Core ([print](evidencias/img/d3-03-core-ia-fora.png)); job `ia-service` verde no GitHub ([run #4](evidencias/img/d3-06-ci-dois-jobs.png), [pytest](evidencias/img/d3-07-ci-pytest.png)); **escala sem teste de carga** |
| **ASR8** — Contrato HTTP/JSON verificável pelos dois lados | Desafio 3 | Interoperabilidade / + | Contrato versionado, [ADR-003](adr/ADR-003-servico-python-de-ia.md) | [`contract/`](../../labs/desafio-3/ia-service/contract/), usado por [`test_api.py`](../../labs/desafio-3/ia-service/tests/test_api.py) e [`IaRecommendationAdapterTest`](../../src/test/java/org/example/ac1devops/recomendacao/internal/adapter/http/IaRecommendationAdapterTest.java) | **Atendido** |
| **ASR9** — Conclusão independente dos consumidores | Desafio 4 | Tolerância a falhas, Disponibilidade, Latência e Desacoplamento / R, P, S | Consumidor fora da transação, depois do commit, [ADR-004](adr/ADR-004-eventos-com-outbox-transacional.md) | [`CourseCompletionIntegrationTest`](../../src/test/java/org/example/ac1devops/web/CourseCompletionIntegrationTest.java); [falhas simuladas](evidencias/d4-falhas-simuladas.txt); [PostgreSQL](evidencias/d4-postgres.txt); prints ([conclusão com o canal fora](evidencias/img/d4-01-conclusao-canal-fora.png), [evento pendente](evidencias/img/d4-02-evento-pendente.png), [CI](evidencias/img/d4-06-ci-verde.png)) | **Atendido** |
| **ASR10** — Conclusão e recompensa atômicas e exatamente uma vez | Desafio 4 | Consistência / R | Transação única, `@Version` e restrição única, [ADR-004](adr/ADR-004-eventos-com-outbox-transacional.md) | [`CourseCompletionIntegrationTest`](../../src/test/java/org/example/ac1devops/web/CourseCompletionIntegrationTest.java); [PostgreSQL](evidencias/d4-postgres.txt): 0 alunos com XP divergente; [repetição sem novo XP](evidencias/img/d4-04-recuperado.png) | **Atendido** |
| **ASR11** — Nenhum evento confirmado se perde | Desafio 4 | Recuperabilidade / R | Outbox transacional, novas tentativas e reenvio manual, [ADR-004](adr/ADR-004-eventos-com-outbox-transacional.md) | [`CourseCompletionIntegrationTest`](../../src/test/java/org/example/ac1devops/web/CourseCompletionIntegrationTest.java); [PostgreSQL](evidencias/d4-postgres.txt): queda forçada e 1.601 de 1.601 notificações; prints ([queda](evidencias/img/d4-03-queda-processo.png), [recuperação](evidencias/img/d4-04-recuperado.png)) | **Atendido** em uma réplica; várias réplicas sem teste |

---

## Desafio 1 — Drivers e Evolução do Core

Como o Core deve evoluir quando o MVP cresce de 1.000 para até 100.000 usuários, com o Ranking
crescendo mais que as demais funcionalidades.

> Retrato do fim do Desafio 1. Mudanças posteriores estão no desafio correspondente; os totais de testes e
> cobertura vigentes estão na evidência do desafio mais recente.

### 1. RF

| RF | Estado | Classes / endpoints reais | Onde entra na arquitetura |
|---|---|---|---|
| **RF01 — Ranking** | **Existe.** | `GET /ranking?page&size`, `GET /ranking/top?n`, `GET /ranking/students/{id}` → `RankingController` → `RankingApi` (impl. `RankingService`) → `StudentProgressionApi.listByScore` / `getScore` / `countStudentsAhead` → `StudentRepository.findScoresOrderedByXp` / `countAhead`. DTOs `RankingEntryDTO`, `RankingPageDTO`; `400` via `InvalidRankingQueryException`. | Módulo `ranking` (`ranking.api` + `ranking.internal`), que consome **apenas** `StudentProgressionApi`; ordenação e paginação no banco, índice `idx_students_xp_total` e cache do top N (item 9.4). |
| **RF02 — Conquistas** | **Planejado no Desafio 1**; badges implementados no [Desafio 2](#desafio-2--organização-interna-do-core). | Conceito mais próximo: histórico de level-up — `LevelUpEvent` (domínio) / `LevelUpEventEntity` (tabela `level_up_events`), gerado em `Student.receiveXp`. | Novo módulo `conquistas` que reage a eventos de progressão em processo (Spring `ApplicationEvent`), sem chamada remota. |
| **RF03 — Cadastrar aluno** (US4) | **Existe.** | `POST /students` → `StudentController.createStudent` → `StudentProgressionApi.createStudent` (impl. `StudentService`) → `StudentRepository.save`. DTO `CreateStudentRequestDTO`. | Módulo `progression`. |
| **RF04 — Conceder XP e subir de nível** (US1, US2, US3) | **Existe.** | `POST /students/{id}/xp` → `StudentController.receiveXp` → `StudentService.receiveXp`; regra em `Student.receiveXp` + `Level.fromXp` (BRONZE 0, PRATA 100, OURO 300, DIAMANTE 700); um `LevelUpEvent` por faixa atravessada. DTO `ReceiveXpRequestDTO`. | Módulo `progression` (domínio em `progression.internal.domain`). |
| **RF05 — Consultar perfil do aluno** (US4) | **Existe.** | `GET /students/{id}` → `StudentController.getStudent` → `StudentService.getStudent`; `404` via `StudentNotFoundException` + `ApiExceptionHandler`. DTOs `StudentResponseDTO`, `LevelUpEventDTO`. | Módulo `progression`. |
| **RF06 — Listar alunos** | **Existe.** | `GET /students` → `StudentController.listStudents` → `StudentService.listStudents` (`findAll()` sem paginação, mantido por compatibilidade com o front-end). | Módulo `progression`. Para volume, o caminho é o `GET /ranking` paginado. |
| **RF-EVO — Cursos e recompensas** | **Planejado.** Não existem no código. | — | Módulos irmãos (`cursos`, `recompensas`) no mesmo padrão `api/` + `internal/`. |

Interfaces de apoio que já existem: front-end Vue 3 em `src/main/resources/static/index.html`
(consome `/students`) e documentação OpenAPI (`OpenApiConfig`, `/swagger-ui.html`, `/v3/api-docs`).

### 2. RNF (FURPS+)

| RNF | FURPS+ | Análise no contexto da AC1 | Meta mensurável | Situação hoje |
|---|---|---|---|---|
| **Manutenibilidade** | S (Supportability) | Com Ranking, Conquistas, Cursos e Recompensas entrando, uma organização só por camada técnica espalharia cada funcionalidade por todos os pacotes e permitiria qualquer import. | 0 violações das regras de `ModularArchitectureTest`; um novo módulo não exige alterar classes de `progression.internal`. | Existe: 8 regras ArchUnit verdes no `mvn verify`. |
| **Desempenho** | P (Performance) | O risco é de consulta, não de processo: `GET /students` faz `findAll()` sem paginação e carrega `levelUpEvents` em modo lazy (N+1). Um Ranking ingênuo em cima disso degradaria. | Ranking (top 100 e posição do aluno) com **p95 ≤ 300 ms** e **base de 100.000 alunos**; `POST /students/{id}/xp` com p95 ≤ 200 ms. | Ranking com 100.000 alunos no H2 de teste: página 0 em **15 ms**, última página em **49 ms**, posição de um aluno em **52 ms** (item 9.4). É medição única, não p95: falta medir em PostgreSQL sob carga HTTP. |
| **Escalabilidade** | P (Performance: capacidade) | Ranking é *read-heavy* e cresce mais que o resto. Monólito *stateless* escala horizontalmente; o ranking pode vir de cache. | Atender 100.000 usuários escalando réplicas do Core sem mudar código; nenhuma sessão em memória. | Core *stateless* (sem sessão HTTP); 1 réplica no `docker-compose.yml`. O cache do top N é por instância: com várias réplicas, trocar por um cache compartilhado (ex.: Redis), sem mudar o código do módulo. |
| **Testabilidade** | S (Supportability) | Domínio em POJO e regra de fronteira verificável por teste. | **Cobertura de 100%** de linhas e branches; piso de **90%** que quebra o build (`jacoco:check`). | Existe: 100% instruções/linhas/branches/métodos; piso de 90% no `pom.xml`. |
| **Observabilidade** | S (Supportability: serviceability) | Sem métricas não há como saber se os gatilhos de extração do Ranking (item 5) foram atingidos — a decisão depende disso. | Latência p95, taxa de erro e throughput por endpoint expostos em formato Prometheus e visualizados no Grafana; alerta se p95 do ranking > 300 ms. | **Não existe:** sem Actuator, Micrometer, Prometheus ou Grafana no projeto. Ver item 9.6. |
| **Complexidade operacional** | + (restrição de implementação e operação) | Hoje: 1 jar, 1 imagem Docker, 1 PostgreSQL, 1 `docker-compose.yml`, 1 workflow de CI. Serviços separados trariam múltiplos pipelines, contratos versionados, rede e dados distribuídos. | Continuar com **1 artefato, 1 banco e 1 pipeline**; ambiente completo sobe com um único `docker compose up`. | Atendido. |

### 3. ASR

| ASR | RNF de origem | Como altera a arquitetura (uma frase) |
|---|---|---|
| **ASR1 — Evoluir funcionalidades com baixo acoplamento** | Manutenibilidade | Altera a **estrutura**: o Core é dividido em módulos por funcionalidade (`api/` público + `internal/` privado) e só a API de um módulo pode ser usada pelo restante do sistema. |
| **ASR2 — Ranking com p95 ≤ 300 ms para 100.000 usuários** | Desempenho + Escalabilidade (medido via Observabilidade) | Altera **dados e implantação**: o Ranking ganha consulta indexada/paginada e cache próprios, o Core roda em réplicas *stateless* e expõe métricas que dizem quando escalar ou extrair. |
| **ASR3 — Fronteiras verificáveis automaticamente** | Testabilidade | Altera a **operação** (pipeline): as regras de dependência viram testes ArchUnit que rodam em todo commit e Pull Request, bloqueando o merge quando violadas. |

Observabilidade não é um driver isolado: é o meio de verificar a ASR2 e os gatilhos do item 5.
Complexidade operacional entra como **restrição** na escolha (itens 4 e 6), não como requisito a maximizar.

### 4. RPC

| RPC | Restrição / premissa | Impacto na decisão |
|---|---|---|
| **RPC1** | Core em **Java 21 / Spring Boot 4.1** (stack atual). | Modularização com recursos da própria stack (pacotes, interfaces, `ApplicationEvent`), sem nova linguagem ou runtime. |
| **RPC2** | Equipe de **6 desenvolvedores**. | Um único time; não há times independentes que justifiquem deploys independentes (lei de Conway). |
| **RPC3** | **Banco relacional único** (PostgreSQL 16). | Dividir o banco por serviço exigiria migração de dados e consistência eventual. |
| **RPC4** | **Legado da AC1**: tabelas `students` e `level_up_events`, endpoints `/students` consumidos pelo front-end Vue, 100% de cobertura herdada do ciclo RED→GREEN→BLUE. | Contratos HTTP, JSON e schema não podem quebrar (protegido por `StudentEndpointIntegrationTest`). |

**Premissas:** não há medição de carga em produção; 100.000 usuários cadastrados não significam
100.000 simultâneos; o Ranking pode tolerar alguns segundos de defasagem (cache).

### 5. Decisão do Tech Lead

**Quais RNFs viram ASRs?** Manutenibilidade (ASR1), Desempenho + Escalabilidade do Ranking (ASR2)
e Testabilidade das fronteiras (ASR3). Complexidade operacional é tratada como restrição.

**Há necessidade COMPROVADA de escala ou deploy independente?** **Não.** Na data da decisão não
havia medição de latência, de carga nem de gargalo do Ranking, que sequer existia. 100 mil usuários
é carga moderada para um monólito bem estruturado, com índices, paginação e cache, rodando em
réplicas *stateless*. Também não há demanda de deploy independente: um único time de 6 pessoas
publica um único artefato.

**A complexidade distribuída é justificável?** **Não, agora.** Microsserviços trariam rede,
contratos versionados, banco por serviço, consistência eventual e observabilidade distribuída
para resolver um problema que ainda não foi medido. Com 6 devs e um banco relacional único, o
custo superaria o benefício.

**Resumo da decisão:** **Monólito Modular.** Não há necessidade comprovada de escala ou deploy
independente, e a complexidade distribuída não se justifica para 6 devs com um banco relacional
único. O Ranking já é um módulo com API própria (`ranking`) e Conquistas entrará no mesmo padrão; a
extração do Ranking como serviço tem gatilhos mensuráveis (abaixo). Tabela completa em
**[Rastreabilidade](#911-rastreabilidade)**; registro formal no
[ADR-001](adr/ADR-001-estilo-arquitetural-core.md).

#### Gatilhos mensuráveis para extrair o Ranking como serviço

A extração (alternativa C) só deve ser reavaliada quando **pelo menos dois** gatilhos forem
observados de forma sustentada (ex.: 2 semanas em produção), **depois** de aplicados índice,
paginação e cache. As medições vêm do Prometheus/Grafana (item 8.2).

| # | Gatilho | Limite |
|---|---|---|
| G1 | Latência do endpoint de ranking | p95 > 300 ms sob carga real, mesmo com cache e índice |
| G2 | Carga desproporcional | Ranking > 60% das requisições ou da CPU do Core, forçando escalar o monólito inteiro só por ele |
| G3 | Contenção no banco | Consultas de ranking > 40% do tempo de banco ou elevando o p95 de `POST /students/{id}/xp` acima de 200 ms |
| G4 | Frequência de deploy | Ranking precisa de deploy ≥ 2×/semana independente dos demais e > 30% dos releases ficam bloqueados por outros módulos |
| G5 | Organização | Equipe cresce para ≥ 2 times com ownership separado do Ranking |

Pré-condição para extrair barato: o módulo `ranking` isolado atrás da sua API, com ArchUnit
verde — exatamente o que a alternativa B garante.

### 6. Alternativas e trade-offs

| Critério | (A) Monólito em Camadas | (B) Monólito Modular | (C) Decomposição em serviços (Ranking como serviço) |
|---|---|---|---|
| Manutenibilidade | Baixa com o crescimento: funcionalidades espalhadas pelas camadas, acoplamento livre. | **Alta**: cada funcionalidade isolada atrás de uma API; mudanças locais ao módulo. | Alta para o Ranking, mas exige versionar contrato HTTP/eventos entre Core e serviço. |
| Desempenho | Adequado com índice e cache. | Adequado com índice e cache (chamadas em memória, igual a A). | Ganho só se o Ranking for gargalo isolado; adiciona latência de rede e serialização. |
| Escalabilidade | Horizontal, do sistema inteiro. | Horizontal, do sistema inteiro; `ranking` já fica pronto para extração. | Escala o Ranking de forma independente (único benefício real, ainda não necessário). |
| Testabilidade | Testes unitários bons, sem verificação de fronteiras. | **Fronteiras testáveis** (ArchUnit) + testes por módulo. | Exige testes de contrato e ambientes integrados; mais difícil reproduzir localmente. |
| Observabilidade | Simples: um processo, um `/actuator/prometheus`. | Simples: um processo; métricas por endpoint/módulo com tags. | Distribuída: métricas por serviço, *tracing* entre serviços para seguir uma requisição. |
| Complexidade operacional | Mínima (1 jar, 1 banco). | **Mínima** (1 jar, 1 banco, 1 pipeline). | Alta: 2+ pipelines e deploys, rede, *service discovery*, dados duplicados ou banco compartilhado (antipadrão). |
| Aderência às RPCs | Atende RPC1–4. | Atende RPC1–4. | Conflita com RPC2 (6 devs) e RPC3 (banco único). |
| **Prós** | Simples, já existia. | Baixo acoplamento com custo operacional de monólito; prepara extração futura. | Escala e deploy independentes do Ranking. |
| **Contras** | Degrada a cada nova funcionalidade; não protege fronteiras. | Exige disciplina (garantida por ArchUnit no CI); a escala ainda é do conjunto. | Custo alto e prematuro; resolve um gargalo não comprovado. |

### 7. ADR

[**ADR-001 — Estilo arquitetural do Core: Monólito Modular**](adr/ADR-001-estilo-arquitetural-core.md)
— status **Aceito**, 2026-10-27.

- **Decisão:** Monólito Modular (B). Cada funcionalidade é um pacote-módulo
  `org.example.ac1devops.<modulo>` com `api/` (fachada pública) e `internal/` (service, domínio,
  persistência); o resto do sistema só usa a `api/`. Primeiro módulo: `progression`, com a
  fachada `StudentProgressionApi`.
- **Garantia:** `ModularArchitectureTest` (ArchUnit) no `mvn verify`, executado pelo CI em todo
  push e Pull Request.
- **Ranking:** não é extraído agora; reavaliação pelos gatilhos G1–G5, medidos com
  Prometheus/Grafana.
- **Trade-off aceito:** a escala continua sendo do conjunto e o banco é compartilhado (fronteira
  de dados só lógica), em troca de manter 1 artefato, 1 banco e 1 pipeline.

### 8. C4

Elementos marcados **(planejado)** ainda não existem no código; os demais existem nesta branch.

#### 8.1 Contexto (C4Context)

```mermaid
C4Context
  title Educação Continuada Gamificada — Contexto

  Person(aluno, "Aluno", "Ganha XP, sobe de nível, consulta perfil e ranking; (planejado) conquistas")
  Person(equipe, "Equipe (6 devs)", "Desenvolve, opera e monitora a plataforma")

  System(plataforma, "Plataforma de Educação Continuada Gamificada", "Front-end Vue + Core API (Monólito Modular) + PostgreSQL")

  System_Ext(github, "GitHub / GitHub Actions", "Repositório e CI: mvn verify (testes, ArchUnit, JaCoCo)")
  System_Ext(obs, "Stack de observabilidade (planejado)", "Prometheus + Grafana")

  Rel(aluno, plataforma, "Usa", "HTTPS")
  Rel(equipe, github, "push / Pull Request")
  Rel(equipe, obs, "Acompanha métricas e gatilhos G1–G5")
  Rel(obs, plataforma, "Coleta métricas", "HTTP")
```

#### 8.2 Container (C4Container)

```mermaid
C4Container
  title Educação Continuada Gamificada — Containers

  Person(aluno, "Aluno")
  Person(equipe, "Equipe (6 devs)")

  System_Boundary(plataforma, "Plataforma") {
    Container(spa, "Front-end", "Vue 3 via CDN (static/index.html)", "Servido pelo próprio Core na raiz '/'")
    Container(core, "Core API", "Java 21, Spring Boot 4.1 — Monólito Modular", "Módulos progression e ranking (com cache do top N); conquistas (planejado). Réplicas stateless.")
    ContainerDb(db, "Banco relacional", "PostgreSQL 16", "students (índice idx_students_xp_total), level_up_events")
    Container(pgadmin, "pgAdmin", "pgAdmin 4", "Inspeção do banco (docker-compose)")
    Container(prom, "Prometheus (planejado)", "Prometheus", "Coleta /actuator/prometheus do Core e guarda séries temporais")
    Container(graf, "Grafana (planejado)", "Grafana", "Painéis de latência p95, erros e throughput; acompanha G1–G5")
  }

  System_Ext(gha, "GitHub Actions", "CI: .github/workflows/ci.yml")

  Rel(aluno, spa, "Usa", "HTTPS")
  Rel(spa, core, "Chama /students", "JSON/HTTP")
  Rel(core, db, "Lê e escreve", "JPA/JDBC")
  Rel(pgadmin, db, "Conecta", "SQL")
  Rel(prom, core, "Scrape de métricas", "HTTP")
  Rel(graf, prom, "Consulta", "PromQL")
  Rel(equipe, graf, "Monitora")
  Rel(equipe, pgadmin, "Consulta")
  Rel(equipe, gha, "push / Pull Request")
```

#### 8.3 Component do Core (C4Component)

```mermaid
C4Component
  title Core API — Componentes (Monólito Modular)

  Container(spa, "Front-end", "Vue 3")
  ContainerDb(db, "Banco relacional", "PostgreSQL 16")
  Container(prom, "Prometheus (planejado)", "Prometheus")

  Container_Boundary(core, "Core API (Spring Boot)") {
    Component(ctrl, "StudentController + RankingController + ApiExceptionHandler", "Spring MVC (pacote web)", "REST /students e /ranking; usa só progression.api e ranking.api")
    Component(api, "StudentProgressionApi", "Interface Java (progression.api)", "Fachada pública: DTOs, Level, StudentNotFoundException")
    Component(svc, "StudentService", "Spring @Service (progression.internal)", "Implementa a fachada; orquestra domínio e persistência")
    Component(dom, "Student / LevelUpEvent", "POJO (progression.internal.domain)", "Regra de XP e níveis")
    Component(repo, "StudentRepository + StudentEntity / LevelUpEventEntity", "Spring Data JPA (progression.internal.persistence)", "students, level_up_events; consulta ordenada por XP com índice")
    Component(rankApi, "RankingApi", "Interface Java (ranking.api)", "Fachada pública do ranking: RankingEntryDTO, RankingPageDTO")
    Component(rank, "RankingService + cache do top N", "Spring @Service + Spring Cache (ranking.internal)", "Pagina, calcula posição e guarda o top N em cache")
    Component(inval, "RankingCacheInvalidator", "@EventListener (ranking.internal)", "Esvazia o cache ao receber StudentScoreChangedEvent")
    Component(conq, "Módulo conquistas (planejado, RF02)", "conquistas.api / conquistas.internal", "Reage a eventos de level-up (ApplicationEvent)")
    Component(metrics, "Métricas (planejado)", "Spring Boot Actuator + Micrometer", "Expõe /actuator/prometheus")
  }

  Rel(spa, ctrl, "JSON/HTTP")
  Rel(ctrl, api, "Chama")
  Rel(ctrl, rankApi, "Chama")
  Rel(rank, rankApi, "Implementa")
  Rel(svc, inval, "Publica StudentScoreChangedEvent", "ApplicationEvent em processo")
  Rel(svc, api, "Implementa")
  Rel(svc, dom, "Usa")
  Rel(svc, repo, "Usa")
  Rel(repo, db, "JPA/JDBC")
  Rel(rank, api, "listByScore / getScore / countStudentsAhead (em memória)")
  Rel(conq, api, "Escuta eventos de level-up (em memória)")
  Rel(prom, metrics, "Scrape", "HTTP")
```

### 9. Evidência

#### 9.1 Como reproduzir e resultado atual

Executado em 2026-11-02, Windows 11, Java 21.0.12, sobre o commit desta branch.

| Evidência | Como reproduzir | Resultado obtido |
|---|---|---|
| Build, testes, ArchUnit e piso de cobertura | `mvnw.cmd -B verify` | Sucesso. **61 testes**, 0 falhas, 0 erros, 0 ignorados. |
| Fronteiras do monólito modular | `mvnw.cmd test -Dtest=ModularArchitectureTest` | **8 regras** verdes. A hipótese "uma violação quebra o build" foi testada com falha simulada (item 9.9). |
| Cobertura | Abrir `target/site/jacoco/index.html` depois do `verify` | **100%**: linhas 172/172, branches 20/20, instruções 785/785, métodos 80/80, em 23 classes. |
| Ranking com 100.000 alunos | `mvnw.cmd test -Dtest=RankingPerformanceTest` | Item 9.4. Tempos no console, com o prefixo `[RankingPerformanceTest]`. |
| Cache do top N | `mvnw.cmd test -Dtest=RankingCacheIntegrationTest` | 2 testes verdes: serve do cache, invalida quando a pontuação muda e não invalida com XP inválido. |

#### 9.2 Implementação

**Implementado:** módulo `progression` (RF03–RF06) e módulo `ranking` (RF01), ambos no padrão
`api/` + `internal/`, consumidos pela camada `web`. O `ranking` usa apenas `StudentProgressionApi` e
recebe `StudentScoreChangedEvent` em processo para invalidar o cache.

**Não implementado ao fim do Desafio 1:** módulo `conquistas` (RF02, entregue no Desafio 2), cursos e recompensas (RF-EVO), observabilidade
(item 9.6). O front-end Vue ainda consome só `/students`; o ranking é acessível pela API e pelo
Swagger.

#### 9.3 Monólito Modular x Camadas

A comparação de critérios está no item 6. O efeito medido da modularização, detalhado no
mini-lab (item 9.8), foi:

| Métrica | Camadas (antes) | Modular (depois) |
|---|---|---|
| Arestas entre pacotes | 9 | 6 |
| Pacotes que `web` conhece | 2 | 1 |
| Dependências de `web` em classe concreta de implementação | 1 | 0 |
| Dependências de fora do módulo para `internal` | sem fronteira | 0, garantido por ArchUnit |

#### 9.4 Ranking para 100.000 usuários

`RankingPerformanceTest` (H2 do `@DataJpaTest`, consulta real do service ao banco, **sem cache**):

| Operação | Execução anterior (AC2) | Execução de 2026-11-02 |
|---|---|---|
| Página 0 (size 20) | 10 ms | 15 ms |
| Página 4999 (size 20, últimas posições) | 25 ms | 49 ms |
| Posição de um aluno | 45 ms | 52 ms |

O plano de execução registrado pelo teste mostra o uso do índice:
`PUBLIC.IDX_STUDENTS_XP_TOTAL ... /* index sorted */`.

**Limites desta evidência:** cada valor é uma medição única e sem concorrência, em H2 em memória. O
teste só falha acima de 1.000 ms, portanto **não comprova o p95 ≤ 300 ms da ASR2**. Isso continua
pendente: medir em PostgreSQL 16, sob carga HTTP. O cache do top N é por instância.

#### 9.5 Cobertura de testes

Situação atual no item 9.1: **100%** de linhas, branches, instruções e métodos, com
`Ac1DevOpsApplication` excluída da métrica. O piso de 90% de linhas e branches é verificado por
`jacoco:check` no `verify`. O histórico antes/depois da modularização está no item 9.9.

#### 9.6 Observabilidade

**Não implementada.** Não há Actuator, Micrometer, Prometheus nem Grafana no `pom.xml`, no
`docker-compose.yml`, em `src/` ou no CI; os termos aparecem apenas neste documento e no ADR-001,
como desenho-alvo. Consequência: os gatilhos G1–G5 (item 5) ainda não podem ser medidos.

#### 9.7 Teste com Mocks

Os testes unitários usam Mockito para isolar cada unidade de suas dependências, e os mocks dos
consumidores são as fachadas públicas dos módulos, o que reforça a fronteira do ADR-001:

| Teste | Dependência simulada | Testes |
|---|---|---|
| [`StudentServiceTest`](../../src/test/java/org/example/ac1devops/progression/internal/StudentServiceTest.java) | `StudentRepository`, `ApplicationEventPublisher` | 16 |
| [`RankingServiceTest`](../../src/test/java/org/example/ac1devops/ranking/internal/RankingServiceTest.java) | `StudentProgressionApi` | 5 |
| [`RankingControllerTest`](../../src/test/java/org/example/ac1devops/web/RankingControllerTest.java) | `RankingApi` | 5 |
| [`StudentControllerTest`](../../src/test/java/org/example/ac1devops/web/StudentControllerTest.java) | `StudentProgressionApi` | 6 |

`StudentEndpointIntegrationTest` e `RankingCacheIntegrationTest` complementam esses testes sem mocks
nas dependências principais.

> Itens 9.8 a 9.10: retrato do momento do mini-lab (36 testes e 6 regras ArchUnit). Os números atuais estão no item 9.1.

#### 9.8 Mini-lab — modularização do módulo `progression`

Conteúdo produzido no Desafio 1 e mantido como evidência da modularização já aplicada ao código.

**Funcionalidade escolhida:** como Ranking e Conquistas ainda não existiam (item 1), foi
modularizada a funcionalidade real mais próxima: a **progressão do aluno (XP e níveis, US1–US4)**,
que é exatamente a fonte de dados do futuro Ranking e dos eventos que alimentarão Conquistas.

**Comportamento externo inalterado:** mesmos endpoints (`/students`), mesmos DTOs/JSON, mesmas
tabelas (`students`, `level_up_events` — nomes explícitos em `@Table`/`@Column`). Os 26 testes
existentes continuaram passando sem alteração de asserções; só mudaram `package`/`import` e o
`StudentControllerTest` passou a mockar a interface `StudentProgressionApi` no lugar da classe
concreta `StudentService`.

##### Estrutura

ANTES (organização só por camada técnica):

```
org.example.ac1devops
├── web/         StudentController, ApiExceptionHandler
├── service/     StudentService, StudentNotFoundException
├── repository/  StudentRepository
├── entity/      StudentEntity, LevelUpEventEntity
├── dto/         CreateStudentRequestDTO, ReceiveXpRequestDTO, StudentResponseDTO, LevelUpEventDTO
├── domain/      Student, Level, LevelUpEvent
└── config/      OpenApiConfig
```

DEPOIS (módulo por funcionalidade, com API pública e implementação privada):

```
org.example.ac1devops
├── web/                         StudentController, ApiExceptionHandler  → usam só progression.api
├── config/                      OpenApiConfig
└── progression/                 MÓDULO de progressão (alunos, XP, níveis)
    ├── api/                     PÚBLICO: StudentProgressionApi (fachada), DTOs, Level,
    │                            StudentNotFoundException
    └── internal/                PRIVADO ao módulo
        ├── StudentService       implementa StudentProgressionApi
        ├── domain/              Student, LevelUpEvent (POJOs, regra de XP/nível)
        └── persistence/         StudentEntity, LevelUpEventEntity, StudentRepository
```

`Level` ficou em `api/` porque faz parte do contrato publicado (é o tipo do campo `level` e dos
eventos nos DTOs de resposta).

##### Dependências entre pacotes (imports `org.example.ac1devops.*`)

Levantamento feito por script sobre `src/main/java` (cada linha = pacote de origem → pacote
importado, com a quantidade de imports):

| ANTES | | DEPOIS | |
|---|---|---|---|
| `web → service` | 2 | `web → progression.api` | 5 |
| `web → dto` | 3 | `progression.internal → progression.api` | 7 |
| `service → domain` | 3 | `progression.internal → progression.internal.domain` | 2 |
| `service → dto` | 4 | `progression.internal → progression.internal.persistence` | 3 |
| `service → entity` | 2 | `progression.internal.domain → progression.api` | 2 |
| `service → repository` | 1 | `progression.internal.persistence → progression.api` | 2 |
| `repository → entity` | 1 | | |
| `entity → domain` | 2 | | |
| `dto → domain` | 2 | | |

```mermaid
%% ANTES
graph LR
  web --> service
  web --> dto
  service --> domain
  service --> dto
  service --> entity
  service --> repository
  repository --> entity
  entity --> domain
  dto --> domain
```

```mermaid
%% DEPOIS
graph LR
  web --> api
  subgraph progression["módulo progression"]
    api["progression.api"]
    subgraph internal["internal (privado)"]
      svc["progression.internal"]
      dom["internal.domain"]
      per["internal.persistence"]
    end
  end
  svc --> api
  svc --> dom
  svc --> per
  dom --> api
  per --> api
```

##### Contagem de dependências

| Métrica | ANTES | DEPOIS |
|---|---|---|
| Arestas entre pacotes | 9 | 6 |
| Imports entre pacotes | 20 | 21 (+1: a interface `StudentProgressionApi`) |
| Pacotes que `web` conhece | 2 (`service`, `dto`) | 1 (`progression.api`) |
| Dependências de `web` em classe concreta de implementação | 1 (`StudentService`) | 0 |
| Dependências de fora do módulo para `internal` | — (não havia fronteira; `web` podia importar `entity`/`repository`) | 0 (garantido por ArchUnit) |
| Ciclos entre pacotes | 0 | 0 |

#### 9.9 Evidência de QA

##### Testes que protegem a decisão

| Teste | Tipo | O que garante |
|---|---|---|
| [`ModularArchitectureTest`](../../src/test/java/org/example/ac1devops/architecture/ModularArchitectureTest.java) | ArchUnit (6 regras) | `ninguemForaDoModuloAcessaInternal`, `apiDoModuloNaoDependeDoInternal`, `camadasRespeitadas` (Web → ProgressionApi ← ProgressionInternal; ninguém acessa Web/Config/Internal), `dominioSemFramework` (domínio sem Spring/JPA), `controllersFicamNaCamadaWeb`, `semCiclosEntrePacotes`. |
| [`StudentServiceTest`](../../src/test/java/org/example/ac1devops/progression/internal/StudentServiceTest.java) | Unitário (JUnit 5 + Mockito) | 10 cenários do service do módulo (8 existentes + 2 novos: reconstrução a partir do estado persistido gerando só os eventos novos, e XP inválido mantendo o estado). |
| [`StudentEndpointIntegrationTest`](../../src/test/java/org/example/ac1devops/web/StudentEndpointIntegrationTest.java) | Integração (`@SpringBootTest`, HTTP real + H2) | Fluxo completo `POST /students` → `POST /students/{id}/xp` → `GET /students/{id}` → `GET /students` e o `404` com mensagem: prova que o contrato externo não mudou. |
| `StudentControllerTest`, `StudentRepositoryTest`, `StudentTest`, `LevelTest`, `SwaggerIntegrationTest`, `Ac1DevOpsApplicationTests` | Já existentes | Continuam passando sem mudança de asserções. |

**Prova de que a regra funciona:** uma classe temporária em `web` com um campo do tipo
`progression.internal.persistence.StudentRepository` fez o build **falhar** em 2 regras
(`ninguemForaDoModuloAcessaInternal` e `camadasRespeitadas`); a classe foi removida em seguida.
Saída registrada em [`evidencias/archunit-violacao.txt`](evidencias/archunit-violacao.txt).

##### Resultado dos testes

| | ANTES (`main`, `ecc44f7`) | DEPOIS (fim do mini-lab) |
|---|---|---|
| Testes executados | 26 | 36 |
| Falhas / erros | 0 / 0 | 0 / 0 |
| Regras de arquitetura verificadas | 0 | 6 (ArchUnit) |
| Testes de integração HTTP do módulo (sem mock) | 0 | 2 |
| Piso de cobertura no build | não havia | `jacoco:check` — linhas ≥ 90% e branches ≥ 90% (falha o `verify`) |

##### Cobertura JaCoCo do módulo (classes da funcionalidade de progressão)

| Classe (ANTES → DEPOIS) | Instruções | Linhas | Branches |
|---|---|---|---|
| `service.StudentService` → `progression.internal.StudentService` | 100% → 100% (155) | 100% → 100% (29) | 100% → 100% (4/4) |
| `domain.Student` → `progression.internal.domain.Student` | 100% → 100% (74) | 100% → 100% (22) | 100% → 100% (4/4) |
| `domain.LevelUpEvent` → `progression.internal.domain.LevelUpEvent` | 100% → 100% (15) | 100% → 100% (6) | — |
| `entity.StudentEntity` / `LevelUpEventEntity` → `progression.internal.persistence.*` | 100% → 100% (85) | 100% → 100% (32) | — |
| `domain.Level` → `progression.api.Level` | 100% → 100% (68) | 100% → 100% (14) | 100% → 100% (4/4) |
| DTOs + `StudentNotFoundException` → `progression.api.*` | 100% → 100% (47) | 100% → 100% (6) | — |
| **Total do módulo** | **100% → 100% (444/444)** | **100% → 100% (109/109)** | **100% → 100% (12/12)** |

A cobertura de linhas já era 100% antes (herança do ciclo RED→GREEN→BLUE da AC1) e foi
**mantida** após a refatoração. O ganho de qualidade não está no percentual, mas em **o que** é
verificado: fronteiras do módulo (ArchUnit), fluxo HTTP real sem mocks e um piso de cobertura
que impede regressão. `StudentProgressionApi` é interface e não gera instruções no relatório.

Fontes brutas: [`evidencias/jacoco-antes.csv`](evidencias/jacoco-antes.csv) e
[`evidencias/jacoco-depois.csv`](evidencias/jacoco-depois.csv). Relatório HTML gerado em
`target/site/jacoco/index.html` a cada `./mvnw verify` e publicado como artifact pelo CI
(item 9.10).

#### 9.10 Evidência de DevOps — pipeline de CI

**Run no GitHub (ASR3):** run #1 do workflow `CI`, no commit `4d21fa4` da branch
`feature/b1-architecture-lab`: status **Success**, 1m 13s e 2 artifacts. O aviso de Node.js 20 que
aparece nos prints veio das actions usadas naquele run; o `ci.yml` foi atualizado depois para versões
que rodam em Node 24.

| Lista de runs | Resumo do run | Etapas do job |
|---|---|---|
| ![Lista de runs do CI com o run #1 verde](evidencias/img/d1-ci-verde.png) | ![Resumo do run: Success e 2 artifacts](evidencias/img/d1-ci-resumo.png) | ![Etapas do job concluídas](evidencias/img/d1-ci-etapas.png) |

Workflow: [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml) (não havia workflow
anterior no repositório).

| Gatilho | Passos |
|---|---|
| `push` em qualquer branch e `pull_request` para `main` | 1. `actions/checkout` → 2. `actions/setup-java` (Temurin **21**, igual ao `<java.version>` do `pom.xml`, com cache Maven) → 3. `./mvnw -B clean verify` → 4. publica `target/site/jacoco/` como artifact **`jacoco-report`** e `target/surefire-reports/` como **`surefire-reports`** (mesmo se o build falhar). |

##### Como o pipeline protege a decisão arquitetural

1. **A regra modular é código executável.** `ModularArchitectureTest` roda dentro do
   `mvn verify` como qualquer teste; não depende de revisão manual lembrar da regra.
2. **Verificada a cada commit e a cada Pull Request.** Todo `push` dispara o workflow; todo PR
   para a `main` mostra o check `Build, testes, ArchUnit e cobertura`. Um import de
   `progression.internal` fora do módulo deixa o check **vermelho** com a mensagem do ArchUnit
   apontando classe e campo/método infrator (exemplo real em
   [`evidencias/archunit-violacao.txt`](evidencias/archunit-violacao.txt)).
3. **O contrato externo também é protegido.** `StudentEndpointIntegrationTest` falha se a
   refatoração (ou uma futura extração do Ranking) mudar status/JSON de `/students`.
4. **Qualidade não regride em silêncio.** `jacoco:check` falha o build abaixo de 90% de linhas ou
   branches; o relatório fica anexado ao run para auditoria.
5. **Merge só com CI verde.** Recomendado ativar em *Settings → Branches → Branch protection
   rules* da `main`: "Require a pull request before merging" + "Require status checks to pass"
   marcando o check `Build, testes, ArchUnit e cobertura`. Assim a decisão do ADR-001 não pode
   ser violada nem por push direto na `main`.

#### 9.11 Rastreabilidade

| RF | RNF | ASR | RPC | ADR | C4 | Evidência no código |
|---|---|---|---|---|---|---|
| RF01 Ranking (`/ranking`) | Desempenho, Escalabilidade, Observabilidade | ASR2 | RPC3 (banco único), RPC4 (legado) | ADR-001, Decisão itens 4–6 + gatilhos G1–G5 | Component: `RankingController` → `RankingApi` ← `RankingService` → `StudentProgressionApi`; Container: Prometheus/Grafana | [`RankingService`](../../src/main/java/org/example/ac1devops/ranking/internal/RankingService.java), [`StudentRepository`](../../src/main/java/org/example/ac1devops/progression/internal/persistence/StudentRepository.java), índice em [`StudentEntity`](../../src/main/java/org/example/ac1devops/progression/internal/persistence/StudentEntity.java), [`RankingPerformanceTest`](../../src/test/java/org/example/ac1devops/ranking/internal/RankingPerformanceTest.java), [`RankingCacheIntegrationTest`](../../src/test/java/org/example/ac1devops/ranking/internal/RankingCacheIntegrationTest.java) |
| RF02 Conquistas (planejado no Desafio 1; badges implementados no Desafio 2) | Manutenibilidade | ASR1 | RPC1 (Spring) | ADR-001, Decisão item 4 | Component: módulo `conquistas` (planejado) | `StudentScoreChangedEvent` (mecanismo de eventos já usado pelo ranking); `LevelUpEvent` em `progression.internal.domain`; regra `dominioSemFramework` |
| RF03–RF06 Progressão (`/students`, US1–US4) | Manutenibilidade, Testabilidade | ASR1, ASR3 | RPC2 (6 devs), RPC4 (legado) | ADR-001, Decisão itens 1–4 | Component: `StudentController` → `StudentProgressionApi` ← `StudentService` | [`StudentProgressionApi`](../../src/main/java/org/example/ac1devops/progression/api/StudentProgressionApi.java), [`ModularArchitectureTest`](../../src/test/java/org/example/ac1devops/architecture/ModularArchitectureTest.java), [`StudentServiceTest`](../../src/test/java/org/example/ac1devops/progression/internal/StudentServiceTest.java), [`StudentEndpointIntegrationTest`](../../src/test/java/org/example/ac1devops/web/StudentEndpointIntegrationTest.java) |
| (todos) | Testabilidade (cobertura 100%) | ASR3 | RPC4 (cobertura herdada) | ADR-001, Consequências | Context/Container: GitHub Actions | `jacoco:check` no [`pom.xml`](../../pom.xml), [`ci.yml`](../../.github/workflows/ci.yml) |
| (todos) | Observabilidade | ASR2 (meio de verificação) | RPC1 (Actuator/Micrometer da stack) | ADR-001, Decisão item 6 | Container: Prometheus, Grafana; Component: Métricas | Não implementado: item 9.6 |
| (todos) | Complexidade operacional | Restrição | RPC2, RPC3 | ADR-001, Alternativas (C rejeitada) | Container: 1 Core, 1 banco | [`docker-compose.yml`](../../docker-compose.yml), um único [`ci.yml`](../../.github/workflows/ci.yml) |

---

## Desafio 2 — Organização interna do Core

Como proteger o domínio contra dependências técnicas, se as regras de ranking, moedas, badges e
recompensas mudam com frequência.

### 1. RF

| RF | Estado | Classes / endpoints reais | Efeito deste desafio |
|---|---|---|---|
| **RF01 — Ranking** | **Existe.** | `GET /ranking`, `/ranking/top`, `/ranking/students/{id}`; ordenação em `StudentRepository` | Nenhuma mudança: a estrutura foi avaliada e mantida (item 5). |
| **RF02 — Conquistas e regras de gamificação** | **Existe (badges).** Moedas e recompensas seguem planejadas. | `GET /conquistas/students/{id}` → `ConquistasController` → `ConquistasApi`; concessão por `StudentScoreChangedEvent` → `StudentScoreEventListener` → `BadgeAwardService` | Módulo novo `conquistas`. |

O case não define quais badges existem. O catálogo em
[`BadgeRules`](../../src/main/java/org/example/ac1devops/conquistas/internal/domain/BadgeRules.java) é
uma **proposta da equipe**, não um requisito do case:

| Badge | Condição |
|---|---|
| `PRIMEIROS_PASSOS` | XP total ≥ 1 |
| `NIVEL_PRATA`, `NIVEL_OURO`, `NIVEL_DIAMANTE` | Nível alcançado, pela regra existente `Level.fromXp` |
| `MIL_XP` | XP total ≥ 1000 |

### 2. RNF (FURPS+)

| RNF | FURPS+ | Meta mensurável | Resultado |
|---|---|---|---|
| **Modificabilidade** | S (Supportability) | Regra nova sem alterar porta, adapters ou schema | Atendida: cenário `deveAplicarRegraNovaSemAlterarOAdapter` nos dois adapters |
| **Testabilidade** | S (Supportability) | Todos os cenários da regra rodam sem Spring nem banco | Atendida: 9 cenários em 0,3 s, sem contexto Spring |
| **Manutenibilidade** | S (Supportability) | 0 violações das regras do ArchUnit | Atendida: 9 regras verdes |
| **Facilidade de evolução** | S (Supportability) | Módulo novo sem mudar contratos existentes | Atendida: `/students` e `/ranking` mantidos; `progression` não conhece `conquistas` |

### 3. ASR

| ASR | RNF de origem | Como altera a arquitetura (uma frase) |
|---|---|---|
| **ASR4 — Mudar regra de gamificação sem tocar em persistência ou framework** | Modificabilidade | Altera a **estrutura**: a regra fica em um domínio puro e a dependência com a persistência é invertida por uma porta. |
| **ASR5 — Regra de gamificação testável sem Spring nem banco** | Testabilidade | Altera a **operação**: a regra é verificada em todo build por um contrato executado com um adapter em memória, sem subir infraestrutura. |

Manutenibilidade e facilidade de evolução não geram ASR novo: já estão cobertas pela ASR1, e as
fronteiras, pela ASR3.

### 4. RPC

| RPC | Restrição | Impacto na decisão |
|---|---|---|
| **RPC1** | Core continua em Java 21 / Spring Boot. | O Spring permanece nas bordas do módulo; não há troca de framework. |
| **RPC5** | Regras existentes devem ser reaproveitadas. | `Level.fromXp` é reusada, e não reescrita, nas regras de nível. |
| **RPC6** | Proibido reescrever o produto inteiro só para aplicar um padrão. | `progression` e `ranking` mantêm a estrutura atual. |

### 5. Decisão do Tech Lead

**Qual dependência realmente precisa ser invertida?** Apenas **regra de concessão de badges →
persistência**, porque é a única combinação de regra volátil com dependência técnica.

| Dependência | Inverter? | Motivo |
|---|---|---|
| Concessão de badges → persistência | **Sim** | A regra muda com frequência e não pode exigir banco para ser testada. |
| XP e níveis → persistência | Não | A regra já é POJO puro e estável; o `StudentService` faz a ponte. |
| Ordenação do ranking → banco | Não | É uma decisão de desempenho: ordenar no domínio carregaria 100.000 linhas em memória, contrariando a ASR2. |
| Domínio → Spring | Já não existe | Verificado por ArchUnit. O Spring fica nas bordas. |
| Integrações externas | Não | Nenhuma existe no código; uma porta agora seria especulativa. |

**O custo estrutural é proporcional ao problema?** Sim, porque ficou limitado a um módulo. Conquistas tem
15 arquivos e 336 linhas: 6 arquivos de domínio e porta (116 linhas), 4 de adapters (127) e 5 de API,
aplicação e controller (93). Fora do módulo, só mudaram 8 linhas do `StudentService`. O retorno é a regra
testada em 0,3 s e alterável sem tocar nos adapters. Em `progression` o mesmo custo não teria retorno,
pois a regra já é pura e estável.

**DDD, Hexagonal, Clean e Onion são concorrentes ou podem ser combinados?** Podem ser combinados, pois
respondem a perguntas diferentes. DDD trata de **como modelar** o domínio e suas fronteiras. Hexagonal,
Clean e Onion tratam de **para onde apontam as dependências** e, entre si, são variações da mesma ideia:
são alternativas umas às outras, mas não ao DDD. Aqui se usa só o necessário (item 6).

**Decisão:** Ports & Adapters **somente em `conquistas`**, com os demais módulos inalterados. Registro
formal no [ADR-002](adr/ADR-002-ports-and-adapters-em-conquistas.md).

### 6. Alternativas e trade-offs

| Critério | (A) Modular com camadas | (B) Ports & Adapters só em Conquistas | (C) Hexagonal em todo o Core |
|---|---|---|---|
| Mudar uma regra | Possível, mas nada impede a regra de depender do banco | **Alteração só no catálogo do domínio** | Igual a B, inclusive onde a regra não muda |
| Testar a regra | Exige mocks ou banco | **Sem Spring nem banco** | Sem Spring nem banco |
| Custo estrutural | Menor | **Limitado a um módulo** | Alto: reescreve `progression` e `ranking` |
| Desempenho do ranking | Mantido | **Mantido** | Em risco, se a ordenação for ao domínio (ASR2) |
| Aderência às RPCs | Atende | **Atende** | Conflita com a RPC6 |
| Contra principal | Regra acoplada à persistência | Duas organizações internas no Core | Custo sem problema correspondente |

Uso de cada abordagem:

| Abordagem | Usada? | Justificativa |
|---|---|---|
| **Ports & Adapters / Hexagonal** | **Sim, em `conquistas`** | Resolve o problema do case com uma porta e dois adapters. |
| **DDD estratégico** | Parcial | O módulo já funciona como fronteira de contexto, e o código usa a linguagem do domínio. |
| **DDD tático** | Parcial | `Badge` e `StudentProgress` são objetos de valor, e `BadgeAwardService` é um serviço de domínio. Agregados e eventos de domínio próprios não se justificam em uma regra tão pequena. |
| **Clean Architecture** | Não | Casos de uso e presenters como camadas próprias não resolvem problema algum neste módulo. |
| **Onion** | Não | Tem a mesma direção de dependência do Hexagonal, com mais anéis nomeados. |

### 7. ADR

[**ADR-002 — Ports & Adapters só onde a regra muda: módulo Conquistas**](adr/ADR-002-ports-and-adapters-em-conquistas.md)
— status **Aceito**, 2026-11-02. Complementa o [ADR-001](adr/ADR-001-estilo-arquitetural-core.md), sem
substituí-lo.

### 8. C4

Nível afetado: **Component do Core**. Mostra só o módulo `conquistas` e o que o toca; os demais
componentes seguem como no Desafio 1.

```mermaid
C4Component
  title Core API — Módulo conquistas (Ports & Adapters)

  ContainerDb(db, "Banco relacional", "PostgreSQL 16", "student_badges")

  Container_Boundary(core, "Core API (Spring Boot)") {
    Component(ctrl, "ConquistasController", "Spring MVC (web)", "GET /conquistas/students/{id}")
    Component(progApi, "progression.api", "Interface, evento e enum", "StudentProgressionApi, StudentScoreChangedEvent, Level")
    Component(prog, "StudentService", "progression.internal", "Publica StudentScoreChangedEvent")

    Boundary(mod, "Módulo conquistas") {
      Component(api, "ConquistasApi", "conquistas.api", "Fachada pública e BadgeDTO")
      Component(app, "ConquistasService + ConquistasConfig", "conquistas.internal", "Implementa a fachada e monta o domínio")

      Boundary(dom, "Domínio puro: sem Spring, JPA ou adapters") {
        Component(svc, "BadgeAwardService", "POJO", "Regra de concessão idempotente")
        Component(rules, "BadgeRules + BadgeRule", "POJO", "Catálogo de regras")
        Component(port, "AwardedBadgePort", "Porta (interface)", "Persistência de badges concedidos")
      }

      Boundary(adp, "Adapters") {
        Component(listener, "StudentScoreEventListener", "Adapter de entrada, Spring", "Evento para o domínio; isola falhas")
        Component(jpa, "JpaAwardedBadgeAdapter", "Adapter de saída, Spring Data JPA (produção)", "Implementa a porta")
        Component(fake, "InMemoryAwardedBadgeAdapter", "Adapter de saída, em memória (testes)", "Implementa a porta")
      }
    }
  }

  Rel(ctrl, api, "Chama")
  Rel(app, api, "Implementa")
  Rel(app, svc, "Consulta badges")
  Rel(app, progApi, "Valida aluno")
  Rel(prog, listener, "StudentScoreChangedEvent", "ApplicationEvent em processo")
  Rel(listener, svc, "Avalia regras")
  Rel(svc, rules, "Usa")
  Rel(svc, port, "Usa")
  Rel(jpa, port, "Implementa")
  Rel(fake, port, "Implementa")
  Rel(jpa, db, "JPA/JDBC")
```

Sentido das dependências: os adapters apontam para o domínio, e o domínio não aponta para nenhum deles.
`Level` é o único tipo externo que o domínio usa.

### 9. Evidência

**Hipótese testada:** isolar a regra atrás de uma porta permite trocar a persistência sem mudar o
comportamento da regra, testá-la sem infraestrutura e alterá-la sem tocar nos adapters, sem afetar os
endpoints existentes.

A microsolução está no próprio Core, em
[`conquistas/`](../../src/main/java/org/example/ac1devops/conquistas/). Resultados de 2026-11-02:

| # | O que se testa | Como reproduzir | Resultado obtido |
|---|---|---|---|
| 1 | Mesma regra nos dois adapters | `mvnw.cmd test -Dtest=BadgeAwardInMemoryTest,BadgeAwardJpaIntegrationTest` | Os **mesmos 9 cenários** de [`BadgeAwardContract`](../../src/test/java/org/example/ac1devops/conquistas/internal/domain/BadgeAwardContract.java) passam nos dois. |
| 2 | Os cenários exercitam mesmo a regra | Trocar temporariamente `xpAtLeast(1, ...)` por `xpAtLeast(2, ...)` e repetir o comando | **Os mesmos 3 cenários falham em cada adapter**: [`d2-mutacao-regra.txt`](evidencias/d2-mutacao-regra.txt). |
| 3 | Teste sem infraestrutura | Rodar cada teste isolado e comparar o log | Em memória: **0,3 s**, sem Spring e sem banco. JPA: **7,9 s**, com Spring e H2. |
| 4 | Regra nova sem tocar nos adapters | Cenário `deveAplicarRegraNovaSemAlterarOAdapter` | Passa nos dois adapters, sem mudança neles. |
| 5 | Fronteira do domínio | Criar no domínio uma classe com `@Component` e um campo `JpaAwardedBadgeAdapter`; rodar `mvnw.cmd test -Dtest=ModularArchitectureTest` | Build **falha em 3 regras**: [`d2-archunit-violacao.txt`](evidencias/d2-archunit-violacao.txt). |
| 6 | Falha em Conquistas não derruba o XP | `mvnw.cmd test -Dtest=ConquistasFailureIsolationTest` | Com a persistência falhando e com a restrição única violada, `POST /students/{id}/xp` responde **200** e salva o XP. |
| 7 | Endpoints existentes e fluxo real | `StudentEndpointIntegrationTest`, `ConquistasEndpointIntegrationTest` e chamadas em PostgreSQL 16 | `/students` e `/ranking` respondem como antes; 350 XP geram 3 badges, sem duplicar no evento seguinte. |
| 8 | Concorrência em PostgreSQL | [`concorrencia_xp.py`](../../labs/desafio-2/concorrencia_xp.py), com os comandos abaixo | **2.000 de 2.000 XP** nas 10 rodadas, 3 level-ups, 5 badges, nada repetido e 400 de 400 POST com 200: [`d2-concorrencia.txt`](evidencias/d2-concorrencia.txt). |
| 9 | Concorrência no build | `mvnw.cmd test -Dtest=XpConcurrencyIntegrationTest` | 40 POST em paralelo, com HTTP real e H2: 2.000 de 2.000 XP. O teste falhava antes da correção, com 350 XP. |
| 10 | Build completo | `mvnw.cmd -B verify` | **88 testes**, 0 falhas. **100%** de cobertura: linhas 259/259, branches 30/30. |

Comandos da linha 8 (PowerShell). A porta 55432 evita conflito com um PostgreSQL local:

```powershell
docker run -d --rm --name d2-pg -e POSTGRES_DB=ac1devops_db -e POSTGRES_USER=postgres -e POSTGRES_PASSWORD=postgres -p 55432:5432 postgres:16
.\mvnw.cmd -B package -DskipTests
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:55432/ac1devops_db"; $env:SERVER_PORT="18081"
java -jar target\Grupo10_ATDD-0.0.1-SNAPSHOT.jar
# em outro terminal:
python labs\desafio-2\concorrencia_xp.py
docker stop d2-pg
```

#### Defeito encontrado pela evidência

A primeira versão passava em todos os testes com H2 e mocks, mas o teste em PostgreSQL mostrou **2 de 40
requisições com 500** em `POST /students/{id}/xp`. Quando duas requisições disputavam o mesmo badge, o
rollback da restrição única limpava a sessão do open-in-view, e o `StudentService` ainda lia a coleção lazy
depois do evento. A correção foi montar a resposta antes de publicar o evento, com o teste
`violacaoDaRestricaoUnicaNaoDeveAfetarAConcessaoDeXp` reproduzindo a falha antes do ajuste.

#### Correção da perda de XP

Os mesmos testes revelaram um problema anterior a este desafio: `receiveXp` lia e gravava sem controle de
concorrência, e requisições simultâneas sobrescreviam o XP umas das outras.

| 10 rodadas de 40 POST de 50 XP no mesmo aluno, em PostgreSQL 16 | Antes | Depois |
|---|---|---|
| XP aplicado, de 2.000 | 150 a 350 | **2.000 em todas** |
| Level-ups por aluno (correto: 3) | 6 a 13 | **3 em todas** |
| Badges por aluno | 3, em desacordo com os 2.000 XP enviados | **5, nenhum repetido** |
| Respostas | 400 de 400 com 200, mesmo perdendo XP | **400 de 400 com 200, com o XP aplicado** |

- **Como funciona:** `StudentEntity` ganhou `@Version`. Quem perde a disputa lê o estado de novo e repete,
  em até 15 tentativas com espera aleatória crescente. XP, nível e level-ups são gravados juntos, e o evento
  que concede badges só sai depois da gravação que valeu, uma única vez.
- **Se as tentativas acabarem:** a API responde **409** e nenhum XP é aplicado; o cliente fica sabendo, em vez
  de o XP sumir em silêncio. O caso é coberto por teste, mas não ocorreu nas rodadas acima.
- **Banco existente:** testado subindo a versão nova sobre um PostgreSQL com dados da versão antiga. A coluna
  `version` entrou com valor 0, sem perda de dados, e o aluno antigo seguiu recebendo XP.

#### CI no GitHub

Run #2 do workflow `CI`, no commit `03007e1`: **Success**. O **aviso de Node.js 20 foi resolvido**: o run #1 tinha
2 warnings e 1 notice, e o run #2 mostra só 1 notice, sem warnings, com as actions atualizadas.

| Lista de runs | Etapas do job e anotações |
|---|---|
| ![Lista com os runs #1 e #2 verdes](evidencias/img/d2-ci-verde.png) | ![Run #2 com 1 notice, sem warnings](evidencias/img/d2-ci-etapas.png) |

#### Limites desta evidência

- **Disputa extrema:** 20 requisições simultâneas no mesmo aluno são improváveis no uso real; o teste mostra
  que nada se perde, mas o tempo extra causado pelas novas tentativas não foi medido.
- **Latência:** o custo da avaliação síncrona em `POST /students/{id}/xp` não foi medido.
- **Tempos da linha 3:** são de uma única execução; o que importa é a ordem de grandeza, não o valor exato.

---

## Desafio 3 — Serviço Python e IA

Onde fica a fronteira de ML e RAG/LLM, se os modelos são feitos em Python, têm ciclo de atualização próprio e
carga variável e podem ficar indisponíveis sem impedir as operações essenciais do Core.

### 1. RF

> **Numeração:** RF03 e RF04 seguem o enunciado deste desafio. Não confundir com os RF03 e RF04 do Desafio 1
> (cadastrar aluno e conceder XP).

| RF | Estado | Classes / endpoints reais | Efeito deste desafio |
|---|---|---|---|
| **RF03 — Recomendação inteligente** | **Existe, com regra simulada.** | Core: `GET /recomendacoes/students/{id}` → `RecomendacaoController` → `RecomendacaoApi` → `RecommendationService` → `RecommendationPort` ← `IaRecommendationAdapter`. Serviço: `POST /recomendacoes` | Módulo novo `recomendacao` e serviço novo em [`labs/desafio-3/ia-service`](../../labs/desafio-3/ia-service/). |
| **RF04 — Assistente educacional** | **Parcial.** Só no serviço, com resposta simulada. | Serviço: `POST /assistente`. O Core ainda não consome. | Define a fronteira; a integração fica para quando houver necessidade. |

A regra de recomendação é determinística: cursos do nível do aluno, depois dos níveis seguintes, sem repetir os
concluídos. O catálogo do serviço e a lista de cursos populares são **propostas da equipe**, porque o Core ainda
não tem cursos nem dados de popularidade.

### 2. RNF (FURPS+)

| RNF | FURPS+ | Meta mensurável | Resultado |
|---|---|---|---|
| **Tolerância a falhas** | R (Reliability) | Serviço fora do ar ou lento: recomendações respondem 200 em até 1,5 s, e o fluxo essencial não muda | Atendida: 0,02 s fora do ar; 0,82 s lento; 0,007 s com o circuito aberto |
| **Desempenho** | P (Performance) | Espera máxima limitada pelo timeout, e sem espera com o circuito aberto | Atendida no ambiente local: 5,03 s direto no serviço lento contra 0,82 s pelo Core |
| **Escalabilidade** | P (Performance: capacidade) | Réplicas da IA sem réplicas do Core | **Só habilitada pela estrutura:** nenhum teste de carga foi feito |
| **Deploy independente** | + (restrição de implantação) | Imagem e pipeline próprios; Core sobe e responde sem a IA | Atendida: imagem própria, job próprio no CI e serviço reiniciado sem reiniciar o Core |
| **Interoperabilidade** | + (requisito de interface) | Contrato verificado pelos dois lados | Atendida: mesmos JSON nos testes pytest e Java |
| **Manutenibilidade** | S (Supportability) | Domínio sem Spring nem HTTP; 0 violações do ArchUnit | Atendida: 10 regras verdes |

### 3. ASR

| ASR | RNF de origem | Como altera a arquitetura (uma frase) |
|---|---|---|
| **ASR6 — Indisponibilidade da IA não bloqueia o Core** | Tolerância a falhas e Desempenho | Altera a **comunicação**: toda chamada à IA tem timeout curto, circuit breaker e uma resposta degradada definida pelo domínio. |
| **ASR7 — IA publicada e escalada sem o Core** | Deploy independente e Escalabilidade | Altera a **implantação**: a IA vira um contêiner próprio, com imagem e job de CI próprios. |
| **ASR8 — Contrato HTTP/JSON verificável pelos dois lados** | Interoperabilidade | Altera a **operação**: o contrato vira arquivos versionados, verificados pelos testes das duas linguagens em todo build. |

Manutenibilidade não gera ASR novo: já está coberta pelas ASR1 e ASR4.

### 4. RPC

| RPC | Restrição | Impacto na decisão |
|---|---|---|
| **RPC1** | Core permanece em Java/Spring Boot. | A IA não entra no processo do Core em outra linguagem. |
| **RPC7** | ML e RAG em Python. | Exige uma fronteira com o Core, seja de processo ou de serviço. |
| **RPC8** | A indisponibilidade da IA não pode bloquear o fluxo essencial. | Limita o quanto o Core pode esperar e o que pode depender da IA. |

### 5. Decisão do Tech Lead

**Python por si só justifica um serviço?** **Não.** A linguagem poderia ser resolvida exportando o modelo para a
JVM ou gerando recomendações em lote. O serviço se justifica por ciclo de atualização próprio, carga variável e
falhas que não podem alcançar o Core.

**ML e RAG precisam da mesma fronteira?** **Não precisam, mas podem compartilhar enquanto nada exigir o contrário.**
Eles têm naturezas diferentes:

| Aspecto | ML (recomendação) | RAG/LLM (assistente) |
|---|---|---|
| Latência esperada | Milissegundos | Segundos |
| Dependências | Modelo treinado | Base vetorial e provedor de LLM |
| Atualização | Novo treino | Novos documentos ou troca de LLM |
| Custo | Principalmente de infraestrutura | Também por uso, se o LLM for externo |
| Sem o serviço | Lista de populares | Aviso de indisponibilidade |

A decisão é **um serviço com rotas separadas e sem estado compartilhado**, porque hoje as duas funções são simulações
e dois serviços trariam custo sem problema medido. No Core, cada função terá timeout e circuit breaker próprios.
Separar será reavaliado por critérios medidos (ver [ADR-003](adr/ADR-003-servico-python-de-ia.md)).

**Quais ASRs justificam autonomia e escala?** As **ASR6 e ASR7**: a IA precisa falhar, ser atualizada e escalar sem o
Core. Isso atende ao critério do ADR-001, que pedia necessidade concreta de deploy e escala independentes. A ASR8 é o
custo dessa autonomia: com dois processos, o contrato precisa ser verificado.

**Qual comportamento degradado é aceitável?**

| Situação | Comportamento | Aceitável? |
|---|---|---|
| Recomendação com a IA fora do ar ou lenta | Cursos populares, com `degraded: true` e HTTP 200 | **Sim** |
| Assistente com a IA fora do ar (quando integrado) | Aviso claro de indisponibilidade | **Sim** |
| XP, level-up, ranking e conquistas | Sem qualquer alteração, pois não chamam a IA | **Obrigatório** |
| Erro 500 ou espera longa para o aluno | — | Não |
| Inventar uma resposta do assistente | — | Não: o aluno tomaria uma simulação por resposta real |
| Esconder que a resposta está degradada | — | Não: o cliente precisa poder distinguir |

**Decisão:** **serviço Python independente**, com comunicação HTTP/JSON, timeouts curtos, circuit breaker e modo
degradado no Core. Registro formal no [ADR-003](adr/ADR-003-servico-python-de-ia.md).

### 6. Alternativas e trade-offs

| Critério | (A) IA embutida no Core | (B) Módulo acoplado | (C) Serviço Python independente |
|---|---|---|---|
| Ciclo de atualização | Modelo novo exige publicar o Core | Publicação única | **Independente** |
| Escala | Do conjunto | Do conjunto | **Da IA, separadamente** |
| Falha da IA | Atinge o processo do Core | Atinge o processo ou contêiner do Core | **Isolada, tratada com timeout e fallback** |
| Python e bibliotecas de ML | Limitado ao que roda na JVM | Atende | **Atende** |
| Latência da chamada | Sem rede | Pequena | Com rede (cerca de 20 ms medidos localmente) |
| Complexidade operacional | Mínima | Média | Maior: 2 imagens, 2 jobs de CI e contrato |
| Aderência às RPCs | Conflita com a RPC7 | Conflita com a RPC8 | **Atende** |

| ML e RAG | Juntos em um serviço (escolhido) | Em dois serviços |
|---|---|---|
| Operação | **1 imagem e 1 job** | 2 imagens e 2 jobs |
| Escala | Do conjunto: uma função pode pressionar a outra | Por função |
| Publicação | Em conjunto | Independente |
| Quando faz sentido | **Enquanto não houver diferença medida** | Quando os critérios do ADR-003 forem atingidos |

### 7. ADR

[**ADR-003 — Serviço Python independente para IA, com modo degradado no Core**](adr/ADR-003-servico-python-de-ia.md)
— status **Aceito**, 2026-11-02.

### 8. C4

Nível afetado: **Container**. Os demais níveis seguem como nos desafios anteriores.

```mermaid
C4Container
  title Educação Continuada Gamificada — Containers com o serviço de IA

  Person(aluno, "Aluno")
  Person(equipe, "Equipe (6 devs)")

  System_Boundary(plataforma, "Plataforma") {
    Container(spa, "Front-end", "Vue 3 via CDN", "Servido pelo próprio Core")
    Container(core, "Core API", "Java 21, Spring Boot 4.1 — Monólito Modular", "Fluxo essencial e módulo recomendacao, com circuit breaker e fallback")
    ContainerDb(db, "Banco relacional", "PostgreSQL 16", "students, level_up_events, student_badges")
    Container(ia, "Serviço de IA", "Python 3.14, FastAPI", "POST /recomendacoes, POST /assistente (simulado), GET /health. Imagem própria e escala independente")
  }

  System_Ext(gha, "GitHub Actions", "Job do Core e job do serviço de IA")

  Rel(aluno, spa, "Usa", "HTTPS")
  Rel(spa, core, "Chama /students", "JSON/HTTP")
  Rel(core, db, "Lê e escreve", "JPA/JDBC")
  Rel(core, ia, "Pede recomendações; opcional para o Core", "HTTP/JSON síncrono; connect 300 ms, read 800 ms")
  Rel(equipe, gha, "push / Pull Request")
```

**Protocolo:** HTTP/JSON síncrono, com connect timeout de 300 ms e read timeout de 800 ms. Sem resposta dentro do
limite, o Core usa a lista de populares. Os contratos estão em
[`contract/`](../../labs/desafio-3/ia-service/contract/). O endereço vem de `IA_SERVICE_URL`.

Prometheus e Grafana seguem planejados (Desafio 1, item 9.6) e foram omitidos aqui para destacar a mudança.

### 9. Evidência

**Hipótese testada:** com a IA fora do Core e o cliente protegido por timeout, circuit breaker e fallback, a queda ou a
lentidão do serviço não bloqueiam o fluxo essencial, e as recomendações seguem respondendo em tempo limitado.

| # | O que se testa | Como reproduzir | Resultado obtido |
|---|---|---|---|
| 1 | Serviço fora do ar (automatizado) | `mvnw.cmd test -Dtest=RecomendacaoEndpointIntegrationTest` | Com o WireMock derrubado, o endpoint responde **200 degradado** em menos de 1,5 s, e XP, aluno, ranking e conquistas seguem com 200. |
| 2 | Lentidão, erro, conexão perdida, resposta inválida | `mvnw.cmd test -Dtest=IaRecommendationAdapterTest` | **7 testes verdes.** Todas as falhas viram indisponibilidade, e o timeout é respeitado. |
| 3 | Circuit breaker | Mesmo comando | Depois de 5 falhas o circuito **abre e o serviço não recebe mais chamadas**; quando volta, o circuito fecha sozinho. |
| 4 | Regra degradada sem Spring | `mvnw.cmd test -Dtest=RecommendationServiceTest` | 3 testes com portas falsas, em 0,01 s. |
| 5 | Os testes detectam a quebra | Remover o fallback ou criar em `ranking` uma dependência de `recomendacao` | Build **falha nos dois casos**: [`d3-falhas-simuladas.txt`](evidencias/d3-falhas-simuladas.txt). |
| 6 | Contrato nos dois lados | `pytest` e `IaRecommendationAdapterTest` | Os mesmos JSON são verificados contra o serviço real e pelo stub. |
| 7 | Serviço em Python | `cd labs\desafio-3\ia-service`, `pip install -r requirements-dev.txt`, `pytest` | **12 testes verdes**. |
| 8 | Roteiro manual, com a imagem real | Comandos abaixo | Tabela abaixo e [`d3-roteiro.txt`](evidencias/d3-roteiro.txt). |
| 9 | Stack completa | `docker compose up -d --build` | App, PostgreSQL e serviço sobem juntos: `degraded: false` e, depois de parar o serviço, `degraded: true`, com XP em 200. Testado em portas alternativas, porque a 5432 está ocupada nesta máquina. |
| 10 | Build completo | `mvnw.cmd -B clean verify` | **101 testes**, 0 falhas. **100%** de cobertura: linhas 334/334, branches 32/32. |

#### Resultados do roteiro

Medidos com `curl` em 2026-11-02, sem carga, em execução única.

| Situação | Tempo de resposta | Resposta |
|---|---|---|
| Serviço no ar, primeira chamada | 0,41 s | Recomendação da IA, `degraded: false` |
| Serviço no ar, chamadas seguintes | 0,02 s | Recomendação da IA, `degraded: false` |
| Serviço fora do ar | 0,01 a 0,03 s | Populares, `degraded: true` |
| XP, aluno, ranking e conquistas com o serviço fora do ar | 0,01 a 0,05 s | HTTP 200, sem alteração |
| Serviço lento (5 s), chamada direta, **sem proteção** | **5,03 s** | — |
| Serviço lento, pelo Core, antes de o circuito abrir | **0,82 s** | Populares, `degraded: true` |
| Serviço lento, pelo Core, com o circuito aberto | **0,007 s** | Populares, `degraded: true` |
| Serviço de volta, circuito ainda aberto | 0,03 s | Populares, `degraded: true` |
| Serviço de volta, depois de 30 s | 0,30 s e depois 0,03 s | Recomendação da IA, `degraded: false` |

Execução do Bruno em 2026-11-02, com os mesmos comandos do roteiro e o log SQL ligado no Core:

| Situação | Tempo de resposta | Resposta | Print |
|---|---|---|---|
| Serviço no ar, primeira chamada | 0,81 s | Recomendação da IA, `degraded: false` | [`d3-02`](evidencias/img/d3-02-core-com-ia.png) |
| Serviço fora do ar | 0,12 s | Populares, `degraded: true` | [`d3-03`](evidencias/img/d3-03-core-ia-fora.png) |
| XP com o serviço fora do ar | 0,14 s | HTTP 200 | [`d3-04`](evidencias/img/d3-04-fluxo-essencial.png) |
| Ranking com o serviço fora do ar | 0,15 s | HTTP 200 | [`d3-04`](evidencias/img/d3-04-fluxo-essencial.png) |

Os tempos absolutos são maiores que os da primeira tabela, mas o resultado é o mesmo: as respostas continuam
com 200, e a recomendação passa a `degraded: true` sem esperar pelo serviço parado.

| Serviço no ar e `/health` | Recomendação com a IA |
|---|---|
| ![docker run e /health com status ok](evidencias/img/d3-01-ia-health.png) | ![Recomendação com degraded false](evidencias/img/d3-02-core-com-ia.png) |

| `docker stop` e modo degradado | Fluxo essencial com a IA fora |
|---|---|
| ![docker stop e recomendação com degraded true](evidencias/img/d3-03-core-ia-fora.png) | ![XP e ranking com HTTP 200](evidencias/img/d3-04-fluxo-essencial.png) |

O teste do serviço lento (`d3-05`) era opcional e não foi repetido nessa execução.

**CI no GitHub:** os dois jobs ficaram verdes nos runs #3 (`b1a3d3a`) e #4 (`3c3cce2`). O job do serviço de IA executou os
12 testes pytest e o build da imagem, em 25 s, separado do job do Core.

| Run #4 com os 2 jobs verdes | Etapa `Testes pytest` com 12 testes |
|---|---|
| ![Resumo do run #4 com os dois jobs verdes](evidencias/img/d3-06-ci-dois-jobs.png) | ![Log do pytest com 12 passed](evidencias/img/d3-07-ci-pytest.png) |

#### Roteiro manual

Comandos em PowerShell, na raiz do projeto. Usa o banco H2 e as portas 8000 e 8080.

```powershell
# Terminal 1: sobe o servico de IA
docker build -t ia-service:lab labs/desafio-3/ia-service
docker run -d --rm --name ia-service -p 8000:8000 ia-service:lab
curl.exe -s http://localhost:8000/health

# Terminal 2: sobe o Core
.\mvnw.cmd -B package -DskipTests
$env:SPRING_PROFILES_ACTIVE="h2"; java -jar target\Grupo10_ATDD-0.0.1-SNAPSHOT.jar

# Terminal 1: cria um aluno, da XP e pede recomendacoes com o servico no ar
Invoke-RestMethod -Method Post -Uri http://localhost:8080/students -ContentType application/json -Body '{"name":"Aluno Roteiro"}' | ConvertTo-Json -Compress
Invoke-RestMethod -Method Post -Uri http://localhost:8080/students/1/xp -ContentType application/json -Body '{"reason":"DESAFIO","amount":150}' | ConvertTo-Json -Compress
curl.exe -s -w "`nHTTP %{http_code} em %{time_total}s`n" http://localhost:8080/recomendacoes/students/1

# Derruba o servico e chama de novo: a resposta muda para degraded=true
docker stop ia-service
curl.exe -s -w "`nHTTP %{http_code} em %{time_total}s`n" http://localhost:8080/recomendacoes/students/1

# Fluxo essencial com o servico fora do ar
curl.exe -s -o NUL -w "XP: HTTP %{http_code} em %{time_total}s`n" -X POST http://localhost:8080/students/1/xp -H "Content-Type: application/json" --data-binary "@labs/desafio-3/xp-exemplo.json"
curl.exe -s -o NUL -w "Ranking: HTTP %{http_code} em %{time_total}s`n" http://localhost:8080/ranking

# Opcional: servico lento. Primeiro direto (5 s) e depois pelo Core (depois de 30 s, 5 vezes)
docker run -d --rm --name ia-service -e IA_DELAY_MS=5000 -p 8000:8000 ia-service:lab
curl.exe -s -o NUL -w "Direto: %{time_total}s`n" -X POST http://localhost:8000/recomendacoes -H "Content-Type: application/json" --data-binary "@labs/desafio-3/ia-service/contract/recomendacoes-requisicao.json"
curl.exe -s -o NUL -w "Pelo Core: %{time_total}s`n" http://localhost:8080/recomendacoes/students/1
docker stop ia-service
```

#### Limites desta evidência

- **Simulação:** o serviço usa uma regra determinística, sem modelo, GPU ou LLM. A evidência testa a fronteira, não a
  qualidade das recomendações.
- **Carga variável:** nenhum teste de carga foi feito, e a escala independente foi apenas habilitada pela estrutura.
- **Threads:** não há bulkhead. Antes de o circuito abrir, requisições simultâneas podem ocupar threads do Core por até
  0,8 s cada.
- **Read timeout:** limita a espera até o início da resposta; um corpo enviado muito lentamente não é coberto.
- **Réplicas:** o estado do circuito é por instância do Core.
- **Tempos:** são de uma execução, em ambiente local; valem como ordem de grandeza.
- **Aviso nos testes pytest:** o Starlette avisa que o uso de `httpx` no cliente de testes está depreciado. Não afeta os
  resultados.

---

## Desafio 4 — Comunicação e eventos

O que precisa ser imediato e o que pode ser eventual, se ao concluir um curso o sistema registra a conclusão, calcula
recompensas, atualiza informações e pode disparar notificação e analytics, e a falha de uma notificação não pode impedir a
conclusão.

### 1. RF

> **Numeração:** RF02 é o mesmo do Desafio 2. RF05 segue o enunciado deste desafio; não confundir com o RF05 do Desafio 1
> (consultar perfil).

| RF | Estado | Classes / endpoints reais | Efeito deste desafio |
|---|---|---|---|
| **Eventos de conclusão de atividades** | **Existe.** | `POST /cursos/{courseId}/alunos/{studentId}/conclusao` → `CursosController` → `CursosApi` → `CourseCompletionService`; `GET /cursos/eventos`; `POST /cursos/eventos/{id}/reenvio` | Módulo novo `cursos`, com outbox. |
| **RF05 — Notificações** | **Existe, com canal simulado.** | `CursoConcluidoEvent` → `NotificacaoService` → `NotificationChannel` ← `SimulatedNotificationChannel`; `GET /notificacoes/students/{id}` | Módulo novo `notificacoes`. |
| **RF02 — Conquistas** | **Existe.** | Badges concedidos a partir do XP da conclusão | Reage ao evento de pontuação depois da transação. |

Analytics é citado no cenário, mas **não foi implementado**: seria um segundo consumidor, o que é um dos gatilhos para adotar
um broker (item 5). O catálogo de cursos e as recompensas são **propostas da equipe**, porque o Core não tinha cursos.

### 2. RNF (FURPS+)

| RNF | FURPS+ | Meta mensurável | Resultado |
|---|---|---|---|
| **Tolerância a falhas** | R (Reliability) | Canal fora do ar: conclusão responde 201 | Atendida: 800 de 800 com 201, em PostgreSQL |
| **Disponibilidade** | R (Reliability) | Conclusão não depende do consumidor | Atendida: nenhuma chamada ao canal na requisição, verificado por teste e ArchUnit |
| **Latência** | P (Performance) | Consumidor lento não atrasa a resposta | Atendida: teste com consumidor preso responde em menos de 1,5 s; sob carga, p95 de 784 ms com o canal no ar e de 920 ms fora do ar |
| **Consistência** | R (Reliability) | Conclusão, XP e evento juntos, ou nada | Atendida: falha ao gravar o evento desfaz tudo; 0 alunos com XP divergente |
| **Desacoplamento** | S (Supportability) | Quem conclui não conhece o consumidor | Atendida: regra do ArchUnit |
| **Recuperabilidade** | R (Reliability) | Nenhum evento confirmado se perde, mesmo com queda do processo | Atendida: 1.601 notificações para 1.601 conclusões, sem repetição |

### 3. ASR

| ASR | RNF de origem | Como altera a arquitetura (uma frase) |
|---|---|---|
| **ASR9 — Conclusão independente dos consumidores** | Tolerância a falhas, Disponibilidade, Latência e Desacoplamento | Altera a **comunicação**: a conclusão responde sozinha, e o consumidor recebe o evento em outra thread, depois do commit. |
| **ASR10 — Conclusão e recompensa atômicas e exatamente uma vez** | Consistência | Altera os **dados**: uma única transação, com bloqueio otimista e restrição única por aluno e curso. |
| **ASR11 — Nenhum evento confirmado se perde** | Recuperabilidade | Altera os **dados e a operação**: o evento é gravado na transação, e a operação ganha novas tentativas, estado de falha e reenvio manual. |

### 4. RPC

| RPC | Restrição | Impacto na decisão |
|---|---|---|
| **RPC9** | Operações essenciais do Core devem concluir mesmo com consumidor externo indisponível. | Nenhum consumidor pode participar da transação de conclusão. |
| **RPC10** | Adotar broker exige custo operacional. | O broker precisa responder a um problema real deste fluxo para compensar. |

### 5. Decisão do Tech Lead

**O que precisa ser imediato e o que pode ser eventual?**

| Passo | Quando | Por quê |
|---|---|---|
| Conclusão, XP, nível e level-ups | **Síncrono, mesma transação** | O aluno precisa ver o resultado, e não pode haver recompensa sem conclusão |
| Registro do evento na outbox | **Síncrono, mesma transação** | É o que garante que o aviso não se perde |
| Cache do ranking e badges | Logo depois da transação, na mesma requisição | Dependem do XP gravado; uma falha é registrada e não desfaz a conclusão |
| Notificação | **Eventual** | Depende de um canal externo, e atrasar minutos é aceitável |
| Analytics (futuro) | **Eventual** | Não afeta o aluno |

**Que ASR justificaria mensageria?** Nenhuma das atuais exige um broker: as ASR9 a ASR11 são atendidas com a outbox.
Justificaria um requisito de **alcançar outros processos ou consumidores independentes**, ou de **reprocessar o histórico**.

**RabbitMQ ou Kafka resolvem um problema real deste fluxo?** Hoje, **não**. Há um consumidor, no mesmo processo. E o risco
principal é a escrita dupla: publicar no broker depois do commit perde o evento se o processo cair entre os dois passos, então
um broker **ainda exigiria uma outbox**. Ele acrescentaria um serviço para operar sem eliminar esse risco.

**RabbitMQ x Kafka, pelo problema do case:**

| Critério | RabbitMQ (fila de tarefas com reentrega) | Kafka (log de eventos com replay) |
|---|---|---|
| Modelo | Mensagem é uma tarefa, removida depois de confirmada | Evento é um registro retido, e cada consumidor controla sua posição |
| Notificar o aluno | **Encaixe direto:** uma tarefa, reentrega e dead letter | Possível, mas novas tentativas e dead letter ficam a cargo do consumidor |
| Analytics reprocessando o passado | Não atende, porque a mensagem já saiu da fila | **Encaixe direto** |
| Vários consumidores independentes | Atende, com uma fila por consumidor | Atende, com grupos de consumidores |
| Custo para 6 devs | Um serviço a mais | Maior: partições, retenção e cluster |
| **Se o gatilho surgir** | **Primeira opção**, se a necessidade for entregar tarefas a outro processo | Primeira opção, se a necessidade for replay |

**Decisão:** **outbox transacional com eventos internos**, sem broker, com gatilhos medidos para migrar. Os gatilhos estão no
[ADR-004](adr/ADR-004-eventos-com-outbox-transacional.md), e a migração fica limitada ao `OutboxRelay`.

### 6. Alternativas e trade-offs

| Critério | (A) Síncrono | (B) `@Async` sem persistência | (C) Outbox + eventos internos | (D) RabbitMQ | (E) Kafka |
|---|---|---|---|---|---|
| Consumidor fora do ar | **Falha para o aluno** | Conclui, mas o evento se perde | **Conclui e entrega depois** | Conclui e entrega depois | Conclui e entrega depois |
| Queda logo depois do commit | Sem aviso | **Evento perdido** | **Recuperado** | Perdido, sem outbox | Perdido, sem outbox |
| Serviços novos | 0 | 0 | **0** | 1 | 1 ou mais |
| Consumidor em outro processo | Por chamada direta | Não | Não | **Sim** | **Sim** |
| Replay do histórico | Não | Não | Não | Não | **Sim** |
| Aderência às RPCs | Conflita com a RPC9 | Atende | **Atende** | Custo da RPC10 sem problema correspondente | Custo da RPC10 sem problema correspondente |

### 7. ADR

[**ADR-004 — Eventos internos com outbox transacional, sem broker por enquanto**](adr/ADR-004-eventos-com-outbox-transacional.md)
— status **Aceito**, 2026-11-03.

### 8. C4

Nível afetado: **Container**. Não há container novo: essa é a consequência da decisão. A outbox fica dentro do banco, e os eventos
circulam dentro do Core; o broker aparece só como planejado, condicionado aos gatilhos.

```mermaid
C4Container
  title Educação Continuada Gamificada — Containers com eventos de conclusão

  Person(aluno, "Aluno")

  System_Boundary(plataforma, "Plataforma") {
    Container(core, "Core API", "Java 21, Spring Boot 4.1 — Monólito Modular", "cursos publica CursoConcluidoEvent em processo; notificacoes consome")
    ContainerDb(db, "Banco relacional", "PostgreSQL 16 (porta 5433 no host)", "Inclui course_completions, course_outbox_events e notifications")
    Container(ia, "Serviço de IA", "Python 3.14, FastAPI", "Inalterado; opcional para o Core")
    Container(broker, "Broker (planejado)", "RabbitMQ ou Kafka", "Só se os gatilhos B1 a B5 do ADR-004 ocorrerem")
  }

  System_Ext(canal, "Canal de notificação", "E-mail ou push; simulado neste lab")

  Rel(aluno, core, "Conclui curso", "HTTPS, síncrono")
  Rel(core, db, "Conclusão, XP e outbox na mesma transação; varredura", "JPA/JDBC")
  Rel(core, ia, "Pede recomendações", "HTTP/JSON, com timeout")
  Rel(core, canal, "Envia notificação, de forma assíncrona e com novas tentativas", "Porta NotificationChannel")
```

**Fluxo crítico:** a parte síncrona termina no commit; o que vem depois não muda a resposta.

```mermaid
sequenceDiagram
    autonumber
    actor Aluno
    participant Cursos as Core: cursos
    participant Prog as Core: progression
    participant DB as PostgreSQL
    participant Relay as Core: OutboxRelay
    participant Notif as Core: notificacoes
    participant Canal as Canal externo

    rect rgb(225, 240, 255)
    Note over Aluno,DB: SÍNCRONO: mesma transação
    Aluno->>Cursos: POST /cursos/{c}/alunos/{a}/conclusao
    Cursos->>DB: grava conclusão (única por aluno e curso)
    Cursos->>Prog: concede XP na transação
    Prog->>DB: grava XP, nível e level-ups (@Version)
    Cursos->>DB: grava evento na outbox (PENDING)
    DB-->>Cursos: COMMIT
    end
    Cursos->>Cursos: badges e cache do ranking, sem desfazer a conclusão
    Cursos-->>Aluno: 201 com XP e nível

    rect rgb(255, 243, 224)
    Note over Relay,Canal: ASSÍNCRONO: depois do commit, em outra thread
    Relay->>DB: reserva o evento (update condicional)
    Relay->>Notif: CursoConcluidoEvent
    Notif->>Canal: envia
    alt canal disponível
        Canal-->>Notif: ok
        Notif->>DB: grava notificação (única por evento)
        Relay->>DB: DELIVERED
    else canal fora do ar
        Canal--xNotif: exceção
        Relay->>DB: PENDING com espera maior, ou DEAD ao esgotar
        Note over Relay,DB: a varredura tenta de novo, inclusive depois de uma queda
    end
    end
```

### 9. Evidência

**Hipótese testada:** com o evento gravado na mesma transação, a conclusão não depende do consumidor, e nada se perde mesmo com o
consumidor fora do ar e queda do processo, **sem precisar de broker**.

| # | O que se testa | Como reproduzir | Resultado obtido |
|---|---|---|---|
| 1 | Consumidor fora do ar | `mvnw.cmd test -Dtest=CourseCompletionIntegrationTest` | Conclusão responde **201** com o XP gravado; o evento fica `PENDING` com tentativas e erro; a notificação sai sozinha quando o canal volta. |
| 2 | Consumidor preso | Mesmo comando | Responde em menos de 1,5 s. |
| 3 | Tentativas esgotadas | Mesmo comando | Evento vai para `DEAD`; o reenvio manual entrega. |
| 4 | Queda do processo | Mesmo comando, com evento gravado sem aviso de commit | A varredura entrega; acumulado maior que um lote também é drenado. |
| 5 | Atomicidade | Mesmo comando | Falha ao gravar o evento desfaz conclusão, XP e level-ups. |
| 6 | Uma única recompensa | Mesmo comando | 8 conclusões simultâneas do mesmo curso: uma 201, as demais 200, e XP concedido uma vez. Repetição: 200, sem novo XP nem notificação. |
| 7 | Concorrência e pool | Mesmo comando | 8 cursos simultâneos somam todo o XP; 30 conclusões acima do pool de conexões não travam nem perdem badges. |
| 8 | Os testes detectam a quebra | 4 mudanças temporárias na decisão | Build **falha em todas**: [`d4-falhas-simuladas.txt`](evidencias/d4-falhas-simuladas.txt). |
| 9 | Cenário real em PostgreSQL | Roteiro abaixo | Tabela abaixo e [`d4-postgres.txt`](evidencias/d4-postgres.txt). |
| 10 | Build completo | `mvnw.cmd -B clean verify` | **123 testes**, 0 falhas. **100%** de cobertura: linhas 505/505, branches 54/54. |

#### Resultados em PostgreSQL

Execução única em 2026-11-03, em um computador local, com banco, Core e carga na mesma máquina.

| Cenário | Resultado |
|---|---|
| Conclusão com o canal fora do ar | **201 em 0,13 s**; evento `PENDING`, 1 tentativa e o erro registrado; nenhuma notificação; XP gravado |
| Queda forçada do processo (`Stop-Process -Force`) e volta do canal | 2 eventos pendentes entregues **0,2 s depois de o Core subir**; sem repetição |
| Repetir a conclusão | 200, `alreadyCompleted: true`, XP inalterado |
| Tentativas esgotadas | `DEAD`, mesmo depois de reiniciar; reenvio responde 202, e o evento vai para `DELIVERED` |
| 800 conclusões, 20 threads, canal no ar | 45 por segundo; **800 com 201**; mediana de 393 ms e p95 de 784 ms; 800 notificações em 24,8 s |
| 800 conclusões, 20 threads, canal fora do ar | 40 por segundo; **800 com 201**; mediana de 443 ms e p95 de 920 ms |
| Recuperação do acumulado de 800 | Entregue cerca de 43 s depois de o Core subir |
| Total ao final | 1.601 conclusões e 1.601 notificações; **0 notificações repetidas; 0 alunos com XP divergente** |

#### Execução manual do roteiro

Execução do Bruno em 2026-11-03, com os comandos do roteiro abaixo:

| Passo | Valor observado | Print |
|---|---|---|
| Conclusão com o canal fora do ar | **HTTP 201 em 0,16 s**, 150 XP, nível Prata | [`d4-01`](evidencias/img/d4-01-conclusao-canal-fora.png) |
| Estado depois de 6 s | **`PENDING` com 3 tentativas**, erro `Canal de notificacao indisponivel`, XP 150 gravado, notificações `[]` | [`d4-02`](evidencias/img/d4-02-evento-pendente.png) |
| Queda forçada do Core | PID 24824 encerrado com `Stop-Process -Force` | [`d4-03`](evidencias/img/d4-03-queda-processo.png) |
| Depois de reiniciar com o canal no ar | **`DELIVERED`**, com 5 tentativas no total, e notificação entregue, sem nova chamada | [`d4-04`](evidencias/img/d4-04-recuperado.png) |
| Repetir a conclusão | **HTTP 200 em 0,12 s, `alreadyCompleted: true`**, XP continua 150 | [`d4-04`](evidencias/img/d4-04-recuperado.png) |

A entrega ocorreu na 5ª tentativa, que é o limite padrão. Se o canal tivesse continuado fora do ar, o evento iria para `DEAD`
e dependeria do reenvio manual, que foi verificado no item 9 e no [`d4-postgres.txt`](evidencias/d4-postgres.txt). O print
opcional `d4-05` não foi tirado. O run #4 do CI ficou verde nos dois jobs ([`d4-06`](evidencias/img/d4-06-ci-verde.png)).

| Conclusão com o canal fora do ar | Evento pendente |
|---|---|
| ![HTTP 201 com o canal fora do ar](evidencias/img/d4-01-conclusao-canal-fora.png) | ![Evento PENDING, XP gravado e nenhuma notificação](evidencias/img/d4-02-evento-pendente.png) |

| Queda forçada do processo | Recuperação e repetição |
|---|---|
| ![Core encerrado à força](evidencias/img/d4-03-queda-processo.png) | ![Evento DELIVERED, notificação e repetição com HTTP 200](evidencias/img/d4-04-recuperado.png) |

#### Defeito encontrado pela evidência

A primeira versão passava em todos os testes, mas a carga em PostgreSQL **travou**: com 20 threads, as requisições ficaram mais
de 60 s sem resposta, e só 44 conclusões foram gravadas. O log mostrou `Connection is not available` (`total=10, active=10`) 74
vezes. Os badges rodavam depois do commit em `REQUIRES_NEW`, pedindo uma segunda conexão enquanto a primeira continuava presa;
acima de 10 requisições, todas esperavam umas pelas outras, e badges se perdiam.

A correção foi publicar o evento de pontuação **depois que a transação termina**. O teste
`conclusoesAcimaDoPoolDeConexoesNaoTravamNemPerdemBadges` falhava antes do ajuste, e a carga repetida teve 0 esgotamentos.

#### Roteiro manual

Comandos em PowerShell, na raiz do projeto. Usa as portas 5433 (PostgreSQL) e 8080 (Core).

```powershell
# Terminal 1: sobe o PostgreSQL e gera o jar
docker compose up -d --wait postgres
.\mvnw.cmd -B package -DskipTests

# Terminal 2: sobe o Core com o canal de notificacao fora do ar
$env:SPRING_DATASOURCE_URL="jdbc:postgresql://localhost:5433/ac1devops_db"; $env:NOTIFICACOES_CANAL_DISPONIVEL="false"; java -jar target\Grupo10_ATDD-0.0.1-SNAPSHOT.jar

# Terminal 1: conclui um curso com o canal fora do ar (print d4-01)
$id = (Invoke-RestMethod -Method Post -Uri http://localhost:8080/students -ContentType application/json -Body '{"name":"Aluno Roteiro"}').id; "Aluno $id"
curl.exe -s -w "`nHTTP %{http_code} em %{time_total}s`n" -X POST "http://localhost:8080/cursos/ci-cd/alunos/$id/conclusao"

# Terminal 1: a conclusao ficou gravada, e a notificacao, pendente (print d4-02)
Start-Sleep 6
Invoke-RestMethod http://localhost:8080/cursos/eventos | ForEach-Object { $_ } | Where-Object studentId -eq $id | Format-Table id,status,attempts,lastError
curl.exe -s "http://localhost:8080/students/$id"; ""
curl.exe -s "http://localhost:8080/notificacoes/students/$id"; ""

# Terminal 1: simula a queda do processo (print d4-03)
Get-CimInstance Win32_Process -Filter "Name='java.exe'" | Where-Object CommandLine -like '*Grupo10_ATDD*' | ForEach-Object { Stop-Process -Id $_.ProcessId -Force; "Core encerrado a forca: PID $($_.ProcessId)" }

# Terminal 2: sobe o Core de novo, agora com o canal no ar
$env:NOTIFICACOES_CANAL_DISPONIVEL="true"; java -jar target\Grupo10_ATDD-0.0.1-SNAPSHOT.jar

# Terminal 1: depois que o Core subir, a notificacao foi recuperada sem nova chamada (print d4-04)
Invoke-RestMethod http://localhost:8080/cursos/eventos | ForEach-Object { $_ } | Where-Object studentId -eq $id | Format-Table id,status,attempts,lastError
curl.exe -s "http://localhost:8080/notificacoes/students/$id"; ""
curl.exe -s -w "`nHTTP %{http_code} em %{time_total}s`n" -X POST "http://localhost:8080/cursos/ci-cd/alunos/$id/conclusao"

# Opcional: tentativas esgotadas e reenvio manual (print d4-05)
# Terminal 2: Ctrl+C e depois:
$env:NOTIFICACOES_CANAL_DISPONIVEL="false"; $env:CURSOS_OUTBOX_MAX_ATTEMPTS="2"; $env:CURSOS_OUTBOX_BASE_BACKOFF="1s"; java -jar target\Grupo10_ATDD-0.0.1-SNAPSHOT.jar
# Terminal 1, depois que o Core subir:
curl.exe -s -o NUL -w "Conclusao: HTTP %{http_code}`n" -X POST "http://localhost:8080/cursos/docker-essencial/alunos/$id/conclusao"
Start-Sleep 9
$evento = Invoke-RestMethod http://localhost:8080/cursos/eventos | ForEach-Object { $_ } | Where-Object { $_.studentId -eq $id -and $_.status -eq 'DEAD' } | Select-Object -First 1; $evento | Format-Table id,status,attempts,lastError
# Terminal 2: Ctrl+C e depois:
$env:NOTIFICACOES_CANAL_DISPONIVEL="true"; Remove-Item Env:CURSOS_OUTBOX_MAX_ATTEMPTS; Remove-Item Env:CURSOS_OUTBOX_BASE_BACKOFF; java -jar target\Grupo10_ATDD-0.0.1-SNAPSHOT.jar
# Terminal 1, depois que o Core subir:
curl.exe -s -o NUL -w "Reenvio: HTTP %{http_code}`n" -X POST "http://localhost:8080/cursos/eventos/$($evento.id)/reenvio"
Start-Sleep 7
Invoke-RestMethod http://localhost:8080/cursos/eventos | ForEach-Object { $_ } | Where-Object studentId -eq $id | Format-Table id,status,attempts,lastError

# Opcional: carga
py labs\desafio-4\carga_conclusoes.py 100 20

# Ao terminar: Ctrl+C no Terminal 2 e depois, no Terminal 1:
docker compose stop postgres
```

#### Limites desta evidência

- **Canal simulado:** não há envio real de e-mail ou push.
- **Medição local:** os números de carga são de uma execução, com tudo na mesma máquina; servem de ordem de grandeza e de base para
  os gatilhos, não como capacidade de produção.
- **Latência sob carga:** o p95 maior com o canal fora do ar pode refletir também o trabalho das falhas na mesma máquina.
- **Uma réplica:** a reserva dos eventos foi pensada para várias réplicas, mas só foi testada com uma.
- **Duplicação possível:** uma queda entre o envio e a gravação da notificação pode repetir o envio; não ocorreu nos testes.
- **Tabela da outbox:** cresce sem limpeza, e eventos `DEAD` não geram alerta.

---

## Desafio 5

> **Status:** aguardando enunciado.

### 1. RF

A preencher.

### 2. RNF (FURPS+)

A preencher.

### 3. ASR

A preencher.

### 4. RPC

A preencher.

### 5. Decisão do Tech Lead

A preencher.

### 6. Alternativas e trade-offs

A preencher.

### 7. ADR

A preencher.

### 8. C4

A preencher.

### 9. Evidência

A preencher.

---

## Desafio 6

> **Status:** aguardando enunciado.

### 1. RF

A preencher.

### 2. RNF (FURPS+)

A preencher.

### 3. ASR

A preencher.

### 4. RPC

A preencher.

### 5. Decisão do Tech Lead

A preencher.

### 6. Alternativas e trade-offs

A preencher.

### 7. ADR

A preencher.

### 8. C4

A preencher.

### 9. Evidência

A preencher.

---

## Síntese final para a PoC da AF

A consolidar ao fim do Desafio 6, a partir da matriz cumulativa de ASRs.

**Histórico do CI da branch** (`feature/b1-architecture-lab`), conforme a API do GitHub em 2026-11-03: 4 runs, todos com
sucesso: #1 (`4d21fa4`), #2 (`03007e1`), #3 (`b1a3d3a`) e #4 (`3c3cce2`). O print `b1-ci-historico.png` ainda não foi anexado.

## Equipe e links

**Grupo 10.** Contribuições registradas no [README do projeto](../../README.md#créditos):
`viniciusalegre20`, `bruno-cO70`, `LeovLuz` e `GuilhermDias`.

| Artefato | Caminho |
|---|---|
| Repositório | <https://github.com/GuilhermDias/AC1-Devops> |
| ADRs | [ADR-001](adr/ADR-001-estilo-arquitetural-core.md), [ADR-002](adr/ADR-002-ports-and-adapters-em-conquistas.md), [ADR-003](adr/ADR-003-servico-python-de-ia.md), [ADR-004](adr/ADR-004-eventos-com-outbox-transacional.md) |
| Diagramas | Desafio 1: item 8 (C4) e item 9.8 (dependências antes/depois). Desafio 2: item 8 (Component). Desafio 3: item 8 (Container). Desafio 4: item 8 (Container e sequência) |
| Fachadas dos módulos | [`StudentProgressionApi`](../../src/main/java/org/example/ac1devops/progression/api/StudentProgressionApi.java), [`RankingApi`](../../src/main/java/org/example/ac1devops/ranking/api/RankingApi.java), [`ConquistasApi`](../../src/main/java/org/example/ac1devops/conquistas/api/ConquistasApi.java), [`RecomendacaoApi`](../../src/main/java/org/example/ac1devops/recomendacao/api/RecomendacaoApi.java), [`CursosApi`](../../src/main/java/org/example/ac1devops/cursos/api/CursosApi.java), [`NotificacoesApi`](../../src/main/java/org/example/ac1devops/notificacoes/api/NotificacoesApi.java) |
| Regras de arquitetura | [`ModularArchitectureTest`](../../src/test/java/org/example/ac1devops/architecture/ModularArchitectureTest.java) |
| Workflow de CI | [`.github/workflows/ci.yml`](../../.github/workflows/ci.yml) |
| Serviço de IA | [`labs/desafio-3/ia-service`](../../labs/desafio-3/ia-service/) |
| Relatório de cobertura | Artifact `jacoco-report` de cada run do CI; local em `target/site/jacoco/index.html`; CSVs em [`evidencias/`](evidencias/) |
| Prova de violação barrada | [`evidencias/archunit-violacao.txt`](evidencias/archunit-violacao.txt) |
