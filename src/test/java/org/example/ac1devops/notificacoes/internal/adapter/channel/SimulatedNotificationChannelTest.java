package org.example.ac1devops.notificacoes.internal.adapter.channel;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.example.ac1devops.notificacoes.internal.domain.Notification;
import org.junit.jupiter.api.Test;

class SimulatedNotificationChannelTest {

    private static final Notification NOTIFICATION = Notification.courseCompleted(1L, 2L, "ci-cd", 150);

    @Test
    void deveEnviarQuandoDisponivel() {
        assertThatCode(() -> new SimulatedNotificationChannel(true).send(NOTIFICATION)).doesNotThrowAnyException();
    }

    @Test
    void deveFalharQuandoIndisponivel() {
        assertThatThrownBy(() -> new SimulatedNotificationChannel(false).send(NOTIFICATION))
                .isInstanceOf(IllegalStateException.class);
    }
}
