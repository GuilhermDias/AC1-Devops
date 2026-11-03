package org.example.ac1devops.conquistas.internal;

import java.util.List;
import org.example.ac1devops.conquistas.api.BadgeDTO;
import org.example.ac1devops.conquistas.api.ConquistasApi;
import org.example.ac1devops.conquistas.internal.domain.BadgeAwardService;
import org.example.ac1devops.progression.api.StudentProgressionApi;
import org.springframework.stereotype.Service;

/** Implementacao de {@link ConquistasApi}: valida o aluno pela API de progressao e consulta o dominio. */
@Service
public class ConquistasService implements ConquistasApi {

    private final StudentProgressionApi studentProgression;
    private final BadgeAwardService badgeAwardService;

    public ConquistasService(StudentProgressionApi studentProgression, BadgeAwardService badgeAwardService) {
        this.studentProgression = studentProgression;
        this.badgeAwardService = badgeAwardService;
    }

    @Override
    public List<BadgeDTO> listBadges(Long studentId) {
        studentProgression.getScore(studentId);
        return badgeAwardService.badgesOf(studentId).stream()
                .map(badge -> new BadgeDTO(badge.code(), badge.description()))
                .toList();
    }
}
