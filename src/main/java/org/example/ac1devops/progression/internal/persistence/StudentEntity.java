package org.example.ac1devops.progression.internal.persistence;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.util.ArrayList;
import java.util.List;
import org.example.ac1devops.progression.api.Level;

/**
 * Representacao JPA de um {@link org.example.ac1devops.progression.internal.domain.Student}. Guarda o estado final
 * (xpTotal, level, historico) calculado pelo dominio; a regra de progressao de nivel continua
 * vivendo exclusivamente em {@link org.example.ac1devops.progression.internal.domain.Student}.
 *
 * <p>Nomes de tabela e colunas sao explicitos para que o schema gerado no Postgres nao dependa
 * da estrategia de naming padrao do Hibernate.
 *
 * <p>O indice {@code idx_students_xp_total} segue a mesma ordem da consulta do ranking
 * ({@code xp_total DESC, id ASC}), entao o banco devolve uma pagina sem ordenar a tabela inteira.
 *
 * <p>{@code version} faz o bloqueio otimista: uma gravacao baseada em estado desatualizado falha em
 * vez de sobrescrever o XP de outra requisicao. O {@code default 0} permite que o
 * {@code ddl-auto=update} adicione a coluna em uma tabela que ja tem linhas.
 */
@Entity
@Table(name = "students",
        indexes = @Index(name = "idx_students_xp_total", columnList = "xp_total DESC, id ASC"))
public class StudentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Version
    @Column(name = "version", nullable = false, columnDefinition = "bigint default 0 not null")
    private long version;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "xp_total", nullable = false)
    private int xpTotal;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 20)
    private Level level;

    @OneToMany(mappedBy = "student", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LevelUpEventEntity> levelUpEvents = new ArrayList<>();

    protected StudentEntity() {
    }

    public StudentEntity(String name) {
        this.name = name;
        this.xpTotal = 0;
        this.level = Level.BRONZE;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getXpTotal() {
        return xpTotal;
    }

    public void setXpTotal(int xpTotal) {
        this.xpTotal = xpTotal;
    }

    public Level getLevel() {
        return level;
    }

    public void setLevel(Level level) {
        this.level = level;
    }

    public List<LevelUpEventEntity> getLevelUpEvents() {
        return levelUpEvents;
    }

    public void addLevelUpEvent(LevelUpEventEntity event) {
        event.setStudent(this);
        levelUpEvents.add(event);
    }
}
