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
import org.springframework.util.StringUtils;

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

    private static final Integer NOT_DELETED = 0;
    private static final String GRANT_STATUS_ACTIVE = CaptainGrantStatusEnum.ACTIVE.getCode();

    @Resource
    private BasketCaptainGrantDao captainGrantDao;

    public BasketCaptainGrant findCurrentGrant(Long memberId) {
        if (memberId == null) {
            return null;
        }
        List<BasketCaptainGrant> unbound = captainGrantDao.selectList(new LambdaQueryWrapper<BasketCaptainGrant>()
                .eq(BasketCaptainGrant::getMemberId, memberId)
                .eq(BasketCaptainGrant::getGrantStatus, GRANT_STATUS_ACTIVE)
                .eq(BasketCaptainGrant::getIsDelete, NOT_DELETED)
                .isNull(BasketCaptainGrant::getTeamId)
                .orderByDesc(BasketCaptainGrant::getCreateTime)
                .orderByDesc(BasketCaptainGrant::getId));
        if (!unbound.isEmpty()) {
            return unbound.get(0);
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

    public BasketCaptainGrant findActiveGrantForDivision(Long memberId, Long divisionId) {
        return findActiveGrantForDivision(memberId, null, divisionId);
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
                .orderByDesc(BasketCaptainGrant::getCreateTime)
                .orderByDesc(BasketCaptainGrant::getId)
                .last("limit 1"));
    }

    /**
     * 按 memberId + divisionId (+ teamId) 精确匹配 ACTIVE 授权记录。
     */
    public BasketCaptainGrant findActiveGrantForDivision(Long memberId, Long teamId, Long divisionId) {
        if (memberId == null || divisionId == null) {
            return null;
        }
        LambdaQueryWrapper<BasketCaptainGrant> wrapper = new LambdaQueryWrapper<BasketCaptainGrant>()
                .eq(BasketCaptainGrant::getMemberId, memberId)
                .eq(BasketCaptainGrant::getDivisionId, divisionId)
                .eq(BasketCaptainGrant::getGrantStatus, GRANT_STATUS_ACTIVE)
                .eq(BasketCaptainGrant::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketCaptainGrant::getCreateTime)
                .orderByDesc(BasketCaptainGrant::getId);
        if (teamId != null) {
            wrapper.and(w -> w.eq(BasketCaptainGrant::getTeamId, teamId).or().isNull(BasketCaptainGrant::getTeamId));
        }
        List<BasketCaptainGrant> candidates = captainGrantDao.selectList(wrapper);
        for (BasketCaptainGrant grant : candidates) {
            if (teamId != null && grant.getTeamId() != null && !Objects.equals(grant.getTeamId(), teamId)) {
                continue;
            }
            return grant;
        }
        return null;
    }

    /**
     * 兼容旧 seasonId 入参：按 season 查询后校验 team 绑定关系。
     */
    public BasketCaptainGrant findActiveGrantForEvent(Long memberId, Long teamId, Long seasonId) {
        if (memberId == null || seasonId == null) {
            return null;
        }
        List<BasketCaptainGrant> candidates = captainGrantDao.selectList(new LambdaQueryWrapper<BasketCaptainGrant>()
                .eq(BasketCaptainGrant::getMemberId, memberId)
                .eq(BasketCaptainGrant::getSeasonId, seasonId)
                .eq(BasketCaptainGrant::getGrantStatus, GRANT_STATUS_ACTIVE)
                .eq(BasketCaptainGrant::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketCaptainGrant::getCreateTime)
                .orderByDesc(BasketCaptainGrant::getId));
        for (BasketCaptainGrant grant : candidates) {
            if (teamId != null && grant.getTeamId() != null && !Objects.equals(grant.getTeamId(), teamId)) {
                continue;
            }
            return grant;
        }
        return null;
    }

    public List<BasketCaptainGrant> findActiveGrantsForMemberAndTeam(Long memberId, Long teamId) {
        if (memberId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<BasketCaptainGrant> wrapper = new LambdaQueryWrapper<BasketCaptainGrant>()
                .eq(BasketCaptainGrant::getMemberId, memberId)
                .eq(BasketCaptainGrant::getGrantStatus, GRANT_STATUS_ACTIVE)
                .eq(BasketCaptainGrant::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketCaptainGrant::getCreateTime)
                .orderByDesc(BasketCaptainGrant::getId);
        if (teamId != null) {
            wrapper.and(w -> w.eq(BasketCaptainGrant::getTeamId, teamId).or().isNull(BasketCaptainGrant::getTeamId));
        }
        return captainGrantDao.selectList(wrapper);
    }

    public void assertGrantBoundToTeam(BasketCaptainGrant grant, Long teamId) {
        if (grant == null) {
            throw new ServiceException(NO_ACTIVE_GRANT_MESSAGE, 403);
        }
        if (grant.getTeamId() == null) {
            throw new ServiceException("队长授权尚未绑定球队，请先创建或绑定球队", 403);
        }
        if (!Objects.equals(grant.getTeamId(), teamId)) {
            throw new ServiceException("当前队长授权已绑定其他球队，无法管理该球队", 403);
        }
    }

    public void assertCanManageTeam(BasketCaptainGrant grant, BasketTeam team, Long memberId) {
        if (grant == null) {
            throw new ServiceException(NO_ACTIVE_GRANT_MESSAGE, 403);
        }
        if (grant.getTeamId() != null) {
            if (!Objects.equals(grant.getTeamId(), team.getId())) {
                throw new ServiceException("当前队长授权已绑定其他球队，无法管理该球队", 403);
            }
            return;
        }
        if (!Objects.equals(team.getCaptainMemberId(), memberId)) {
            throw new ServiceException("无权操作该球队", 403);
        }
    }

    public void bindGrantToTeam(Long memberId, Long teamId) {
        BasketCaptainGrant grant = findCurrentGrant(memberId);
        if (grant == null) {
            throw new ServiceException(NO_ACTIVE_GRANT_MESSAGE, 403);
        }
        if (grant.getTeamId() != null && !Objects.equals(grant.getTeamId(), teamId)) {
            throw new ServiceException("当前队长授权已绑定其他球队", 403);
        }
        if (Objects.equals(grant.getTeamId(), teamId)) {
            return;
        }
        if (grant.getDivisionId() == null) {
            throw new ServiceException("队长授权缺少 Division 信息，无法绑定球队", 403);
        }
        String activeKey = buildActiveUniqueKey(grant.getDivisionId(), teamId);
        captainGrantDao.update(null, new LambdaUpdateWrapper<BasketCaptainGrant>()
                .eq(BasketCaptainGrant::getId, grant.getId())
                .eq(BasketCaptainGrant::getMemberId, memberId)
                .eq(BasketCaptainGrant::getIsDelete, NOT_DELETED)
                .set(BasketCaptainGrant::getTeamId, teamId)
                .set(BasketCaptainGrant::getActiveUniqueKey, activeKey));
        grant.setTeamId(teamId);
        grant.setActiveUniqueKey(activeKey);
    }

    public void assertUnboundForCreate(BasketCaptainGrant grant) {
        if (grant == null) {
            throw new ServiceException(NO_ACTIVE_GRANT_MESSAGE, 403);
        }
        if (grant.getTeamId() != null) {
            throw new ServiceException("当前队长授权已绑定球队，请使用已有球队报名赛事", 403);
        }
    }

    public boolean isActiveStatus(BasketCaptainGrant grant) {
        return grant != null
                && Objects.equals(GRANT_STATUS_ACTIVE, grant.getGrantStatus())
                && StringUtils.hasText(grant.getActiveUniqueKey());
    }

    public static String buildActiveUniqueKey(Long divisionId, Long teamId) {
        return divisionId + "_" + teamId;
    }
}
