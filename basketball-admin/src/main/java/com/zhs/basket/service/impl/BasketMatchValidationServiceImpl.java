package com.zhs.basket.service.impl;

import com.zhs.basket.constant.BasketMatchConstants;
import com.zhs.basket.domain.BasketMatch;
import com.zhs.basket.domain.BasketMatchStatusLog;
import com.zhs.basket.domain.BasketTournamentGroup;
import com.zhs.basket.domain.BasketTournamentRegistration;
import com.zhs.basket.domain.BasketTournamentRound;
import com.zhs.basket.domain.BasketTournamentSeason;
import com.zhs.basket.domain.BasketTournamentStage;
import com.zhs.basket.enums.MatchChangeTypeEnum;
import com.zhs.basket.enums.MatchDataStatusEnum;
import com.zhs.basket.enums.MatchPublishStatusEnum;
import com.zhs.basket.enums.MatchStatusEnum;
import com.zhs.basket.mapper.BasketMatchMapper;
import com.zhs.basket.mapper.BasketMatchStatusLogMapper;
import com.zhs.basket.mapper.BasketTournamentGroupMapper;
import com.zhs.basket.mapper.BasketTournamentGroupTeamMapper;
import com.zhs.basket.mapper.BasketTournamentRegistrationMapper;
import com.zhs.basket.mapper.BasketTournamentRoundMapper;
import com.zhs.basket.mapper.BasketTournamentSeasonMapper;
import com.zhs.basket.mapper.BasketTournamentStageMapper;
import com.zhs.basket.service.IBasketMatchValidationService;
import com.zhs.basket.vo.MatchDetailVo;
import com.zhs.basket.vo.MatchListVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Date;

@Service
public class BasketMatchValidationServiceImpl implements IBasketMatchValidationService {

    private static final Integer NOT_DELETED = 0;

    @Autowired private BasketMatchMapper matchMapper;
    @Autowired private BasketMatchStatusLogMapper statusLogMapper;
    @Autowired private BasketTournamentSeasonMapper seasonMapper;
    @Autowired private BasketTournamentStageMapper stageMapper;
    @Autowired private BasketTournamentRoundMapper roundMapper;
    @Autowired private BasketTournamentGroupMapper groupMapper;
    @Autowired private BasketTournamentRegistrationMapper registrationMapper;
    @Autowired private BasketTournamentGroupTeamMapper groupTeamMapper;

    @Override
    public BasketMatch requireMatch(Long matchId) {
        BasketMatch match = matchMapper.selectById(matchId);
        if (match == null) {
            throw new ServiceException("比赛不存在");
        }
        return match;
    }

    @Override
    public MatchDetailVo requireDetail(Long matchId) {
        MatchDetailVo vo = matchMapper.selectDetailById(matchId);
        if (vo == null) {
            throw new ServiceException("比赛不存在");
        }
        return enrichDetailVo(vo);
    }

    @Override
    public void validateTeamsForSeason(Long seasonId, Long homeTeamId, Long awayTeamId) {
        if (homeTeamId == null || awayTeamId == null) {
            throw new ServiceException("主客队不能为空");
        }
        if (homeTeamId.equals(awayTeamId)) {
            throw new ServiceException("主客队不能相同");
        }
        BasketTournamentRegistration homeReg = registrationMapper.selectApprovedBySeasonAndTeam(seasonId, homeTeamId);
        if (homeReg == null) {
            throw new ServiceException("主队未通过本赛季报名审核");
        }
        BasketTournamentRegistration awayReg = registrationMapper.selectApprovedBySeasonAndTeam(seasonId, awayTeamId);
        if (awayReg == null) {
            throw new ServiceException("客队未通过本赛季报名审核");
        }
    }

    @Override
    public void validateTeamsInGroup(Long groupId, Long homeTeamId, Long awayTeamId) {
        if (groupId == null) {
            return;
        }
        if (groupTeamMapper.countByGroupIdAndTeamId(groupId, homeTeamId) <= 0) {
            throw new ServiceException("主队不在该分组内");
        }
        if (groupTeamMapper.countByGroupIdAndTeamId(groupId, awayTeamId) <= 0) {
            throw new ServiceException("客队不在该分组内");
        }
    }

