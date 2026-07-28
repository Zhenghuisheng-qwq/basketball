package com.zhs.support;

import com.zhs.enums.CaptainQualificationStatusEnum;
import com.zhs.enums.PlayerCertStatusEnum;
import com.zhs.enums.TournamentSeasonStatusEnum;
import com.zhs.model.BasketCaptainQualification;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * 队长授权运行时有效性判断，与后台管理端保持一致。
 */
public final class CaptainQualificationValiditySupport {

    private static final int CURRENT = 1;
    private static final int PAYMENT_CONFIRMED = 1;
    private static final Set<Integer> ENDED_SEASON_STATUSES = new HashSet<>(Arrays.asList(
            TournamentSeasonStatusEnum.FINISHED.getCode(),
            TournamentSeasonStatusEnum.CLOSED.getCode(),
            TournamentSeasonStatusEnum.CANCELLED.getCode()
    ));

    private CaptainQualificationValiditySupport() {
    }

    public static boolean isRuntimeValid(BasketCaptainQualification qualification, Integer playerCertStatus, Date now) {
        return resolveInvalidReason(qualification, playerCertStatus, now, null) == null;
    }

    public static String resolveInvalidReason(BasketCaptainQualification qualification,
                                              Integer playerCertStatus,
                                              Date now) {
        return resolveInvalidReason(qualification, playerCertStatus, now, null);
    }

    public static String resolveInvalidReason(BasketCaptainQualification qualification,
                                              Integer playerCertStatus,
                                              Date now,
                                              Integer seasonStatus) {
        if (qualification == null) {
            return CaptainGrantSupport.NO_ACTIVE_GRANT_MESSAGE;
        }
        if (!Integer.valueOf(CURRENT).equals(qualification.getIsCurrent())) {
            return "队长权限已失效";
        }
        CaptainQualificationStatusEnum status = CaptainQualificationStatusEnum.fromCode(qualification.getQualificationStatus());
        if (status == CaptainQualificationStatusEnum.REVOKED) {
            return "队长权限已收回";
        }
        if (status == CaptainQualificationStatusEnum.EXPIRED) {
            return "队长权限已过期";
        }
        if (!Objects.equals(CaptainQualificationStatusEnum.ACTIVE.getCode(), qualification.getQualificationStatus())) {
            return status.getDescription();
        }
        if (!Integer.valueOf(PAYMENT_CONFIRMED).equals(qualification.getPaymentConfirmed())) {
            return "缴费未确认";
        }
        if (playerCertStatus != null && !Objects.equals(PlayerCertStatusEnum.APPROVED.getCode(), playerCertStatus)) {
            return "球员认证未通过";
        }
        Date checkTime = now != null ? now : new Date();
        if (qualification.getValidFrom() != null && qualification.getValidFrom().after(checkTime)) {
            return "队长权限尚未生效";
        }
        if (qualification.getValidTo() != null && qualification.getValidTo().before(checkTime)) {
            return "队长权限已过期";
        }
        if (qualification.getSeasonId() != null && seasonStatus != null && ENDED_SEASON_STATUSES.contains(seasonStatus)) {
            return "当前赛事已结束，队长权限已失效";
        }
        return null;
    }
}
