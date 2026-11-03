package org.example.ac1devops.cursos.internal;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;
import org.example.ac1devops.cursos.internal.persistence.OutboxEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    @Mock
    private OutboxEventRepository outbox;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    /** Outra thread ou replica reservou o evento antes: entregar de novo geraria envio duplicado. */
    @Test
    void naoDeveEntregarEventoReservadoPorOutraThread() {
        when(outbox.findDueIds(any(), any())).thenReturn(List.of(7L));
        when(outbox.claim(eq(7L), any(), any())).thenReturn(0);
        OutboxRelay relay = new OutboxRelay(outbox, eventPublisher,
                new OutboxProperties(3, Duration.ofMillis(50), Duration.ofSeconds(60)));

        relay.sweep();

        verify(eventPublisher, never()).publishEvent(any(Object.class));
        verify(outbox, never()).markDelivered(any());
    }
}
