package org.example.ac1devops.recomendacao.internal.domain;

import org.example.ac1devops.progression.api.Level;

/** Dados do aluno enviados para gerar a recomendacao. */
public record StudentProfile(Long studentId, Level level) {
}
