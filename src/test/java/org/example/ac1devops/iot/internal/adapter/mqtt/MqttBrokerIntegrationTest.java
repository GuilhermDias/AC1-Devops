package org.example.ac1devops.iot.internal.adapter.mqtt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.doThrow;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.example.ac1devops.iot.api.DeviceEventDTO;
import org.example.ac1devops.iot.api.IotApi;
import org.example.ac1devops.iot.internal.domain.ActivityCompletionPort;
import org.example.ac1devops.progression.api.CreateStudentRequestDTO;
import org.example.ac1devops.progression.api.StudentProgressionApi;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.MountableFile;

/**
 * Broker Mosquitto real, com a configuracao do laboratorio. Testa o que os testes sem broker nao alcancam:
 * reentrega, sessao persistente e reconexao. Ignorado se nao houver Docker.
 */
@SpringBootTest(properties = {"mqtt.enabled=true", "mqtt.reconnect-interval-ms=200"})
@Timeout(60)
class MqttBrokerIntegrationTest {

    private static final Duration TIMEOUT = Duration.ofSeconds(15);
    private static final int MQTT_PORT = 1883;
    private static final GenericContainer<?> BROKER = new GenericContainer<>("eclipse-mosquitto:2.0")
            .withExposedPorts(MQTT_PORT)
            .withCopyFileToContainer(
                    MountableFile.forHostPath(Path.of("labs/desafio-5/mosquitto/mosquitto.conf")),
                    "/mosquitto/config/mosquitto.conf");

    private static MqttClient device;

    @Autowired
    private MqttDeviceEventGateway gateway;

    @Autowired
    private StudentProgressionApi students;

    @Autowired
    private IotApi iot;

    @MockitoSpyBean
    private ActivityCompletionPort activityCompletion;

    @BeforeAll
    static void startBroker() throws Exception {
        assumeTrue(DockerClientFactory.instance().isDockerAvailable(), "Docker indisponivel");
        BROKER.start();
        device = new MqttClient(brokerUrl(), "dispositivo-teste", new MemoryPersistence());
        device.connect();
    }

    @AfterAll
    static void stopBroker() throws Exception {
        if (device != null) {
            device.disconnect();
            device.close();
        }
        BROKER.stop();
    }

    @DynamicPropertySource
    static void brokerProperties(DynamicPropertyRegistry registry) {
        registry.add("mqtt.url", MqttBrokerIntegrationTest::brokerUrl);
    }

    private static String brokerUrl() {
        return "tcp://" + BROKER.getHost() + ":" + BROKER.getMappedPort(MQTT_PORT);
    }

    @BeforeEach
    void waitForGateway() {
        await().atMost(TIMEOUT).until(gateway::isConnected);
    }

    private long createStudent() {
        return students.createStudent(new CreateStudentRequestDTO("Aluno IoT")).id();
    }

    private void publish(String eventId, String type, long studentId, String activityId) throws Exception {
        String body = "{\"eventId\":\"" + eventId + "\",\"deviceId\":\"sensor-01\",\"tipo\":\"" + type + "\","
                + "\"alunoId\":" + studentId + ",\"atividadeId\":\"" + activityId + "\","
                + "\"timestamp\":\"2026-11-03T12:00:00Z\"}";
        device.publish("labs/lab-a/devices/sensor-01/events", body.getBytes(StandardCharsets.UTF_8), 1, false);
    }

    private DeviceEventDTO eventOf(String eventId) {
        return iot.listEvents().stream().filter(event -> event.eventId().equals(eventId)).findFirst().orElse(null);
    }

    @Test
    void conclusaoDeveReaproveitarOFluxoDoCoreEDescartarAReentrega() throws Exception {
        long id = createStudent();

        publish("broker-duplicado", "conclusao", id, "ci-cd");
        publish("broker-duplicado", "conclusao", id, "ci-cd");

        await().atMost(TIMEOUT).until(() -> eventOf("broker-duplicado") != null
                && eventOf("broker-duplicado").duplicatesDiscarded() == 1);
        assertThat(eventOf("broker-duplicado").status()).isEqualTo("ACCEPTED");
        assertThat(students.getScore(id).xpTotal()).isEqualTo(150);
    }

    @Test
    void sessaoPersistenteDeveEntregarOQueChegouComOCoreForaDoAr() throws Exception {
        long id = createStudent();

        gateway.stop();
        publish("broker-offline", "presenca", id, "ci-cd");
        Thread.sleep(500);
        assertThat(eventOf("broker-offline")).isNull();
        gateway.start();

        await().atMost(TIMEOUT).until(() -> eventOf("broker-offline") != null);
    }

    @Test
    void falhaTemporariaNaoDevePerderOEvento() throws Exception {
        long id = createStudent();
        doThrow(new IllegalStateException("banco fora do ar")).doCallRealMethod()
                .when(activityCompletion).complete(any(), any());

        publish("broker-reentrega", "conclusao", id, "docker-essencial");

        await().atMost(TIMEOUT).until(() -> eventOf("broker-reentrega") != null);
        assertThat(students.getScore(id).xpTotal()).isEqualTo(100);
    }

    @Test
    void referenciaDesconhecidaDeveFicarRejeitadaSemTravarAFila() throws Exception {
        long id = createStudent();

        publish("broker-rejeitado", "conclusao", id, "curso-inexistente");
        publish("broker-depois", "inicio", id, "ci-cd");

        await().atMost(TIMEOUT).until(() -> eventOf("broker-depois") != null);
        assertThat(eventOf("broker-rejeitado").status()).isEqualTo("REJECTED");
        assertThat(eventOf("broker-rejeitado").rejectionReason()).contains("curso-inexistente");
    }
}
