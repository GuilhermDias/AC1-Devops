package org.example.ac1devops.iot.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Regra de processamento com portas falsas: sem broker, sem Spring e sem banco. */
class DeviceEventProcessorTest {

    private final FakeStore store = new FakeStore();
    private final List<String> completions = new ArrayList<>();
    private RuntimeException completionFailure;

    private final DeviceEventProcessor processor = new DeviceEventProcessor(store, (activityId, studentId) -> {
        if (completionFailure != null) {
            throw completionFailure;
        }
        completions.add(activityId + "/" + studentId);
    });

    private static DeviceEvent event(EventType type) {
        return new DeviceEvent("e1", "lab-01", type, 7L, "ci-cd", Instant.parse("2026-11-03T12:00:00Z"));
    }

    @Test
    void conclusaoDeveAcionarOFluxoDaAplicacaoComOsDadosTransformados() {
        assertThat(processor.process(event(EventType.CONCLUSAO))).isEqualTo(ProcessingResult.ACCEPTED);

        assertThat(completions).containsExactly("ci-cd/7");
        assertThat(store.saved).containsEntry("e1", ProcessingResult.ACCEPTED);
    }

    @Test
    void presencaEInicioDevemSoSerRegistrados() {
        assertThat(processor.process(event(EventType.PRESENCA))).isEqualTo(ProcessingResult.ACCEPTED);

        assertThat(completions).isEmpty();
        assertThat(store.saved).containsKey("e1");
    }

    @Test
    void deveDescartarEventoRepetidoSemAcionarOFluxoDeNovo() {
        processor.process(event(EventType.CONCLUSAO));

        assertThat(processor.process(event(EventType.CONCLUSAO))).isEqualTo(ProcessingResult.DUPLICATE);

        assertThat(completions).hasSize(1);
        assertThat(store.duplicates).containsExactly("e1");
    }

    @Test
    void deveTratarComoDuplicadoQuandoOutraEntregaRegistraPrimeiro() {
        store.rejectSave = true;

        assertThat(processor.process(event(EventType.PRESENCA))).isEqualTo(ProcessingResult.DUPLICATE);

        assertThat(store.duplicates).containsExactly("e1");
    }

    @Test
    void referenciaDesconhecidaDeveFicarRegistradaComoRejeitada() {
        completionFailure = new UnknownActivityReferenceException("Curso nao encontrado: x");

        assertThat(processor.process(event(EventType.CONCLUSAO))).isEqualTo(ProcessingResult.REJECTED);

        assertThat(store.saved).containsEntry("e1", ProcessingResult.REJECTED);
        assertThat(store.reasons).containsEntry("e1", "Curso nao encontrado: x");
    }

    /** Sem registrar o eventId, a reentrega do broker ainda consegue concluir: nada se perde. */
    @Test
    void falhaTemporariaNaoDeveRegistrarOEventoParaPermitirAReentrega() {
        completionFailure = new IllegalStateException("banco fora do ar");

        assertThatThrownBy(() -> processor.process(event(EventType.CONCLUSAO)))
                .isInstanceOf(IllegalStateException.class);
        assertThat(store.saved).isEmpty();

        completionFailure = null;
        assertThat(processor.process(event(EventType.CONCLUSAO))).isEqualTo(ProcessingResult.ACCEPTED);
        assertThat(completions).containsExactly("ci-cd/7");
    }

    private static final class FakeStore implements DeviceEventStore {

        private final Map<String, ProcessingResult> saved = new HashMap<>();
        private final Map<String, String> reasons = new HashMap<>();
        private final List<String> duplicates = new ArrayList<>();
        private boolean rejectSave;

        @Override
        public boolean isKnown(String eventId) {
            return saved.containsKey(eventId);
        }

        @Override
        public boolean save(DeviceEvent event, ProcessingResult status, String rejectionReason) {
            if (rejectSave) {
                return false;
            }
            saved.put(event.eventId(), status);
            reasons.put(event.eventId(), rejectionReason);
            return true;
        }

        @Override
        public void countDuplicate(String eventId) {
            duplicates.add(eventId);
        }

        @Override
        public List<StoredDeviceEvent> recent() {
            return List.of();
        }
    }
}
