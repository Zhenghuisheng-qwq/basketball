package com.zhs.basket.support;

import com.zhs.basket.domain.BasketCaptainQualification;
import com.zhs.basket.enums.CaptainQualificationStatusEnum;
import com.zhs.basket.enums.PlayerCertStatusEnum;

import java.util.Date;
import java.util.Objects;

/**
 * 队长授权运行时有效性判断，与前台保持一致。
 */
public final class CaptainQualificationValiditySupport {

    private static final int CURRENT = 1;
    private static final int PAYMENT_CONFIRMED = 1;

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
            return "当前暂无有效队长权限，队长权限由平台根据赛事缴费结果后台开通";
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
            return "当前用户尚未完成球员认证，不能授予队长权限";
        }
        Date checkTime = now != null ? now : new Date();
        if (qualification.getValidFrom() != null && qualification.getValidFrom().after(checkTime)) {
            return "队长权限尚未生效";
        }
        if (qualification.getValidTo() != null && qualification.getValidTo().before(checkTime)) {
            return "队长权限已过期";
        }
        if (qualification.getSeasonId() != null && seasonStatus != null
                && CaptainGrantSeasonSupport.isSeasonEnded(seasonStatus)) {
            return "当前赛事已结束，队长权限已失效";
        }
        return null;
    }
}
