package org.example.ac1devops.progression.internal.persistence;

import java.util.Optional;
import org.example.ac1devops.progression.api.StudentScoreDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentRepository extends JpaRepository<StudentEntity, Long> {

    /**
     * Alunos ordenados por XP (maior primeiro; empate pelo id, quem chegou antes fica na frente).
     * Ordenacao e paginacao acontecem no banco, apoiadas pelo indice {@code idx_students_xp_total};
     * a projecao nao carrega {@code levelUpEvents}, evitando N+1.
     */
    @Query(value = "select new org.example.ac1devops.progression.api.StudentScoreDTO("
            + "s.id, s.name, s.xpTotal, s.level) "
            + "from StudentEntity s order by s.xpTotal desc, s.id asc",
            countQuery = "select count(s) from StudentEntity s")
    Page<StudentScoreDTO> findScoresOrderedByXp(Pageable pageable);

    @Query("select new org.example.ac1devops.progression.api.StudentScoreDTO("
            + "s.id, s.name, s.xpTotal, s.level) from StudentEntity s where s.id = :id")
    Optional<StudentScoreDTO> findScoreById(@Param("id") Long id);

    /** Quantos alunos ficam a frente na mesma ordenacao de {@link #findScoresOrderedByXp}. */
    @Query("select count(s) from StudentEntity s "
            + "where s.xpTotal > :xpTotal or (s.xpTotal = :xpTotal and s.id < :id)")
    long countAhead(@Param("xpTotal") int xpTotal, @Param("id") Long id);
}
