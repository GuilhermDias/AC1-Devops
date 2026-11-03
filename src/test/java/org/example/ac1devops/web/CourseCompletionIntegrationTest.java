package org.example.ac1devops.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.example.ac1devops.cursos.api.CursoConcluidoEvent;
import org.example.ac1devops.cursos.internal.persistence.CourseCompletionRepository;
import org.example.ac1devops.cursos.internal.persistence.OutboxEventEntity;
import org.example.ac1devops.cursos.internal.persistence.OutboxEventRepository;
import org.example.ac1devops.notificacoes.internal.domain.NotificationChannel;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * Fluxo "concluir curso" com HTTP real: a parte sincrona responde mesmo com o consumidor indisponivel,
 * e a parte assincrona se recupera quando ele volta.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "cursos.outbox.sweep-interval-ms=100",
        "cursos.outbox.base-backoff=50ms",
        "cursos.outbox.max-attempts=3"
})
@Timeout(60)
class CourseCompletionIntegrationTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(10);

    @LocalServerPort
    private int port;

    @MockitoSpyBean
    private NotificationChannel channel;

    @MockitoSpyBean
    private OutboxEventRepository outbox;

    @Autowired
    private CourseCompletionRepository completions;

    @Autowired
    private ApplicationEventPublisher eventPublisher;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private HttpClient client;

    @BeforeEach
    void setUp() {
        client = HttpClient.newHttpClient();
    }

    @AfterEach
    void tearDown() {
        client.close();
    }

    @Test
    void deveConcluirERecompensarNaRespostaEEntregarDepois() throws Exception {
        long id = createStudent();

        HttpResponse<String> response = complete("ci-cd", id);

        assertThat(response.statusCode()).isEqualTo(201);
        JsonNode body = objectMapper.readTree(response.body());
        assertThat(body.path("xpAwarded").asInt()).isEqualTo(150);
        assertThat(body.path("xpTotal").asInt()).isEqualTo(150);
        assertThat(body.path("level").asText()).isEqualTo("PRATA");
        assertThat(body.path("alreadyCompleted").asBoolean()).isFalse();

        await().atMost(TIMEOUT).until(() -> "DELIVERED".equals(eventOf(id).path("status").asText()));
        JsonNode notifications = notificationsOf(id);
        assertThat(notifications.size()).isEqualTo(1);
        assertThat(notifications.get(0).path("message").asText()).contains("ci-cd", "150 XP");
        assertThat(notifications.get(0).path("sentAt").asText()).isNotBlank();
        assertThat(notifications.get(0).path("eventId").asLong()).isEqualTo(eventOf(id).path("id").asLong());
        assertThat(notifications.get(0).path("studentId").asLong()).isEqualTo(id);
    }

    /** Os badges saem de um listener depois do commit: sem transacao propria, nao seriam gravados. */
    @Test
    void deveConcederBadgesDepoisDoCommit() throws Exception {
        long id = createStudent();

        complete("ci-cd", id);

        JsonNode badges = objectMapper.readTree(get("/conquistas/students/" + id).body());
        assertThat(badges.size()).isEqualTo(2);
        assertThat(badges.get(1).path("code").asText()).isEqualTo("NIVEL_PRATA");
    }

    @Test
    void consumidorForaDoArNaoImpedeAConclusaoEARecuperacaoEAutomatica() throws Exception {
        doThrow(new IllegalStateException("canal fora do ar")).when(channel).send(any());
        long id = createStudent();

        HttpResponse<String> response = complete("docker-essencial", id);

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(objectMapper.readTree(response.body()).path("xpTotal").asInt()).isEqualTo(100);
        assertThat(objectMapper.readTree(get("/students/" + id).body()).path("xpTotal").asInt()).isEqualTo(100);
        await().atMost(TIMEOUT).until(() -> eventOf(id).path("attempts").asInt() >= 1
                && eventOf(id).path("lastError").asText().contains("canal fora do ar"));
        assertThat(notificationsOf(id).size()).isZero();

        doCallRealMethod().when(channel).send(any());
        requeueIfDead(id);

        await().atMost(TIMEOUT).until(() -> "DELIVERED".equals(eventOf(id).path("status").asText()));
        assertThat(notificationsOf(id).size()).isEqualTo(1);
        assertThat(eventOf(id).path("lastError").isNull()).isTrue();
    }

    @Test
    void deveEsgotarAsTentativasESerRecuperadoPorReenvioManual() throws Exception {
        doThrow(new IllegalStateException("canal fora do ar")).when(channel).send(any());
        long id = createStudent();
        complete("testes-unitarios", id);

        await().atMost(TIMEOUT).until(() -> "DEAD".equals(eventOf(id).path("status").asText()));
        assertThat(eventOf(id).path("attempts").asInt()).isEqualTo(3);

        doCallRealMethod().when(channel).send(any());
        assertThat(post("/cursos/eventos/" + eventOf(id).path("id").asLong() + "/reenvio").statusCode()).isEqualTo(202);

        await().atMost(TIMEOUT).until(() -> "DELIVERED".equals(eventOf(id).path("status").asText()));
        assertThat(notificationsOf(id).size()).isEqualTo(1);
    }

    @Test
    void deveRetornar404AoReenviarEventoQueNaoEstaEsgotado() throws Exception {
        assertThat(post("/cursos/eventos/999999/reenvio").statusCode()).isEqualTo(404);
    }

    @Test
    void consumidorLentoNaoAtrasaAResposta() throws Exception {
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> {
            // Espera limitada: se a chamada voltar a ser sincrona, o teste falha em vez de travar o build.
            release.await(5, TimeUnit.SECONDS);
            return null;
        }).when(channel).send(any());
        long id = createStudent();

        long start = System.nanoTime();
        HttpResponse<String> response = complete("ci-cd", id);
        long elapsedMillis = Duration.ofNanos(System.nanoTime() - start).toMillis();
        release.countDown();

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(elapsedMillis).isLessThan(1_500);
    }

    /** Evento gravado sem aviso de commit: e o estado que sobra se o processo cair logo depois do commit. */
    @Test
    void deveEntregarEventoPendenteDeixadoPorUmaQuedaDoProcesso() throws Exception {
        long id = createStudent();

        outbox.save(new OutboxEventEntity(id, "observabilidade", 200, Instant.now()));

        await().atMost(TIMEOUT).until(() -> "DELIVERED".equals(eventOf(id).path("status").asText()));
        assertThat(notificationsOf(id).size()).isEqualTo(1);
    }

    @Test
    void deveDrenarAcumuladoMaiorQueUmLoteDaVarredura() throws Exception {
        long id = createStudent();
        List<OutboxEventEntity> backlog = new ArrayList<>();
        for (int i = 0; i < 60; i++) {
            backlog.add(new OutboxEventEntity(id, "observabilidade", 200, Instant.now()));
        }

        outbox.saveAll(backlog);

        await().atMost(TIMEOUT).until(() -> notificationsOf(id).size() == 60);
    }

    @Test
    void repetirAConclusaoNaoConcedeXpNemNotificaDeNovo() throws Exception {
        long id = createStudent();
        complete("ci-cd", id);

        HttpResponse<String> repeated = complete("ci-cd", id);

        assertThat(repeated.statusCode()).isEqualTo(200);
        JsonNode body = objectMapper.readTree(repeated.body());
        assertThat(body.path("alreadyCompleted").asBoolean()).isTrue();
        assertThat(body.path("xpAwarded").asInt()).isEqualTo(150);
        assertThat(body.path("xpTotal").asInt()).isEqualTo(150);
        await().atMost(TIMEOUT).until(() -> "DELIVERED".equals(eventOf(id).path("status").asText()));
        assertThat(notificationsOf(id).size()).isEqualTo(1);
    }

    @Test
    void falhaAoGravarOEventoDesfazConclusaoERecompensa() throws Exception {
        long id = createStudent();
        doThrow(new IllegalStateException("falha ao gravar")).when(outbox).save(any());

        HttpResponse<String> response = complete("ci-cd", id);

        assertThat(response.statusCode()).isEqualTo(500);
        assertThat(completions.countByStudentIdAndCourseId(id, "ci-cd")).isZero();
        JsonNode student = objectMapper.readTree(get("/students/" + id).body());
        assertThat(student.path("xpTotal").asInt()).isZero();
        assertThat(student.path("levelUpEvents").size()).isZero();
    }

    @Test
    void deveRetornar404SemGravarNadaParaCursoOuAlunoInexistente() throws Exception {
        long id = createStudent();

        assertThat(complete("nao-existe", id).statusCode()).isEqualTo(404);
        assertThat(complete("ci-cd", 999_999).statusCode()).isEqualTo(404);
        assertThat(completions.countByStudentIdAndCourseId(999_999L, "ci-cd")).isZero();
        assertThat(get("/notificacoes/students/999999").statusCode()).isEqualTo(404);
    }

    @Test
    void conclusoesSimultaneasDoMesmoCursoConcedemUmaUnicaRecompensa() throws Exception {
        long id = createStudent();

        List<Integer> statuses = completeInParallel(id, List.of(
                "ci-cd", "ci-cd", "ci-cd", "ci-cd", "ci-cd", "ci-cd", "ci-cd", "ci-cd"));

        assertThat(statuses).containsOnly(200, 201);
        assertThat(statuses).filteredOn(status -> status == 201).hasSize(1);
        assertThat(completions.countByStudentIdAndCourseId(id, "ci-cd")).isEqualTo(1);
        assertThat(objectMapper.readTree(get("/students/" + id).body()).path("xpTotal").asInt()).isEqualTo(150);
    }

    @Test
    void conclusoesSimultaneasDeCursosDiferentesSomamTodoOXp() throws Exception {
        long id = createStudent();

        List<Integer> statuses = completeInParallel(id, List.of(
                "fundamentos-git", "logica-programacao", "testes-unitarios", "docker-essencial",
                "ci-cd", "arquitetura-software", "observabilidade", "sistemas-distribuidos"));

        assertThat(statuses).containsOnly(201);
        JsonNode student = objectMapper.readTree(get("/students/" + id).body());
        assertThat(student.path("xpTotal").asInt()).isEqualTo(1000);
        assertThat(student.path("levelUpEvents").size()).isEqualTo(3);
    }

    @Test
    void consumidorDeveIgnorarEventoRepetido() throws Exception {
        long id = createStudent();
        CursoConcluidoEvent event = new CursoConcluidoEvent(Math.negateExact(id), id, "ci-cd", 150);

        eventPublisher.publishEvent(event);
        eventPublisher.publishEvent(event);

        verify(channel, times(1)).send(any());
        assertThat(notificationsOf(id).size()).isEqualTo(1);
    }

    private List<Integer> completeInParallel(long id, List<String> courses) throws Exception {
        try (ExecutorService pool = Executors.newFixedThreadPool(courses.size())) {
            List<Future<Integer>> futures = new ArrayList<>();
            for (String course : courses) {
                futures.add(pool.submit(() -> complete(course, id).statusCode()));
            }
            List<Integer> statuses = new ArrayList<>();
            for (Future<Integer> future : futures) {
                statuses.add(future.get());
            }
            return statuses;
        }
    }

    private void requeueIfDead(long id) throws Exception {
        JsonNode event = eventOf(id);
        if ("DEAD".equals(event.path("status").asText())) {
            post("/cursos/eventos/" + event.path("id").asLong() + "/reenvio");
        }
    }

    private long createStudent() throws Exception {
        HttpResponse<String> created = send(HttpRequest.newBuilder(uri("/students"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("{\"name\":\"Aluno Curso\"}")));
        return objectMapper.readTree(created.body()).path("id").asLong();
    }

    private JsonNode eventOf(long studentId) throws Exception {
        for (JsonNode event : objectMapper.readTree(get("/cursos/eventos").body())) {
            if (event.path("studentId").asLong() == studentId) {
                return event;
            }
        }
        return objectMapper.createObjectNode();
    }

    private JsonNode notificationsOf(long studentId) throws Exception {
        return objectMapper.readTree(get("/notificacoes/students/" + studentId).body());
    }

    private HttpResponse<String> complete(String courseId, long studentId) throws Exception {
        return post("/cursos/" + courseId + "/alunos/" + studentId + "/conclusao");
    }

    private HttpResponse<String> post(String path) throws Exception {
        return send(HttpRequest.newBuilder(uri(path)).POST(HttpRequest.BodyPublishers.noBody()));
    }

    private HttpResponse<String> get(String path) throws Exception {
        return send(HttpRequest.newBuilder(uri(path)).GET());
    }

    private HttpResponse<String> send(HttpRequest.Builder request) throws Exception {
        return client.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}
