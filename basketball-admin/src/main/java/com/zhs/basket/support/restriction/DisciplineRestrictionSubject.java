package com.zhs.basket.support.restriction;

import com.zhs.basket.domain.BasketDisciplineRecord;
import com.zhs.basket.enums.DisciplineTargetTypeEnum;
import com.zhs.common.utils.StringUtils;

public final class DisciplineRestrictionSubject {
    private final String subjectType;
    private final Long subjectId;

    public DisciplineRestrictionSubject(String subjectType, Long subjectId) {
        this.subjectType = subjectType;
        this.subjectId = subjectId;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public Long getSubjectId() {
        return subjectId;
    }

    public static DisciplineRestrictionSubject from(BasketDisciplineRecord record) {
        if (record == null) {
            return null;
        }
        String targetType = StringUtils.isNotEmpty(record.getTargetType())
                ? record.getTargetType() : DisciplineTargetTypeEnum.PLAYER.getCode();
        if (DisciplineTargetTypeEnum.PLAYER.getCode().equalsIgnoreCase(targetType) && record.getPlayerId() != null) {
            return new DisciplineRestrictionSubject(DisciplineTargetTypeEnum.PLAYER.getCode(), record.getPlayerId());
        }
        if (DisciplineTargetTypeEnum.PERMANENT_TEAM.getCode().equalsIgnoreCase(targetType) && record.getTeamId() != null) {
            return new DisciplineRestrictionSubject(DisciplineTargetTypeEnum.PERMANENT_TEAM.getCode(), record.getTeamId());
        }
        if (DisciplineTargetTypeEnum.EVENT_TEAM.getCode().equalsIgnoreCase(targetType) && record.getEventTeamId() != null) {
            return new DisciplineRestrictionSubject(DisciplineTargetTypeEnum.EVENT_TEAM.getCode(), record.getEventTeamId());
        }
        if (record.getMemberId() != null) {
            return new DisciplineRestrictionSubject(DisciplineTargetTypeEnum.ACCOUNT.getCode(), record.getMemberId());
        }
        if (record.getPlayerId() != null) {
            return new DisciplineRestrictionSubject(DisciplineTargetTypeEnum.PLAYER.getCode(), record.getPlayerId());
        }
        if (record.getTeamId() != null) {
            return new DisciplineRestrictionSubject(DisciplineTargetTypeEnum.PERMANENT_TEAM.getCode(), record.getTeamId());
        }
        return null;
    }
}
