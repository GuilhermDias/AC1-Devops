package org.example.ac1devops.web;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import org.example.ac1devops.conquistas.internal.domain.AwardedBadgePort;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Falhas simuladas: o endpoint existente de XP continua respondendo e salvando o XP quando a
 * concessao de conquistas falha. Conquistas nao pode derrubar a progressao do aluno.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ConquistasFailureIsolationTest {

    @LocalServerPort
    private int port;

    @MockitoSpyBean
    private AwardedBadgePort awardedBadges;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void falhaDaPersistenciaNaoDeveAfetarAConcessaoDeXp() throws Exception {
        doThrow(new IllegalStateException("persistencia fora do ar")).when(awardedBadges).findByStudent(any());

        try (HttpClient client = HttpClient.newHttpClient()) {
            long id = createStudent(client);

            assertXpGranted(client, id, 150, 150, "PRATA");
        }
    }

    /**
     * Reproduz de forma deterministica a corrida entre duas requisicoes do mesmo aluno: ambas leem
     * que o badge nao existe e a segunda esbarra na restricao unica do banco. O rollback limpa a
     * sessao compartilhada do open-in-view, entao a resposta nao pode depender dela depois do evento.
     */
    @Test
    void violacaoDaRestricaoUnicaNaoDeveAfetarAConcessaoDeXp() throws Exception {
        try (HttpClient client = HttpClient.newHttpClient()) {
            long id = createStudent(client);
            assertXpGranted(client, id, 50, 50, "BRONZE");

            doReturn(List.of()).when(awardedBadges).findByStudent(any());

            assertXpGranted(client, id, 50, 100, "PRATA");
        }
    }

    private long createStudent(HttpClient client) throws Exception {
        HttpResponse<String> created = post(client, "/students", "{\"name\":\"Aluno Falha\"}");
        assertEquals(201, created.statusCode());
        return objectMapper.readTree(created.body()).path("id").asLong();
    }

    private void assertXpGranted(HttpClient client, long id, int amount, int expectedXp, String expectedLevel)
            throws Exception {
        HttpResponse<String> updated = post(client, "/students/" + id + "/xp",
                "{\"reason\":\"DESAFIO\",\"amount\":" + amount + "}");

        assertEquals(200, updated.statusCode());
        JsonNode student = objectMapper.readTree(updated.body());
        assertEquals(expectedXp, student.path("xpTotal").asInt());
        assertEquals(expectedLevel, student.path("level").asText());
    }

    private HttpResponse<String> post(HttpClient client, String path, String json) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + path))
                        .header("Content-Type", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(json))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
    }
}
