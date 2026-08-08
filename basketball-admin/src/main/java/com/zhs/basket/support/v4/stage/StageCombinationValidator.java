package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * @deprecated R1-2 起由 {@link LinearStageChainValidator} 承担线性链校验，本类仅保留兼容委托。
 */
@Deprecated
@Component
public class StageCombinationValidator {

    private final LinearStageChainValidator delegate = new LinearStageChainValidator();

    public void validate(List<BasketDivisionStage> stages) {
        delegate.validate(stages);
    }
}
