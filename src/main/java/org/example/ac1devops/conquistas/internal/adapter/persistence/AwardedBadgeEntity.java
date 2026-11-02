package org.example.ac1devops.conquistas.internal.adapter.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

/**
 * Badge concedido a um aluno. {@code student_id} nao tem chave estrangeira para {@code students}:
 * a tabela pertence a outro modulo (ADR-001). A restricao unica impede concessao duplicada quando
 * duas requisicoes concorrentes avaliam o mesmo aluno.
 */
@Entity
@Table(name = "student_badges", uniqueConstraints = @UniqueConstraint(
        name = "uk_student_badges_student_code", columnNames = {"student_id", "code"}))
public class AwardedBadgeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "student_id", nullable = false)
    private Long studentId;

    @Column(name = "code", nullable = false, length = 50)
    private String code;

    @Column(name = "description", nullable = false)
    private String description;

    protected AwardedBadgeEntity() {
    }

    public AwardedBadgeEntity(Long studentId, String code, String description) {
        this.studentId = studentId;
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
