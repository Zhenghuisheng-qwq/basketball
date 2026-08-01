package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketMatch;
import com.zhs.basket.domain.BasketTeamSeasonProfile;
import com.zhs.basket.domain.BasketTournamentStage;
import com.zhs.basket.enums.MatchDataStatusEnum;
import com.zhs.basket.enums.MatchPublishStatusEnum;
import com.zhs.basket.enums.MatchStatusEnum;
import com.zhs.basket.enums.ScheduleAuditActionTypeEnum;
import com.zhs.basket.mapper.BasketMatchMapper;
import com.zhs.basket.mapper.BasketMatchTeamMapper;
import com.zhs.basket.mapper.BasketTournamentStageMapper;
import com.zhs.basket.query.ScheduleMatchQuery;
import com.zhs.basket.req.AuditFieldInput;
import com.zhs.basket.req.ScheduleMatchCreateReq;
import com.zhs.basket.req.ScheduleMatchUpdateReq;
import com.zhs.basket.req.ScheduleScoreSaveReq;
import com.zhs.basket.schedule.model.EventTeamScheduleSnapshot;
import com.zhs.basket.service.IBasketScheduleMatchAdminService;
import com.zhs.basket.support.PlayerNoGenerator;
import com.zhs.basket.support.ScheduleAuditSupport;
import com.zhs.basket.support.ScheduleEventTeamSupport;
import com.zhs.basket.support.ScheduleMatchTeamSupport;
import com.zhs.basket.support.ScheduleMatchValidationSupport;
import com.zhs.basket.vo.ScheduleMatchDetailVo;
import com.zhs.basket.vo.ScheduleMatchListVo;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Service
public class BasketScheduleMatchAdminServiceImpl implements IBasketScheduleMatchAdminService {

    @Autowired private BasketMatchMapper matchMapper;
    @Autowired private BasketMatchTeamMapper matchTeamMapper;
    @Autowired private BasketTournamentStageMapper stageMapper;
    @Autowired private ScheduleEventTeamSupport eventTeamSupport;
    @Autowired private ScheduleAuditSupport auditSupport;
    @Autowired private ScheduleMatchValidationSupport validationSupport;
    @Autowired private PlayerNoGenerator playerNoGenerator;
    @Autowired private ScheduleMatchTeamSupport matchTeamSupport;

    @Override
    public List<ScheduleMatchListVo> list(ScheduleMatchQuery query) {
        List<ScheduleMatchListVo> list = matchMapper.selectScheduleList(query != null ? query : new ScheduleMatchQuery());
        for (ScheduleMatchListVo vo : list) {
            fillActionFlags(vo);
        }
        return list;
    }

    @Override
    public ScheduleMatchDetailVo getDetail(Long matchId) {
        ScheduleMatchDetailVo vo = matchMapper.selectScheduleDetailById(matchId);
        if (vo == null) {
            throw new ServiceException("比赛不存在");
        }
        fillActionFlags(vo);
        return vo;
    }

    private void fillActionFlags(ScheduleMatchListVo vo) {
        vo.setMatchStatusName(MatchStatusEnum.fromCode(vo.getMatchStatus()).getDescription());
        vo.setPublishStatusName(MatchPublishStatusEnum.fromCode(vo.getPublishStatus()).getDescription());
        if (vo.getDataAbnormal() != null && vo.getDataAbnormal() == 1) {
            if (vo.getHomeTeamName() == null || vo.getHomeTeamName().isBlank()) {
                vo.setHomeTeamName("历史数据异常");
            }
            if (vo.getAwayTeamName() == null || vo.getAwayTeamName().isBlank()) {
                vo.setAwayTeamName("历史数据异常");
            }
        }
        boolean draft = vo.getPublishStatus() != null
                && vo.getPublishStatus() == MatchPublishStatusEnum.DRAFT.getCode();
        boolean published = vo.getPublishStatus() != null
                && vo.getPublishStatus() == MatchPublishStatusEnum.PUBLISHED.getCode();
        boolean editableStatus = vo.getMatchStatus() != null
                && vo.getMatchStatus() != MatchStatusEnum.FINISHED.getCode()
                && vo.getMatchStatus() != MatchStatusEnum.CANCELLED.getCode();
        boolean scoreableStatus = vo.getMatchStatus() != null
                && (vo.getMatchStatus() == MatchStatusEnum.SCHEDULED.getCode()
                || vo.getMatchStatus() == MatchStatusEnum.RUNNING.getCode()
                || vo.getMatchStatus() == MatchStatusEnum.FINISHED.getCode());
        vo.setCanEdit(draft && editableStatus && (vo.getDataAbnormal() == null || vo.getDataAbnormal() == 0));
        vo.setCanDelete(draft && editableStatus && (vo.getDataAbnormal() == null || vo.getDataAbnormal() == 0));
        vo.setCanSwap(canSwapMatch(draft, editableStatus, vo.getDataAbnormal(), vo.getHomeEventTeamId(), vo.getAwayEventTeamId()));
        vo.setCanScore(published && scoreableStatus && (vo.getDataAbnormal() == null || vo.getDataAbnormal() == 0));
    }