    @Override
    public void validateHierarchy(Long seasonId, Long stageId, Long roundId, Long groupId) {
        BasketTournamentSeason season = seasonMapper.selectById(seasonId);
        if (season == null) {
            throw new ServiceException("赛季不存在");
        }
        BasketTournamentStage stage = stageMapper.selectById(stageId);
        if (stage == null || !seasonId.equals(stage.getSeasonId())) {
            throw new ServiceException("阶段不存在或不属于该赛季");
        }
        if (roundId != null) {
            BasketTournamentRound round = roundMapper.selectById(roundId);
            if (round == null || !stageId.equals(round.getStageId())) {
                throw new ServiceException("轮次不存在或不属于该阶段");
            }
        }
        if (groupId != null) {
            BasketTournamentGroup group = groupMapper.selectById(groupId);
            if (group == null || !stageId.equals(group.getStageId())) {
                throw new ServiceException("分组不存在或不属于该阶段");
            }
        }
    }

    @Override
    public void checkScheduleConflict(Long excludeMatchId, Long venueId, Long homeTeamId, Long awayTeamId,
                                      Date startTime, Date endTime) {
        if (startTime == null) {
            throw new ServiceException("计划开始时间不能为空");
        }
        Date effectiveEnd = resolveEndTime(startTime, endTime);
        if (!effectiveEnd.after(startTime)) {
            throw new ServiceException("计划结束时间须晚于开始时间");
        }
        if (matchMapper.countTeamScheduleConflict(excludeMatchId, homeTeamId, startTime, effectiveEnd) > 0) {
            throw new ServiceException("主队在该时间段已有其他比赛");
        }
        if (matchMapper.countTeamScheduleConflict(excludeMatchId, awayTeamId, startTime, effectiveEnd) > 0) {
            throw new ServiceException("客队在该时间段已有其他比赛");
        }
        if (venueId != null && matchMapper.countVenueScheduleConflict(excludeMatchId, venueId, startTime, effectiveEnd) > 0) {
            throw new ServiceException("场地在该时间段已被占用");
        }
    }

    @Override
    public Date resolveEndTime(Date startTime, Date endTime) {
        if (endTime != null) {
            return endTime;
        }
        return new Date(startTime.getTime() + BasketMatchConstants.DEFAULT_MATCH_DURATION_MS);
    }

    @Override
    public void writeStatusLog(BasketMatch match, Integer beforeStatus, MatchChangeTypeEnum changeType, String reason) {
        insertStatusLog(match, beforeStatus, changeType, reason, SecurityUtils.getUserId(), SecurityUtils.getUsername());
    }

    @Override
    public void writeSystemStatusLog(BasketMatch match, Integer beforeStatus, MatchChangeTypeEnum changeType, String reason) {
        insertStatusLog(match, beforeStatus, changeType, reason, null, "SYSTEM");
    }

    private void insertStatusLog(BasketMatch match, Integer beforeStatus, MatchChangeTypeEnum changeType, String reason,
                                 Long operatorId, String operatorName) {
        Date now = new Date();
        BasketMatchStatusLog log = new BasketMatchStatusLog();
        log.setMatchId(match.getId());
        log.setBeforeStatus(beforeStatus);
        log.setAfterStatus(match.getMatchStatus());
        log.setChangeType(changeType.getCode());
        log.setChangeReason(reason);
        log.setOperatorId(operatorId);
        log.setOperatorName(operatorName);
        log.setChangeTime(now);
        log.setCreateTime(now);
        log.setUpdateTime(now);
        log.setIsDelete(NOT_DELETED);
        statusLogMapper.insert(log);
    }

    @Override
    public MatchListVo enrichListVo(MatchListVo vo) {
        MatchStatusEnum status = MatchStatusEnum.fromCode(vo.getMatchStatus());
        MatchDataStatusEnum dataStatus = resolveDataStatus(vo.getDataStatus());
        MatchStatusEnum displayStatus = resolveDisplayMatchStatus(status, vo.getPublishStatus(), vo.getScheduledStartTime());
        vo.setMatchStatusName(displayStatus.getDescription());
        vo.setDataStatusName(resolveDataStatusDisplayName(dataStatus));
        applyPublishContext(vo, status, dataStatus, displayStatus);
        return vo;
    }

    @Override
    public MatchDetailVo enrichDetailVo(MatchDetailVo vo) {
        MatchStatusEnum status = MatchStatusEnum.fromCode(vo.getMatchStatus());
        MatchDataStatusEnum dataStatus = resolveDataStatus(vo.getDataStatus());
        MatchStatusEnum displayStatus = resolveDisplayMatchStatus(status, vo.getPublishStatus(), vo.getScheduledStartTime());
        vo.setMatchStatusName(displayStatus.getDescription());
        vo.setDataStatusName(resolveDataStatusDisplayName(dataStatus));
        applyPublishContext(vo, status, dataStatus, displayStatus);
        vo.setCanConfirmResult(dataStatus.canConfirm() && status == MatchStatusEnum.FINISHED);
        vo.setCanReopenResult(dataStatus.canReopen());
        return vo;
    }

