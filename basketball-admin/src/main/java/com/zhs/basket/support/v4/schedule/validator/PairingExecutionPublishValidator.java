package com.zhs.basket.support.v4.schedule.validator;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketStagePairingExecution;
import com.zhs.basket.enums.v4.PairingExecutionStatusEnum;
import com.zhs.basket.support.v4.schedule.StagePublishContext;
import com.zhs.basket.support.v4.schedule.StagePublishValidator;
import com.zhs.basket.support.v4.schedule.ValidationIssue;
import com.zhs.basket.support.v4.schedule.ValidationResult;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(50)
public class PairingExecutionPublishValidator implements StagePublishValidator {
    @Override
    public void validate(StagePublishContext context, ValidationResult result) {
        BasketDivisionStage stage = context.getStage();
        if (stage == null || !requiresConfirmedPairing(stage)) {
            return;
        }
        BasketStagePairingExecution pairing = context.getPairingExecution();
        if (pairing == null || !PairingExecutionStatusEnum.CONFIRMED.getCode().equals(pairing.getStatus())) {
            result.add(ValidationIssue.error("PAIRING_NOT_CONFIRMED", "抽签/签位尚未确认，不能发布"));
        }
    }

    static boolean requiresConfirmedPairing(BasketDivisionStage stage) {
        String draw = stage.getDrawRule();
        String pairing = stage.getPairingRule();
        return "RANDOM_CONFIRM".equals(draw)
                || "MANUAL_SLOT".equals(draw)
                || "GROUP_CROSS".equals(pairing)
                || "MANUAL_SLOT".equals(pairing)
                || "RANDOM_DRAW".equals(pairing);
    }
}
