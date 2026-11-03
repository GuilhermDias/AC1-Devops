package org.example.ac1devops.iot.internal.adapter.mqtt;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.net.ServerSocket;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.example.ac1devops.iot.internal.domain.DeviceEvent;
import org.example.ac1devops.iot.internal.domain.DeviceEventProcessor;
import org.example.ac1devops.iot.internal.domain.EventType;
import org.example.ac1devops.iot.internal.domain.ProcessingResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/** Tratamento das mensagens sem broker real: {@code messageArrived} e chamado diretamente. */
@ExtendWith(MockitoExtension.class)
class MqttDeviceEventGatewayTest {

    private static final String TOPIC = "labs/lab-a/devices/sensor-01/events";
    private static final String VALID = "{\"eventId\":\"e1\",\"deviceId\":\"sensor-01\",\"tipo\":\"conclusao\","
            + "\"alunoId\":7,\"atividadeId\":\"ci-cd\",\"timestamp\":\"2026-11-03T12:00:00Z\",\"extra\":true}";

    @Mock
    private DeviceEventProcessor processor;

    @Mock
    private MqttClient client;

    private MqttDeviceEventGateway gateway;

    @BeforeAll
    static void enableMetrics() {
        Metrics.addRegistry(new SimpleMeterRegistry());
    }

    private static double eventCount(String result) {
        return Metrics.counter("iot.events", "result", result).count();
    }

    @BeforeEach
    void setUp() throws Exception {
        gateway = gatewayFor("tcp://localhost:1");
    }

    private MqttDeviceEventGateway gatewayFor(String url) throws MqttException {
        return new MqttDeviceEventGateway(
                new MqttProperties(url, "teste-unitario", "labs/+/devices/+/events", Duration.ofSeconds(1)), processor);
    }

    private MqttDeviceEventGateway gatewayWithMockedClient() {
        return new MqttDeviceEventGateway(
                new MqttProperties("tcp://localhost:1", "teste-unitario", "labs/+/devices/+/events", Duration.ofSeconds(2)),
                processor, client);
    }

    private static MqttMessage message(String body) {
        return new MqttMessage(body.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void deveTransformarAMensagemEEntregarAoDominio() {
        when(processor.process(any())).thenReturn(ProcessingResult.DUPLICATE);
        double before = eventCount("DUPLICATE");

        gateway.messageArrived(TOPIC, message(VALID));

        assertThat(eventCount("DUPLICATE")).isEqualTo(before + 1);
        verify(processor).process(new DeviceEvent(
                "e1", "sensor-01", EventType.CONCLUSAO, 7L, "ci-cd", Instant.parse("2026-11-03T12:00:00Z")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
        "isto nao e json",
        "null",
        "{}",
        "{\"eventId\":\"e1\",\"deviceId\":\"sensor-01\",\"tipo\":\"conclusao\",\"alunoId\":\"abc\"}"
    })
    void deveDescartarMensagemInvalidaSemLancarExcecao(String body) {
        double before = eventCount("INVALID");

        gateway.messageArrived(TOPIC, message(body));

        assertThat(eventCount("INVALID")).isEqualTo(before + 1);
        verify(processor, never()).process(any());
    }

    @Test
    void deveDescartarDispositivoPublicandoNoTopicoDeOutro() {
        gateway.messageArrived("labs/lab-a/devices/outro-sensor/events", message(VALID));

        verify(processor, never()).process(any());
    }

    /** A excecao precisa subir: e ela que faz o Paho nao confirmar, para o broker reentregar. */
    @Test
    void falhaTemporariaDeveSubirParaNaoConfirmarAMensagem() {
        when(processor.process(any())).thenThrow(new IllegalStateException("banco fora do ar"));

        assertThatThrownBy(() -> gateway.messageArrived(TOPIC, message(VALID)))
                .isInstanceOf(IllegalStateException.class);
    }

    /** Sem sessao persistente e QoS 1, o broker nao guardaria eventos com o Core fora do ar (ASR12). */
    @Test
    void deveConectarComSessaoPersistenteEAssinarComQos1() throws Exception {
        gatewayWithMockedClient().start();

        ArgumentCaptor<MqttConnectOptions> options = ArgumentCaptor.forClass(MqttConnectOptions.class);
        verify(client).connect(options.capture());
        assertThat(options.getValue().isCleanSession()).isFalse();
        assertThat(options.getValue().getConnectionTimeout()).isEqualTo(2);
        verify(client).subscribe("labs/+/devices/+/events", 1);
    }

    @Test
    void naoDeveReconectarQuandoJaEstaConectado() throws Exception {
        when(client.isConnected()).thenReturn(true);
        MqttDeviceEventGateway connected = gatewayWithMockedClient();

        connected.start();

        assertThat(connected.isConnected()).isTrue();
        verify(client, never()).connect(any(MqttConnectOptions.class));
    }

    @Test
    void deveTentarDeNovoEAssinarQuandoOBrokerVolta() throws Exception {
        doThrow(new MqttException(MqttException.REASON_CODE_SERVER_CONNECT_ERROR))
                .doThrow(new MqttException(MqttException.REASON_CODE_SERVER_CONNECT_ERROR))
                .doNothing()
                .when(client).connect(any(MqttConnectOptions.class));
        MqttDeviceEventGateway unavailable = gatewayWithMockedClient();

        unavailable.start();
        unavailable.ensureConnected();
        verify(client, never()).subscribe("labs/+/devices/+/events", 1);

        unavailable.ensureConnected();

        verify(client, times(3)).connect(any(MqttConnectOptions.class));
        verify(client).subscribe("labs/+/devices/+/events", 1);
    }

    @Test
    void deveDesconectarAoPararSemEncerrarASessao() throws Exception {
        when(client.isConnected()).thenReturn(true);
        doNothing().when(client).disconnect();
        MqttDeviceEventGateway connected = gatewayWithMockedClient();

        connected.stop();

        verify(client).disconnect();
        assertThat(connected.isRunning()).isFalse();
    }

    /** Uma falha ao desconectar nao pode impedir o encerramento da aplicacao. */
    @Test
    void falhaAoDesconectarNaoDeveImpedirOEncerramento() throws Exception {
        when(client.isConnected()).thenReturn(true);
        doThrow(new MqttException(MqttException.REASON_CODE_CLIENT_EXCEPTION)).when(client).disconnect();
        MqttDeviceEventGateway connected = gatewayWithMockedClient();
        connected.start();

        assertThatCode(connected::stop).doesNotThrowAnyException();

        verify(client).disconnect();
        assertThat(connected.isRunning()).isFalse();
    }

    @Test
    void naoDeveImpedirOCoreDeSubirSemBroker() throws Exception {
        int closedPort;
        try (ServerSocket socket = new ServerSocket(0)) {
            closedPort = socket.getLocalPort();
        }
        MqttDeviceEventGateway withoutBroker = gatewayFor("tcp://localhost:" + closedPort);

        withoutBroker.start();
        withoutBroker.ensureConnected();

        assertThat(withoutBroker.isRunning()).isTrue();
        assertThat(withoutBroker.isConnected()).isFalse();

        withoutBroker.stop();
        withoutBroker.ensureConnected();
        withoutBroker.connectionLost(new IllegalStateException("queda"));
        withoutBroker.deliveryComplete(null);
        assertThat(withoutBroker.isRunning()).isFalse();
    }
}
