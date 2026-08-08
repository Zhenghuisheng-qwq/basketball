package com.zhs.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhs.dao.v4.BasketCaptainGrantDao;
import com.zhs.enums.v4.CaptainGrantStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketTeam;
import com.zhs.model.v4.BasketCaptainGrant;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 队长授权（basket_captain_grant）绑定与校验。
 */
@Component
public class CaptainGrantSupport {

    public static final String NO_ACTIVE_GRANT_MESSAGE =
            "当前暂无有效队长权限，队长权限由平台根据赛事缴费结果后台开通";

    public static final String GRANT_BOUND_OTHER_TEAM_MESSAGE = "该赛事队长授权已用于其他球队";

    private static final Integer NOT_DELETED = 0;
    private static final String GRANT_STATUS_ACTIVE = CaptainGrantStatusEnum.ACTIVE.getCode();

    @Resource
    private BasketCaptainGrantDao captainGrantDao;

    public BasketCaptainGrant findActiveGrantForDivision(Long memberId, Long divisionId) {
        return findActiveGrantForDivision(memberId, divisionId, null);
    }

    public BasketCaptainGrant findActiveGrantForDivision(Long memberId, Long divisionId, Long playerId) {
        if (memberId == null || divisionId == null) {
            return null;
        }
        LambdaQueryWrapper<BasketCaptainGrant> wrapper = new LambdaQueryWrapper<BasketCaptainGrant>()
                .eq(BasketCaptainGrant::getMemberId, memberId)
                .eq(BasketCaptainGrant::getDivisionId, divisionId)
                .eq(BasketCaptainGrant::getGrantStatus, GRANT_STATUS_ACTIVE)
                .eq(BasketCaptainGrant::getIsDelete, NOT_DELETED)
                .isNotNull(BasketCaptainGrant::getActiveUniqueKey)
                .orderByDesc(BasketCaptainGrant::getCreateTime)
                .orderByDesc(BasketCaptainGrant::getId);
        if (playerId != null) {
            wrapper.eq(BasketCaptainGrant::getPlayerId, playerId);
        }
        List<BasketCaptainGrant> candidates = captainGrantDao.selectList(wrapper);
        return candidates.isEmpty() ? null : candidates.get(0);
    }

    public BasketCaptainGrant findActiveGrantByDivisionPlayer(Long divisionId, Long playerId) {
        if (divisionId == null || playerId == null) {
            return null;
        }
        return captainGrantDao.selectOne(new LambdaQueryWrapper<BasketCaptainGrant>()
                .eq(BasketCaptainGrant::getDivisionId, divisionId)
                .eq(BasketCaptainGrant::getPlayerId, playerId)
                .eq(BasketCaptainGrant::getGrantStatus, GRANT_STATUS_ACTIVE)
                .eq(BasketCaptainGrant::getIsDelete, NOT_DELETED)
                .isNotNull(BasketCaptainGrant::getActiveUniqueKey)
                .last("limit 1"));
    }

    public BasketCaptainGrant findCurrentGrant(Long memberId) {
        if (memberId == null) {
            return null;
        }
        return captainGrantDao.selectOne(new LambdaQueryWrapper<BasketCaptainGrant>()
                .eq(BasketCaptainGrant::getMemberId, memberId)
                .eq(BasketCaptainGrant::getGrantStatus, GRANT_STATUS_ACTIVE)
                .eq(BasketCaptainGrant::getIsDelete, NOT_DELETED)
                .isNotNull(BasketCaptainGrant::getActiveUniqueKey)
                .orderByDesc(BasketCaptainGrant::getCreateTime)
                .orderByDesc(BasketCaptainGrant::getId)
                .last("limit 1"));
    }

    public BasketCaptainGrant findActiveGrantForSeason(Long memberId, Long seasonId) {
        if (memberId == null || seasonId == null) {
            return null;
        }
        return captainGrantDao.selectOne(new LambdaQueryWrapper<BasketCaptainGrant>()
                .eq(BasketCaptainGrant::getMemberId, memberId)
                .eq(BasketCaptainGrant::getSeasonId, seasonId)
                .eq(BasketCaptainGrant::getGrantStatus, GRANT_STATUS_ACTIVE)
                .eq(BasketCaptainGrant::getIsDelete, NOT_DELETED)
                .isNotNull(BasketCaptainGrant::getActiveUniqueKey)
                .orderByDesc(BasketCaptainGrant::getCreateTime)
                .orderByDesc(BasketCaptainGrant::getId)
                .last("limit 1"));
    }

    public List<BasketCaptainGrant> findActiveGrantsForMember(Long memberId) {
        if (memberId == null) {
            return Collections.emptyList();
        }
        return captainGrantDao.selectList(new LambdaQueryWrapper<BasketCaptainGrant>()
                .eq(BasketCaptainGrant::getMemberId, memberId)
                .eq(BasketCaptainGrant::getGrantStatus, GRANT_STATUS_ACTIVE)
                .eq(BasketCaptainGrant::getIsDelete, NOT_DELETED)
                .isNotNull(BasketCaptainGrant::getActiveUniqueKey)
                .orderByDesc(BasketCaptainGrant::getCreateTime)
                .orderByDesc(BasketCaptainGrant::getId));
    }

    public void assertCanManageTeam(BasketCaptainGrant grant, BasketTeam team, Long memberId) {
        if (grant == null) {
            throw new ServiceException(NO_ACTIVE_GRANT_MESSAGE, 403);
        }
        if (!Objects.equals(team.getCaptainMemberId(), memberId)) {
            throw new ServiceException("无权操作该球队", 403);
        }
        if (grant.getTeamId() != null && !Objects.equals(grant.getTeamId(), team.getId())) {
            throw new ServiceException(GRANT_BOUND_OTHER_TEAM_MESSAGE, 403);
        }
    }

    public void bindGrantToTeam(BasketCaptainGrant grant, Long teamId) {
        if (grant == null || teamId == null) {
            return;
        }
        if (grant.getTeamId() != null) {
            if (!Objects.equals(grant.getTeamId(), teamId)) {
                throw new ServiceException(GRANT_BOUND_OTHER_TEAM_MESSAGE, 403);
            }
            return;
        }
        captainGrantDao.update(null, new LambdaUpdateWrapper<BasketCaptainGrant>()
                .eq(BasketCaptainGrant::getId, grant.getId())
                .eq(BasketCaptainGrant::getIsDelete, NOT_DELETED)
                .isNull(BasketCaptainGrant::getTeamId)
                .set(BasketCaptainGrant::getTeamId, teamId));
        grant.setTeamId(teamId);
    }

    public void assertTeamBindingForRegistration(BasketCaptainGrant grant, Long teamId) {
        if (grant == null || teamId == null) {
            throw new ServiceException("无有效队长授权");
        }
        bindGrantToTeam(grant, teamId);
    }

    public static String buildActiveUniqueKey(Long divisionId, Long playerId) {
        if (divisionId == null || playerId == null) {
            return null;
        }
        return divisionId + "_" + playerId;
    }
}
