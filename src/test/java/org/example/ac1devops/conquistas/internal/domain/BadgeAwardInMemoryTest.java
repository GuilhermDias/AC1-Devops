package org.example.ac1devops.conquistas.internal.domain;

/** Regra de concessao com o adapter em memoria: nenhum contexto Spring, nenhum banco. */
class BadgeAwardInMemoryTest extends BadgeAwardContract {

    private final AwardedBadgePort port = new InMemoryAwardedBadgeAdapter();

    @Override
    protected AwardedBadgePort port() {
        return port;
    }
}
