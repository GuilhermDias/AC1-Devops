package org.example.ac1devops.conquistas.internal.domain;

import org.example.ac1devops.conquistas.internal.adapter.persistence.AwardedBadgeRepository;
import org.example.ac1devops.conquistas.internal.adapter.persistence.JpaAwardedBadgeAdapter;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;

/**
 * Mesmos cenarios de {@link BadgeAwardContract}, agora com o adapter de producao e banco real (H2).
 *
 * <p>O {@code @Transactional} do {@code @DataJpaTest} nao se aplica a metodos herdados de uma
 * superclasse sem a anotacao: cada chamada ao adapter comita, como em producao, e por isso a tabela
 * e limpa antes de cada cenario.
 */
@DataJpaTest
@Import(JpaAwardedBadgeAdapter.class)
class BadgeAwardJpaIntegrationTest extends BadgeAwardContract {

    @Autowired
    private JpaAwardedBadgeAdapter port;

    @Autowired
    private AwardedBadgeRepository repository;

    @BeforeEach
    void cleanTable() {
        repository.deleteAll();
    }

    @Override
    protected AwardedBadgePort port() {
        return port;
    }
}
