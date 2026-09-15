# Evidências — Fase BLUE (US1–US4)

Comando usado: `./mvnw -q test`

Ponto de partida: fase GREEN (ver [`GREEN.md`](GREEN.md)), já com os 4 cenários de US1–US4
passando, 91% de instruções e 100% de branches. Os pontos amarelo/vermelho identificados em
[`cobertura-jacoco.md`](cobertura-jacoco.md) eram getters nunca exercitados pelos testes
(`Student.getName()`, `Level.getMinXp()`, `LevelUpEvent.getFromLevel()`) e o `main()` do
bootstrap Spring (`Ac1DevOpsApplication`).

## O que mudou nesta fase (refactor, sem alterar comportamento externo)

Trabalho feito em duas frentes que se complementam (mesclado via `feature/us3-blue` +
`feature/swagger` em `main`):

- **`Student`**: a lógica de progressão de nível foi extraída para o método privado
  `processLevelUp()`, melhorando a legibilidade. Também foi adicionada uma cláusula de
  guarda para `amount <= 0` em `receiveXp` (XP inválido não altera estado nem gera eventos).
- **`StudentTest`**: reescrito com casos mais explícitos — inicialização do aluno,
  transição Bronze→Prata com checagem de `getFromLevel()`/`getToLevel()`, acúmulo sem troca
  de faixa, ausência de novo evento no nível máximo (Diamante) mesmo recebendo XP extra, e
  rejeição de XP zero/negativo.
- **`LevelTest`** (novo): valida a tabela de faixas de XP via `Level.getMinXp()` para os 4
  níveis — não existia nenhum teste dedicado a `Level` isoladamente.
- **`pom.xml`**: `Ac1DevOpsApplication` (classe de bootstrap do Spring Boot, só chama
  `SpringApplication.run`) foi excluída da análise do JaCoCo
  (`<configuration><excludes>**/Ac1DevOpsApplication.class</excludes></configuration>`).
  Testar `main()` diretamente não agrega valor de negócio; essa é a prática padrão em projetos
  Spring para não poluir a métrica de cobertura com a classe de entrada da aplicação.
- **Swagger/OpenAPI** (`feature/swagger`, trazido junto por já ser pré-requisito da próxima
  entrega de Controller): `OpenApiConfig` + `SwaggerIntegrationTest`, cobertos a 100% como
  efeito colateral do teste de integração.
- **Nenhuma classe de domínio (`Student`, `Level`, `LevelUpEvent`) teve comportamento de
  negócio alterado** — só ganharam robustez (guarda de XP inválido) e cobertura de testes.

## Resultado (depois do refactor)

Saída completa em [`blue-output.txt`](blue-output.txt).

```
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in org.example.ac1devops.Ac1DevOpsApplicationTests
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in org.example.ac1devops.SwaggerIntegrationTest
Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in org.example.ac1devops.domain.LevelTest
Tests run: 6, Failures: 0, Errors: 0, Skipped: 0 -- in org.example.ac1devops.domain.StudentTest
BUILD SUCCESS
```

| Cenário | Teste | Resultado |
|---|---|---|
| Inicialização do aluno (nome, XP zero, nível Bronze) | `deveInicializarAlunoCorretamente` | PASSOU |
| US1 — sobe de Bronze para Prata ao cruzar a faixa | `deveSubirDeNivelQuandoXpCruzaFaixaBronzeParaPrata` | PASSOU |
| US2 — acumula XP sem mudar de nível | `deveAcumularXpSemMudarNivel` | PASSOU |
| US3 — não gera novo level-up no nível máximo | `naoDeveGerarNovoLevelUpNoNivelMaximo` | PASSOU |
| Guarda de XP inválido (zero/negativo) | `naoDeveProcessarXpInvalido` | PASSOU |
| Faixas de XP expostas por `Level.getMinXp()` (via `StudentTest`) | `deveRetornarMinXpDosNiveis` | PASSOU |
| Faixas de XP expostas por `Level.getMinXp()` (teste dedicado) | `deveExporOXpMinimoDeCadaFaixa` | PASSOU |
| Documentação OpenAPI/Swagger disponível | `deveDisponibilizarEspecificacaoOpenApiEInterfaceSwagger` | PASSOU |

## Cobertura final

Ver [`cobertura-jacoco.md`](cobertura-jacoco.md) — **100% de instruções, linhas, métodos e
branches** em todas as classes analisadas (`Student`, `Level`, `LevelUpEvent`, `OpenApiConfig`),
sem nenhum ponto amarelo ou vermelho. `Ac1DevOpsApplication` excluído por não agregar valor de
negócio (ver acima). Relatório navegável: [`jacoco-report/index.html`](jacoco-report/index.html).
