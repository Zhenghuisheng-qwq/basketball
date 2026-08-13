package com.zhs.basket.support.v4.version;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionRosterVersion;
import com.zhs.basket.enums.v4.RegistrationModeEnum;
import com.zhs.basket.enums.v4.RosterWorkflowModeEnum;
import com.zhs.common.exception.ServiceException;

public final class RosterWorkflowSupport {

    public static final String PERSONAL_ROSTER_REQUIRED_MESSAGE =
            "\u8bf7\u5148\u5b8c\u6210\u5e76\u9501\u5b9a\u6700\u7ec8\u4e34\u65f6\u7403\u961f\u540d\u5355";
    public static final String TEAM_ROSTER_REQUIRED_MESSAGE =
            "\u8bf7\u5148\u5b8c\u6210\u5e76\u9501\u5b9a\u6700\u7ec8\u53c2\u8d5b\u540d\u5355";

    private RosterWorkflowSupport() {
    }

    public static boolean isVersioned(RuleVersionView rule) {
        if (rule == null) {
            return false;
        }
        return RosterWorkflowModeEnum.fromCode(rule.getRosterWorkflowMode()).isVersioned();
    }

    public static boolean isVersioned(String rosterWorkflowMode) {
        return RosterWorkflowModeEnum.fromCode(rosterWorkflowMode).isVersioned();
    }

    public static void assertStructureRosterReady(RuleVersionView rule, BasketDivisionRosterVersion roster,
                                                 BasketDivision division) {
        if (!isVersioned(rule)) {
            return;
        }
        if (roster != null) {
            return;
        }
        throw new ServiceException(structureRosterRequiredMessage(division));
    }

    public static String structureRosterRequiredMessage(BasketDivision division) {
        if (division != null && RegistrationModeEnum.PERSONAL.getCode().equals(division.getRegistrationMode())) {
            return PERSONAL_ROSTER_REQUIRED_MESSAGE;
        }
        return TEAM_ROSTER_REQUIRED_MESSAGE;
    }
}
