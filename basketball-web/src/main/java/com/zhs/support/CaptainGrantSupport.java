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

    public static final String GRANT_BIND_RACE_MESSAGE = "该队长权限已被其他建队请求占用，请刷新后重试";
    public static final String GRANT_DIVISION_MISMATCH_MESSAGE = "队长授权与球队所属赛事不一致";
    public static final String GRANT_ALREADY_BOUND_MESSAGE = "该赛事队长资格已创建过球队，不能再次创建";

    public void assertCanManageTeam(BasketCaptainGrant grant, BasketTeam team, Long memberId) {
        if (grant == null) {
            throw new ServiceException(NO_ACTIVE_GRANT_MESSAGE, 403);
        }
        if (team == null || team.getDivisionId() == null) {
            throw new ServiceException(TeamWritableSupport.LEGACY_TEAM_READONLY_MESSAGE, 403);
        }
        if (!Objects.equals(grant.getDivisionId(), team.getDivisionId())) {
            throw new ServiceException(GRANT_DIVISION_MISMATCH_MESSAGE, 403);
        }
        if (!Objects.equals(team.getCaptainMemberId(), memberId)
                && !Objects.equals(grant.getMemberId(), memberId)) {
            throw new ServiceException("无权操作该球队", 403);
        }
        if (grant.getTeamId() == null || !Objects.equals(grant.getTeamId(), team.getId())) {
            throw new ServiceException(GRANT_BOUND_OTHER_TEAM_MESSAGE, 403);
        }
    }

    public void assertGrantUnbound(BasketCaptainGrant grant) {
        if (grant == null) {
            throw new ServiceException(NO_ACTIVE_GRANT_MESSAGE, 403);
        }
        if (grant.getTeamId() != null) {
            throw new ServiceException(GRANT_ALREADY_BOUND_MESSAGE, 403);
        }
    }

    public void assertGrantTeamDivisionMatch(BasketCaptainGrant grant, BasketTeam team) {
        if (grant == null || team == null) {
            throw new ServiceException("无有效队长授权或球队");
        }
        if (team.getDivisionId() == null) {
            throw new ServiceException(TeamWritableSupport.LEGACY_TEAM_READONLY_MESSAGE, 403);
        }
        if (!Objects.equals(grant.getDivisionId(), team.getDivisionId())) {
            throw new ServiceException(GRANT_DIVISION_MISMATCH_MESSAGE, 403);
        }
    }

    public void bindGrantToTeam(BasketCaptainGrant grant, Long teamId) {
        if (grant == null || teamId == null) {
            throw new ServiceException("无有效队长授权");
        }
        if (grant.getTeamId() != null) {
            if (!Objects.equals(grant.getTeamId(), teamId)) {
                throw new ServiceException(GRANT_BOUND_OTHER_TEAM_MESSAGE, 403);
            }
            return;
        }
        int updated = captainGrantDao.update(null, new LambdaUpdateWrapper<BasketCaptainGrant>()
                .eq(BasketCaptainGrant::getId, grant.getId())
                .eq(BasketCaptainGrant::getIsDelete, NOT_DELETED)
                .eq(BasketCaptainGrant::getGrantStatus, GRANT_STATUS_ACTIVE)
                .isNull(BasketCaptainGrant::getTeamId)
                .set(BasketCaptainGrant::getTeamId, teamId));
        assertBindUpdatedRows(updated);
        grant.setTeamId(teamId);
    }

    /** CAS：bind 更新行数必须为 1，否则视为并发占用并回滚建队事务 */
    public void assertBindUpdatedRows(int updated) {
        if (updated != 1) {
            throw new ServiceException(GRANT_BIND_RACE_MESSAGE, 409);
        }
    }

    public void assertTeamBindingForRegistration(BasketCaptainGrant grant, BasketTeam team) {
        if (grant == null || team == null || team.getId() == null) {
            throw new ServiceException("无有效队长授权");
        }
        assertGrantTeamDivisionMatch(grant, team);
        if (grant.getTeamId() == null) {
            bindGrantToTeam(grant, team.getId());
            return;
        }
        if (!Objects.equals(grant.getTeamId(), team.getId())) {
            throw new ServiceException(GRANT_BOUND_OTHER_TEAM_MESSAGE, 403);
        }
    }

    /** @deprecated 请使用 {@link #assertTeamBindingForRegistration(BasketCaptainGrant, BasketTeam)} */
    @Deprecated
    public void assertTeamBindingForRegistration(BasketCaptainGrant grant, Long teamId) {
        if (grant == null || teamId == null) {
            throw new ServiceException("无有效队长授权");
        }
        if (grant.getTeamId() == null) {
            bindGrantToTeam(grant, teamId);
            return;
        }
        if (!Objects.equals(grant.getTeamId(), teamId)) {
            throw new ServiceException(GRANT_BOUND_OTHER_TEAM_MESSAGE, 403);
        }
    }

    public static String buildActiveUniqueKey(Long divisionId, Long playerId) {
        if (divisionId == null || playerId == null) {
            return null;
        }
        return divisionId + "_" + playerId;
    }
}
