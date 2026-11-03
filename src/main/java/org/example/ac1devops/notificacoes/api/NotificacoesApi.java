package org.example.ac1devops.notificacoes.api;

import java.util.List;

/**
 * Fachada publica do modulo de notificacoes (RF05 do Desafio 4). As notificacoes sao enviadas em
 * reacao a eventos; esta interface so expoe a consulta do que ja foi enviado.
 */
public interface NotificacoesApi {

    /**
     * @throws org.example.ac1devops.progression.api.StudentNotFoundException se o aluno nao existir
     */
    List<NotificationDTO> listByStudent(Long studentId);
}
