package com.zhs.basket.support.v4.formation;

import com.zhs.basket.enums.v4.GameModeEnum;
import com.zhs.basket.support.v4.roster.GameModeRosterRule;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class BalancedRandomFormationStrategyTest {

    private final BalancedRandomFormationStrategy strategy = new BalancedRandomFormationStrategy();

    @Test
    void test3_threeVthreeFivePersonsRejected() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> strategy.plan(context(GameModeEnum.THREE_V_THREE, 5, 1L)));
        assertTrue(ex.getMessage().contains("6"));
    }

    @Test
    void test4_threeVthreeSixPersonsTwoTeams() {
        FormationPlan plan = strategy.plan(context(GameModeEnum.THREE_V_THREE, 6, 2L));
        assertEquals(List.of(3, 3), sizes(plan));
    }

    @Test
    void test5_threeVthreeSeventeenPersons() {
        FormationPlan plan = strategy.plan(context(GameModeEnum.THREE_V_THREE, 17, 3L));
        assertEquals(List.of(4, 4, 3, 3, 3), sizes(plan));
    }

    @Test
    void test6_fiveVfiveTenPersons() {
        FormationPlan plan = strategy.plan(context(GameModeEnum.FIVE_V_FIVE, 10, 4L));
        assertEquals(List.of(5, 5), sizes(plan));
    }

    @Test
    void test7_fiveVfiveFiftyThreePersons() {
        FormationPlan plan = strategy.plan(context(GameModeEnum.FIVE_V_FIVE, 53, 5L));
        assertEquals(List.of(6, 6, 6, 5, 5, 5, 5, 5, 5, 5), sizes(plan));
    }

    @Test
    void test8_eachPersonOnceNoMissing() {
        FormationPlan plan = strategy.plan(context(GameModeEnum.FIVE_V_FIVE, 53, 6L));
        List<Long> ids = plan.getTeams().stream()
                .flatMap(t -> t.getPlayers().stream())
                .map(FormationPlayer::getPersonalRegistrationId)
                .toList();
        assertEquals(53, ids.size());
        assertEquals(53, new HashSet<>(ids).size());
        Set<Long> expected = new HashSet<>();
        for (long i = 1; i <= 53; i++) {
            expected.add(i);
        }
        assertEquals(expected, new HashSet<>(ids));
        int max = plan.getTeams().stream().mapToInt(t -> t.getPlayers().size()).max().orElse(0);
        int min = plan.getTeams().stream().mapToInt(t -> t.getPlayers().size()).min().orElse(0);
        assertTrue(max - min <= 1);
    }

    @Test
    void sameSeedAndSamePlayers_reproducibleRegardlessOfInputOrder() {
        FormationContext first = context(GameModeEnum.FIVE_V_FIVE, 10, 42L);
        FormationContext second = context(GameModeEnum.FIVE_V_FIVE, 10, 42L);
        Collections.reverse(second.getValidPlayers());
        FormationPlan planA = strategy.plan(first);
        FormationPlan planB = strategy.plan(second);
        assertEquals(playerIds(planA), playerIds(planB));
    }

    private FormationContext context(GameModeEnum mode, int n, long seed) {
        FormationContext context = new FormationContext();
        context.setDivisionId(100L);
        context.setRuleVersionId(11L);
        context.setRosterRule(GameModeRosterRule.of(mode));
        context.setRandomSeed(seed);
        List<FormationPlayer> players = new ArrayList<>();
        for (int i = 1; i <= n; i++) {
            FormationPlayer p = new FormationPlayer();
            p.setPersonalRegistrationId((long) i);
            p.setPlayerId((long) i);
            p.setUserId((long) i);
            players.add(p);
        }
        context.setValidPlayers(players);
        return context;
    }

    private List<Integer> sizes(FormationPlan plan) {
        return plan.getTeams().stream().map(t -> t.getPlayers().size()).collect(Collectors.toList());
    }

    private List<Long> playerIds(FormationPlan plan) {
        return plan.getTeams().stream()
                .flatMap(t -> t.getPlayers().stream())
                .map(FormationPlayer::getPersonalRegistrationId)
                .collect(Collectors.toList());
    }
}
