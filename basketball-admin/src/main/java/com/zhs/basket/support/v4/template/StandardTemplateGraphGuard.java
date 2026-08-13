package com.zhs.basket.support.v4.template;

import com.zhs.basket.domain.v4.BasketDivisionFormatSnapshot;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.enums.v4.TemplateTypeEnum;
import com.zhs.common.exception.ServiceException;
import org.springframework.stereotype.Component;

@Component
public class StandardTemplateGraphGuard {

    public boolean isStandard(BasketDivisionFormatSnapshot snapshot) {
        if (snapshot == null) {
            return false;
        }
        if (TemplateTypeEnum.STANDARD.getCode().equals(snapshot.getSourceTemplateType())) {
            return true;
        }
        return StandardTemplateCodes.isStandard(snapshot.getSourceTemplateCode());
    }

    public void assertCanAddOrDeleteOrReorder(BasketDivisionFormatSnapshot snapshot) {
        if (isStandard(snapshot)) {
            throw new ServiceException("STANDARD \u6a21\u677f\u4e0d\u5141\u8bb8\u589e\u5220\u6216\u8c03\u6574\u6838\u5fc3\u9636\u6bb5\uff0c\u8bf7\u6539\u7528 CUSTOM");
        }
    }

    public void assertCanUpdateStage(BasketDivisionFormatSnapshot snapshot, BasketDivisionStage existing,
                                     String newStageCode, String newStageType, String newCompetitionFormat,
                                     String newNextStageCode) {
        if (!isStandard(snapshot) || existing == null) {
            return;
        }
        if (notSame(existing.getStageCode(), newStageCode)
                || notSame(existing.getStageType(), newStageType)
                || notSame(existing.getCompetitionFormat(), newCompetitionFormat)
                || notSame(existing.getNextStageCode(), newNextStageCode)) {
            throw new ServiceException("STANDARD \u6a21\u677f\u4e0d\u5141\u8bb8\u4fee\u6539\u9636\u6bb5\u7ed3\u6784\uff0c\u4ec5\u53ef\u8c03\u5141\u8bb8\u7684\u53c2\u6570");
        }
    }

    private static boolean notSame(String left, String right) {
        if (left == null || left.isEmpty()) {
            return right != null && !right.isEmpty();
        }
        return !left.equals(right);
    }
}
