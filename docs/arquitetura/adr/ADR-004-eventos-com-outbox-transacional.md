# ADR-004 — Eventos internos com outbox transacional, sem broker por enquanto

| Campo | Valor |
|---|---|
| **Status** | Aceito |
| **Data** | 2026-11-03 |
| **Decisor** | Tech Lead do Grupo 10 |
| **Documento de apoio** | [`docs/arquitetura/README.md`](../README.md), Desafio 4 |
| **Relacionado a** | [ADR-001](ADR-001-estilo-arquitetural-core.md) (monólito modular) e [ADR-003](ADR-003-servico-python-de-ia.md) (dependência opcional fora do fluxo essencial) |

## Contexto

Ao concluir um curso, o sistema registra a conclusão, calcula a recompensa, atualiza informações e pode disparar
notificação e analytics. Uma falha de notificação não pode impedir a conclusão. As operações essenciais devem
terminar mesmo com um consumidor externo indisponível, e adotar um broker tem custo operacional.

O risco principal é a **escrita dupla**: gravar a conclusão e depois avisar o consumidor são dois passos. Se o
processo cair entre eles, o aluno conclui o curso e o aviso se perde. Isso acontece com evento em memória
(`@TransactionalEventListener` + `@Async`) e também ao publicar em um broker depois do commit.

## ASRs e RPCs

- **ASR9 — Conclusão independente dos consumidores** (tolerância a falhas, disponibilidade e latência).
- **ASR10 — Conclusão e recompensa atômicas e exatamente uma vez** (consistência).
- **ASR11 — Nenhum evento confirmado se perde** (recuperabilidade).
- **RPC9:** operações essenciais do Core devem concluir mesmo com consumidor externo indisponível.
- **RPC10:** adotar broker exige custo operacional.

## Alternativas consideradas

| Alternativa | Avaliação resumida |
|---|---|
| **A — Tudo síncrono na requisição** | Mais simples, mas a falha ou lentidão do consumidor atinge o aluno. Não atende à ASR9. |
| **B — Só `@TransactionalEventListener` + `@Async`** | Atende à ASR9 sem infraestrutura, mas o evento fica só na memória: uma queda o perde. Não atende à ASR11. |
| **C — Outbox transacional + eventos internos** | Atende às ASR9, ASR10 e ASR11 com o banco que já existe. Limitada a consumidores dentro do processo e à capacidade do PostgreSQL. |
| **D — RabbitMQ** | Boa fila de tarefas, com reentrega e dead letter prontas, e alcança outros processos. Sozinho não resolve a escrita dupla, então exigiria outbox também, além de um novo serviço para operar. |
| **E — Kafka** | Log de eventos com replay e vários consumidores independentes. Nenhum desses recursos é exigido hoje, e o custo operacional é o maior. |

## Decisão

Adotar a **alternativa C**, sem broker:

1. **Imediato, na mesma transação:** conclusão, XP, nível, level-ups e registro do evento na outbox. A resposta
   ao aluno depende só disso.
2. **Logo depois da transação, na mesma requisição:** cache do ranking e badges, sem poder desfazer a conclusão.
3. **Eventual, fora da requisição:** `CursoConcluidoEvent`, consumido pelo módulo `notificacoes`. A primeira
   tentativa sai depois do commit (`@TransactionalEventListener` + `@Async`).
4. **Recuperação:** uma varredura periódica entrega o que ficou pendente, com espera que dobra a cada falha. Após 5
   tentativas, o evento fica como `DEAD`, à espera de reenvio manual.
5. **Entrega ao menos uma vez:** cada tentativa é reservada por um update condicional, e o consumidor é idempotente
   pelo `eventId`.
6. **Exatamente uma recompensa:** restrição única por aluno e curso; repetir a chamada não concede XP.
7. **Fronteira:** `cursos` não conhece `notificacoes`, verificado pelo ArchUnit.

**Por que nenhum broker resolve o problema real deste fluxo:** hoje há um consumidor, no mesmo processo. O que falta é
não perder o evento e tentar de novo; a outbox faz isso, e um broker ainda precisaria dela para evitar a escrita dupla.

## Gatilhos para migrar para um broker

Reavaliar quando **qualquer um** ocorrer de forma sustentada. Os limites são valores iniciais, baseados na medição
local do item 9 do Desafio 4 (40 a 45 conclusões por segundo).

| # | Gatilho | Limite | Indica |
|---|---|---|---|
| B1 | Consumidor em outro processo ou linguagem | Primeiro caso real, como analytics em um serviço próprio | RabbitMQ ou Kafka |
| B2 | Consumidores independentes para o mesmo evento | 2 ou mais, cada um com falhas e novas tentativas próprias: a outbox tem um único estado por evento | RabbitMQ (fan-out) |
| B3 | Necessidade de reprocessar o histórico | Novo consumidor precisa ler eventos passados | Kafka |
| B4 | Volume | Acima de 20 conclusões por segundo, metade do medido, ou a varredura disputando recursos com o fluxo essencial | RabbitMQ ou Kafka, conforme B2 e B3 |
| B5 | Atraso de entrega | Eventos `PENDING` com mais de 5 minutos, com o consumidor saudável | Avaliar capacidade antes do broker |

**Migração:** a outbox continua, e `OutboxRelay` passa a publicar no broker, em vez de publicar internamente. A
conclusão do curso não muda.

## Consequências e trade-offs

**Positivas**

- Canal de notificação fora do ar: conclusão responde 201, e nada se perde, inclusive com queda forçada do processo.
- Nenhum serviço novo para operar, monitorar ou subir no ambiente local.
- Migração para um broker limitada ao `OutboxRelay`.

**Negativas e riscos aceitos**

- Consumidores precisam estar no mesmo processo.
- A varredura consulta o banco a cada 5 s, e a tabela `course_outbox_events` cresce sem limpeza.
- A entrega é ao menos uma vez: uma queda entre o envio e a gravação pode repetir uma notificação.
- `lease` (60 s) precisa ser maior que a entrega mais demorada, senão o evento pode ser enviado em paralelo.
- Eventos `DEAD` dependem de reenvio manual. O [ADR-006](ADR-006-esteira-unica-e-observabilidade.md) acrescentou um alerta no Prometheus, sem envio de notificação.
- Os endpoints de eventos e reenvio não têm autenticação, como todo o Core hoje.
- A reserva por update condicional foi pensada para várias réplicas, mas só foi testada com uma.
