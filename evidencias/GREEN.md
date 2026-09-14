# Evidências — Fase GREEN (US1–US4)

Comando usado nas duas fases: `./mvnw -B test`

## 1. RED (antes da implementação)

O teste `StudentTest` não compilava: `Student` não expunha `getLevelUpEvents()`.
Saída completa em [`red-output.txt`](red-output.txt).

```
[ERROR] .../StudentTest.java:[18,27] cannot find symbol
[ERROR]   symbol:   method getLevelUpEvents()
[ERROR]   location: variable student of type org.example.ac1devops.domain.Student
[ERROR] .../StudentTest.java:[19,27] cannot find symbol
[ERROR] .../StudentTest.java:[31,27] cannot find symbol
[ERROR] .../StudentTest.java:[43,27] cannot find symbol
[ERROR] .../StudentTest.java:[55,27] cannot find symbol
[INFO] 5 errors
[INFO] BUILD FAILURE
```

## 2. Implementação mínima (GREEN)

- **`LevelUpEvent`** (nova classe): evento de transição com `getFromLevel()` / `getToLevel()`,
  substituindo o antigo record interno `Student.LevelChange`, que não atendia à API do teste.
- **`Student.receiveXp()`**: passou a promover o aluno **faixa por faixa**. Um salto de 0 → 750 XP
  gera 3 eventos (Bronze→Prata, Prata→Ouro, Ouro→Diamante), e não um só. Quando o nível calculado
  é igual ao atual (US3, aluno já em Diamante), o laço não executa e o histórico fica intacto.
- **`Level`**: sem alteração.
- **`pom.xml`**: JaCoCo 0.8.12 → 0.8.13. A 0.8.12 não instrumenta bytecode Java 24 e poluía a saída
  com `IllegalClassFormatException` nas classes do Spring. Não muda comportamento, só limpa o relatório.

## 3. GREEN (depois da implementação)

Saída completa em [`green-output.txt`](green-output.txt).

```
[INFO] Tests run: 1, Failures: 0, Errors: 0, Skipped: 0 -- in org.example.ac1devops.Ac1DevOpsApplicationTests
[INFO] Tests run: 4, Failures: 0, Errors: 0, Skipped: 0 -- in org.example.ac1devops.domain.StudentTest
[INFO]
[INFO] Results:
[INFO]
[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0
[INFO]
[INFO] --- jacoco:0.8.13:report (report) @ Grupo10_ATDD ---
[INFO] Analyzed bundle 'AC1-DevOps' with 4 classes
[INFO] BUILD SUCCESS
```

| Cenário | Teste | Resultado |
|---|---|---|
| US1 — sobe de Bronze para Prata ao cruzar a faixa | `deveSubirDeNivelQuandoXpCruzaFaixaBronzeParaPrata` | PASSOU |
| US2 — acumula XP sem mudar de nível | `deveAcumularXpSemMudarNivelQuandoPermaneceNaMesmaFaixa` | PASSOU |
| US3 — não gera novo level-up no nível máximo | `naoDeveGerarNovoLevelUpQuandoAlunoJaEstaNoNivelMaximo` | PASSOU |
| US4 — consulta de perfil com XP, nível e histórico | `deveRetornarXpTotalNivelEHistoricoDeLevelUpsAoConsultarPerfil` | PASSOU |

## 4. Cobertura

Ver [`cobertura-jacoco.md`](cobertura-jacoco.md) — 91% de instruções, 100% de branches.
Os trechos amarelos/vermelhos estão listados lá com a justificativa de cada um.
Relatório navegável: [`jacoco-report/index.html`](jacoco-report/index.html).
