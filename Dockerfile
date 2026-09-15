# ---------------------------------------------------------------------------
# Etapa 1: build da aplicacao
# Imagem com JDK 21 (versao alvo do projeto, definida em <java.version> no pom).
# ---------------------------------------------------------------------------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# pom.xml em camada separada: enquanto as dependencias nao mudarem, o Docker reaproveita
# o cache do 'go-offline' e nao baixa o Maven repo inteiro a cada alteracao de codigo.
COPY pom.xml .
RUN mvn -B dependency:go-offline

COPY src ./src
# Testes ficam fora da imagem: rodam no pipeline/local com H2 (./mvnw test), sem exigir
# um Postgres no ar durante o build.
RUN mvn -B clean package -DskipTests

# ---------------------------------------------------------------------------
# Etapa 2: imagem final enxuta (apenas JRE + jar)
# ---------------------------------------------------------------------------
FROM eclipse-temurin:21-jre
WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "/app/app.jar"]
