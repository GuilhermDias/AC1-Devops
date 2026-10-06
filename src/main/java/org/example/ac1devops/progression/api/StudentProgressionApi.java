package org.example.ac1devops.progression.api;

import java.util.List;

/**
 * Fachada publica do modulo de progressao (alunos, XP e niveis). E o unico ponto de entrada do
 * modulo para o restante do sistema: controllers e futuros modulos (ranking, conquistas) dependem
 * desta interface e dos DTOs deste pacote, nunca de {@code progression.internal}. A regra e
 * verificada pelo {@code ModularArchitectureTest} (ArchUnit) em todo build.
 */
public interface StudentProgressionApi {

    StudentResponseDTO createStudent(CreateStudentRequestDTO request);

    /**
     * Concede XP ao aluno, registrando um evento de level-up para cada faixa atravessada.
     *
     * @throws StudentNotFoundException se nao existir aluno com o {@code id} informado
     */
    StudentResponseDTO receiveXp(Long id, ReceiveXpRequestDTO request);

    /**
     * @throws StudentNotFoundException se nao existir aluno com o {@code id} informado
     */
    StudentResponseDTO getStudent(Long id);

    List<StudentResponseDTO> listStudents();
}
