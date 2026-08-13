package com.zhs.basket.support.v4.formation;

import com.zhs.basket.support.v4.roster.GameModeRosterRule;
import com.zhs.common.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Component
public class BalancedRandomFormationStrategy implements FormationStrategy {

    public static final String TYPE = "BALANCED_RANDOM";

    @Override
    public String type() {
        return TYPE;
    }

    @Override
    public FormationPlan plan(FormationContext context) {
        if (context == null || context.getRosterRule() == null) {
            throw new ServiceException("FormationContext \u4e0d\u5b8c\u6574");
        }
        GameModeRosterRule rule = context.getRosterRule();
        List<FormationPlayer> players = context.getValidPlayers() == null
                ? List.of() : new ArrayList<>(context.getValidPlayers());
        int n = players.size();
        int minTeamSize = rule.getMinTeamSize();
        int maxTeamSize = rule.getMaxTeamSize();
        rule.assertValidPersonCount(n);

        int teamCount = n / minTeamSize;
        if (teamCount < 2) {
            throw new ServiceException(rule.getGameMode().getDescription() + " \u81f3\u5c11\u9700\u8981 "
                    + rule.getMinimumValidPersonCount() + " \u540d\u6709\u6548\u4e2a\u4eba\u624d\u80fd\u5f62\u6210 2 \u652f\u5408\u6cd5\u7403\u961f");
        }
        int maxPerTeam = (n + teamCount - 1) / teamCount;
        if (maxPerTeam > maxTeamSize) {
            throw new ServiceException("\u5f53\u524d\u6709\u6548\u4eba\u6570\u65e0\u6cd5\u5728\u6bcf\u961f "
                    + minTeamSize + "-" + maxTeamSize + " \u4eba\u9650\u5236\u4e0b\u5206\u914d");
        }

        long seed = context.getRandomSeed() != null ? context.getRandomSeed() : System.nanoTime();
        Collections.shuffle(players, new Random(seed));

        int base = n / teamCount;
        int remainder = n % teamCount;
        FormationPlan plan = new FormationPlan();
        plan.setRandomSeed(seed);
        plan.setStrategyType(TYPE);
        int cursor = 0;
        for (int i = 0; i < teamCount; i++) {
            int size = base + (i < remainder ? 1 : 0);
            FormationTeamPlan team = new FormationTeamPlan();
            team.setTeamIndex(i + 1);
            team.setDefaultTeamCode(String.format("T%02d", i + 1));
            team.setDefaultTeamName(String.format("\u4e34\u65f6\u961f\u4f0d %02d", i + 1));
            team.setPlayers(new ArrayList<>(players.subList(cursor, cursor + size)));
            plan.getTeams().add(team);
            cursor += size;
        }
        return plan;
    }
}
