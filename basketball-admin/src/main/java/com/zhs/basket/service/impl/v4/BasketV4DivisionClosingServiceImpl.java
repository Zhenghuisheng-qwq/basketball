package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketCaptainGrant;
import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.enums.CaptainGrantLogActionTypeEnum;
import com.zhs.basket.enums.TeamStatusEnum;
import com.zhs.basket.enums.v4.CaptainGrantStatusEnum;
import com.zhs.basket.enums.v4.DivisionStatusEnum;
import com.zhs.basket.mapper.BasketPlayerProfileMapper;
import com.zhs.basket.mapper.BasketTeamMapper;
import com.zhs.basket.mapper.BasketTeamMemberMapper;
import com.zhs.basket.mapper.v4.BasketCaptainGrantMapper;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketTeamLifecycleClosingMapper;
import com.zhs.basket.service.v4.IBasketV4DivisionClosingService;
import com.zhs.basket.service.v4.IBasketV4SeasonService;
import com.zhs.basket.support.CaptainGrantLogSupport;
import com.zhs.basket.support.v4.V4PublicReadCacheEvictEvent;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class BasketV4DivisionClosingServiceImpl implements IBasketV4DivisionClosingService {

    private static final Integer NOT_DELETED = 0;
    private static final int LEGACY_ACTIVE = 1;
    private static final int LEGACY_EXPIRED = 3;
    private static final int MEMBER_ACTIVE = 1;
    private static final int MEMBER_LEFT = 0;
    private static final int INVITE_PENDING = 0;
    private static final int INVITE_EXPIRED = 3;
    private static final int APPLICATION_PENDING = 0;
    private static final int APPLICATION_CANCELLED = 3;
    private static final String LEAVE_REASON = "DIVISION_FINISHED";

    private static final Set<String> FINISHABLE_STATUSES = new HashSet<>(Arrays.asList(
            DivisionStatusEnum.REGISTRATION_CLOSED.getCode(),
            DivisionStatusEnum.SCHEDULED.getCode(),
            DivisionStatusEnum.PLAYING.getCode()
    ));

    @Autowired
    private BasketDivisionMapper divisionMapper;
    @Autowired
    private BasketCaptainGrantMapper captainGrantMapper;
    @Autowired
    private BasketTeamMapper teamMapper;
    @Autowired
    private BasketTeamMemberMapper teamMemberMapper;
    @Autowired
    private BasketPlayerProfileMapper playerProfileMapper;
    @Autowired
    private BasketTeamLifecycleClosingMapper lifecycleClosingMapper;
    @Autowired
    private CaptainGrantLogSupport grantLogSupport;
    @Autowired
    @Lazy
    private IBasketV4SeasonService seasonService;
    @Autowired
    private ApplicationEventPublisher eventPublisher;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finishDivision(Long divisionId, String reason) {
        if (divisionId == null) {
            throw new ServiceException("divisionId 不能为空");
        }
        BasketDivision division = divisionMapper.selectById(divisionId);
        if (division == null || !NOT_DELETED.equals(division.getIsDelete())) {
            throw new ServiceException("Division 不存在");
        }

        String status = division.getDivisionStatus();
        if (DivisionStatusEnum.FINISHED.getCode().equals(status)
                || DivisionStatusEnum.ARCHIVED.getCode().equals(status)) {
            seasonService.refreshAggregatedStatus(division.getSeasonId());
            return;
        }
        if (!FINISHABLE_STATUSES.contains(status)) {
            throw new ServiceException("当前 Division 状态不允许结束赛事：" + status);
        }

        Date finishTime = new Date();
        int cas = lifecycleClosingMapper.casFinishDivision(divisionId, FINISHABLE_STATUSES, finishTime);
        if (cas == 0) {
            BasketDivision latest = divisionMapper.selectById(divisionId);
            if (latest != null && (DivisionStatusEnum.FINISHED.getCode().equals(latest.getDivisionStatus())
                    || DivisionStatusEnum.ARCHIVED.getCode().equals(latest.getDivisionStatus()))) {
                seasonService.refreshAggregatedStatus(latest.getSeasonId());
                return;
            }
            throw new ServiceException("结束赛事失败，Division 状态已变更");
        }

        String finishReason = StringUtils.isNotEmpty(reason) ? reason.trim() : LEAVE_REASON;

        List<BasketCaptainGrant> activeGrants = captainGrantMapper.selectActiveByDivisionId(divisionId);
        captainGrantMapper.expireActiveByDivisionId(divisionId, finishTime);
        for (BasketCaptainGrant grant : activeGrants) {
            grant.setGrantStatus(CaptainGrantStatusEnum.EXPIRED.getCode());
            grant.setActiveUniqueKey(null);
            if (grant.getValidTo() == null || grant.getValidTo().after(finishTime)) {
                grant.setValidTo(finishTime);
            }
            grantLogSupport.writeGrantLog(grant, CaptainGrantLogActionTypeEnum.EXPIRE,
                    LEGACY_ACTIVE, LEGACY_EXPIRED, finishReason, "Division 结束自动失效", "ADMIN");
        }

        List<Long> teamIds = teamMapper.selectIdsByDivisionId(divisionId);
        teamMapper.archiveActiveByDivisionId(divisionId, TeamStatusEnum.ACTIVE.getCode(),
                TeamStatusEnum.ARCHIVED.getCode(), finishTime);

        teamMemberMapper.historicalizeActiveByDivisionId(
                divisionId, MEMBER_ACTIVE, MEMBER_LEFT, finishTime, LEAVE_REASON);

        if (!CollectionUtils.isEmpty(teamIds)) {
            lifecycleClosingMapper.expirePendingInvitationsByTeamIds(
                    teamIds, INVITE_PENDING, INVITE_EXPIRED, finishTime);
            lifecycleClosingMapper.cancelPendingApplicationsByTeamIds(
                    teamIds, APPLICATION_PENDING, APPLICATION_CANCELLED, finishTime);
            playerProfileMapper.clearCurrentTeamIdByTeamIds(teamIds);
        }
        seasonService.refreshAggregatedStatus(division.getSeasonId());
        eventPublisher.publishEvent(new V4PublicReadCacheEvictEvent(
                divisionId, null, division.getSeasonId(), null, null, null));
    }
}

