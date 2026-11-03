package org.example.ac1devops.iot.internal;

import org.example.ac1devops.iot.internal.domain.ActivityCompletionPort;
import org.example.ac1devops.iot.internal.domain.DeviceEventProcessor;
import org.example.ac1devops.iot.internal.domain.DeviceEventStore;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Monta o dominio, que nao tem anotacoes do Spring, com os adapters das portas presentes no contexto. */
@Configuration
public class IotConfig {

    @Bean
    DeviceEventProcessor deviceEventProcessor(DeviceEventStore store, ActivityCompletionPort activityCompletion) {
        return new DeviceEventProcessor(store, activityCompletion);
    }
}
