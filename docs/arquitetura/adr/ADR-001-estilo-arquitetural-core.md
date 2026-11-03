# ADR-001 — Estilo arquitetural do Core: Monólito Modular

| Campo | Valor |
|---|---|
| **Status** | Aceito |
| **Data** | 2026-10-27 (revisão AC2 ExB1; versão original 2026-10-06) |
| **Decisor** | Tech Lead do Grupo 10 (AC1) |
| **Documento de apoio** | [`docs/arquitetura/README.md`](../README.md) |

## Contexto

O MVP da plataforma de Educação Continuada Gamificada cresce de 1.000 para até **100.000
usuários**. O **Ranking (RF01)** deve crescer mais que as demais funcionalidades, enquanto
**Conquistas (RF02)**, cursos e recompensas continuam evoluindo. Hoje o Core é um monólito em
camadas técnicas (`web`, `service`, `repository`, `entity`, `dto`, `domain`) com uma única
funcionalidade real — progressão do aluno por XP e níveis (US1–US4, endpoints `/students`) —
persistida em PostgreSQL (`students`, `level_up_events`). Ranking e Conquistas ainda não existem
no código. Não há medição de latência, carga ou gargalo.

A equipe precisa escolher entre (A) Monólito em Camadas, (B) Monólito Modular e (C) decomposição em
serviços, começando por extrair o Ranking como serviço.

## ASRs e RPCs

**ASRs** (detalhe no Desafio 1, item 3 do README):

- **ASR1 — Evoluir funcionalidades com baixo acoplamento** (manutenibilidade). Impacta a
  **estrutura**, com módulos por funcionalidade (`api/` público + `internal/` privado).
- **ASR2 — Ranking com p95 ≤ 300 ms para 100 mil usuários** (desempenho e escalabilidade,
  medidos via observabilidade). Impacta **dados e implantação**: consulta indexada e paginada,
  cache, réplicas *stateless* e métricas que dizem quando escalar ou extrair.
- **ASR3 — Fronteiras verificáveis automaticamente** (testabilidade). Impacta a **operação**,
  com regras ArchUnit no CI a cada commit e Pull Request.

**RPCs:**

- **RPC1:** Core em Java 21 e Spring Boot.
- **RPC2:** equipe de 6 desenvolvedores.
- **RPC3:** banco relacional único (PostgreSQL 16).
- **RPC4:** legado da AC1. Os contratos de `/students`, o front-end Vue e o schema existente
  (`students`, `level_up_events`) não podem quebrar.

## Alternativas consideradas

| Alternativa | Avaliação resumida |
|---|---|
| **A — Monólito em Camadas** (manter) | Custo operacional mínimo, mas cada nova funcionalidade se espalha por todas as camadas e nada impede acoplamento indevido. Não atende a ASR1 nem a ASR3. |
| **B — Monólito Modular** | Um deploy e um banco, com custo operacional igual ao de A. Módulos isolados atrás de uma API, com fronteiras verificadas por ArchUnit. Atende às ASR1 e ASR3. A ASR2 é atendida com índice, cache e escala horizontal do conjunto. |
| **C — Decomposição em serviços** (Ranking primeiro) | Escala e deploy independentes do Ranking, à custa de rede, contrato versionado, segundo pipeline, observabilidade distribuída e dados duplicados ou banco compartilhado. Resolve um gargalo **não comprovado**, com custo desproporcional para 6 devs e banco único. |

## Decisão

Adotar o **Monólito Modular (B)** no Core:

1. Cada funcionalidade vira um pacote-módulo `org.example.ac1devops.<modulo>` com `api/` (fachada
   pública, DTOs, tipos publicados) e `internal/` (service, domínio, entidades, repositórios).
2. O restante do sistema, incluindo os outros módulos e os controllers, acessa um módulo **somente**
   pela sua `api/`.
3. As regras são verificadas pelo `ModularArchitectureTest` (ArchUnit) no `mvn verify`, executado
   pelo CI (`.github/workflows/ci.yml`) em todo push e Pull Request.
4. Primeiro módulo entregue: `progression`, que concentra alunos, XP e níveis e expõe a fachada
   `StudentProgressionApi`. Ranking e Conquistas entram como novos módulos que consomem
   `progression.api`.
5. O Ranking **não** será extraído agora. A extração será reavaliada quando ao menos dois dos
   gatilhos G1–G5 (Desafio 1, item 5 do README) forem observados de forma sustentada, depois de aplicados
   índice, paginação e cache.
6. Os gatilhos são medidos, não estimados: o Core passa a expor métricas (Spring Boot Actuator +
   Micrometer em `/actuator/prometheus`), coletadas pelo Prometheus e exibidas no Grafana. Entregue no
   [ADR-006](ADR-006-esteira-unica-e-observabilidade.md), em um compose próprio, e não no principal como previsto aqui.

## Consequências e trade-offs

**Positivas**

- Baixo acoplamento entre funcionalidades com o mesmo custo operacional de hoje (1 jar, 1 imagem
  Docker, 1 PostgreSQL, 1 pipeline).
- Fronteiras garantidas por máquina. Uma violação quebra o build no PR, e há evidência disso em
  `docs/arquitetura/evidencias/archunit-violacao.txt`.
- Contrato externo preservado: mesmos endpoints, JSON e tabelas, o que é coberto por
  `StudentEndpointIntegrationTest`.
- Prepara uma eventual extração do Ranking, que já nasce isolado atrás de uma API. Trocar a
  chamada em memória por HTTP ou mensageria passa a ser uma mudança localizada.

**Negativas e riscos aceitos**

- A escala continua sendo do conjunto: se só o Ranking ficar pesado, todo o Core é replicado.
  Mitigação: cache, índice, paginação e monitoramento dos gatilhos G1–G5.
- Um único banco é compartilhado entre módulos. A fronteira de dados é só lógica (cada módulo dono
  das suas tabelas, sem acesso cruzado por repositório). Risco: *joins* entre módulos tentarem
  atravessar a fronteira, o que deve ser recusado em revisão de código.
- Exige disciplina para criar novos módulos no mesmo padrão. Cada novo módulo precisa ganhar suas
  regras no `ModularArchitectureTest`.
- Dívida conhecida e registrada, fora do escopo desta decisão: `GET /students` faz `findAll()`
  sem paginação e com carregamento lazy (N+1). Isso precisa ser resolvido antes de o Ranking
  entrar em produção.
