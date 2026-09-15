# Cobertura JaCoCo

## Fase GREEN (antes do refactor)

| Classe | Instruções | Branches | Linhas não cobertas | Status |
|---|---|---|---|---|
| `Ac1DevOpsApplication` | 38% | n/a | 2 de 3 | VERMELHO |
| `Student` | 96% | 100% | 1 de 18 | VERDE |
| `Level` | 96% | 100% | 1 de 14 | VERDE |
| `LevelUpEvent` | 80% | n/a | 1 de 6 | AMARELO |
| **TOTAL** | **91%** | **100%** | **5 de 41** | |

### O que estava amarelo/vermelho e por quê

| Ponto não coberto | Motivo |
|---|---|
| `Ac1DevOpsApplication.main()` (2 linhas) | O `@SpringBootTest` sobe o contexto, mas nunca chama `main()`. Fora do escopo da lógica de domínio. |
| `LevelUpEvent.getFromLevel()` (1 linha) | Os testes RED só assertavam `getToLevel()`. |
| `Student.getName()` (1 linha) | Nenhum cenário das US1–US4 consultava o nome do aluno. |
| `Level.getMinXp()` (1 linha) | A faixa era lida internamente por `fromXp()`; o getter público não era chamado pelos testes. |

## Fase BLUE (depois do refactor) — ver [`BLUE.md`](BLUE.md)

| Classe | Instruções | Branches | Linhas | Métodos | Status |
|---|---|---|---|---|---|
| `Student` | 100% | 100% | 100% | 100% | VERDE |
| `Level` | 100% | 100% | 100% | 100% | VERDE |
| `LevelUpEvent` | 100% | 100% | 100% | 100% | VERDE |
| `OpenApiConfig` | 100% | 100% | 100% | 100% | VERDE |
| **TOTAL (domínio + config)** | **100%** | **100%** | **100%** | **100%** | |

`Ac1DevOpsApplication` foi excluído da análise do JaCoCo (ver justificativa em
[`BLUE.md`](BLUE.md) e em `pom.xml`) — não é código de domínio, é apenas o bootstrap do Spring
Boot.

### O que resolveu cada ponto

| Ponto que estava amarelo/vermelho | Como foi coberto |
|---|---|
| `Ac1DevOpsApplication.main()` | Excluído da métrica (classe de bootstrap, não de domínio) |
| `LevelUpEvent.getFromLevel()` | `StudentTest` passou a assertar também `getFromLevel()` do primeiro evento de level-up |
| `Student.getName()` | `StudentTest.deveInicializarAlunoCorretamente()` assertar `getName()` |
| `Level.getMinXp()` | `StudentTest.deveRetornarMinXpDosNiveis()` + `LevelTest.deveExporOXpMinimoDeCadaFaixa()` validam a tabela de faixas |
| `Student.receiveXp()` com XP inválido | `StudentTest.naoDeveProcessarXpInvalido()` cobre a guarda `amount <= 0` adicionada no refactor |
| `OpenApiConfig` | `SwaggerIntegrationTest` exercita o bean `educationOpenAPI()` ao consultar `/v3/api-docs` |

## Camadas REST (Entity/Repository/DTO/Service/Controller) — após BLUE

Com o domínio já 100% coberto, as camadas novas em cima dele (`entity`, `service`, `web`, `dto`)
também fecharam em 100% em todas as métricas, com `StudentRepositoryTest` (`@DataJpaTest`),
`StudentServiceTest` (Mockito) e `StudentControllerTest` (`@WebMvcTest` + `MockMvcTester`):

| Classe | Instruções | Branches | Linhas | Métodos | Status |
|---|---|---|---|---|---|
| `Student`, `Level`, `LevelUpEvent` (domínio) | 100% | 100% | 100% | 100% | VERDE |
| `OpenApiConfig` | 100% | 100% | 100% | 100% | VERDE |
| `StudentEntity`, `LevelUpEventEntity` | 100% | 100% | 100% | 100% | VERDE |
| `StudentService`, `StudentNotFoundException` | 100% | 100% | 100% | 100% | VERDE |
| `StudentController`, `ApiExceptionHandler` | 100% | 100% | 100% | 100% | VERDE |
| `CreateStudentRequestDTO`, `ReceiveXpRequestDTO`, `LevelUpEventDTO`, `StudentResponseDTO` | 100% | 100% | 100% | 100% | VERDE |
| **TOTAL (projeto inteiro, exceto bootstrap)** | **100%** | **100%** | **100%** | **100%** | |

Fonte bruta (CSV gerado pelo JaCoCo com o projeto completo):

```
GROUP,PACKAGE,CLASS,INSTRUCTION_MISSED,INSTRUCTION_COVERED,BRANCH_MISSED,BRANCH_COVERED,LINE_MISSED,LINE_COVERED,COMPLEXITY_MISSED,COMPLEXITY_COVERED,METHOD_MISSED,METHOD_COVERED
AC1-DevOps,org.example.ac1devops.web,StudentController,0,32,0,0,0,7,0,5,0,5
AC1-DevOps,org.example.ac1devops.web,ApiExceptionHandler,0,11,0,0,0,2,0,2,0,2
AC1-DevOps,org.example.ac1devops.entity,StudentEntity,0,57,0,0,0,20,0,10,0,10
AC1-DevOps,org.example.ac1devops.entity,LevelUpEventEntity,0,28,0,0,0,12,0,7,0,7
AC1-DevOps,org.example.ac1devops.config,OpenApiConfig,0,17,0,0,0,6,0,2,0,2
AC1-DevOps,org.example.ac1devops.service,StudentNotFoundException,0,5,0,0,0,2,0,1,0,1
AC1-DevOps,org.example.ac1devops.service,StudentService,0,155,0,4,0,29,0,12,0,10
AC1-DevOps,org.example.ac1devops.domain,Student,0,74,0,4,0,22,0,9,0,7
AC1-DevOps,org.example.ac1devops.domain,LevelUpEvent,0,15,0,0,0,6,0,3,0,3
AC1-DevOps,org.example.ac1devops.domain,Level,0,68,0,4,0,14,0,6,0,4
AC1-DevOps,org.example.ac1devops.dto,ReceiveXpRequestDTO,0,9,0,0,0,1,0,1,0,1
AC1-DevOps,org.example.ac1devops.dto,LevelUpEventDTO,0,9,0,0,0,1,0,1,0,1
AC1-DevOps,org.example.ac1devops.dto,CreateStudentRequestDTO,0,6,0,0,0,1,0,1,0,1
AC1-DevOps,org.example.ac1devops.dto,StudentResponseDTO,0,18,0,0,0,1,0,1,0,1
```

Relatório HTML navegável (para print): `evidencias/jacoco-report/index.html`
