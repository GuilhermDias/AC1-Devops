package org.example.ac1devops.web;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Teste de integracao ponta a ponta do modulo progression: HTTP real -> StudentController ->
 * StudentProgressionApi -> StudentService -> JPA -> H2. Garante que a modularizacao (ADR-001)
 * nao mudou o contrato externo de /students (status, campos e valores do JSON).
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class StudentEndpointIntegrationTest {

    @LocalServerPort
    private int port;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deveCriarAlunoDarXpCruzandoFaixasEConsultarPerfilCompleto() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> created = post(client, "/students", "{\"name\":\"Aluno Integracao\"}");
            assertEquals(201, created.statusCode());
            JsonNode student = objectMapper.readTree(created.body());
            long id = student.path("id").asLong();
            assertEquals("Aluno Integracao", student.path("name").asText());
            assertEquals(0, student.path("xpTotal").asInt());
            assertEquals("BRONZE", student.path("level").asText());
            assertEquals(0, student.path("levelUpEvents").size());

            HttpResponse<String> updated = post(client, "/students/" + id + "/xp",
                    "{\"reason\":\"DESAFIO_HARD\",\"amount\":350}");
            assertEquals(200, updated.statusCode());
            JsonNode afterXp = objectMapper.readTree(updated.body());
            assertEquals(350, afterXp.path("xpTotal").asInt());
            assertEquals("OURO", afterXp.path("level").asText());
            assertEquals(2, afterXp.path("levelUpEvents").size());
            assertEquals("BRONZE", afterXp.path("levelUpEvents").get(0).path("fromLevel").asText());
            assertEquals("PRATA", afterXp.path("levelUpEvents").get(0).path("toLevel").asText());
            assertEquals("OURO", afterXp.path("levelUpEvents").get(1).path("toLevel").asText());

            HttpResponse<String> fetched = get(client, "/students/" + id);
            assertEquals(200, fetched.statusCode());
            assertEquals(afterXp, objectMapper.readTree(fetched.body()));

            HttpResponse<String> listed = get(client, "/students");
            assertEquals(200, listed.statusCode());
            boolean found = false;
            for (JsonNode node : objectMapper.readTree(listed.body())) {
                found |= node.path("id").asLong() == id;
            }
            assertTrue(found);
        }
    }

    @Test
    void deveRetornar404ComMensagemParaAlunoInexistente() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> response = get(client, "/students/999999");
            assertEquals(404, response.statusCode());
            assertEquals("Aluno nao encontrado: 999999",
                    objectMapper.readTree(response.body()).path("message").asText());

            HttpResponse<String> xp = post(client, "/students/999999/xp", "{\"reason\":\"X\",\"amount\":10}");
            assertEquals(404, xp.statusCode());
        }
    }

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> post(HttpClient client, String path, String json) throws Exception {
        return client.send(HttpRequest.newBuilder(uri(path))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
