package com.zhs.basket.support.v4.bracket;

import com.zhs.basket.enums.v4.DrawRuleEnum;
import com.zhs.basket.enums.v4.PairingRuleEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class BracketSlotAllocationStrategyFactory {

    @Autowired
    private SeededHighVsLowSlotStrategy seededHighVsLowSlotStrategy;
    @Autowired
    private ConfirmedSlotStrategy confirmedSlotStrategy;
    @Autowired
    private RandomDrawSlotStrategy randomDrawSlotStrategy;

    public BracketSlotAllocationStrategy resolve(String pairingRule, String drawRule) {
        if (DrawRuleEnum.RANDOM_CONFIRM.getCode().equals(drawRule)
                || DrawRuleEnum.MANUAL_SLOT.getCode().equals(drawRule)
                || PairingRuleEnum.MANUAL.getCode().equals(pairingRule)) {
            return confirmedSlotStrategy;
        }
        if (PairingRuleEnum.RANDOM_DRAW.getCode().equals(pairingRule)) {
            return randomDrawSlotStrategy;
        }
        return seededHighVsLowSlotStrategy;
    }
}
