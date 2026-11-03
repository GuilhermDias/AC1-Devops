package org.example.ac1devops.conquistas.api;

import java.util.List;

/**
 * Fachada publica do modulo de conquistas (RF02). A concessao acontece em reacao a eventos de
 * progressao; esta interface so expoe a consulta.
 */
public interface ConquistasApi {

    /**
     * Badges ja concedidos ao aluno, na ordem em que foram concedidos.
     *
     * @throws org.example.ac1devops.progression.api.StudentNotFoundException se o aluno nao existir
     */
    List<BadgeDTO> listBadges(Long studentId);
}
