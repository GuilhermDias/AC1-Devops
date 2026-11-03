package org.example.ac1devops.iot.internal.adapter.mqtt;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Configuracao do gateway MQTT. A variavel de ambiente {@code MQTT_URL} define o endereco do broker. */
@ConfigurationProperties("mqtt")
public record MqttProperties(
        @DefaultValue("tcp://localhost:1883") String url,
        @DefaultValue("core-gateway") String clientId,
        @DefaultValue("labs/+/devices/+/events") String topic,
        @DefaultValue("2s") Duration connectTimeout) {
}
