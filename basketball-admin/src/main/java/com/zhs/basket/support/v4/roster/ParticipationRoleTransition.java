package com.zhs.basket.support.v4.roster;

import com.zhs.basket.enums.v4.ParticipationRoleEnum;
import com.zhs.common.exception.ServiceException;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

public final class ParticipationRoleTransition {

    private static final Map<ParticipationRoleEnum, Set<ParticipationRoleEnum>> ALLOWED = Map.of(
            ParticipationRoleEnum.UNALLOCATED, EnumSet.of(
                    ParticipationRoleEnum.FORMAL, ParticipationRoleEnum.CANDIDATE, ParticipationRoleEnum.EXCLUDED),
            ParticipationRoleEnum.FORMAL, EnumSet.of(
                    ParticipationRoleEnum.CANDIDATE, ParticipationRoleEnum.EXCLUDED),
            ParticipationRoleEnum.CANDIDATE, EnumSet.of(
                    ParticipationRoleEnum.FORMAL, ParticipationRoleEnum.EXCLUDED),
            ParticipationRoleEnum.EXCLUDED, EnumSet.of(
                    ParticipationRoleEnum.FORMAL, ParticipationRoleEnum.CANDIDATE)
    );

    private ParticipationRoleTransition() {
    }

    public static void assertCanTransit(ParticipationRoleEnum from, ParticipationRoleEnum to) {
        if (from == null || to == null) {
            throw new ServiceException("名单角色不能为空");
        }
        if (from == to) {
            return;
        }
        Set<ParticipationRoleEnum> next = ALLOWED.get(from);
        if (next == null || !next.contains(to)) {
            throw new ServiceException("不允许从 " + from.getDescription() + " 调整为 " + to.getDescription());
        }
    }
}
