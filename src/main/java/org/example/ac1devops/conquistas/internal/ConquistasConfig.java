package org.example.ac1devops.conquistas.internal;

import org.example.ac1devops.conquistas.internal.domain.AwardedBadgePort;
import org.example.ac1devops.conquistas.internal.domain.BadgeAwardService;
import org.example.ac1devops.conquistas.internal.domain.BadgeRules;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Monta o dominio, que nao tem anotacoes do Spring, com o adapter da porta presente no contexto. */
@Configuration
public class ConquistasConfig {

    @Bean
    BadgeAwardService badgeAwardService(AwardedBadgePort awardedBadges) {
        return new BadgeAwardService(awardedBadges, BadgeRules.defaults());
    }
}
