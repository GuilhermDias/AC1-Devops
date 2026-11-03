package org.example.ac1devops.iot.api;

import java.util.List;

/**
 * Fachada publica do modulo de IoT (RF06 do Desafio 5). Os eventos entram pelo broker MQTT; esta
 * interface so expoe a consulta do que foi recebido.
 */
public interface IotApi {

    /** Os eventos recebidos mais recentes, com o resultado do processamento. */
    List<DeviceEventDTO> listEvents();
}
