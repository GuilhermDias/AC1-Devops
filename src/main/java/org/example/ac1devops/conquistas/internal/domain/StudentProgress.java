package org.example.ac1devops.conquistas.internal.domain;

import org.example.ac1devops.progression.api.Level;

/**
 * Estado do aluno usado para avaliar as regras. {@link Level} e o unico tipo externo do dominio:
 * e um enum puro do contrato de progressao, e o reuso evita duplicar a tabela de faixas de XP.
 */
public record StudentProgress(Long studentId, int xpTotal, Level level) {
}
