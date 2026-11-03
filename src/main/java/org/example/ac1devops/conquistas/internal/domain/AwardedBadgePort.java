package org.example.ac1devops.conquistas.internal.domain;

import java.util.List;

/**
 * Porta de persistencia dos badges concedidos. Pertence ao dominio e fala so a linguagem dele; a
 * dependencia e invertida: os adapters conhecem esta interface, nunca o contrario.
 */
public interface AwardedBadgePort {

    /** Badges do aluno, na ordem em que foram concedidos. */
    List<Badge> findByStudent(Long studentId);

    /**
     * Registra a concessao.
     *
     * @throws RuntimeException se o aluno ja tiver um badge com o mesmo codigo
     */
    void award(Long studentId, Badge badge);
}
