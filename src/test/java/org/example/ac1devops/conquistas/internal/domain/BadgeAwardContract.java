package org.example.ac1devops.conquistas.internal.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import org.example.ac1devops.progression.api.Level;
import org.junit.jupiter.api.Test;

/**
 * Cenarios da regra de concessao, escritos so contra a porta. As subclasses so escolhem o adapter:
 * se todos passam nas duas, trocar o adapter nao muda a regra (hipotese do ADR-002).
 */
abstract class BadgeAwardContract {

    private static final Long STUDENT = 1L;

    protected abstract AwardedBadgePort port();

    private BadgeAwardService service() {
        return new BadgeAwardService(port(), BadgeRules.defaults());
    }

    private static StudentProgress progress(Long studentId, int xp) {
        return new StudentProgress(studentId, xp, Level.fromXp(xp));
    }

    private static List<String> codes(List<Badge> badges) {
        return badges.stream().map(Badge::code).toList();
    }

    @Test
    void naoDeveConcederBadgeSemXp() {
        assertThat(service().evaluate(progress(STUDENT, 0))).isEmpty();
    }

    @Test
    void deveConcederPrimeirosPassosNoPrimeiroXp() {
        assertThat(codes(service().evaluate(progress(STUDENT, 1)))).containsExactly("PRIMEIROS_PASSOS");
    }

    @Test
    void deveRespeitarOLimiteExatoDasFaixas() {
        assertThat(codes(service().evaluate(progress(STUDENT, 99)))).containsExactly("PRIMEIROS_PASSOS");
        assertThat(codes(service().evaluate(progress(STUDENT, 100)))).containsExactly("NIVEL_PRATA");
        assertThat(codes(service().evaluate(progress(STUDENT, 999))))
                .containsExactly("NIVEL_OURO", "NIVEL_DIAMANTE");
        assertThat(codes(service().evaluate(progress(STUDENT, 1000)))).containsExactly("MIL_XP");
    }

    @Test
    void deveConcederTodosOsBadgesDevidosDeUmaVez() {
        assertThat(codes(service().evaluate(progress(STUDENT, 1000)))).containsExactly(
                "PRIMEIROS_PASSOS", "NIVEL_PRATA", "NIVEL_OURO", "NIVEL_DIAMANTE", "MIL_XP");
    }

    @Test
    void naoDeveConcederDuasVezesOMesmoBadge() {
        BadgeAwardService service = service();
        service.evaluate(progress(STUDENT, 150));

        assertThat(service.evaluate(progress(STUDENT, 150))).isEmpty();
        assertThat(codes(service.badgesOf(STUDENT))).containsExactly("PRIMEIROS_PASSOS", "NIVEL_PRATA");
    }

    @Test
    void naoDeveMisturarBadgesDeAlunosDiferentes() {
        BadgeAwardService service = service();
        service.evaluate(progress(STUDENT, 150));

        assertThat(service.badgesOf(2L)).isEmpty();
        assertThat(codes(service.evaluate(progress(2L, 1)))).containsExactly("PRIMEIROS_PASSOS");
    }

    /** Modificabilidade: uma regra nova entra sem nenhuma mudanca na porta ou nos adapters. */
    @Test
    void deveAplicarRegraNovaSemAlterarOAdapter() {
        List<BadgeRule> rules = new ArrayList<>(BadgeRules.defaults());
        rules.add(new BadgeRule(new Badge("XP_PAR", "XP total par"), p -> p.xpTotal() % 2 == 0));
        BadgeAwardService service = new BadgeAwardService(port(), rules);

        assertThat(codes(service.evaluate(progress(STUDENT, 2)))).containsExactly("PRIMEIROS_PASSOS", "XP_PAR");
    }

    @Test
    void deveManterBadgeConcedidoQuandoARegraDeixaDeExistir() {
        Badge retired = new Badge("TEMPORADA_1", "Participou da temporada 1");
        new BadgeAwardService(port(), List.of(new BadgeRule(retired, p -> true))).evaluate(progress(STUDENT, 1));

        BadgeAwardService service = service();
        service.evaluate(progress(STUDENT, 1));

        assertThat(service.badgesOf(STUDENT)).containsExactly(
                retired, new Badge("PRIMEIROS_PASSOS", "Recebeu o primeiro XP"));
    }

    @Test
    void portaDeveRecusarBadgeDuplicado() {
        Badge badge = new Badge("DUPLICADO", "Badge de teste");
        port().award(STUDENT, badge);

        assertThatThrownBy(() -> port().award(STUDENT, badge)).isInstanceOf(RuntimeException.class);
    }
}
