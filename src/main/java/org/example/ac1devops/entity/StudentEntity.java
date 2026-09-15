package org.example.ac1devops.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import java.util.ArrayList;
import java.util.List;
import org.example.ac1devops.domain.Level;

/**
 * Representacao JPA de um {@link org.example.ac1devops.domain.Student}. Guarda o estado final
 * (xpTotal, level, historico) calculado pelo dominio; a regra de progressao de nivel continua
 * vivendo exclusivamente em {@link org.example.ac1devops.domain.Student}.
 */
@Entity
public class StudentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private int xpTotal;

    @Enumerated(EnumType.STRING)
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
