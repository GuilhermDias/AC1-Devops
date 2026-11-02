# ADR-003 — Serviço Python independente para IA, com modo degradado no Core

| Campo | Valor |
|---|---|
| **Status** | Aceito |
| **Data** | 2026-11-02 |
| **Decisor** | Tech Lead do Grupo 10 |
| **Documento de apoio** | [`docs/arquitetura/README.md`](../README.md), Desafio 3 |
| **Relacionado a** | [ADR-001](ADR-001-estilo-arquitetural-core.md), cuja regra para extrair serviços é aplicada aqui, e [ADR-002](ADR-002-ports-and-adapters-em-conquistas.md), de onde vem o padrão de porta e adapter |

## Contexto

A plataforma passa a ter recomendação inteligente e assistente educacional. Os modelos são feitos em
Python, têm ciclo de atualização próprio, carga variável e podem ficar indisponíveis sem impedir as
operações essenciais do Core, que permanece em Java/Spring Boot. O ADR-001 manteve o Core como monólito
por falta de necessidade comprovada de deploy e escala independentes; aqui essa necessidade faz parte do
próprio requisito.

## ASRs e RPCs

- **ASR6 — Indisponibilidade da IA não bloqueia o Core** (tolerância a falhas).
- **ASR7 — IA publicada e escalada sem o Core** (deploy independente e escalabilidade).
- **ASR8 — Contrato HTTP/JSON verificável pelos dois lados** (interoperabilidade).
- **RPC1:** Core em Java 21 e Spring Boot.
- **RPC7:** ML e RAG em Python.
- **RPC8:** a indisponibilidade da IA não pode bloquear o fluxo essencial.

## Alternativas consideradas

| Alternativa | Avaliação resumida |
|---|---|
| **A — IA embutida no Core** (modelo exportado e executado na JVM) | Sem rede, mas todo novo modelo exige publicar o Core, e o consumo de CPU e memória dos modelos disputa recursos com o fluxo essencial. Conflita com a RPC7 no caso de RAG. Não atende às ASR6 e ASR7. |
| **B — Módulo acoplado** (Python no mesmo processo ou contêiner) | Atende à RPC7, mas uma falha ou um pico de memória atinge o Core, e o ciclo de publicação continua único. Não atende às ASR6 e ASR7. |
| **C — Serviço Python independente** | Atende às ASR6, ASR7 e ASR8. Traz rede, contrato, um segundo pipeline e falhas parciais, que precisam ser tratados. |

## Decisão

Adotar a **alternativa C**:

1. O serviço de IA é um contêiner próprio, em FastAPI, com testes e imagem próprios.
2. O Core acessa a IA pela porta `RecommendationPort`, implementada por `IaRecommendationAdapter`, em
   HTTP/JSON síncrono.
3. A chamada tem connect timeout de 300 ms, read timeout de 800 ms e circuit breaker do Resilience4j: abre
   com 50% de falhas nas últimas 10 chamadas (mínimo de 5) e testa o retorno depois de 30 s.
4. O comportamento degradado é regra de domínio: sem resposta útil da IA, o aluno recebe os cursos
   populares, e a resposta traz `degraded: true`.
5. O fluxo essencial não chama a IA. Uma regra do ArchUnit impede `progression`, `ranking` e `conquistas`
   de conhecerem `recomendacao`.
6. ML e RAG ficam **no mesmo serviço por enquanto**, em rotas separadas e sem estado compartilhado. Quando o
   Core consumir o assistente, cada função terá timeout e circuit breaker próprios.

**Python, por si só, não justifica o serviço:** a linguagem poderia ser resolvida por um modelo exportado para
a JVM ou por um processo em lote. O que justifica são as ASR6 e ASR7.

**Critério para separar ML e RAG:** reavaliar quando ocorrer ao menos um destes casos, medido e sustentado: o
assistente passar a exigir escala ou recursos diferentes (como GPU) dos da recomendação; os ciclos de
publicação começarem a se bloquear; ou a carga de uma função degradar a latência da outra.

## Consequências e trade-offs

**Positivas**

- Com o serviço fora do ar ou lento, o endpoint de recomendações continua respondendo, e o fluxo essencial
  segue sem alteração.
- Um modelo novo é publicado sem tocar no Core.
- O contrato é verificado pelos dois lados, com os mesmos arquivos JSON.

**Negativas e riscos aceitos**

- Segundo artefato, segundo job de CI e uma linguagem a mais para a equipe de 6 pessoas manter.
- Até o circuito abrir, cada chamada pode esperar até o timeout. Não há bulkhead limitando as threads ocupadas.
- O estado do circuit breaker é por instância do Core: com várias réplicas, cada uma descobre a falha sozinha.
- A lista de populares é fixa e proposta pela equipe, porque o Core ainda não tem cursos nem dados de
  popularidade.
- A recomendação degradada não é personalizada.
- Os timeouts são valores iniciais. O case não define SLA, e um modelo real ou RAG deve exigir ajuste, com
  base em medição.
- O serviço é uma simulação: regra determinística, sem modelo, GPU ou LLM. A evidência testa a **fronteira**,
  não a qualidade da IA nem a carga real.
