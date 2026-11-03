package org.example.ac1devops.progression.api;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Fachada publica do modulo de progressao (alunos, XP e niveis). E o unico ponto de entrada do
 * modulo para o restante do sistema: controllers e outros modulos (ranking; conquistas no futuro) dependem
 * desta interface e dos DTOs deste pacote, nunca de {@code progression.internal}. A regra e
 * verificada pelo {@code ModularArchitectureTest} (ArchUnit) em todo build.
 */
public interface StudentProgressionApi {

    StudentResponseDTO createStudent(CreateStudentRequestDTO request);

    /**
     * Concede XP ao aluno, registrando um evento de level-up para cada faixa atravessada.
     *
     * @throws StudentNotFoundException se nao existir aluno com o {@code id} informado
     * @throws StudentUpdateConflictException se as tentativas acabarem por disputa com outras
     *         requisicoes; nesse caso nenhum XP foi aplicado
     */
    StudentResponseDTO receiveXp(Long id, ReceiveXpRequestDTO request);

    /**
     * Concede XP dentro da transacao de quem chama, para outro modulo gravar seus dados de forma
     * atomica com a recompensa. Nao repete em caso de conflito: um conflito invalida a transacao
     * inteira, e quem a abriu decide se repete. O evento de pontuacao so sai depois do commit.
     *
     * @throws StudentNotFoundException se nao existir aluno com o {@code id} informado
     */
    StudentResponseDTO grantXpInCurrentTransaction(Long id, ReceiveXpRequestDTO request);

    /**
     * @throws StudentNotFoundException se nao existir aluno com o {@code id} informado
     */
    StudentResponseDTO getStudent(Long id);

    List<StudentResponseDTO> listStudents();

    /**
     * Pagina de alunos ordenados por XP total (maior primeiro; empate pelo id). Ordenacao e
     * paginacao sao feitas no banco; a ordenacao do {@code pageable} e ignorada.
     */
    Page<StudentScoreDTO> listByScore(Pageable pageable);

    /**
     * @throws StudentNotFoundException se nao existir aluno com o {@code id} informado
     */
    StudentScoreDTO getScore(Long id);

    /** Quantos alunos ficam a frente de {@code score} na ordenacao de {@link #listByScore}. */
    long countStudentsAhead(StudentScoreDTO score);
}
