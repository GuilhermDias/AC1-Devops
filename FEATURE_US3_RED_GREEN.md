# TDD — Entrega 1 (RED → GREEN) — US3: Sistema de Níveis por XP

## Escopo desta entrega

Esta é a **primeira parte** da atividade AC1-ATDD (entrega 14/09). Nesta etapa **NÃO** entram:
Service, Repository, Entity JPA, DTO, Controller, Swagger, banco H2/Postgres/PgAdmin,
front-end VueJS ou Docker — tudo isso é de entregas futuras (BLUE + camadas seguintes).

O que entra **agora**:
1. Pacote `domain` com **uma única classe de domínio** (POJO puro, sem anotação de
   framework nenhuma — nem `@Entity`, nem Spring).
2. Pacote de teste correspondente com a classe `...Test`, no mesmo padrão do exercício da
   calculadora feito em aula.
3. **RED**: escrever o teste primeiro, rodar e capturar evidência de que ele **falha**
   (classe/método ainda não existem ou não fazem o que o teste espera).
4. **GREEN**: implementar o código mínimo necessário para o teste passar, rodar de novo e
   capturar evidência (print do teste verde + print do relatório de cobertura do Jacoco,
   mesmo que venha
   amarelo/vermelho em partes ainda não cobertas — isso é esperado nesta fase).

Não fazer refactor para 100% de cobertura ainda — isso é a fase BLUE, de uma entrega futura.

## User Story desta entrega

**US3** — Não gerar novo level-up quando o aluno já está no nível máximo

```
COMO Aluno que já atingiu o nível máximo (Diamante)
QUERO continuar acumulando XP sem gerar novos eventos de level-up
PARA que meu histórico de evolução permaneça consistente
```

## Cenário BDD desta entrega (escrito por Guilherme)

```
Dado um aluno no nível Diamante com 750 XP
E histórico de level-up já contendo as transições Bronze, Prata e Ouro
Quando o aluno recebe mais 100 XP por concluir uma atividade
E o total de XP soma 850, ainda dentro da faixa Diamante
Então o nível do aluno permanece Diamante
E nenhum novo evento de level-up é registrado
```

Este é o **único** cenário implementado nesta entrega. Os outros 3 (US1, US2, US4) ficam a
cargo dos demais integrantes do grupo, cada um em sua própria classe/teste de domínio.

## Regras de negócio necessárias para este cenário

Para o teste acima fazer sentido, a classe de domínio precisa saber calcular o nível a partir
do XP total, usando estas faixas fixas (mesmas definidas para as 4 US do grupo):

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
domain/
├── Student.java
└── Level.java          (enum: BRONZE, PRATA, OURO, DIAMANTE)
```

O teste (`StudentTest.java`) vai no pacote `domain` correspondente dentro de `src/test`,
espelhando o pacote de `src/main` — mesmo padrão do exercício da calculadora.

Use o groupId/pacote base que já estiver definido no `pom.xml` do projeto (ex.:
`com.<seuGrupo>.gamificacao.domain`) — não há vínculo com nenhuma empresa aqui, é um
projeto acadêmico. No repositório atual o pacote base é `org.example.ac1devops`, então a
raiz de domínio é `org.example.ac1devops.domain`.

## Convenção: um subpacote por integrante

Os **4 integrantes do grupo implementam esta mesma US3**, cada um o seu próprio ciclo
RED → GREEN de forma independente. Para não haver conflito de merge, cada pessoa
trabalha num **subpacote próprio** dentro de `domain`, com as mesmas classes
(`Student`, `Level`, `StudentTest`) porém em pacote distinto:

```
src/main/java/org/example/ac1devops/domain/
├── guilherme/   Student.java   Level.java
├── <colega2>/   Student.java   Level.java
├── <colega3>/   Student.java   Level.java
└── <colega4>/   Student.java   Level.java

