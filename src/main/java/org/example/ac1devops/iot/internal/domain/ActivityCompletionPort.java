package org.example.ac1devops.iot.internal.domain;

/** Porta para o fluxo de conclusao da aplicacao. Precisa ser idempotente por aluno e atividade. */
public interface ActivityCompletionPort {

    /**
     * @throws UnknownActivityReferenceException se o aluno ou a atividade nao existirem
     */
    void complete(String activityId, Long studentId);
}
