package com.zhs.basket.support;

import com.zhs.basket.domain.v4.BasketCaptainGrant;
import com.zhs.basket.enums.PlayerCertStatusEnum;
import com.zhs.basket.enums.v4.CaptainGrantStatusEnum;
import com.zhs.basket.enums.v4.SeasonStatusEnum;
import com.zhs.basket.support.v4.CaptainGrantActiveKeySupport;
import com.zhs.common.utils.StringUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

/**
 * V4 队长授权运行时有效性判断。
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
                                              Date now,
                                              String seasonStatus) {
        if (grant == null) {
            return "当前暂无有效队长权限，队长权限由平台根据赛事缴费结果后台开通";
        }
        CaptainGrantStatusEnum status = CaptainGrantStatusEnum.fromCode(grant.getGrantStatus());
        if (status == CaptainGrantStatusEnum.REVOKED) {
            return "队长权限已收回";
        }
        if (status == CaptainGrantStatusEnum.EXPIRED) {
            return "队长权限已过期";
        }
        if (!Objects.equals(CaptainGrantStatusEnum.ACTIVE.getCode(), grant.getGrantStatus())) {
            return status.getDescription();
        }
        if (StringUtils.isEmpty(grant.getActiveUniqueKey())) {
            return "队长权限已失效";
        }
        if (grant.getDivisionId() != null && grant.getPlayerId() != null
                && !Objects.equals(grant.getActiveUniqueKey(),
                CaptainGrantActiveKeySupport.buildActiveUniqueKey(grant.getDivisionId(), grant.getPlayerId()))) {
            return "队长权限已失效";
        }
        if (playerCertStatus != null && !Objects.equals(PlayerCertStatusEnum.APPROVED.getCode(), playerCertStatus)) {
            return "当前用户尚未完成球员认证，不能授予队长权限";
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
