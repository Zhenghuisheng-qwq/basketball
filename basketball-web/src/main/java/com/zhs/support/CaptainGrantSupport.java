package com.zhs.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.zhs.dao.BasketCaptainQualificationDao;
import com.zhs.enums.CaptainQualificationStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketCaptainQualification;
import com.zhs.model.BasketTeam;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * 队长授权（captainGrant）绑定与校验，基于 basket_captain_qualification 扩展 seasonId/teamId。
 */
@Component
public class CaptainGrantSupport {

    public static final String NO_ACTIVE_GRANT_MESSAGE =
            "当前暂无有效队长权限，队长权限由平台根据赛事缴费结果后台开通";

    private static final Integer NOT_DELETED = 0;
    private static final int IS_CURRENT = 1;

    @Resource
    private BasketCaptainQualificationDao captainQualificationDao;

    public BasketCaptainQualification findCurrentGrant(Long memberId) {
        return captainQualificationDao.selectOne(new LambdaQueryWrapper<BasketCaptainQualification>()
                .eq(BasketCaptainQualification::getMemberId, memberId)
                .eq(BasketCaptainQualification::getIsCurrent, IS_CURRENT)
                .eq(BasketCaptainQualification::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketCaptainQualification::getGrantedTime)
                .orderByDesc(BasketCaptainQualification::getId)
                .last("limit 1"));
    }

    public BasketCaptainQualification findActiveGrantForSeason(Long memberId, Long seasonId) {
        return findActiveGrantForEvent(memberId, null, seasonId);
    }

    /**
     * 按 userId + teamId + eventId(seasonId) 精确匹配队长授权记录（仅查 ACTIVE，运行时有效性由调用方校验）。
     */
    public BasketCaptainQualification findActiveGrantForEvent(Long memberId, Long teamId, Long seasonId) {
        if (memberId == null || seasonId == null) {
            return null;
        }
        List<BasketCaptainQualification> candidates = captainQualificationDao.selectList(
                new LambdaQueryWrapper<BasketCaptainQualification>()
                        .eq(BasketCaptainQualification::getMemberId, memberId)
                        .eq(BasketCaptainQualification::getSeasonId, seasonId)
                        .eq(BasketCaptainQualification::getQualificationStatus, CaptainQualificationStatusEnum.ACTIVE.getCode())
                        .eq(BasketCaptainQualification::getIsDelete, NOT_DELETED)
                        .orderByDesc(BasketCaptainQualification::getGrantedTime)
                        .orderByDesc(BasketCaptainQualification::getId));
        for (BasketCaptainQualification grant : candidates) {
            if (teamId != null && grant.getTeamId() != null && !Objects.equals(grant.getTeamId(), teamId)) {
                continue;
            }
            return grant;
        }
        return null;
    }

    /**
     * 查询会员在指定球队下的全部 ACTIVE 队长授权（teamId 为空表示尚未绑定球队）。
     */
    public List<BasketCaptainQualification> findActiveGrantsForMemberAndTeam(Long memberId, Long teamId) {
        if (memberId == null) {
            return Collections.emptyList();
        }
        LambdaQueryWrapper<BasketCaptainQualification> wrapper = new LambdaQueryWrapper<BasketCaptainQualification>()
                .eq(BasketCaptainQualification::getMemberId, memberId)
                .eq(BasketCaptainQualification::getQualificationStatus, CaptainQualificationStatusEnum.ACTIVE.getCode())
                .eq(BasketCaptainQualification::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketCaptainQualification::getGrantedTime)
                .orderByDesc(BasketCaptainQualification::getId);
        if (teamId != null) {
            wrapper.and(w -> w.eq(BasketCaptainQualification::getTeamId, teamId).or().isNull(BasketCaptainQualification::getTeamId));
        }
        return captainQualificationDao.selectList(wrapper);
    }

    public void assertGrantBoundToTeam(BasketCaptainQualification grant, Long teamId) {
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

    public void assertCanManageTeam(BasketCaptainQualification grant, BasketTeam team, Long memberId) {
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
        BasketCaptainQualification grant = findCurrentGrant(memberId);
        if (grant == null) {
            throw new ServiceException(NO_ACTIVE_GRANT_MESSAGE, 403);
        }
        if (grant.getTeamId() != null && !Objects.equals(grant.getTeamId(), teamId)) {
            throw new ServiceException("当前队长授权已绑定其他球队", 403);
        }
        if (Objects.equals(grant.getTeamId(), teamId)) {
            return;
        }
        captainQualificationDao.update(null, new LambdaUpdateWrapper<BasketCaptainQualification>()
                .eq(BasketCaptainQualification::getId, grant.getId())
                .eq(BasketCaptainQualification::getMemberId, memberId)
                .eq(BasketCaptainQualification::getIsDelete, NOT_DELETED)
                .set(BasketCaptainQualification::getTeamId, teamId));
        grant.setTeamId(teamId);
    }

    public void assertUnboundForCreate(BasketCaptainQualification grant) {
        if (grant == null) {
            throw new ServiceException(NO_ACTIVE_GRANT_MESSAGE, 403);
        }
        if (grant.getTeamId() != null) {
            throw new ServiceException("当前队长授权已绑定球队，请使用已有球队报名赛事", 403);
        }
    }

    public boolean isActiveStatus(BasketCaptainQualification grant) {
        return grant != null
                && Objects.equals(CaptainQualificationStatusEnum.ACTIVE.getCode(), grant.getQualificationStatus());
    }
}
