# ADR-002 — Ports & Adapters só onde a regra muda: módulo Conquistas

| Campo | Valor |
|---|---|
| **Status** | Aceito |
| **Data** | 2026-11-02 |
| **Decisor** | Tech Lead do Grupo 10 |
| **Documento de apoio** | [`docs/arquitetura/README.md`](../README.md), Desafio 2 |
| **Relacionado a** | [ADR-001](ADR-001-estilo-arquitetural-core.md), que este ADR complementa sem substituir |

## Contexto

As regras de ranking, moedas, badges e recompensas mudam com frequência, e domínio, banco, framework
e integrações externas não podem ficar fortemente acoplados. O Core já é um Monólito Modular
(ADR-001), mas o isolamento varia por módulo:

| Regra | Onde está hoje | Muda com frequência? |
|---|---|---|
| XP e níveis (`progression`) | POJOs em `internal.domain`, com testes de domínio sem Spring | Não: a tabela de faixas é estável desde a AC1 |
| Ordenação do ranking (`ranking`) | JPQL em `StudentRepository`, apoiada por índice | Não: XP decrescente e, no empate, id crescente |
| Concessão de badges (RF02) | Ainda não existia | **Sim:** é o que o cenário aponta como volátil |

O Core deve continuar em Spring Boot, as regras existentes devem ser reaproveitadas e é proibido
reescrever o produto só para aplicar um padrão.

## ASRs e RPCs

- **ASR4 — Mudar regra de gamificação sem tocar em persistência ou framework** (modificabilidade).
- **ASR5 — Regra de gamificação testável sem Spring nem banco** (testabilidade).
- **RPC1:** Core em Java 21 e Spring Boot.
- **RPC5:** regras existentes devem ser reaproveitadas.
- **RPC6:** proibido reescrever o produto inteiro só para aplicar um padrão.

## Alternativas consideradas

| Alternativa | Avaliação resumida |
|---|---|
| **A — Modular com camadas em Conquistas** (`service` → `JpaRepository`) | Menos classes, mas a regra passa a depender do banco para ser testada. Não atende à ASR5 e deixa a ASR4 só na disciplina da equipe. |
| **B — Ports & Adapters só em Conquistas** | Custo limitado a um módulo novo. Atende às ASR4 e ASR5, e os demais módulos não mudam de estrutura. |
| **C — Hexagonal em todo o Core** | Isolamento uniforme, mas exige reescrever módulos cujas regras não mudam (RPC6). Para o ranking, levar a ordenação ao domínio carregaria a tabela inteira em memória, contrariando a ASR2. |

## Decisão

Adotar a **alternativa B**:

1. A dependência invertida é **regra de concessão → persistência dos badges**. A porta
   `AwardedBadgePort` fica no domínio e os adapters a implementam.
2. `conquistas.internal.domain` não depende de Spring, JPA nem adapters. O único tipo externo é o enum
   `Level`, de `progression.api`, para reaproveitar a tabela de faixas sem duplicá-la.
3. O Spring fica nas bordas: `ConquistasConfig` monta o domínio, `JpaAwardedBadgeAdapter` persiste e
   `StudentScoreEventListener` recebe o evento de progressão.
4. As regras são dados do catálogo `BadgeRules`. Criar, alterar ou remover um badge não exige mudar
   porta, adapter ou schema.
5. Os dois adapters passam pelo mesmo contrato, `BadgeAwardContract`.
6. Uma falha em Conquistas não pode derrubar a concessão de XP: o listener registra o erro e a
   avaliação é idempotente.
7. Regras ArchUnit no `verify` protegem a fronteira: `dominioSemFrameworkNemAdapters` e
   `nadaDependeDosAdaptersDeConquistas`.

**Não invertido, de propósito:** a persistência de `progression`, porque a regra já é pura e estável;
a ordenação do ranking, porque ela no banco é o que sustenta a ASR2; e integrações externas, porque
nenhuma existe no código e uma porta agora seria especulativa.

**DDD, Clean e Onion:** usa-se de DDD a fronteira de contexto (o módulo) e a linguagem do domínio. Clean
e Onion não são adotados como estrutura própria, porque resolvem o mesmo problema de direção de
dependência com mais camadas do que este módulo precisa.

**Critério de expansão:** aplicar a mesma estrutura a outro módulo só quando as regras dele passarem a
mudar com frequência e a mudança exigir tocar em persistência ou framework. O candidato natural é
moedas e recompensas, quando existirem.

## Consequências e trade-offs

**Positivas**

- Regra nova com uma linha no catálogo, sem tocar nos adapters.
- Os 9 cenários da regra rodam em 0,3 s com o adapter em memória, contra 7,9 s com JPA e Spring.
- Evidência de que trocar o adapter não muda a regra: mesmos cenários passam nos dois, e uma mutação
  da regra derruba os mesmos cenários nos dois.

**Negativas e riscos aceitos**

- O Core passa a ter duas organizações internas, e quem entra na equipe precisa saber por quê.
- Custo estrutural de Conquistas: 15 arquivos e 336 linhas no módulo, além do adapter em memória nos
  testes.
- O adapter em memória não reproduz o banco. Ele não prova índices, transações nem concorrência; por
  isso o contrato também roda no JPA e houve teste em PostgreSQL.
- A concessão roda de forma síncrona em `POST /students/{id}/xp` e acrescenta ao menos uma consulta.
  O impacto na latência não foi medido.
- Se a avaliação falhar no último evento de um aluno, o badge só é concedido no próximo evento.
- Herda a concorrência de `StudentService.receiveXp`, que perde XP sob requisições simultâneas. O
  problema é anterior a este ADR e segue aberto.
- O catálogo inicial de badges é uma proposta técnica: o case não define os badges nem os limites.
