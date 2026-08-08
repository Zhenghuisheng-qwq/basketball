package com.zhs.support;

import com.zhs.enums.PlayerCertStatusEnum;
import com.zhs.enums.v4.CaptainGrantStatusEnum;
import com.zhs.enums.v4.SeasonStatusEnum;
import com.zhs.model.v4.BasketCaptainGrant;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * V4 队长授权（basket_captain_grant）运行时有效性判断。
 */
public final class CaptainGrantValiditySupport {

    private static final Set<String> ENDED_SEASON_STATUSES = new HashSet<>(Arrays.asList(
            SeasonStatusEnum.FINISHED.getCode(),
            SeasonStatusEnum.ARCHIVED.getCode()
    ));

    private CaptainGrantValiditySupport() {
    }

    public static boolean isRuntimeValid(BasketCaptainGrant grant, Integer playerCertStatus, Date now) {
        return resolveInvalidReason(grant, playerCertStatus, now, null) == null;
    }

    public static String resolveInvalidReason(BasketCaptainGrant grant,
                                              Integer playerCertStatus,
                                              Date now) {
        return resolveInvalidReason(grant, playerCertStatus, now, null);
    }

    public static String resolveInvalidReason(BasketCaptainGrant grant,
                                              Integer playerCertStatus,
                                              Date now,
                                              String seasonStatus) {
        if (grant == null) {
            return CaptainGrantSupport.NO_ACTIVE_GRANT_MESSAGE;
        }
        CaptainGrantStatusEnum status = CaptainGrantStatusEnum.fromCode(grant.getGrantStatus());
        if (status == CaptainGrantStatusEnum.REVOKED) {
            return "队长权限已收回";
        }
        if (status == CaptainGrantStatusEnum.EXPIRED) {
            return "队长权限已过期";
        }
        if (!Objects.equals(CaptainGrantStatusEnum.ACTIVE.getCode(), grant.getGrantStatus())) {
            return status != null ? status.getDescription() : "队长权限不可用";
        }
        if (org.springframework.util.StringUtils.hasText(grant.getActiveUniqueKey())
                && !Objects.equals(grant.getActiveUniqueKey(),
                CaptainGrantSupport.buildActiveUniqueKey(grant.getDivisionId(), grant.getPlayerId()))) {
            return "队长权限已失效";
        }
        if (playerCertStatus != null && !Objects.equals(PlayerCertStatusEnum.APPROVED.getCode(), playerCertStatus)) {
            return "球员认证未通过";
        }
        Date checkTime = now != null ? now : new Date();
        if (grant.getValidFrom() != null && grant.getValidFrom().after(checkTime)) {
            return "队长权限尚未生效";
        }
        if (grant.getValidTo() != null && grant.getValidTo().before(checkTime)) {
            return "队长权限已过期";
        }
        if (grant.getSeasonId() != null && seasonStatus != null && ENDED_SEASON_STATUSES.contains(seasonStatus)) {
            return "当前赛事已结束，队长权限已失效";
        }
        return null;
    }
}
