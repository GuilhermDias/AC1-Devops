package org.example.ac1devops.iot.internal.adapter.mqtt;

import io.micrometer.core.instrument.Metrics;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.persist.MemoryPersistence;
import org.example.ac1devops.iot.internal.domain.DeviceEvent;
import org.example.ac1devops.iot.internal.domain.DeviceEventProcessor;
import org.example.ac1devops.iot.internal.domain.InvalidDeviceEventException;
import org.example.ac1devops.iot.internal.domain.ProcessingResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.SmartLifecycle;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Gateway entre o broker MQTT e o dominio: um unico assinante, com sessao persistente e QoS 1, para o
 * broker guardar o que chegar enquanto o Core estiver fora. O Core sobe mesmo sem o broker: a conexao e
 * tentada periodicamente.
 *
 * <p>Evento invalido e registrado no log e confirmado, porque reentregar nao adiantaria. Falha temporaria
 * sobe de {@link #messageArrived}: o Paho derruba a conexao sem confirmar, e o broker reentrega na reconexao.
 */
@Component
@ConditionalOnProperty(name = "mqtt.enabled", havingValue = "true", matchIfMissing = true)
public class MqttDeviceEventGateway implements MqttCallback, SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(MqttDeviceEventGateway.class);
    private static final int QOS_AT_LEAST_ONCE = 1;

    private final MqttProperties properties;
    private final DeviceEventProcessor processor;
    private final MqttClient client;
    private boolean running;
    private boolean brokerAvailable = true;

    @Autowired
    public MqttDeviceEventGateway(MqttProperties properties, DeviceEventProcessor processor) throws MqttException {
        this(properties, processor, new MqttClient(properties.url(), properties.clientId(), new MemoryPersistence()));
    }

    MqttDeviceEventGateway(MqttProperties properties, DeviceEventProcessor processor, MqttClient client) {
        this.properties = properties;
        this.processor = processor;
        this.client = client;
        this.client.setCallback(this);
    }

    @Override
    public synchronized void start() {
        running = true;
        ensureConnected();
    }

    @Override
    public synchronized void stop() {
        running = false;
        try {
            if (client.isConnected()) {
                client.disconnect();
            }
        } catch (MqttException failure) {
            log.warn("Falha ao desconectar do broker MQTT: {}", failure.getMessage());
        }
    }

    @Override
    public synchronized boolean isRunning() {
        return running;
    }

    public boolean isConnected() {
        return client.isConnected();
    }

    @Scheduled(fixedDelayString = "${mqtt.reconnect-interval-ms:5000}")
    public synchronized void ensureConnected() {
        if (running && !client.isConnected()) {
            connect();
        }
    }

    private void connect() {
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(false);
        options.setConnectionTimeout((int) properties.connectTimeout().toSeconds());
        try {
            client.connect(options);
            client.subscribe(properties.topic(), QOS_AT_LEAST_ONCE);
            brokerAvailable = true;
            log.info("Conectado ao broker MQTT em {}, topico {}", properties.url(), properties.topic());
        } catch (MqttException unavailable) {
            if (brokerAvailable) {
                log.warn("Broker MQTT indisponivel em {}: nova tentativa em instantes", properties.url());
            }
            brokerAvailable = false;
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) {
        DeviceEvent event;
        try {
            event = DeviceEventParser.parse(topic, message.getPayload());
        } catch (InvalidDeviceEventException invalid) {
            log.warn("Evento IoT invalido descartado do topico {}: {}", topic, invalid.getMessage());
            countEvent("INVALID");
            return;
        }
        ProcessingResult result = processor.process(event);
        log.info("Evento IoT {} de {} ({}): {}", event.eventId(), event.deviceId(), event.type(), result);
        countEvent(result.name());
    }

    /** {@code iot_events_total{result}}: aceitos, duplicados, rejeitados e invalidos (ASR12 e ASR13). */
    private static void countEvent(String result) {
        Metrics.counter("iot.events", "result", result).increment();
    }

    @Override
    public void connectionLost(Throwable cause) {
        log.warn("Conexao com o broker MQTT perdida: {}", cause.getMessage());
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        // O gateway so assina: nunca publica.
    }
}
