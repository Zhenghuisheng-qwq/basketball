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
import com.zhs.basket.vo.ScheduleMatchListVo;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    @Autowired private PlayerNoGenerator playerNoGenerator;
    @Autowired private ScheduleMatchTeamSupport matchTeamSupport;

    @Override
    public List<ScheduleMatchListVo> list(ScheduleMatchQuery query) {
        List<ScheduleMatchListVo> list = matchMapper.selectScheduleList(query != null ? query : new ScheduleMatchQuery());
        for (ScheduleMatchListVo vo : list) {
            vo.setMatchStatusName(MatchStatusEnum.fromCode(vo.getMatchStatus()).getDescription());
            vo.setPublishStatusName(MatchPublishStatusEnum.fromCode(vo.getPublishStatus()).getDescription());
        }
        return list;
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
        assertTimeConflict(null, home.getId(), req.getMatchTime());
        assertTimeConflict(null, away.getId(), req.getMatchTime());

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
        assertTimeConflict(matchId, home.getId(), req.getMatchTime());
        assertTimeConflict(matchId, away.getId(), req.getMatchTime());

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
        if (req.getPublishStatus() != null) {
            match.setPublishStatus(req.getPublishStatus());
            match.setPublicStatus(MatchPublishStatusEnum.PUBLISHED.getCode() == req.getPublishStatus() ? 1 : 0);
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
    public void saveScore(Long matchId, ScheduleScoreSaveReq req) {
        if (req == null || req.getHomeScore() == null || req.getAwayScore() == null) {
            throw new ServiceException("比分不能为空");
        }
        if (req.getHomeScore().equals(req.getAwayScore())) {
            throw new ServiceException("篮球比赛不允许平局，请确认比分");
        }
        BasketMatch match = requireMatch(matchId);
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

    private void assertTimeConflict(Long excludeMatchId, Long eventTeamId, Date matchTime) {
        if (matchMapper.countEventTeamTimeConflict(excludeMatchId, eventTeamId, matchTime) > 0) {
            throw new ServiceException("同一时间点该球队已有比赛");
        }
    }

    private boolean teamChanged(BasketMatch match, ScheduleMatchUpdateReq req) {
        return req.getHomeEventTeamId() != null && !req.getHomeEventTeamId().equals(match.getHomeEventTeamId())
                || req.getAwayEventTeamId() != null && !req.getAwayEventTeamId().equals(match.getAwayEventTeamId());
    }
}
