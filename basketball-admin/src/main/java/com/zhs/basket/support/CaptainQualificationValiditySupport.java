package com.zhs.basket.support;

import com.zhs.basket.domain.BasketCaptainQualification;
import com.zhs.basket.enums.CaptainQualificationStatusEnum;
import com.zhs.basket.enums.PlayerCertStatusEnum;

import java.util.Date;
import java.util.Objects;

/**
 * 队长资格运行时有效性判断，与前台 {@code CaptainPermissionServiceImpl} 保持一致。
 */
public final class CaptainQualificationValiditySupport {

    private static final int CURRENT = 1;
    private static final int PAYMENT_CONFIRMED = 1;

    private CaptainQualificationValiditySupport() {
    }

    public static boolean isRuntimeValid(BasketCaptainQualification qualification, Integer playerCertStatus, Date now) {
        return resolveInvalidReason(qualification, playerCertStatus, now) == null;
    }

    public static boolean isRuntimeValid(BasketCaptainQualification qualification, Date now) {
        return resolveInvalidReason(qualification, null, now) == null;
    }

    public static String resolveInvalidReason(BasketCaptainQualification qualification, Integer playerCertStatus, Date now) {
        if (qualification == null) {
            return "暂无有效队长资格";
        }
        if (!Integer.valueOf(CURRENT).equals(qualification.getIsCurrent())) {
            return "队长资格已失效";
        }
        if (!Objects.equals(CaptainQualificationStatusEnum.ACTIVE.getCode(), qualification.getQualificationStatus())) {
            return CaptainQualificationStatusEnum.fromCode(qualification.getQualificationStatus()).getDescription();
        }
        if (!Integer.valueOf(PAYMENT_CONFIRMED).equals(qualification.getPaymentConfirmed())) {
            return "缴费未确认";
        }
        if (playerCertStatus != null && !Objects.equals(PlayerCertStatusEnum.APPROVED.getCode(), playerCertStatus)) {
            return "球员认证未通过";
        }
        Date checkTime = now != null ? now : new Date();
        if (qualification.getValidFrom() != null && qualification.getValidFrom().after(checkTime)) {
            return "资格尚未生效";
        }
        if (qualification.getValidTo() != null && qualification.getValidTo().before(checkTime)) {
            return "资格已过期";
        }
        return null;
    }
}
