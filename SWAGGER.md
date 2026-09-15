# Swagger — AC1 / Grupo 10

## O que foi implementado

- Dependência `springdoc-openapi-starter-webmvc-ui` 3.1.1, da linha compatível com Spring Boot 4.
- `OpenApiConfig`: título, versão e descrição da API.
- Geração automática da especificação OpenAPI e interface Swagger UI.
- `SwaggerIntegrationTest`: verifica por HTTP a especificação, a página do Swagger UI e a URL usada pela interface.

Referência: https://springdoc.org/getting-started.html

## Validação realizada

Em 15/09/2026, o Maven concluiu com **BUILD SUCCESS: 6 testes, 0 falhas, 0 erros, 0 ignorados**, usando o JDK 25.0.3 do IntelliJ e compilando para Java 21 conforme o `pom.xml`.

O teste de integração confirmou HTTP 200 para `/v3/api-docs`, `/swagger-ui/index.html` e `/v3/api-docs/swagger-config`, além do título da API e da ligação da interface com a especificação. Os cinco testes anteriores também passaram. Para reproduzir a verificação, execute `./mvnw test` (ou `.\mvnw.cmd test` no PowerShell).

Essa verificação cobre a infraestrutura do Swagger. Ainda falta validar a operação de negócio quando o Controller estiver disponível.

## Como executar

O projeto exige JDK 21 ou superior compatível com o Spring Boot usado no projeto. Configure o SDK do projeto no IntelliJ e o JDK do Maven. No terminal, confira `java -version` e `JAVA_HOME`.

Na pasta que contém `pom.xml`, execute no PowerShell:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

Com a aplicação em execução, abra:

- Swagger UI: http://localhost:8080/swagger-ui/index.html
- Especificação OpenAPI em JSON: http://localhost:8080/v3/api-docs

Os endereços pressupõem a porta padrão 8080 e nenhum caminho de contexto configurado. Ajuste-os caso o grupo altere essas configurações.

## Integração com o Controller do grupo

O ZIP recebido ainda não possui Controller, Service, Repository ou Entity JPA. Por isso, inicialmente o Swagger pode mostrar **No operations defined in spec!**. A configuração está preparada, mas a demonstração do endpoint depende da integração do Controller real.

Mantenha os Controllers no pacote `org.example.ac1devops` ou em seus subpacotes, para que sejam encontrados pela aplicação. Métodos expostos com `@GetMapping`, `@PostMapping` e demais mapeamentos do Spring serão documentados automaticamente, incluindo seus parâmetros e tipos de entrada e saída.

Para melhorar a descrição, o responsável pode acrescentar no Controller real:

```java
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

// Na classe do Controller:
@Tag(name = "Alunos", description = "Operações de alunos e evolução por XP")

// No método do endpoint, junto ao mapeamento HTTP já existente:
@Operation(summary = "Conceder XP ao aluno",
        description = "Acumula XP e retorna o estado atualizado do aluno.")
```

Esses trechos ilustram anotações, não são um Controller completo. Ajuste as descrições ao comportamento efetivamente implementado. Não é necessário adicionar Swagger nas classes de domínio.

## Como demonstrar e coletar evidências

1. Integre o Controller do grupo e execute os testes.
2. Inicie a aplicação e abra o Swagger UI.
3. Verifique o título da API e a operação que será apresentada.
4. Expanda a operação, clique em **Try it out**, preencha os dados válidos e clique em **Execute**.
5. Confira o status HTTP e o corpo da resposta. Para a US3, prepare o aluno em Diamante e confirme que receber mais XP não cria outro evento de level-up.
6. Capture uma imagem com a rota e outra com a requisição e a resposta reais. Inclua essas evidências no README final.

Não foram geradas evidências de execução do endpoint, pois ele ainda não existe nesta versão do projeto.

## Arquivos para levar ao repositório compartilhado

- `pom.xml`: acrescente somente a dependência do springdoc se os colegas já tiverem alterado esse arquivo.
- `src/main/java/org/example/ac1devops/config/OpenApiConfig.java`
- `src/test/java/org/example/ac1devops/SwaggerIntegrationTest.java`
- `SWAGGER.md`

O domínio e os testes TDD existentes não precisam ser modificados para essa configuração. A entrega final do endpoint deve ser validada novamente depois da integração das demais camadas.
