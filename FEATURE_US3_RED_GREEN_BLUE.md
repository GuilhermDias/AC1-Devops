# TDD — US3: Sistema de Níveis por XP (RED → GREEN → BLUE)

## Escopo desta entrega

Entrega da atividade AC1-ATDD. Aqui entra o ciclo **completo** de TDD para a US3, feito
sobre uma **única classe de domínio** (POJO puro, sem anotação de framework — nem
`@Entity`, nem Spring):

1. **RED** — escrever o teste primeiro, rodar e capturar evidência de que ele **falha**
   (classe/método ainda não existem ou não fazem o que o teste espera).
2. **GREEN** — implementar o código mínimo necessário para o teste passar, rodar de novo e
   capturar evidência (teste verde + relatório de cobertura do Jacoco,
   mesmo que venha amarelo/vermelho em partes ainda não cobertas).
3. **BLUE** — refatorar o código já verde (limpar duplicação, extrair métodos, melhorar
   nomes, aumentar cobertura das faixas de XP) **sem alterar o comportamento**, rodar o
   teste de novo e confirmar que continua verde após a refatoração.

Ficam fora do escopo (entregas futuras): Service, Repository, Entity JPA, DTO, Controller,
Swagger, banco H2/Postgres/PgAdmin, front-end VueJS e Docker.

## User Story desta entrega

**US3** — Não gerar novo level-up quando o aluno já está no nível máximo

## Cenário BDD desta entrega

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

## Especificação da classe de domínio (`Student`)

Classe **POJO simples**, sem anotações de framework. Campos e comportamento mínimos para
sustentar o cenário acima:

- `String name`
- `int xpTotal` (inicia em 0)
- `Level level` (inicia em `BRONZE`)
- `List<LevelUpEvent> levelUpEvents` — histórico de mudanças de nível.
- Construtor: `Student(String name)`
- Método principal: `void receiveXp(String reason, int amount)`
    - Soma `amount` a `xpTotal`.
    - Recalcula o nível a partir do novo `xpTotal` (usando a tabela de faixas acima).
    - Se o novo nível for **diferente** do nível anterior, atualiza `level` e adiciona um
      registro em `levelUpEvents`.
    - Se o novo nível for **igual** ao anterior (caso desta US), não altera `levelUpEvents`.
- Getters para `xpTotal`, `level`, `levelUpEvents`.

## Passo a passo TDD realizado (RED → GREEN → BLUE)

### 1. RED
1. Criação prévia de `StudentTest.java` estruturado com o cenário da US3 antes da implementação lógica completa das classes de domínio.
2. Execução inicial confirmando a falha de compilação/execução, validando o ciclo RED.

### 2. GREEN
1. Implementação inicial das classes `Level.java` e `Student.java` com o código estritamente necessário para fazer o teste passar.
2. Integração do `jacoco-maven-plugin` no `pom.xml` para monitoramento de métricas.
3. Execução bem-sucedida dos testes no estado verde inicial.

### 3. BLUE (Refatoração e Otimização)
1. **Refatoração estrutural da classe `Student`**: A lógica repetitiva ou complexa dentro do fluxo de XP foi isolada em um método privado auxiliar (`processLevelUp()`), aumentando a legibilidade e aplicando o princípio de responsabilidade única. Adicionou-se também uma cláusula de guarda para tratar preventivamente valores de XP menores ou iguais a zero (`amount <= 0`).
2. **Expansão da suíte de testes (`StudentTest`)**: Novos cenários foram incorporados para validar a integridade da inicialização do aluno, a robustez contra entradas inválidas e a cobertura de métodos auxiliares do domínio (`getMinXp()` e `getFromLevel()`).
3. **Consolidação da Cobertura (JaCoCo)**: A suíte completa foi reexecutada com sucesso, garantindo estabilidade e atingindo **100% de cobertura de código** no pacote de domínio (`org.example.ac1devops.domain`).