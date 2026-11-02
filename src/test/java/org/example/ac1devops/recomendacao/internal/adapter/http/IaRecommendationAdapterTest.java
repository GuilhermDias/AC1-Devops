package org.example.ac1devops.recomendacao.internal.adapter.http;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.serverError;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.http.Fault;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import org.example.ac1devops.progression.api.Level;
import org.example.ac1devops.recomendacao.internal.domain.CourseRecommendation;
import org.example.ac1devops.recomendacao.internal.domain.RecommendationUnavailableException;
import org.example.ac1devops.recomendacao.internal.domain.StudentProfile;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Adapter contra um servidor HTTP real (WireMock), sem Spring. Os JSON de exemplo sao os mesmos
 * verificados pelos testes pytest do servico, entao o stub nao pode divergir do servico real.
 */
class IaRecommendationAdapterTest {

    private static final Path CONTRACT = Path.of("labs/desafio-3/ia-service/contract");
    private static final Duration READ_TIMEOUT = Duration.ofMillis(300);
    private static final StudentProfile PROFILE = new StudentProfile(7L, Level.PRATA);

    private WireMockServer ia;
    private CircuitBreaker circuitBreaker;
    private IaRecommendationAdapter adapter;

    @BeforeEach
    void setUp() {
        ia = new WireMockServer(wireMockConfig().dynamicPort());
        ia.start();
        circuitBreaker = IaClientConfig.circuitBreaker(Duration.ofMillis(200));
        adapter = new IaRecommendationAdapter(new IaServiceProperties(
                ia.baseUrl(), Duration.ofMillis(300), READ_TIMEOUT, Duration.ofMillis(200)), circuitBreaker);
    }

    @AfterEach
    void tearDown() {
        ia.stop();
    }

    private void stubContract() throws IOException {
        ia.stubFor(post(urlEqualTo("/recomendacoes"))
                .withRequestBody(equalToJson(Files.readString(CONTRACT.resolve("recomendacoes-requisicao.json"))))
                .willReturn(aResponse()
                        .withHeader("Content-Type", "application/json")
                        .withBody(Files.readString(CONTRACT.resolve("recomendacoes-resposta.json")))));
    }

    private void assertUnavailable() {
        assertThatThrownBy(() -> adapter.recommend(PROFILE)).isInstanceOf(RecommendationUnavailableException.class);
    }

    @Test
    void deveEnviarERespeitarOContratoDoServico() throws IOException {
        stubContract();

        List<CourseRecommendation> courses = adapter.recommend(PROFILE);

        assertThat(courses).hasSize(3);
        assertThat(courses.get(0)).isEqualTo(
                new CourseRecommendation("testes-unitarios", "Testes Unitarios", "Indicado para o seu nivel"));
    }

    @Test
    void deveFalharQuandoOServicoRespondeErro() {
        ia.stubFor(post(urlEqualTo("/recomendacoes")).willReturn(serverError()));

        assertUnavailable();
    }

    @Test
    void deveFalharComRespostaForaDoContrato() {
        ia.stubFor(post(urlEqualTo("/recomendacoes")).willReturn(aResponse()
                .withHeader("Content-Type", "application/json").withBody("{\"outro\":true}")));

        assertUnavailable();
    }

    @Test
    void deveFalharQuandoAConexaoCai() {
        ia.stubFor(post(urlEqualTo("/recomendacoes")).willReturn(aResponse().withFault(Fault.CONNECTION_RESET_BY_PEER)));

        assertUnavailable();
    }

    @Test
    void deveDesistirNoTimeoutQuandoOServicoEstaLento() {
        ia.stubFor(post(urlEqualTo("/recomendacoes")).willReturn(aResponse().withFixedDelay(5_000)));

        long start = System.nanoTime();
        assertUnavailable();
        long elapsedMillis = Duration.ofNanos(System.nanoTime() - start).toMillis();

        assertThat(elapsedMillis).isLessThan(1_500);
    }

    @Test
    void deveAbrirOCircuitoEPararDeChamarOServico() {
        ia.stubFor(post(urlEqualTo("/recomendacoes")).willReturn(serverError()));

        for (int i = 0; i < 5; i++) {
            assertUnavailable();
        }
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        assertUnavailable();

        ia.verify(5, postRequestedFor(urlEqualTo("/recomendacoes")));
    }

    @Test
    void deveFecharOCircuitoQuandoOServicoVolta() throws Exception {
        ia.stubFor(post(urlEqualTo("/recomendacoes")).willReturn(serverError()));
        for (int i = 0; i < 5; i++) {
            assertUnavailable();
        }
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.OPEN);

        ia.resetAll();
        stubContract();
        Thread.sleep(300);

        assertThat(adapter.recommend(PROFILE)).hasSize(3);
        assertThat(adapter.recommend(PROFILE)).hasSize(3);
        assertThat(circuitBreaker.getState()).isEqualTo(CircuitBreaker.State.CLOSED);
    }
}
