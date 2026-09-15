# TDD — US3: Sistema de Níveis por XP (RED → GREEN → BLUE)

## Escopo desta entrega

Entrega da atividade AC1-ATDD. Aqui entra o ciclo **completo** de TDD para a US3, feito
sobre uma **única classe de domínio** (POJO puro, sem anotação de framework — nem
`@Entity`, nem Spring):

1. **RED** — escrever o teste primeiro, rodar e capturar evidência de que ele **falha**
   (classe/método ainda não existem ou não fazem o que o teste espera).
2. **GREEN** — implementar o código mínimo necessário para o teste passar, rodar de novo e
   capturar evidência (print do teste verde + print do relatório de cobertura do Jacoco,
   mesmo que venha amarelo/vermelho em partes ainda não cobertas).
3. **BLUE** — refatorar o código já verde (limpar duplicação, extrair métodos, melhorar
   nomes, aumentar cobertura das faixas de XP) **sem alterar o comportamento**, rodar o
   teste de novo e confirmar que continua verde após a refatoração.

Ficam fora do escopo (entregas futuras): Service, Repository, Entity JPA, DTO, Controller,
Swagger, banco H2/Postgres/PgAdmin, front-end VueJS e Docker.

## User Story desta entrega

**US3** — Não gerar novo level-up quando o aluno já está no nível máximo

```
COMO Aluno que já atingiu o nível máximo (Diamante)
QUERO continuar acumulando XP sem gerar novos eventos de level-up
PARA que meu histórico de evolução permaneça consistente
```

## Cenário BDD desta entrega

```
Dado um aluno no nível Diamante com 750 XP
E histórico de level-up já contendo as transições Bronze, Prata e Ouro
Quando o aluno recebe mais 100 XP por concluir uma atividade
E o total de XP soma 850, ainda dentro da faixa Diamante
Então o nível do aluno permanece Diamante
E nenhum novo evento de level-up é registrado
```

## Regras de negócio necessárias para este cenário

| Nível     | XP necessário |
|-----------|---------------|
| BRONZE    | 0 – 99        |
| PRATA     | 100 – 299     |
| OURO      | 300 – 699     |
| DIAMANTE  | 700+          |

Regra específica desta US: se o nível calculado após receber XP for **igual** ao nível
anterior, nenhum evento de level-up deve ser adicionado ao histórico.

## Estrutura de pacotes

```
src/main/java/org/example/ac1devops/domain/
├── Student.java
└── Level.java          (enum: BRONZE, PRATA, OURO, DIAMANTE)

src/test/java/org/example/ac1devops/domain/
└── StudentTest.java
```

## Especificação da classe de domínio (`Student`)

Classe **POJO simples**, sem anotações de framework. Campos e comportamento mínimos para
sustentar o cenário acima:

- `String name`
- `int xpTotal` (inicia em 0)
- `Level level` (inicia em `BRONZE`)
- `List<LevelUpEvent> levelUpEvents` — histórico de mudanças de nível. Pode ser uma classe
  interna simples ou um `record LevelUpEvent(Level from, Level to)`.
- Construtor: `Student(String name)`
- Método principal: `void receiveXp(String reason, int amount)`
    - Soma `amount` a `xpTotal`.
    - Recalcula o nível a partir do novo `xpTotal` (usando a tabela de faixas acima).
    - Se o novo nível for **diferente** do nível anterior, atualiza `level` e adiciona um
      registro em `levelUpEvents`.
    - Se o novo nível for **igual** ao anterior (caso desta US), não altera `levelUpEvents`.
- Getters para `xpTotal`, `level`, `levelUpEvents`.

> Para deixar a classe já "pré-carregada" no estado do cenário (Diamante, 750 XP, 3 eventos
> no histórico) sem simular 4 chamadas de `receiveXp`, decidiu-se simular as chamadas reais
> de `receiveXp` no `@BeforeEach`, em vez de depender de um construtor "mágico".

## Decisões tomadas (gaps entre a planilha ATDD e o domínio implementado)

A planilha ATDD (aba `pb`) descreve o cenário da US3 usando `studentService.awardXp(...)`
e `atualizado.getLevelUpEvents()`, enquanto a especificação de domínio usa
`student.receiveXp(...)`. Como esta entrega cobre só o domínio (sem Service/Repository),
essas divergências foram resolvidas assim:

