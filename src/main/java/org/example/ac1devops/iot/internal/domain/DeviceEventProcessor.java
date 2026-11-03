package org.example.ac1devops.iot.internal.domain;

/**
 * Regra de processamento dos eventos, sem Spring nem MQTT. A conclusao vem antes do registro do
 * {@code eventId}: se o processo cair entre os dois, a reentrega conclui de novo sem efeito, porque a
 * conclusao e idempotente. Na ordem contraria, a reentrega seria descartada como duplicada e a conclusao
 * se perderia. Outras excecoes sobem de proposito: o evento nao fica registrado e pode ser reentregue.
 */
public class DeviceEventProcessor {

    private final DeviceEventStore store;
    private final ActivityCompletionPort activityCompletion;

    public DeviceEventProcessor(DeviceEventStore store, ActivityCompletionPort activityCompletion) {
        this.store = store;
        this.activityCompletion = activityCompletion;
    }

    public ProcessingResult process(DeviceEvent event) {
        if (store.isKnown(event.eventId())) {
            return duplicate(event);
        }
        ProcessingResult result = ProcessingResult.ACCEPTED;
        String rejectionReason = null;
        if (event.type() == EventType.CONCLUSAO) {
            try {
                activityCompletion.complete(event.activityId(), event.studentId());
            } catch (UnknownActivityReferenceException unknown) {
                result = ProcessingResult.REJECTED;
                rejectionReason = unknown.getMessage();
            }
        }
        return store.save(event, result, rejectionReason) ? result : duplicate(event);
    }

    private ProcessingResult duplicate(DeviceEvent event) {
        store.countDuplicate(event.eventId());
        return ProcessingResult.DUPLICATE;
    }
}
