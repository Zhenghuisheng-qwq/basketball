package com.zhs.basket.support.v4;

import com.zhs.basket.domain.BasketTeam;
import com.zhs.basket.domain.v4.BasketCaptainGrant;
import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketSeason;
import com.zhs.basket.enums.CaptainGrantLogActionTypeEnum;
import com.zhs.basket.enums.TeamStatusEnum;
import com.zhs.basket.enums.v4.CaptainGrantStatusEnum;
import com.zhs.basket.mapper.BasketTeamMapper;
import com.zhs.basket.mapper.v4.BasketCaptainGrantMapper;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketSeasonMapper;
import com.zhs.basket.support.CaptainGrantLogSupport;
import com.zhs.basket.support.CaptainGrantSeasonSupport;
import com.zhs.basket.support.CaptainGrantValiditySupport;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Objects;

/**
 * 队长授权运行时：有效期解析、懒过期、后绑定球队。
 */
@Component
public class CaptainGrantRuntimeSupport {

    private static final Integer LEGACY_ACTIVE = 1;
    private static final Integer LEGACY_EXPIRED = 3;

    @Autowired
    private BasketCaptainGrantMapper captainGrantMapper;
    @Autowired
    private BasketDivisionMapper divisionMapper;
    @Autowired
    private BasketSeasonMapper seasonMapper;
    @Autowired
    private BasketTeamMapper teamMapper;
    @Autowired
    private CaptainGrantLogSupport grantLogSupport;

    public Date resolveValidTo(Long divisionId) {
        BasketDivision division = divisionMapper.selectById(divisionId);
        if (division == null) {
            throw new ServiceException("Division 不存在");
        }
        if (division.getMatchEndTime() != null) {
            return division.getMatchEndTime();
        }
        BasketSeason season = seasonMapper.selectById(division.getSeasonId());
        return CaptainGrantSeasonSupport.requireExpireTime(season, null);
    }

    public BasketCaptainGrant requireRuntimeActiveGrant(Long divisionId, Long playerId, Integer playerCertStatus) {
        BasketCaptainGrant grant = captainGrantMapper.selectActiveByDivisionPlayer(divisionId, playerId);
        if (grant == null) {
            throw new ServiceException("无有效队长授权");
        }
        lazyExpireIfNeeded(grant, playerCertStatus);
        grant = captainGrantMapper.selectActiveByDivisionPlayer(divisionId, playerId);
        if (grant == null || !CaptainGrantStatusEnum.ACTIVE.getCode().equals(grant.getGrantStatus())) {
            throw new ServiceException("无有效队长授权");
        }
        String seasonStatus = resolveSeasonStatus(grant.getSeasonId());
        String invalid = CaptainGrantValiditySupport.resolveInvalidReason(grant, playerCertStatus, new Date(), seasonStatus);
        if (invalid != null) {
            throw new ServiceException(invalid);
        }
        return grant;
    }

    public void lazyExpireIfNeeded(BasketCaptainGrant grant, Integer playerCertStatus) {
        if (grant == null || !CaptainGrantStatusEnum.ACTIVE.getCode().equals(grant.getGrantStatus())) {
            return;
        }
        String seasonStatus = resolveSeasonStatus(grant.getSeasonId());
        String invalid = CaptainGrantValiditySupport.resolveInvalidReason(grant, playerCertStatus, new Date(), seasonStatus);
        if (invalid == null) {
            return;
        }
        boolean timeExpired = grant.getValidTo() != null && grant.getValidTo().before(new Date());
        boolean seasonEnded = seasonStatus != null && CaptainGrantSeasonSupport.isSeasonEnded(seasonStatus);
        if (!timeExpired && !seasonEnded) {
            return;
        }
        if (captainGrantMapper.expireById(grant.getId()) == 1) {
            grantLogSupport.writeGrantLog(grant, CaptainGrantLogActionTypeEnum.EXPIRE,
                    LEGACY_ACTIVE, LEGACY_EXPIRED, invalid, "懒过期", "SYSTEM");
        }
    }

    public void bindTeamIfAbsent(BasketCaptainGrant grant, Long teamId, String operatorType) {
        if (grant == null || teamId == null) {
            return;
        }
        BasketTeam team = teamMapper.selectById(teamId);
        if (team == null) {
            throw new ServiceException("球队不存在");
        }
        if (team.getDivisionId() == null) {
            throw new ServiceException("历史球队不能直接报名新赛事，请在当前赛事重新创建球队");
        }
        if (!Objects.equals(TeamStatusEnum.ACTIVE.getCode(), team.getTeamStatus())) {
            throw new ServiceException("仅当前赛事有效球队可绑定队长授权");
        }
        if (!Objects.equals(grant.getDivisionId(), team.getDivisionId())) {
            throw new ServiceException("队长授权与球队所属赛事不一致");
        }
        if (grant.getTeamId() != null) {
            if (!Objects.equals(grant.getTeamId(), teamId)) {
                throw new ServiceException("该赛事队长授权已用于其他球队");
            }
            return;
        }
        if (captainGrantMapper.bindTeamById(grant.getId(), teamId) != 1) {
            throw new ServiceException("该队长权限已被其他建队请求占用");
        }
        grant.setTeamId(teamId);
        grantLogSupport.writeGrantLog(grant, CaptainGrantLogActionTypeEnum.BIND_TEAM,
                LEGACY_ACTIVE, LEGACY_ACTIVE, null, "后绑定球队", operatorType);
    }

    public void assertTeamBindingForRegistration(BasketCaptainGrant grant, BasketTeam team) {
        if (grant == null || team == null || team.getId() == null) {
            throw new ServiceException("无有效队长授权");
        }
        if (team.getDivisionId() == null) {
            throw new ServiceException("历史球队不能直接报名新赛事，请在当前赛事重新创建球队");
        }
        if (!Objects.equals(TeamStatusEnum.ACTIVE.getCode(), team.getTeamStatus())) {
            throw new ServiceException("历史或已结束球队不能用于当前赛事报名");
        }
        if (!Objects.equals(grant.getDivisionId(), team.getDivisionId())) {
            throw new ServiceException("队长授权与球队所属赛事不一致");
        }
        if (grant.getTeamId() == null) {
            bindTeamIfAbsent(grant, team.getId(), "MEMBER");
            return;
        }
        if (!Objects.equals(grant.getTeamId(), team.getId())) {
            throw new ServiceException("该赛事队长授权已用于其他球队");
        }
    }

    /** @deprecated 请使用 {@link #assertTeamBindingForRegistration(BasketCaptainGrant, BasketTeam)} */
    @Deprecated
    public void assertTeamBindingForRegistration(BasketCaptainGrant grant, Long teamId) {
        if (grant == null || teamId == null) {
            throw new ServiceException("无有效队长授权");
        }
        if (grant.getTeamId() == null) {
            bindTeamIfAbsent(grant, teamId, "MEMBER");
            return;
        }
        if (!Objects.equals(grant.getTeamId(), teamId)) {
            throw new ServiceException("该赛事队长授权已用于其他球队");
        }
    }

    private String resolveSeasonStatus(Long seasonId) {
        if (seasonId == null) {
            return null;
        }
        BasketSeason season = seasonMapper.selectById(seasonId);
        return season != null ? season.getSeasonStatus() : null;
    }
}