    private MatchDataStatusEnum resolveDataStatus(Integer code) {
        if (code == null) {
            return MatchDataStatusEnum.DRAFT;
        }
        return MatchDataStatusEnum.fromCode(code);
    }

    private MatchStatusEnum resolveDisplayMatchStatus(MatchStatusEnum status, Integer publishStatus, Date scheduledStartTime) {
        if (status == MatchStatusEnum.CANCELLED || status == MatchStatusEnum.POSTPONED) {
            return status;
        }
        if (publishStatus != null && publishStatus == MatchPublishStatusEnum.UNPUBLISHED.getCode()) {
            return status;
        }
        if (status == MatchStatusEnum.SCHEDULED
                && scheduledStartTime != null
                && !scheduledStartTime.after(new Date())) {
            return MatchStatusEnum.FINISHED;
        }
        return status;
    }

    private String resolveDataStatusDisplayName(MatchDataStatusEnum dataStatus) {
        return switch (dataStatus) {
            case DRAFT -> "未提交";
            case SUBMITTED -> "审核中";
            case CONFIRMED -> "已通过";
            case REOPENED -> "已重开";
            case REJECTED -> "已驳回";
        };
    }

    private void applyPublishContext(MatchDetailVo vo, MatchStatusEnum status, MatchDataStatusEnum dataStatus,
            MatchStatusEnum displayStatus) {
        applyPublishContextFields(vo.getId(), vo.getPublishStatus(), status, dataStatus, displayStatus, new PublishContextMutator() {
            @Override public void setPublishStatusName(String v) { vo.setPublishStatusName(v); }
            @Override public void setOperationalStatusName(String v) { vo.setOperationalStatusName(v); }
            @Override public void setCanEdit(Boolean v) { vo.setCanEdit(v); }
            @Override public void setCanSchedule(Boolean v) { vo.setCanSchedule(v); }
            @Override public void setCanStart(Boolean v) { vo.setCanStart(v); }
            @Override public void setCanPostpone(Boolean v) { vo.setCanPostpone(v); }
            @Override public void setCanCancel(Boolean v) { vo.setCanCancel(v); }
            @Override public void setCanDelete(Boolean v) { vo.setCanDelete(v); }
            @Override public void setCanEnterLineup(Boolean v) { vo.setCanEnterLineup(v); }
            @Override public void setCanEnterScore(Boolean v) { vo.setCanEnterScore(v); }
            @Override public void setCanEnterStat(Boolean v) { vo.setCanEnterStat(v); }
        });
    }

    private void applyPublishContext(MatchListVo vo, MatchStatusEnum status, MatchDataStatusEnum dataStatus,
            MatchStatusEnum displayStatus) {
        applyPublishContextFields(vo.getId(), vo.getPublishStatus(), status, dataStatus, displayStatus, new PublishContextMutator() {
            @Override public void setPublishStatusName(String v) { vo.setPublishStatusName(v); }
            @Override public void setOperationalStatusName(String v) { vo.setOperationalStatusName(v); }
            @Override public void setCanEdit(Boolean v) { vo.setCanEdit(v); }
            @Override public void setCanSchedule(Boolean v) { vo.setCanSchedule(v); }
            @Override public void setCanStart(Boolean v) { vo.setCanStart(v); }
            @Override public void setCanPostpone(Boolean v) { vo.setCanPostpone(v); }
            @Override public void setCanCancel(Boolean v) { vo.setCanCancel(v); }
            @Override public void setCanDelete(Boolean v) { vo.setCanDelete(v); }
            @Override public void setCanEnterLineup(Boolean v) { vo.setCanEnterLineup(v); }
            @Override public void setCanEnterScore(Boolean v) { vo.setCanEnterScore(v); }
            @Override public void setCanEnterStat(Boolean v) { vo.setCanEnterStat(v); }
        });
    }

    private interface PublishContextMutator {
        void setPublishStatusName(String v);
        void setOperationalStatusName(String v);
        void setCanEdit(Boolean v);
        void setCanSchedule(Boolean v);
        void setCanStart(Boolean v);
        void setCanPostpone(Boolean v);
        void setCanCancel(Boolean v);
        void setCanDelete(Boolean v);
        void setCanEnterLineup(Boolean v);
        void setCanEnterScore(Boolean v);
        void setCanEnterStat(Boolean v);
    }

