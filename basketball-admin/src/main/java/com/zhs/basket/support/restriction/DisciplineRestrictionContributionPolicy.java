package com.zhs.basket.support.restriction;

import com.zhs.basket.domain.BasketDisciplineRecord;
import com.zhs.basket.enums.DisciplinePenaltyCodeEnum;
import com.zhs.common.utils.StringUtils;

public final class DisciplineRestrictionContributionPolicy {
    private DisciplineRestrictionContributionPolicy() {
    }

    public static DisciplineRestrictionContribution contribute(BasketDisciplineRecord record) {
        if (record == null || StringUtils.isEmpty(record.getPenaltyCode())) {
            return DisciplineRestrictionContribution.none(record == null ? null : record.getId());
        }
        Long disciplineId = record.getId();
        try {
            DisciplinePenaltyCodeEnum code = DisciplinePenaltyCodeEnum.valueOf(record.getPenaltyCode());
            switch (code) {
                case REGISTRATION_BAN:
                    return new DisciplineRestrictionContribution(false, true, true, true, true, 0, disciplineId);
                case TRANSFER_BAN:
                    return new DisciplineRestrictionContribution(true, false, true, true, true, 0, disciplineId);
                case CAPTAIN_BAN:
                    return new DisciplineRestrictionContribution(true, true, false, true, true, 0, disciplineId);
                case MATCH_SUSPENSION:
                case MATCH_ELIGIBILITY_CANCEL:
                    return new DisciplineRestrictionContribution(true, true, true, false, true,
                            safeInt(record.getMatchSuspensionRemaining()), disciplineId);
                case DATE_RANGE_SUSPENSION:
                case EVENT_DISQUALIFICATION:
                case PLAYER_QUALIFICATION_SUSPENSION:
                    return new DisciplineRestrictionContribution(true, true, true, false, false, 0, disciplineId);
                case TEAM_SUSPENSION:
                    return new DisciplineRestrictionContribution(false, true, true, true, false, 0, disciplineId);
                case POINT_DEDUCTION:
                case WARNING:
                default:
                    return DisciplineRestrictionContribution.none(disciplineId);
            }
        } catch (IllegalArgumentException ignored) {
            return DisciplineRestrictionContribution.none(disciplineId);
        }
    }

    private static int safeInt(Integer value) {
        return value != null && value > 0 ? value : 0;
    }
}
