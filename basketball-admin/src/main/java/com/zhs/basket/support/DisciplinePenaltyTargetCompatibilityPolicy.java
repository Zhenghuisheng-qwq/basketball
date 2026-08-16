package com.zhs.basket.support;

import com.zhs.basket.domain.BasketDisciplineRecord;
import com.zhs.basket.enums.DisciplinePenaltyCodeEnum;
import com.zhs.basket.enums.DisciplineTargetTypeEnum;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.StringUtils;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Target × Penalty 创建/生效/执行安全边界。历史非法记录可查询，但不能新建或进入执行。
 */
public final class DisciplinePenaltyTargetCompatibilityPolicy {

    public static final String MSG_INCOMPATIBLE = "当前处罚对象与处罚措施组合已不再支持，请重新选择。";
    public static final String MSG_UNKNOWN_PENALTY = "未知处罚措施编码";
    public static final String MSG_UNKNOWN_TARGET = "未知处罚对象类型";
    public static final String MSG_DQ_EVENT_TEAM_REQUIRED = "取消赛事资格必须选择赛事球队";
    public static final String MSG_POINT_EVENT_TEAM_REQUIRED = "扣分处罚必须选择赛事球队";

    private static final Map<DisciplinePenaltyCodeEnum, Set<DisciplineTargetTypeEnum>> ALLOWED;

    static {
        Map<DisciplinePenaltyCodeEnum, Set<DisciplineTargetTypeEnum>> map =
                new EnumMap<>(DisciplinePenaltyCodeEnum.class);
        map.put(DisciplinePenaltyCodeEnum.WARNING, EnumSet.of(
                DisciplineTargetTypeEnum.PLAYER,
                DisciplineTargetTypeEnum.PERMANENT_TEAM,
                DisciplineTargetTypeEnum.EVENT_TEAM));
        map.put(DisciplinePenaltyCodeEnum.MATCH_SUSPENSION, EnumSet.of(DisciplineTargetTypeEnum.PLAYER));
        map.put(DisciplinePenaltyCodeEnum.DATE_RANGE_SUSPENSION, EnumSet.of(DisciplineTargetTypeEnum.PLAYER));
        map.put(DisciplinePenaltyCodeEnum.POINT_DEDUCTION, EnumSet.of(DisciplineTargetTypeEnum.EVENT_TEAM));
        map.put(DisciplinePenaltyCodeEnum.REGISTRATION_BAN, EnumSet.of(DisciplineTargetTypeEnum.PLAYER));
        map.put(DisciplinePenaltyCodeEnum.TRANSFER_BAN, EnumSet.of(DisciplineTargetTypeEnum.PLAYER));
        map.put(DisciplinePenaltyCodeEnum.MATCH_ELIGIBILITY_CANCEL, EnumSet.noneOf(DisciplineTargetTypeEnum.class));
        map.put(DisciplinePenaltyCodeEnum.EVENT_DISQUALIFICATION, EnumSet.of(DisciplineTargetTypeEnum.EVENT_TEAM));
        map.put(DisciplinePenaltyCodeEnum.TEAM_SUSPENSION, EnumSet.noneOf(DisciplineTargetTypeEnum.class));
        map.put(DisciplinePenaltyCodeEnum.PLAYER_QUALIFICATION_SUSPENSION, EnumSet.noneOf(DisciplineTargetTypeEnum.class));
        map.put(DisciplinePenaltyCodeEnum.CAPTAIN_BAN, EnumSet.of(DisciplineTargetTypeEnum.PLAYER));
        ALLOWED = Collections.unmodifiableMap(map);
    }

    private DisciplinePenaltyTargetCompatibilityPolicy() {
    }

    public static boolean supports(DisciplinePenaltyCodeEnum penaltyCode, DisciplineTargetTypeEnum targetType) {
        if (penaltyCode == null || targetType == null) {
            return false;
        }
        Set<DisciplineTargetTypeEnum> allowed = ALLOWED.get(penaltyCode);
        return allowed != null && allowed.contains(targetType);
    }

    public static void validate(DisciplinePenaltyCodeEnum penaltyCode, DisciplineTargetTypeEnum targetType) {
        if (!supports(penaltyCode, targetType)) {
            throw new ServiceException(MSG_INCOMPATIBLE);
        }
    }

    public static void validate(String penaltyCode, String targetType) {
        validate(parsePenalty(penaltyCode), parseTarget(targetType));
    }

    public static void validateCreate(String penaltyCode, String targetType, Long eventTeamId) {
        DisciplinePenaltyCodeEnum code = parsePenalty(penaltyCode);
        DisciplineTargetTypeEnum target = parseTarget(targetType);
        validate(code, target);
        validateRequiredContext(code, target, eventTeamId);
    }

    public static void validateRecord(BasketDisciplineRecord record) {
        if (record == null) {
            throw new ServiceException("处罚记录不存在");
        }
        DisciplinePenaltyCodeEnum code = parsePenalty(record.getPenaltyCode());
        DisciplineTargetTypeEnum target = parseTarget(record.getTargetType());
        validate(code, target);
        validateRequiredContext(code, target, record.getEventTeamId());
    }

    public static Set<DisciplineTargetTypeEnum> allowedTargets(DisciplinePenaltyCodeEnum penaltyCode) {
        Set<DisciplineTargetTypeEnum> allowed = ALLOWED.get(penaltyCode);
        return allowed == null ? EnumSet.noneOf(DisciplineTargetTypeEnum.class) : EnumSet.copyOf(allowed);
    }

    private static void validateRequiredContext(DisciplinePenaltyCodeEnum code, DisciplineTargetTypeEnum target,
                                                Long eventTeamId) {
        if (code == DisciplinePenaltyCodeEnum.EVENT_DISQUALIFICATION) {
            if (target != DisciplineTargetTypeEnum.EVENT_TEAM || eventTeamId == null) {
                throw new ServiceException(MSG_DQ_EVENT_TEAM_REQUIRED);
            }
        }
        if (code == DisciplinePenaltyCodeEnum.POINT_DEDUCTION) {
            if (target != DisciplineTargetTypeEnum.EVENT_TEAM || eventTeamId == null) {
                throw new ServiceException(MSG_POINT_EVENT_TEAM_REQUIRED);
            }
        }
    }

    private static DisciplinePenaltyCodeEnum parsePenalty(String penaltyCode) {
        if (StringUtils.isEmpty(penaltyCode)) {
            throw new ServiceException(MSG_UNKNOWN_PENALTY);
        }
        try {
            return DisciplinePenaltyCodeEnum.valueOf(penaltyCode);
        } catch (IllegalArgumentException ex) {
            throw new ServiceException(MSG_UNKNOWN_PENALTY);
        }
    }

    private static DisciplineTargetTypeEnum parseTarget(String targetType) {
        if (StringUtils.isEmpty(targetType)) {
            throw new ServiceException(MSG_UNKNOWN_TARGET);
        }
        return DisciplineTargetTypeEnum.fromCode(targetType);
    }
}
