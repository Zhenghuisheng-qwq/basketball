package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketCaptainGrant;
import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketSeason;
import com.zhs.basket.enums.CaptainGrantLogActionTypeEnum;
import com.zhs.basket.enums.v4.CaptainGrantStatusEnum;
import com.zhs.basket.enums.v4.CaptainGrantValidToModeEnum;
import com.zhs.basket.enums.v4.DivisionStatusEnum;
import com.zhs.basket.mapper.v4.BasketCaptainGrantMapper;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketSeasonMapper;
import com.zhs.basket.support.CaptainGrantLogSupport;
import com.zhs.basket.support.CaptainGrantSeasonSupport;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;
import java.util.Objects;

/**
 * 只改写 valid_to_mode=AUTO_EVENT 且仍为 ACTIVE 的授权。历史空策略和 CUSTOM 不在这里被覆盖。
 */
@Component
public class CaptainGrantEventSyncSupport {

    private static final Integer LEGACY_ACTIVE = 1;

    @Autowired
    private BasketCaptainGrantMapper captainGrantMapper;
    @Autowired
    private BasketDivisionMapper divisionMapper;
    @Autowired
    private BasketSeasonMapper seasonMapper;
    @Autowired
    private CaptainGrantRuntimeSupport grantRuntimeSupport;
    @Autowired
    private CaptainGrantLogSupport grantLogSupport;

    public void onDivisionMatchEndChanged(Long divisionId, Date previousMatchEnd, Date currentMatchEnd) {
        if (divisionId == null || sameInstant(previousMatchEnd, currentMatchEnd)) {
            return;
        }
        Date target = grantRuntimeSupport.resolveValidTo(divisionId);
        for (BasketCaptainGrant grant : captainGrantMapper.selectAutoEventByDivisionId(divisionId)) {
            applyAutoEvent(grant, target, "赛区比赛结束时间变更");
        }
    }

    public void onSeasonEndChanged(Long seasonId, Date previousEnd, Date currentEnd) {
        if (seasonId == null || sameInstant(previousEnd, currentEnd)) {
            return;
        }
        BasketSeason season = seasonMapper.selectById(seasonId);
        if (season == null || season.getSeasonEndTime() == null) {
            return;
        }
        for (BasketCaptainGrant grant : captainGrantMapper.selectAutoEventDependingOnSeason(seasonId)) {
            applyAutoEvent(grant, season.getSeasonEndTime(), "赛季结束时间变更");
        }
    }

    public Date requireEventEnd(Long divisionId) {
        return grantRuntimeSupport.resolveValidTo(divisionId);
    }

    public void assertEventStillOpen(BasketDivision division, BasketSeason season) {
        if (season != null && CaptainGrantSeasonSupport.isSeasonEnded(season.getSeasonStatus())) {
            throw new ServiceException("赛事已结束或归档，不能延长队长授权");
        }
        if (division != null && (DivisionStatusEnum.FINISHED.getCode().equals(division.getDivisionStatus())
                || DivisionStatusEnum.ARCHIVED.getCode().equals(division.getDivisionStatus()))) {
            throw new ServiceException("赛区已结束或归档，不能延长队长授权");
        }
    }

    public void assertActiveGrant(BasketCaptainGrant grant) {
        if (grant == null) {
            throw new ServiceException("队长授权不存在");
        }
        if (CaptainGrantStatusEnum.REVOKED.getCode().equals(grant.getGrantStatus())) {
            throw new ServiceException("已撤销授权不能通过改期恢复");
        }
        if (CaptainGrantStatusEnum.EXPIRED.getCode().equals(grant.getGrantStatus())) {
            throw new ServiceException("已过期授权不能通过改期恢复，请重新授权");
        }
        if (!CaptainGrantStatusEnum.ACTIVE.getCode().equals(grant.getGrantStatus())) {
            throw new ServiceException("仅生效中的授权可以调整有效期");
        }
    }

    public boolean applyWindow(BasketCaptainGrant grant, Date validTo, String mode, String reason,
                               CaptainGrantLogActionTypeEnum action) {
        if (grant == null || !CaptainGrantStatusEnum.ACTIVE.getCode().equals(grant.getGrantStatus())) {
            return false;
        }
        if (sameInstant(grant.getValidTo(), validTo) && Objects.equals(grant.getValidToMode(), mode)) {
            return true;
        }
        int rows = captainGrantMapper.updateValidWindow(
                grant.getId(), validTo, mode, grant.getValidTo(), grant.getValidToMode());
        if (rows != 1) {
            throw new ServiceException("授权有效期已变化，请刷新后重试");
        }
        Date previous = grant.getValidTo();
        grant.setValidTo(validTo);
        grant.setValidToMode(mode);
        grantLogSupport.writeGrantLog(grant, action, LEGACY_ACTIVE, LEGACY_ACTIVE,
                reason, "原到期 " + format(previous) + "，新到期 " + format(validTo) + "，策略 " + mode, "ADMIN");
        return true;
    }

    public String eventEndSource(BasketDivision division) {
        if (division != null && division.getMatchEndTime() != null) {
            return "DIVISION";
        }
        return "SEASON";
    }

    public List<BasketCaptainGrant> listAutoEventByDivision(Long divisionId) {
        return captainGrantMapper.selectAutoEventByDivisionId(divisionId);
    }

    private void applyAutoEvent(BasketCaptainGrant grant, Date target, String reason) {
        if (target == null || sameInstant(grant.getValidTo(), target)) {
            return;
        }
        // 状态仍是 ACTIVE、但截止时间已过：懒过期尚未落库。赛事延期不能把这条授权重新变成可用。
        if (grant.getValidTo() != null && grant.getValidTo().before(new Date())) {
            return;
        }
        applyWindow(grant, target, CaptainGrantValidToModeEnum.AUTO_EVENT.getCode(), reason,
                CaptainGrantLogActionTypeEnum.SYNC_EVENT_TIME);
    }

    private boolean sameInstant(Date left, Date right) {
        if (left == null || right == null) {
            return left == right;
        }
        return left.getTime() == right.getTime();
    }

    private String format(Date value) {
        return value == null ? "-" : String.valueOf(value.getTime());
    }
}
