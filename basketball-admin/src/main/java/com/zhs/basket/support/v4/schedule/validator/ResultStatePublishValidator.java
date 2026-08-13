package com.zhs.basket.support.v4.schedule.validator;

import com.zhs.basket.support.v4.schedule.StagePublishContext;
import com.zhs.basket.support.v4.schedule.StagePublishValidator;
import com.zhs.basket.support.v4.schedule.ValidationResult;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

@Component
@Order(80)
public class ResultStatePublishValidator implements StagePublishValidator {
    @Override
    public void validate(StagePublishContext context, ValidationResult result) {
        // Publish itself does not require zero results. Withdraw with results is guarded elsewhere.
    }
}