- **Nome do método**: `receiveXp(String reason, int amount)` na classe `Student` (a
  planilha usa `awardXp` no nível do Service, que ainda não existe nesta fase).
- **Nome da coleção de histórico**: `levelUpEvents` (nome usado na planilha), já
  consistente com o Service que será criado em entrega futura.
- **Classe do evento**: `LevelUpEvent` com os campos `from`/`to`, alinhado ao
  `getToLevel()` usado no cenário da US1 da planilha.
- **Setup do estado inicial do cenário**: replicado no `@BeforeEach` do teste de domínio,
  chamando `receiveXp` sucessivas vezes (primeiro 750 XP), evitando depender de um
  construtor "mágico" que já nasce no nível Diamante.
- **Biblioteca de assertions**: AssertJ (`assertThat(...).isEqualTo(...)`).

## Especificação do teste (`StudentTest`)

Teste principal, mapeando 1:1 o cenário BDD da US3:

```java
package org.example.ac1devops.domain;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StudentTest {

    private Student student;

    @BeforeEach
    void setUp() {
        student = new Student("Guilherme");
        student.receiveXp("ATIVIDADE_CONCLUIDA", 750); // sobe até Diamante (3 level-ups: Bronze->Prata->Ouro->Diamante)
    }

    @Test
    void naoDeveGerarNovoLevelUpQuandoAlunoJaEstaNoNivelMaximo() {
        student.receiveXp("ATIVIDADE_CONCLUIDA", 100);

        assertThat(student.getXpTotal()).isEqualTo(850);
        assertThat(student.getLevel()).isEqualTo(Level.DIAMANTE);
        assertThat(student.getLevelUpEvents()).hasSize(3);
    }
}
```

## Passo a passo TDD realizado (RED → GREEN → BLUE)

### 1. RED
1. Criação do pacote `domain` (em `src/main` e o espelho em `src/test`) e escrita de
   `StudentTest.java` **antes** da implementação das classes `Student` e `Level`.
2. Execução do teste confirmando a **falha** de compilação/execução (classe/método
   inexistente) — evidência capturada em print, validando o ciclo RED.

### 2. GREEN
1. Implementação de `Level.java` (enum `BRONZE, PRATA, OURO, DIAMANTE`) e `Student.java`
   com o código mínimo necessário para o teste compilar e passar: campos, construtor,
   `receiveXp`, cálculo de nível pelas faixas de XP, registro em `levelUpEvents` só quando
   o nível muda, e os getters.
2. Teste executado com sucesso (verde) — print capturado como evidência.
3. Integração do `jacoco-maven-plugin` no `pom.xml` (execuções `prepare-agent` e `report`
   atreladas à fase `test`).
4. Execução de `mvn test` e print do relatório gerado em `target/site/jacoco/index.html`,
   ainda com partes amarelas/vermelhas (faixas Bronze/Prata/Ouro isoladas) — esperado
   nesta fase, já que o teste único cobre apenas o caminho Diamante.

### 3. BLUE (Refatoração e Otimização)
Com o teste verde como rede de segurança, o código foi refatorado sem alterar o
comportamento externo:

1. **Refatoração estrutural da classe `Student`**: a lógica de atualização de nível foi
   isolada em um método privado auxiliar (`processLevelUp()`), aumentando a legibilidade
   e aplicando o princípio de responsabilidade única. Foi adicionada também uma cláusula
   de guarda para tratar preventivamente valores de XP menores ou iguais a zero
   (`amount <= 0`).
2. **Expansão da suíte de testes (`StudentTest`)**: novos casos de teste foram
   incorporados para cobrir a inicialização do aluno, a robustez contra entradas
   inválidas e os métodos auxiliares do domínio (`getMinXp()` e `getFromLevel()`),
   além das faixas Bronze/Prata/Ouro que ainda não estavam cobertas na fase GREEN.
3. **Consolidação da cobertura (JaCoCo)**: a suíte completa foi reexecutada com sucesso,
   garantindo estabilidade e atingindo **100% de cobertura de código** no pacote de
   domínio (`org.example.ac1devops.domain`) — print do relatório final capturado como
   evidência do BLUE.