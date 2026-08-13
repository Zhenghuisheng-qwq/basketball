package com.zhs.basket.service.v4;

import com.zhs.basket.domain.v4.BasketDivisionFormatSnapshot;
import com.zhs.basket.support.v4.version.RuleVersionView;

/**
 * Rule Version 统一读取。底层仍是 FormatSnapshot。
 */
public interface IBasketV4RuleVersionService {
    RuleVersionView getCurrent(Long divisionId);

    RuleVersionView getById(Long ruleVersionId);

    RuleVersionView fromSnapshot(BasketDivisionFormatSnapshot snapshot);
}
