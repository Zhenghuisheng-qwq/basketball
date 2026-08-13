package com.zhs.basket.support.v4.bracket;

import com.zhs.basket.enums.v4.ByeRuleEnum;
import org.springframework.stereotype.Component;

@Component
public class ByeAllocationStrategyFactory {

    private final SeedPriorityByeStrategy seedPriority = new SeedPriorityByeStrategy();
    private final RandomByeStrategy random = new RandomByeStrategy();
    private final ManualByeStrategy manual = new ManualByeStrategy();

    public ByeAllocationStrategy resolve(String byeRule) {
        ByeRuleEnum rule = byeRule == null || byeRule.isEmpty()
                ? ByeRuleEnum.SEED_PRIORITY : ByeRuleEnum.fromCode(byeRule);
        return switch (rule) {
            case SEED_PRIORITY -> seedPriority;
            case RANDOM -> random;
            case MANUAL -> manual;
        };
    }
}
