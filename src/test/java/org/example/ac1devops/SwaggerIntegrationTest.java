package org.example.ac1devops;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SwaggerIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    void deveDisponibilizarEspecificacaoOpenApiEInterfaceSwagger() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            HttpResponse<String> specification = get(client, "/v3/api-docs");
            assertEquals(200, specification.statusCode());
            var document = new ObjectMapper().readTree(specification.body());
            assertTrue(document.path("openapi").asText().startsWith("3."));
            assertEquals("Educação Continuada Gamificada - API",
                    document.path("info").path("title").asText());

            HttpResponse<String> ui = get(client, "/swagger-ui/index.html");
            assertEquals(200, ui.statusCode());
            assertTrue(ui.body().contains("swagger-ui-bundle.js"));

            HttpResponse<String> config = get(client, "/v3/api-docs/swagger-config");
            assertEquals(200, config.statusCode());
            assertEquals("/v3/api-docs",
                    new ObjectMapper().readTree(config.body()).path("url").asText());
        }
    }

    private HttpResponse<String> get(HttpClient client, String path) throws Exception {
        return client.send(HttpRequest.newBuilder(
                        URI.create("http://localhost:" + port + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