    private void fillActionFlags(ScheduleMatchDetailVo vo) {
        vo.setMatchStatusName(MatchStatusEnum.fromCode(vo.getMatchStatus()).getDescription());
        vo.setPublishStatusName(MatchPublishStatusEnum.fromCode(vo.getPublishStatus()).getDescription());
        if (vo.getDataAbnormal() != null && vo.getDataAbnormal() == 1) {
            if (vo.getHomeTeamName() == null || vo.getHomeTeamName().isBlank()) {
                vo.setHomeTeamName("历史数据异常");
            }
            if (vo.getAwayTeamName() == null || vo.getAwayTeamName().isBlank()) {
                vo.setAwayTeamName("历史数据异常");
            }
        }
        boolean draft = vo.getPublishStatus() != null
                && vo.getPublishStatus() == MatchPublishStatusEnum.DRAFT.getCode();
        boolean published = vo.getPublishStatus() != null
                && vo.getPublishStatus() == MatchPublishStatusEnum.PUBLISHED.getCode();
        boolean editableStatus = vo.getMatchStatus() != null
                && vo.getMatchStatus() != MatchStatusEnum.FINISHED.getCode()
                && vo.getMatchStatus() != MatchStatusEnum.CANCELLED.getCode();
        boolean scoreableStatus = vo.getMatchStatus() != null
                && (vo.getMatchStatus() == MatchStatusEnum.SCHEDULED.getCode()
                || vo.getMatchStatus() == MatchStatusEnum.RUNNING.getCode()
                || vo.getMatchStatus() == MatchStatusEnum.FINISHED.getCode());
        vo.setCanEdit(draft && editableStatus && (vo.getDataAbnormal() == null || vo.getDataAbnormal() == 0));
        vo.setCanDelete(draft && editableStatus && (vo.getDataAbnormal() == null || vo.getDataAbnormal() == 0));
        vo.setCanSwap(canSwapMatch(draft, editableStatus, vo.getDataAbnormal(), vo.getHomeEventTeamId(), vo.getAwayEventTeamId()));
        vo.setCanScore(published && scoreableStatus && (vo.getDataAbnormal() == null || vo.getDataAbnormal() == 0));
    }

