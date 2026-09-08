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
> no histórico) sem simular 4 chamadas de `receiveXp`, decida dentro do TDD a forma mais
> limpa: pode ser um construtor extra, um método de fábrica de teste, ou simular as
> chamadas reais de `receiveXp` no `@BeforeEach`. Ambas as abordagens são aceitáveis nesta
> fase.

## O que falta definir antes de implementar (gaps entre a planilha ATDD e o domínio atual)

A planilha ATDD (aba `pb`) descreve o cenário da US3 usando `studentService.awardXp(...)`
e `atualizado.getLevelUpEvents()`, enquanto a especificação de domínio usa
`student.receiveXp(...)` e `student.getLevelUpHistory()`. Para esta entrega (só domínio,
sem Service/Repository), resolva essas divergências assim:

- **Nome do método**: use `receiveXp(String reason, int amount)` na classe `Student` (a
  planilha usa `awardXp` no nível do Service, que ainda não existe nesta fase).
- **Nome da coleção de histórico**: padronize para `levelUpEvents` (nome usado na
  planilha) em vez de `levelUpHistory`, para já ficar consistente com o Service que será
  criado numa entrega futura.
- **Classe do evento**: crie `LevelUpEvent` (em vez de `LevelChange`) com os campos
  `from` e `to` (ou `fromLevel`/`toLevel`, alinhado ao `getToLevel()` usado no cenário da
  US1 da planilha).
- **Setup do estado inicial do cenário**: a planilha simula o setup chamando
  `receiveXp`/`awardXp` sucessivas vezes (ex. primeiro 750 XP, depois mais 100). Replicar
  esse padrão no `@BeforeEach` do teste de domínio evita depender de um construtor "mágico"
  que já nasce no nível Diamante.
- **Biblioteca de assertions**: a planilha usa AssertJ (`assertThat(...).isEqualTo(...)`).
  Confirme se o projeto já tem a dependência `assertj-core` no `pom.xml`; se não tiver,
  pode-se usar `org.junit.jupiter.api.Assertions` (`assertEquals`) como alternativa
  equivalente.

## Especificação do teste (`StudentTest`)

Um único método de teste, mapeando 1:1 o cenário BDD (adaptado da planilha ATDD para o
nível de domínio, sem Service):

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

## Passo a passo TDD (RED → GREEN → BLUE)

### 1. RED
1. Criar o pacote `domain` (em `src/main` e o espelho em `src/test`) e escrever
   `StudentTest.java` **primeiro**, referenciando `Student` e `Level` que ainda não
   existem (ou existem vazios/incompletos).
2. Rodar o teste no IntelliJ e confirmar que **falha** — erro de compilação (classe/método
   inexistente) ou assertion falhando. Isso é o **RED**.
3. Capturar print da tela vermelha/erro como evidência.

### 2. GREEN
1. Criar `Level.java` (enum `BRONZE, PRATA, OURO, DIAMANTE`) e `Student.java` com o
   mínimo necessário para o teste compilar e passar: campos, construtor, `receiveXp`,
   cálculo de nível pelas faixas de XP, registro em `levelUpEvents` só quando o nível
   muda, e os getters.
2. Rodar o teste de novo até ficar **verde**. Isso é o **GREEN**.
3. Configurar o **Jacoco** no `pom.xml` (plugin `jacoco-maven-plugin`, com as execuções
   `prepare-agent` e `report` atreladas à fase `test` — não depende de Spring Boot, funciona
   em qualquer projeto Maven).
4. Rodar `mvn test` e capturar o print do relatório gerado em
   `target/site/jacoco/index.html`, mesmo que apareça amarelo/vermelho em partes ainda não
   cobertas (ex. faixas Bronze/Prata/Ouro isoladas) — isso é esperado nesta fase.
5. Capturar print do teste passando (verde) como evidência.

### 3. BLUE
1. Com o teste verde como rede de segurança, refatorar `Student`/`Level` sem mudar
   comportamento externo. Candidatos típicos de refatoração:
   - Extrair o cálculo de nível a partir do XP para um método próprio na própria classe
     `Level` (ex. `Level.fromXp(int xp)`), tirando essa lógica de dentro de `receiveXp`.
   - Remover números mágicos (`99`, `299`, `699`, `700`) trocando por constantes nomeadas
     ou pelos limites definidos no próprio enum `Level`.
   - Garantir que `levelUpEvents` seja exposto como lista imutável (`List.copyOf(...)` ou
     `Collections.unmodifiableList(...)`) para não vazar estado interno mutável pelo
     getter.
2. Rodar o teste após cada pequena refatoração e confirmar que continua **verde**. Isso é
   o **BLUE**.
3. Rodar `mvn test` novamente e capturar novo print do relatório do Jacoco, mostrando a
   cobertura após a refatoração (idealmente melhor do que a do GREEN, ainda que não
   precise chegar a 100% nesta entrega).
4. Capturar print do teste passando após a refatoração como evidência do BLUE.

## Lembrete para o README final do projeto

Quando o projeto for consolidado, o README do GitHub precisa conter, além da documentação
técnica: descrição do case (Educação Continuada Gamificada), a User Story desta entrega
(US3) e o cenário BDD acima, identificando Guilherme como autor de ambos.
