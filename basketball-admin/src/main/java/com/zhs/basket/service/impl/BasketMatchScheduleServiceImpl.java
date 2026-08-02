package com.zhs.basket.service.impl;

import com.zhs.basket.constant.BasketMatchConstants;
import com.zhs.basket.domain.BasketMatch;
import com.zhs.basket.domain.BasketMatchTeam;
import com.zhs.basket.enums.MatchChangeTypeEnum;
import com.zhs.basket.enums.MatchDataStatusEnum;
import com.zhs.basket.enums.MatchResultTypeEnum;
import com.zhs.basket.enums.MatchStatusEnum;
import com.zhs.basket.enums.TeamSideEnum;
import com.zhs.basket.mapper.BasketMatchMapper;
import com.zhs.basket.mapper.BasketMatchRosterMapper;
import com.zhs.basket.mapper.BasketMatchTeamMapper;
import com.zhs.basket.mapper.BasketVenueMapper;
import com.zhs.basket.query.MatchQuery;
import com.zhs.basket.req.*;
import com.zhs.basket.service.IBasketMatchScheduleService;
import com.zhs.basket.service.IBasketMatchValidationService;
import com.zhs.basket.support.BasketMatchMessageHelper;
import com.zhs.basket.support.PlayerNoGenerator;
import com.zhs.basket.vo.MatchDetailVo;
import com.zhs.basket.vo.MatchListVo;
import com.zhs.basket.vo.VenueSimpleVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.PageListUtils;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BasketMatchScheduleServiceImpl implements IBasketMatchScheduleService {

    private static final Integer NOT_DELETED = 0;

    @Autowired private BasketMatchMapper matchMapper;
    @Autowired private BasketMatchTeamMapper matchTeamMapper;
    @Autowired private BasketMatchRosterMapper rosterMapper;
    @Autowired private BasketVenueMapper venueMapper;
    @Autowired private IBasketMatchValidationService validationService;
    @Autowired private BasketMatchMessageHelper messageHelper;
    @Autowired private PlayerNoGenerator playerNoGenerator;

    @Override
    public List<MatchListVo> selectList(MatchQuery query) {
        return PageListUtils.map(matchMapper.selectList(query), validationService::enrichListVo);
    }

    @Override
    public MatchDetailVo selectById(Long matchId) {
        return validationService.requireDetail(matchId);
    }

    @Override
    public List<VenueSimpleVo> selectVenues() {
        return venueMapper.selectSimpleList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(MatchCreateReq req) {
        if (req.getScheduledStartTime() == null) {
            throw new ServiceException("计划开始时间不能为空");
        }
        validationService.validateHierarchy(req.getSeasonId(), req.getStageId(), req.getRoundId(), req.getGroupId());
        validationService.validateTeamsForSeason(req.getSeasonId(), req.getHomeTeamId(), req.getAwayTeamId());
        validationService.validateTeamsInGroup(req.getGroupId(), req.getHomeTeamId(), req.getAwayTeamId());

        BasketMatch match = new BasketMatch();
        match.setMatchNo(playerNoGenerator.generateBizNo("M"));
        match.setSeasonId(req.getSeasonId());
        match.setStageId(req.getStageId());
        match.setRoundId(req.getRoundId());
        match.setGroupId(req.getGroupId());
        match.setVenueId(req.getVenueId());
        match.setHomeTeamId(req.getHomeTeamId());
        match.setAwayTeamId(req.getAwayTeamId());
        match.setScheduledStartTime(req.getScheduledStartTime());
        match.setScheduledEndTime(req.getScheduledEndTime());
        match.setMatchStatus(MatchStatusEnum.DRAFT.getCode());
        match.setHomeScore(0);
        match.setAwayScore(0);
        match.setOvertimeCount(0);
        match.setDataStatus(MatchDataStatusEnum.DRAFT.getCode());
        match.setPublicStatus(req.getPublicStatus() != null ? req.getPublicStatus() : 1);
        match.setSortNo(req.getSortNo() != null ? req.getSortNo() : 0);
        match.setIsDelete(NOT_DELETED);
        matchMapper.insert(match);

        List<BasketMatchTeam> teams = buildMatchTeams(match);
        matchTeamMapper.batchInsert(teams);

        BasketMatch after = validationService.requireMatch(match.getId());
        validationService.writeStatusLog(after, null, MatchChangeTypeEnum.CREATE, "创建比赛");
        return match.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long matchId, MatchUpdateReq req) {
        BasketMatch match = validationService.requireMatch(matchId);
        validationService.requirePublishedForOps(match);
        MatchStatusEnum status = MatchStatusEnum.fromCode(match.getMatchStatus());
        MatchDataStatusEnum dataStatus = MatchDataStatusEnum.fromCode(match.getDataStatus());
        if (!status.canEdit() || !dataStatus.canEdit()) {
            throw new ServiceException("当前状态不可编辑");
        }
        if (req.getScheduledStartTime() == null) {
            throw new ServiceException("计划开始时间不能为空");
        }
        validationService.validateTeamsForSeason(match.getSeasonId(), req.getHomeTeamId(), req.getAwayTeamId());
        validationService.validateTeamsInGroup(req.getGroupId() != null ? req.getGroupId() : match.getGroupId(),
                req.getHomeTeamId(), req.getAwayTeamId());

        match.setRoundId(req.getRoundId());
        match.setGroupId(req.getGroupId());
        match.setVenueId(req.getVenueId());
        match.setHomeTeamId(req.getHomeTeamId());
        match.setAwayTeamId(req.getAwayTeamId());
        match.setScheduledStartTime(req.getScheduledStartTime());
        match.setScheduledEndTime(req.getScheduledEndTime());
        if (req.getSortNo() != null) {
            match.setSortNo(req.getSortNo());
        }
        if (req.getPublicStatus() != null) {
            match.setPublicStatus(req.getPublicStatus());
        }
        if (matchMapper.updateById(match) != 1) {
            throw new ServiceException("更新失败");
        }
        matchTeamMapper.softDeleteByMatchId(matchId);
        matchTeamMapper.batchInsert(buildMatchTeams(match));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void schedule(Long matchId, MatchScheduleReq req) {
        BasketMatch match = validationService.requireMatch(matchId);
        validationService.requirePublishedForOps(match);
        Integer beforeStatus = match.getMatchStatus();
        MatchStatusEnum status = MatchStatusEnum.fromCode(beforeStatus);
        if (!status.canSchedule()) {
            throw new ServiceException("当前状态不可编排");
        }
        if (req.getScheduledStartTime() == null) {
            throw new ServiceException("计划开始时间不能为空");
        }
        validationService.checkScheduleConflict(matchId, req.getVenueId(), match.getHomeTeamId(), match.getAwayTeamId(),
                req.getScheduledStartTime(), req.getScheduledEndTime());

        List<Integer> from = Arrays.asList(MatchStatusEnum.DRAFT.getCode(), MatchStatusEnum.POSTPONED.getCode());
        if (matchMapper.updateSchedule(matchId, from, req.getVenueId(), req.getScheduledStartTime(),
                req.getScheduledEndTime(), MatchStatusEnum.SCHEDULED.getCode()) != 1) {
            throw new ServiceException("编排失败");
        }
        match.setMatchStatus(MatchStatusEnum.SCHEDULED.getCode());
        validationService.writeStatusLog(match, beforeStatus, MatchChangeTypeEnum.SCHEDULE, "比赛编排");
        notifyCaptains(match, "比赛已编排", "您的球队比赛已编排，请查看赛程并提交出场名单。");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void postpone(Long matchId, MatchPostponeReq req) {
        if (StringUtils.isEmpty(req.getReason())) {
            throw new ServiceException("延期原因不能为空");
        }
        if (req.getScheduledStartTime() == null) {
            throw new ServiceException("新的计划开始时间不能为空");
        }
        BasketMatch match = validationService.requireMatch(matchId);
        validationService.requirePublishedForOps(match);
        Integer beforeStatus = match.getMatchStatus();
        MatchStatusEnum status = MatchStatusEnum.fromCode(beforeStatus);
        if (!status.canPostpone()) {
            throw new ServiceException("当前状态不可延期");
        }
        Long venueId = req.getVenueId() != null ? req.getVenueId() : match.getVenueId();
        validationService.checkScheduleConflict(matchId, venueId, match.getHomeTeamId(), match.getAwayTeamId(),
                req.getScheduledStartTime(), req.getScheduledEndTime());

        List<Integer> from = Arrays.asList(MatchStatusEnum.SCHEDULED.getCode(), MatchStatusEnum.RUNNING.getCode());
        if (matchMapper.updateSchedule(matchId, from, venueId, req.getScheduledStartTime(), req.getScheduledEndTime(),
                MatchStatusEnum.POSTPONED.getCode()) != 1) {
            throw new ServiceException("延期失败");
        }
        match.setMatchStatus(MatchStatusEnum.POSTPONED.getCode());
        validationService.writeStatusLog(match, beforeStatus, MatchChangeTypeEnum.POSTPONE, req.getReason());
        notifyCaptains(match, "比赛已延期", "您的球队比赛已延期：" + req.getReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancel(Long matchId, MatchCancelReq req) {
        if (StringUtils.isEmpty(req.getReason())) {
            throw new ServiceException("取消原因不能为空");
        }
        BasketMatch match = validationService.requireMatch(matchId);
        validationService.requirePublishedForOps(match);
        Integer beforeStatus = match.getMatchStatus();
        if (!MatchStatusEnum.fromCode(beforeStatus).canCancel()) {
            throw new ServiceException("当前状态不可取消");
        }
        List<Integer> from = Arrays.asList(MatchStatusEnum.DRAFT.getCode(), MatchStatusEnum.SCHEDULED.getCode(),
                MatchStatusEnum.POSTPONED.getCode());
        if (matchMapper.updateCancel(matchId, from, MatchStatusEnum.CANCELLED.getCode(), req.getReason()) != 1) {
            throw new ServiceException("取消失败");
        }
        match.setMatchStatus(MatchStatusEnum.CANCELLED.getCode());
        validationService.writeStatusLog(match, beforeStatus, MatchChangeTypeEnum.CANCEL, req.getReason());
        notifyCaptains(match, "比赛已取消", "您的球队比赛已取消：" + req.getReason());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void start(Long matchId) {
        BasketMatch match = validationService.requireMatch(matchId);
        validationService.requirePublishedForOps(match);
        Integer beforeStatus = match.getMatchStatus();
        if (!MatchStatusEnum.fromCode(beforeStatus).canStart()) {
            throw new ServiceException("仅已编排比赛可开始");
        }
        validateLineupReady(matchId, match.getHomeTeamId());
        validateLineupReady(matchId, match.getAwayTeamId());
        Date now = new Date();
        if (matchMapper.updateStart(matchId, MatchStatusEnum.SCHEDULED.getCode(), MatchStatusEnum.RUNNING.getCode(), now) != 1) {
            throw new ServiceException("开始比赛失败");
        }
        match.setMatchStatus(MatchStatusEnum.RUNNING.getCode());
        validationService.writeStatusLog(match, beforeStatus, MatchChangeTypeEnum.START, "比赛开始");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long matchId) {
        BasketMatch match = validationService.requireMatch(matchId);
        if (!MatchStatusEnum.fromCode(match.getMatchStatus()).canDelete()) {
            throw new ServiceException("仅草稿比赛可删除");
        }
        if (matchMapper.countRosterByMatchId(matchId) > 0) {
            throw new ServiceException("比赛已有出场名单，无法删除");
        }
        if (matchMapper.countPeriodScoreByMatchId(matchId) > 0) {
            throw new ServiceException("比赛已有比分数据，无法删除");
        }
        if (matchMapper.countTeamStatByMatchId(matchId) > 0) {
            throw new ServiceException("比赛已有技术统计，无法删除");
        }
        if (matchMapper.softDelete(matchId, Collections.singletonList(MatchStatusEnum.DRAFT.getCode())) != 1) {
            throw new ServiceException("删除失败");
        }
        matchTeamMapper.softDeleteByMatchId(matchId);
    }

    private void validateLineupReady(Long matchId, Long teamId) {
        if (rosterMapper.countStartersByMatchAndTeam(matchId, teamId) != BasketMatchConstants.STARTER_COUNT) {
            throw new ServiceException("球队出场名单须包含5名首发");
        }
        if (rosterMapper.countCaptainsByMatchAndTeam(matchId, teamId) != BasketMatchConstants.CAPTAIN_COUNT) {
            throw new ServiceException("球队出场名单须指定1名场上队长");
        }
    }

    private List<BasketMatchTeam> buildMatchTeams(BasketMatch match) {
        List<BasketMatchTeam> list = new ArrayList<>(2);
        list.add(buildMatchTeam(match, match.getHomeTeamId(), TeamSideEnum.HOME));
        list.add(buildMatchTeam(match, match.getAwayTeamId(), TeamSideEnum.AWAY));
        return list;
    }

    private BasketMatchTeam buildMatchTeam(BasketMatch match, Long teamId, TeamSideEnum side) {
        BasketMatchTeam team = new BasketMatchTeam();
        team.setMatchId(match.getId());
        team.setTeamId(teamId);
        team.setSideType(side.getCode());
        team.setScore(0);
        team.setResultType(MatchResultTypeEnum.NONE.getCode());
        team.setIsWinner(0);
        team.setTimeoutCount(0);
        team.setFoulsTotal(0);
        team.setIsDelete(NOT_DELETED);
        return team;
    }

    private void notifyCaptains(BasketMatch match, String title, String content) {
        messageHelper.notifyCaptains(match, SecurityUtils.getUserId(), title, content);
    }
}