    private void applyPublishContextFields(Long matchId, Integer publishStatusCode, MatchStatusEnum status,
            MatchDataStatusEnum dataStatus, MatchStatusEnum displayStatus, PublishContextMutator mutator) {
        if (publishStatusCode == null) {
            mutator.setPublishStatusName("-");
        } else {
            mutator.setPublishStatusName(MatchPublishStatusEnum.fromCode(publishStatusCode).getDescription());
        }
        MatchPublishStatusEnum publishStatus = publishStatusCode == null
                ? null
                : MatchPublishStatusEnum.fromCode(publishStatusCode);
        mutator.setOperationalStatusName(resolveOperationalStatusName(status, publishStatus, displayStatus.getDescription()));
        boolean publishedForOps = isPublishedForOps(publishStatusCode);
        boolean hasRoster = matchId != null && matchMapper.countRosterByMatchId(matchId) > 0;
        boolean hasScoreData = matchId != null && matchMapper.countPeriodScoreByMatchId(matchId) > 0;
        boolean hasStat = matchId != null && matchMapper.countTeamStatByMatchId(matchId) > 0;
        mutator.setCanEdit(publishedForOps && status.canEdit() && dataStatus.canEdit());
        mutator.setCanSchedule(publishedForOps && status.canSchedule() && dataStatus.canEdit());
        mutator.setCanStart(publishedForOps && status.canStart());
        mutator.setCanPostpone(publishedForOps && status.canPostpone());
        mutator.setCanCancel(publishedForOps && status.canCancel());
        mutator.setCanDelete(status.canDelete() && !hasRoster && !hasScoreData && !hasStat);
        mutator.setCanEnterLineup(publishedForOps && status.canEnterLineup());
        mutator.setCanEnterScore(publishedForOps && status.canEnterScore() && dataStatus.canEdit());
        mutator.setCanEnterStat(publishedForOps && status.canEnterStat() && dataStatus.canEdit());
    }

    private String resolveOperationalStatusName(MatchStatusEnum status, MatchPublishStatusEnum publishStatus,
            String matchStatusName) {
        if (status == MatchStatusEnum.CANCELLED) {
            return MatchStatusEnum.CANCELLED.getDescription();
        }
        if (publishStatus == MatchPublishStatusEnum.UNPUBLISHED) {
            return MatchPublishStatusEnum.UNPUBLISHED.getDescription();
        }
        return matchStatusName;
    }

    private boolean isPublishedForOps(Integer publishStatus) {
        return publishStatus == null || publishStatus != MatchPublishStatusEnum.UNPUBLISHED.getCode();
    }

    public void requirePublishedForOps(BasketMatch match) {
        if (match.getPublishStatus() != null
                && match.getPublishStatus() == MatchPublishStatusEnum.UNPUBLISHED.getCode()) {
            throw new ServiceException("赛程已下架，请先重新发布后再操作");
        }
    }

    @Override
    public BigDecimal computePct(Integer made, Integer attempted) {
        int m = made != null ? made : 0;
        int a = attempted != null ? attempted : 0;
        if (a <= 0) {
            return BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP);
        }
        return BigDecimal.valueOf(m).divide(BigDecimal.valueOf(a), 4, RoundingMode.HALF_UP);
    }

    @Override
    public int computePointsFromShooting(Integer fgm, Integer tpm, Integer ftm) {
        int fg = fgm != null ? fgm : 0;
        int tp = tpm != null ? tpm : 0;
        int ft = ftm != null ? ftm : 0;
        return 2 * (fg - tp) + 3 * tp + ft;
    }

    @Override
    public void validateShooting(Integer made, Integer attempted, String label) {
        int m = made != null ? made : 0;
        int a = attempted != null ? attempted : 0;
        if (m > a) {
            throw new ServiceException(label + "命中数不能大于出手数");
        }
    }

    @Override
    public void validateThreeWithinFg(Integer fgm, Integer tpm, String label) {
        int fg = fgm != null ? fgm : 0;
        int tp = tpm != null ? tpm : 0;
        if (tp > fg) {
            throw new ServiceException(label + "三分命中数不能大于投篮命中数");
        }
    }

    @Override
    public void validateTheoreticalPoints(Integer points, Integer fgm, Integer tpm, Integer ftm, String label) {
        int theoretical = computePointsFromShooting(fgm, tpm, ftm);
        int p = points != null ? points : 0;
        if (p != theoretical) {
            throw new ServiceException(label + "得分与投篮数据不一致，理论得分=" + theoretical);
        }
    }
}