src/test/java/org/example/ac1devops/domain/
├── guilherme/   StudentTest.java
├── <colega2>/   StudentTest.java
├── <colega3>/   StudentTest.java
└── <colega4>/   StudentTest.java
```

Regras da convenção:

- Nome do subpacote = primeiro nome do integrante, minúsculo e sem acento.
- Cada pasta é **autocontida** (inclui a sua própria cópia de `Level.java`). A
  duplicação é aceitável nesta fase RED/GREEN; a consolidação num `domain` único é
  tarefa da fase BLUE (entrega futura).
- Recomenda-se cada integrante trabalhar numa branch `red-green-<nome>` e abrir PR para
  `main`. Como cada um só mexe na sua pasta, o merge é trivial.
- O `pom.xml` (incluindo o plugin Jacoco) é configurado **uma única vez** e cobre todos
  os subpacotes automaticamente — ninguém mais precisa alterá-lo.
- Este arquivo `.md` é **compartilhado e só de leitura** para os integrantes: a
  especificação da US3 abaixo vale igual para todos; não é preciso editá-lo (salvo
  preencher a tabela de integrantes abaixo).

| Integrante  | Subpacote          | Redigiu a US? | Redigiu o cenário BDD? |
|-------------|--------------------|---------------|------------------------|
| Guilherme   | `guilherme`        | —             | sim                    |
| <colega2>   | `<colega2>`        |               |                        |
| <colega3>   | `<colega3>`        |               |                        |
| <colega4>   | `<colega4>`        |               |                        |

## Especificação da classe de domínio (`Student`)

Classe **POJO simples**, sem anotações de framework. Campos e comportamento mínimos para
sustentar o cenário acima:

- `String name`
- `int xpTotal` (inicia em 0)
- `Level level` (inicia em `BRONZE`)
- `List<LevelChange> levelUpHistory` — histórico de mudanças de nível. Pode ser uma classe
  interna simples ou um `record LevelChange(Level from, Level to)` dentro do mesmo arquivo,
  já que ainda não há entidade JPA nesta fase.
- Construtor: `Student(String name)`
- Método principal: `void receiveXp(String reason, int amount)`
  - Soma `amount` a `xpTotal`.
  - Recalcula o nível a partir do novo `xpTotal` (usando a tabela de faixas acima).
  - Se o novo nível for **diferente** do nível anterior, atualiza `level` e adiciona um
    registro em `levelUpHistory`.
  - Se o novo nível for **igual** ao anterior (caso desta US), não altera `levelUpHistory`.
- Getters para `xpTotal`, `level`, `levelUpHistory`.

> Um jeito simples de deixar a classe já "pré-carregada" no estado do cenário (Diamante,
> 750 XP, 3 eventos no histórico) sem precisar simular 4 chamadas de `receiveXp` é permitir
> um construtor/setup de teste que registre esse estado inicial diretamente — decida a
> forma mais limpa dentro do TDD (pode ser um construtor extra, um método de fábrica de
> teste, ou simular as 4 chamadas reais de `receiveXp` no `@BeforeEach`; ambas as
> abordagens são aceitáveis nesta fase).

## Especificação do teste (`StudentTest`)

Um único método de teste, mapeando 1:1 o cenário BDD:

```java
@Test
void naoDeveGerarNovoLevelUpQuandoAlunoJaEstaNoNivelMaximo() {
    // arrange: aluno já no nível Diamante, 750 XP, 3 eventos no histórico

    student.receiveXp("ATIVIDADE_CONCLUIDA", 100);

    assertEquals(850, student.getXpTotal());
    assertEquals(Level.DIAMANTE, student.getLevel());
    assertEquals(3, student.getLevelUpHistory().size());
}
```

Use JUnit 5 (`org.junit.jupiter.api.Test`) e `org.junit.jupiter.api.Assertions` ou AssertJ,
o que já estiver configurado no projeto — mesma lib usada no exercício da calculadora.

## Passo a passo TDD (RED → GREEN)

1. Criar o pacote `domain` (em `src/main` e o espelho em `src/test`) e escrever
   `StudentTest.java` **primeiro**, referenciando `Student` e `Level` que ainda não existem
   (ou existem vazios).
2. Rodar o teste no IntelliJ e confirmar que **falha** (erro de compilação ou assertion
   falhando) — este é o **RED**. Capturar print da tela vermelha/erro.
3. Criar `Level.java` (enum) e `Student.java` com o mínimo necessário para o teste
   compilar e passar.
4. Rodar o teste de novo até ficar **verde** — este é o **GREEN**. Capturar print do teste
   passando.
5. Configurar o **Jacoco** no `pom.xml` (plugin `jacoco-maven-plugin`, com as execuções
   `prepare-agent` e `report` atreladas à fase `test` — não depende de Spring Boot, funciona
   em qualquer projeto Maven). Rodar `mvn test` e capturar o print do relatório gerado em
   `target/site/jacoco/index.html`, mesmo que apareça amarelo/vermelho em partes ainda não
   cobertas — isso é esperado nesta fase.

## Fora do escopo desta entrega

- Anotações JPA (`@Entity`, etc.) — a classe de domínio é POJO puro por enquanto.
- Service, Repository, Controller, DTO, exception handler.
- Banco H2/Postgres, PgAdmin, Swagger, Docker, front-end VueJS.
- Refactor para 100% de cobertura (fase BLUE).
- Implementação das outras 3 User Stories (US1, US2, US4) — cada uma é responsabilidade de
  outro integrante do grupo, em sua própria classe/teste.

## Lembrete para o README final do projeto (nível do grupo, não desta entrega isolada)

Quando o projeto for consolidado, o README do GitHub precisa conter, além da documentação
técnica: descrição do case (Educação Continuada Gamificada), uma tabela com a User Story de
cada integrante identificando quem a redigiu, e uma tabela com os cenários BDD de cada
integrante identificando o autor. Guarde os dados desta entrega (US3 + cenário acima) para entrar nessa tabela depois.
