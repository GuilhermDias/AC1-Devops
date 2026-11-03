# ADR-006 — Esteira única no Jenkins e observabilidade que valida os ASRs

| Campo | Valor |
|---|---|
| **Status** | Aceito |
| **Data** | 2026-11-03 |
| **Decisor** | Tech Lead do Grupo 10 |
| **Documento de apoio** | [`docs/arquitetura/README.md`](../README.md), Desafio 6 |
| **Relacionado a** | [ADR-001](ADR-001-estilo-arquitetural-core.md) a [ADR-005](ADR-005-iot-com-mqtt-e-gateway.md), cujos ASRs passam a ser verificados em build e em execução |

## Contexto

A solução agora tem Core em Java, serviço de IA em Python, Mosquitto, simulador de dispositivo e PostgreSQL, e precisa ser testada,
implantada e observada com segurança. O Jenkins é a base da esteira, Prometheus e Grafana são as ferramentas do laboratório, e artefatos
e configurações devem ser versionados. Vários ASRs dependem de comportamento em execução que testes não mostram, como o p95 do ranking
e a taxa de fallback da IA.

## ASRs e RPCs

- **ASR15 — Nenhum artefato sai sem passar pelos gates** (testabilidade e confiabilidade).
- **ASR16 — ASRs de execução observáveis em operação** (observabilidade e desempenho).
- **ASR17 — Operação reproduzível a partir do repositório** (recuperabilidade).
- **RPC14:** Jenkins é a base da esteira.
- **RPC15:** Prometheus e Grafana são as ferramentas do laboratório.
- **RPC16:** artefatos e configurações devem ser versionados.

## Alternativas consideradas

| Alternativa | Avaliação resumida |
|---|---|
| **A — Pipeline único sequencial** | Simples, mas mais lento e sem separar o que cada componente verifica. |
| **B — Pipeline único com estágios paralelos por componente** | Um Jenkinsfile e um único resultado; os testes Python rodam em paralelo. Todo build verifica todos os componentes. |
| **C — Um Jenkinsfile por componente** | Autonomia real de publicação, mas 3 jobs para configurar e manter, para 6 pessoas em um único repositório. |
| **D — Pipeline único com detecção de mudança por pasta** | Economiza tempo, mas pode pular a verificação do contrato entre Core e serviço de IA. A esteira leva cerca de 5 minutos no Jenkins, então o ganho não compensa. |

## Decisão

Adotar a **alternativa B**:

1. Um [`Jenkinsfile`](../../../Jenkinsfile) na raiz, com um estágio por gate. Qualquer falha interrompe o build, e o jar só é arquivado se todos
   passarem.
2. **Bloqueiam o artefato:** testes do Core, ArchUnit, gate de 100% do JaCoCo, exigido pela AC2 e sem excluir classes, e pytest do serviço de IA e do simulador.
3. O GitHub Actions continua como está, com jobs separados: verifica cada push, inclusive sem o Jenkins local.
4. O Core expõe só `health`, `info` e `prometheus` no Actuator, com as métricas de cada ASR de execução. O domínio continua sem dependência de
   métricas, verificado pelo ArchUnit: elas ficam na aplicação e nos adapters.
5. Prometheus (porta **9091**, porque a 9090 é do Jenkins) e Grafana (3000) sobem de um compose próprio, com fonte de dados, painel e alertas
   versionados.
6. **Métrica não substitui evidência arquitetural:** ela mostra o que está acontecendo em execução. A sustentação da decisão vem do conjunto de
   hipótese, teste, falha simulada e métrica.

**Serviços independentes exigem pipelines independentes?** Não necessariamente: exigem **artefatos e verificações independentes**, o que os
estágios por componente e as imagens separadas já garantem.

**Gatilhos para separar os pipelines** (qualquer um, de forma sustentada):

| # | Gatilho | Limite |
|---|---|---|
| P1 | Tempo da esteira | Acima de 10 minutos |
| P2 | Publicações bloqueadas | Mais de 30% das publicações de um componente barradas por outro |
| P3 | Organização | Time próprio para o serviço de IA ou para IoT |
| P4 | Implantação | Publicação automática por componente, que hoje não existe |

## Consequências e trade-offs

**Positivas**

- O artefato só existe se todos os gates passarem, com a mesma verificação no Jenkins e no GitHub Actions.
- ASR2, ASR6, ASR11, ASR12 e ASR13 passam a ser visíveis em execução, e o p95 do ranking foi medido pela primeira vez em PostgreSQL.
- O ambiente de observação é recriado com um comando.

**Negativas e riscos aceitos**

- Toda mudança, mesmo só de documentação, roda a esteira inteira.
- Não há estágio de implantação: a esteira entrega o artefato, não o publica.
- O serviço de IA não expõe métricas próprias: seu efeito é visto pelo Core, na taxa de fallback.
- Alertas ficam no Prometheus, sem envio de notificação.
- O endpoint de métricas não tem autenticação, e Prometheus e Grafana só são publicados em `localhost`. Em produção, isso precisa de proteção.
- As métricas usam o registro global do Micrometer, para não alterar construtores nem testes dos módulos; é um acoplamento estático aceito no
  laboratório.
- O job do Jenkins é criado manualmente, seguindo o README. Os builds #1 e #2 ficaram verdes, o segundo com o gate de 100%; a interrupção por um gate com falha não foi provocada no Jenkins.