    private boolean canSwapMatch(boolean draft, boolean editableStatus, Integer dataAbnormal,
            Long homeEventTeamId, Long awayEventTeamId) {
        return draft && editableStatus
                && (dataAbnormal == null || dataAbnormal == 0)
                && homeEventTeamId != null && awayEventTeamId != null;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createManual(ScheduleMatchCreateReq req) {
        validateCreateReq(req);
        Long eventId = req.getEventId();
        String platformCode = eventTeamSupport.resolvePlatformCode(eventId);
        BasketTeamSeasonProfile home = eventTeamSupport.requireActiveEventTeam(req.getHomeEventTeamId(), eventId);
        BasketTeamSeasonProfile away = eventTeamSupport.requireActiveEventTeam(req.getAwayEventTeamId(), eventId);
        if (home.getId().equals(away.getId())) {
            throw new ServiceException("主队和客队不能相同");
        }
        assertPairUnique(eventId, home.getId(), away.getId(), null);
        assertScheduleConstraints(null, eventId, home.getId(), away.getId(), req.getMatchTime());

        Long stageId = resolveStageId(req.getStageId(), eventId);
        EventTeamScheduleSnapshot homeSnapshot = eventTeamSupport.toSnapshot(home);
        EventTeamScheduleSnapshot awaySnapshot = eventTeamSupport.toSnapshot(away);
        Date now = new Date();

        BasketMatch match = new BasketMatch();
        match.setMatchNo(playerNoGenerator.generateBizNo("M"));
        match.setSeasonId(eventId);
        match.setPlatformCode(platformCode);
        match.setStageId(stageId);
        match.setRoundNo(req.getRoundNo());
        match.setHomeTeamId(home.getTeamId());
        match.setAwayTeamId(away.getTeamId());
        match.setHomeEventTeamId(home.getId());
        match.setAwayEventTeamId(away.getId());
        match.setHomeTeamNameSnapshot(homeSnapshot.getTeamName());
        match.setAwayTeamNameSnapshot(awaySnapshot.getTeamName());
        match.setHomeTeamLogoSnapshot(homeSnapshot.getLogoUrl());
        match.setAwayTeamLogoSnapshot(awaySnapshot.getLogoUrl());
        match.setScheduledStartTime(req.getMatchTime());
        validationSupport.assertMatchTimeByRule(match, req.getMatchTime());
        match.setMatchStatus(req.getMatchStatus() != null ? req.getMatchStatus() : MatchStatusEnum.SCHEDULED.getCode());
        match.setPublishStatus(req.getPublishStatus() != null ? req.getPublishStatus() : MatchPublishStatusEnum.DRAFT.getCode());
        match.setPublicStatus(MatchPublishStatusEnum.PUBLISHED.getCode() == match.getPublishStatus() ? 1 : 0);
        match.setHomeScore(0);
        match.setAwayScore(0);
        match.setOvertimeCount(0);
        match.setDataStatus(MatchDataStatusEnum.DRAFT.getCode());
        match.setScheduleRemark(req.getRemark());
        match.setSortNo(0);
        match.setCreateTime(now);
        match.setUpdateTime(now);
        match.setIsDelete(0);
        matchMapper.insertScheduleMatch(match);
        matchTeamSupport.persistMatchTeams(match);

        auditSupport.logMatchAction(match.getId(), eventId, match.getScheduleBatchId(),
                ScheduleAuditActionTypeEnum.CREATE_MATCH, "手动创建比赛", req.getRemark(), null);
        return match.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long matchId, ScheduleMatchUpdateReq req) {
        BasketMatch match = requireMatch(matchId);
        validationSupport.assertDraftEditable(match);
        Long eventId = match.getSeasonId();
        MatchStatusEnum status = MatchStatusEnum.fromCode(match.getMatchStatus());
        boolean teamsWillChange = teamChanged(match, req);
        if (status == MatchStatusEnum.FINISHED && teamsWillChange) {
            throw new ServiceException("已结束比赛不允许修改主客队");
        }

        BasketTeamSeasonProfile home = eventTeamSupport.requireActiveEventTeam(req.getHomeEventTeamId(), eventId);
        BasketTeamSeasonProfile away = eventTeamSupport.requireActiveEventTeam(req.getAwayEventTeamId(), eventId);
        if (home.getId().equals(away.getId())) {
            throw new ServiceException("主队和客队不能相同");
        }
        assertPairUnique(eventId, home.getId(), away.getId(), matchId);
        assertScheduleConstraints(matchId, eventId, home.getId(), away.getId(), req.getMatchTime());
        validationSupport.assertMatchTimeByRule(match, req.getMatchTime());

        List<AuditFieldInput> fields = new ArrayList<>();
        if (teamsWillChange) {
            fields.add(auditSupport.field("homeEventTeamId", "主队", match.getHomeEventTeamId(), home.getId()));
            fields.add(auditSupport.field("awayEventTeamId", "客队", match.getAwayEventTeamId(), away.getId()));
        }
        if (match.getScheduledStartTime() != null && req.getMatchTime() != null
                && match.getScheduledStartTime().getTime() != req.getMatchTime().getTime()) {
            fields.add(auditSupport.field("matchTime", "比赛时间", match.getScheduledStartTime(), req.getMatchTime()));
        }

        EventTeamScheduleSnapshot homeSnapshot = eventTeamSupport.toSnapshot(home);
        EventTeamScheduleSnapshot awaySnapshot = eventTeamSupport.toSnapshot(away);
        match.setStageId(resolveStageId(req.getStageId(), eventId));
        match.setRoundNo(req.getRoundNo());
        match.setHomeTeamId(home.getTeamId());
        match.setAwayTeamId(away.getTeamId());
        match.setHomeEventTeamId(home.getId());
        match.setAwayEventTeamId(away.getId());
        match.setHomeTeamNameSnapshot(homeSnapshot.getTeamName());
        match.setAwayTeamNameSnapshot(awaySnapshot.getTeamName());
        match.setHomeTeamLogoSnapshot(homeSnapshot.getLogoUrl());
        match.setAwayTeamLogoSnapshot(awaySnapshot.getLogoUrl());
        match.setScheduledStartTime(req.getMatchTime());
        if (req.getMatchStatus() != null) {
            match.setMatchStatus(req.getMatchStatus());
        }
        match.setScheduleRemark(req.getRemark());
        matchMapper.updateScheduleMatch(match);
        matchTeamSupport.replaceMatchTeams(match);

        ScheduleAuditActionTypeEnum action = teamsWillChange
                ? ScheduleAuditActionTypeEnum.UPDATE_TEAMS : ScheduleAuditActionTypeEnum.UPDATE_MATCH;
        auditSupport.logMatchAction(matchId, eventId, match.getScheduleBatchId(), action,
                "更新比赛", req.getReason(), fields.isEmpty() ? null : fields);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void swapHomeAway(Long matchId, String reason) {
        BasketMatch match = requireMatch(matchId);
        validationSupport.assertDraftEditable(match);
        eventTeamSupport.resolveEventTeamBindingIfMissing(match);
        eventTeamSupport.requireSwappableEventTeams(match);

        Long oldHomeEventTeamId = match.getHomeEventTeamId();
        Long oldAwayEventTeamId = match.getAwayEventTeamId();
        Long oldHomeTeamId = match.getHomeTeamId();
        Long oldAwayTeamId = match.getAwayTeamId();
        String oldHomeName = match.getHomeTeamNameSnapshot();
        String oldAwayName = match.getAwayTeamNameSnapshot();
        String oldHomeLogo = match.getHomeTeamLogoSnapshot();
        String oldAwayLogo = match.getAwayTeamLogoSnapshot();

        match.setHomeEventTeamId(oldAwayEventTeamId);
        match.setAwayEventTeamId(oldHomeEventTeamId);
        match.setHomeTeamId(oldAwayTeamId);
        match.setAwayTeamId(oldHomeTeamId);
        match.setHomeTeamNameSnapshot(oldAwayName);
        match.setAwayTeamNameSnapshot(oldHomeName);
        match.setHomeTeamLogoSnapshot(oldAwayLogo);
        match.setAwayTeamLogoSnapshot(oldHomeLogo);
        matchMapper.updateScheduleMatch(match);
        matchTeamSupport.replaceMatchTeams(match);

        List<AuditFieldInput> fields = new ArrayList<>();
        fields.add(auditSupport.field("homeEventTeamId", "主队", oldHomeEventTeamId, oldAwayEventTeamId));
        fields.add(auditSupport.field("awayEventTeamId", "客队", oldAwayEventTeamId, oldHomeEventTeamId));
        auditSupport.logMatchAction(matchId, match.getSeasonId(), match.getScheduleBatchId(),
                ScheduleAuditActionTypeEnum.UPDATE_TEAMS, "主客队互换",
                reason != null ? reason : "主客队互换", fields);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDraftMatch(Long matchId, String reason) {
        BasketMatch match = requireMatch(matchId);
        validationSupport.assertDraftEditable(match);
        if (matchMapper.softDeleteDraftMatch(matchId) <= 0) {
            throw new ServiceException("仅草稿比赛可删除");
        }
        matchTeamMapper.softDeleteByMatchId(matchId);
        auditSupport.logMatchAction(matchId, match.getSeasonId(), match.getScheduleBatchId(),
                ScheduleAuditActionTypeEnum.DELETE_MATCH, "删除草稿比赛", reason, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveScore(Long matchId, ScheduleScoreSaveReq req) {
        if (req == null || req.getHomeScore() == null || req.getAwayScore() == null) {
            throw new ServiceException("比分不能为空");
        }
        if (req.getHomeScore().equals(req.getAwayScore())) {
            throw new ServiceException("篮球比赛不允许平局，请确认比分");
        }
        BasketMatch match = requireMatch(matchId);
        if (match.getPublishStatus() == null
                || match.getPublishStatus() != MatchPublishStatusEnum.PUBLISHED.getCode()) {
            throw new ServiceException("草稿赛程不允许录入比分，请先发布赛程");
        }
        MatchStatusEnum status = MatchStatusEnum.fromCode(match.getMatchStatus());
        if (status == MatchStatusEnum.CANCELLED) {
            throw new ServiceException("已取消比赛不能录入比分");
        }
        if (match.getHomeEventTeamId() == null || match.getAwayEventTeamId() == null) {
            throw new ServiceException("比赛缺少主客队 EventTeam，无法录入比分");
        }
        Integer beforeHome = match.getHomeScore();
        Integer beforeAway = match.getAwayScore();

        Long winnerTeamId;
        Long winnerEventTeamId;
        if (req.getHomeScore() > req.getAwayScore()) {
            winnerTeamId = match.getHomeTeamId();
            winnerEventTeamId = match.getHomeEventTeamId();
        } else {
            winnerTeamId = match.getAwayTeamId();
            winnerEventTeamId = match.getAwayEventTeamId();
        }

        matchMapper.updateScheduleScore(matchId, req.getHomeScore(), req.getAwayScore(), winnerTeamId, winnerEventTeamId,
                MatchStatusEnum.FINISHED.getCode(), MatchDataStatusEnum.CONFIRMED.getCode(), new Date());

        ScheduleAuditActionTypeEnum action = beforeHome == 0 && beforeAway == 0
                ? ScheduleAuditActionTypeEnum.SAVE_SCORE : ScheduleAuditActionTypeEnum.UPDATE_SCORE;
        auditSupport.logMatchAction(matchId, match.getSeasonId(), match.getScheduleBatchId(), action,
                "录入比分", req.getReason(), auditSupport.singleField("score", "比分",
                        beforeHome + ":" + beforeAway, req.getHomeScore() + ":" + req.getAwayScore()));
    }

    private void validateCreateReq(ScheduleMatchCreateReq req) {
        if (req == null || req.getEventId() == null) {
            throw new ServiceException("请选择赛事");
        }
        if (req.getHomeEventTeamId() == null || req.getAwayEventTeamId() == null) {
            throw new ServiceException("请选择主客队");
        }
        if (req.getMatchTime() == null) {
            throw new ServiceException("请填写比赛时间");
        }
        eventTeamSupport.requireSeason(req.getEventId());
    }

    private BasketMatch requireMatch(Long matchId) {
        BasketMatch match = matchMapper.selectById(matchId);
        if (match == null) {
            throw new ServiceException("比赛不存在");
        }
        return match;
    }

    private Long resolveStageId(Long stageId, Long eventId) {
        if (stageId == null) {
            throw new ServiceException("请选择赛事阶段");
        }
        BasketTournamentStage stage = stageMapper.selectById(stageId);
        if (stage == null || !eventId.equals(stage.getSeasonId())) {
            throw new ServiceException("阶段不属于当前赛事");
        }
        return stageId;
    }

    private void assertPairUnique(Long eventId, Long homeEventTeamId, Long awayEventTeamId, Long excludeMatchId) {
        if (matchMapper.countEventTeamPair(eventId, homeEventTeamId, awayEventTeamId, excludeMatchId) > 0) {
            throw new ServiceException("该对阵比赛已存在");
        }
    }

    private void assertScheduleConstraints(Long excludeMatchId, Long eventId, Long homeEventTeamId,
            Long awayEventTeamId, Date matchTime) {
        assertTimeConflict(excludeMatchId, homeEventTeamId, matchTime);
        assertTimeConflict(excludeMatchId, awayEventTeamId, matchTime);
        assertDayConflict(excludeMatchId, eventId, homeEventTeamId, matchTime);
        assertDayConflict(excludeMatchId, eventId, awayEventTeamId, matchTime);
    }

    private void assertTimeConflict(Long excludeMatchId, Long eventTeamId, Date matchTime) {
        if (matchMapper.countEventTeamTimeConflict(excludeMatchId, eventTeamId, matchTime) > 0) {
            throw new ServiceException("同一时间点该球队已有比赛");
        }
    }

    private void assertDayConflict(Long excludeMatchId, Long eventId, Long eventTeamId, Date matchTime) {
        LocalDate day = validationSupport.toLocalDate(matchTime);
        Date dayStart = Date.from(day.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date dayEnd = Date.from(day.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());
        if (matchMapper.countEventTeamDayConflict(excludeMatchId, eventTeamId, eventId, dayStart, dayEnd) > 0) {
            throw new ServiceException("同一球队同一比赛日最多打一场");
        }
    }

    private boolean teamChanged(BasketMatch match, ScheduleMatchUpdateReq req) {
        return req.getHomeEventTeamId() != null && !req.getHomeEventTeamId().equals(match.getHomeEventTeamId())
                || req.getAwayEventTeamId() != null && !req.getAwayEventTeamId().equals(match.getAwayEventTeamId());
    }
}
