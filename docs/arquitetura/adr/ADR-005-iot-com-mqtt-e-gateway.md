# ADR-005 — Eventos de dispositivos por MQTT, com gateway no Core e sem segundo broker

| Campo | Valor |
|---|---|
| **Status** | Aceito |
| **Data** | 2026-11-03 |
| **Decisor** | Tech Lead do Grupo 10 |
| **Documento de apoio** | [`docs/arquitetura/README.md`](../README.md), Desafio 5 |
| **Relacionado a** | [ADR-004](ADR-004-eventos-com-outbox-transacional.md), cuja outbox recebe as conclusões vindas dos dispositivos |

## Contexto

Laboratórios produzem eventos de presença, início e conclusão de atividades por dispositivos com poucos recursos e conexão
instável. Esses eventos devem entrar na plataforma sem acoplar o Core a cada dispositivo. Protocolo e broker adicionam
componentes operacionais.

## ASRs e RPCs

- **ASR12 — Evento confirmado sobrevive à conexão instável** (tolerância à conectividade intermitente e disponibilidade).
- **ASR13 — Um único efeito por evento, mesmo com reentrega** (consistência da integração).
- **ASR14 — Core desacoplado dos dispositivos** (interoperabilidade).
- **RPC11:** dispositivos com baixo poder computacional.
- **RPC12:** conectividade intermitente.
- **RPC13:** protocolo e broker adicionam componentes operacionais.

## Alternativas consideradas

| Alternativa | Avaliação resumida |
|---|---|
| **A — REST direto no Core** | Sem componente novo, mas cada dispositivo depende do Core no ar, precisa guardar e repetir eventos por conta própria e conhece o endereço do Core. Não atende às ASR12 e ASR14. |
| **B — MQTT + consumidor** | Um broker a mais. O broker guarda eventos para o consumidor desconectado, e dispositivos e Core só conhecem o broker e o tópico. Atende às três ASRs. |
| **C — MQTT + mensageria interna (RabbitMQ ou Kafka)** | Acrescenta um segundo broker para um único consumidor, sem ASR que o exija. |

## Decisão

Adotar a **alternativa B**:

1. Dispositivos publicam JSON em `labs/{lab}/devices/{deviceId}/events`, com **QoS 1**, no Mosquitto.
2. O gateway no módulo `iot` assina com **sessão persistente** e identificador fixo. O que chegar com o Core fora fica no broker.
3. A idempotência fica por conta do Core: `eventId` único, além da conclusão já idempotente por aluno e curso.
4. **Ordem deliberada:** primeiro a conclusão, depois o registro do `eventId`. Se o processo cair entre os dois, a reentrega conclui de
   novo sem efeito. Na ordem contrária, ela seria descartada como duplicada, e a conclusão se perderia.
5. **Evento inválido** é registrado no log e confirmado, pois reentregar não adiantaria. **Referência desconhecida** fica como
   `REJECTED`. **Falha temporária** não confirma a mensagem, e o broker reentrega.
6. **Conclusão** reaproveita `CursosApi`, com a recompensa na transação e a notificação pela outbox do ADR-004.
7. **Paho direto, sem Spring Integration:** é um único tópico de entrada. O controle de quando confirmar e o ciclo de reconexão ficam
   em poucas linhas, sem um framework de canais para uma única rota.

**Sem segundo broker.** O Mosquitto é o transporte dos dispositivos até o gateway; a outbox garante o que acontece depois. Nenhum
gatilho do ADR-004 ocorre aqui: continua havendo um consumidor, no mesmo processo.

**Gatilhos para rever esta decisão:**

| Gatilho | Limite | Indica |
|---|---|---|
| Mais de uma réplica do Core | Primeira necessidade real: duas réplicas com o mesmo identificador derrubam uma à outra | Gateway em processo próprio ou assinatura compartilhada do MQTT 5 |
| Vazão do gateway | Fila de sessão crescendo de forma sustentada com o Core saudável | Gateway separado e processamento paralelo |
| Consumidor externo dos eventos dos dispositivos | Primeiro caso real | Gatilhos B1 a B3 do ADR-004 |
| Dispositivos fora de rede confiável | Antes de qualquer uso real | TLS, credencial por dispositivo e ACL por tópico |

## Consequências e trade-offs

**Positivas**

- Com o dispositivo, o Core ou o broker reiniciando, os eventos confirmados continuam chegando, sem duplicar efeitos.
- Dispositivos não conhecem o Core, e só o adapter do módulo `iot` conhece MQTT, verificado pelo ArchUnit.
- O Core sobe e opera sem o broker.

**Negativas e riscos aceitos**

- Um serviço a mais para operar, além de um segundo protocolo.
- **Segurança incompleta no laboratório:** sem TLS nem autenticação. As mitigações são publicar a porta só em `localhost`, limitar o
  tamanho da mensagem e conferir o `deviceId` com o tópico. Isso não impede um cliente de se passar por um dispositivo.
- Um único gateway, processando uma mensagem por vez.
- MQTT não tem fila de mensagens mortas: um erro permanente inesperado seria reentregue sem fim.
- O broker guarda até 10.000 mensagens por cliente desconectado; acima disso, descarta.
- A fila do dispositivo fica em memória no simulador: um dispositivo real precisaria de armazenamento local para sobreviver a um
  reinício.
- A ordem entre eventos não é garantida depois de uma reconexão; nenhuma regra atual depende dela.
- Presença e início são apenas registrados, porque ainda não têm consumidor.
