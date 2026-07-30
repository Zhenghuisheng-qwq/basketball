package com.zhs.basket.service.impl;

import com.zhs.basket.domain.*;
import com.zhs.basket.enums.*;
import com.zhs.basket.mapper.*;
import com.zhs.basket.req.ScheduleGenerateReq;
import com.zhs.basket.req.SchedulePublishReq;
import com.zhs.basket.schedule.model.EventTeamScheduleSnapshot;
import com.zhs.basket.schedule.model.MatchPlan;
import com.zhs.basket.schedule.model.ScheduleRuleContext;
import com.zhs.basket.schedule.strategy.ScheduleRuleStrategy;
import com.zhs.basket.schedule.strategy.ScheduleRuleStrategyFactory;
import com.zhs.basket.service.IBasketScheduleGenerateService;
import com.zhs.basket.support.GameTerminalSupport;
import com.zhs.basket.support.PlayerNoGenerator;
import com.zhs.basket.support.ScheduleAuditSupport;
import com.zhs.basket.support.ScheduleEventTeamSupport;
import com.zhs.basket.support.ScheduleMatchTeamSupport;
import com.zhs.basket.vo.ScheduleEventContextVo;
import com.zhs.basket.vo.ScheduleGeneratePreviewVo;
import com.zhs.basket.vo.ScheduleGenerateResultVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BasketScheduleGenerateServiceImpl implements IBasketScheduleGenerateService {

    private static final Integer NOT_DELETED = 0;

    @Autowired private BasketScheduleRuleMapper ruleMapper;
    @Autowired private BasketScheduleRuleWeekdayMapper weekdayMapper;
    @Autowired private BasketScheduleRuleTimeSlotMapper timeSlotMapper;
    @Autowired private BasketScheduleGenerateBatchMapper batchMapper;
    @Autowired private BasketMatchMapper matchMapper;
    @Autowired private ScheduleMatchTeamSupport matchTeamSupport;
    @Autowired private BasketTournamentMapper tournamentMapper;
    @Autowired private BasketTournamentStageMapper stageMapper;
    @Autowired private BasketTournamentSeasonMapper seasonMapper;
    @Autowired private ScheduleRuleStrategyFactory strategyFactory;
    @Autowired private ScheduleEventTeamSupport eventTeamSupport;
    @Autowired private ScheduleAuditSupport auditSupport;
    @Autowired private PlayerNoGenerator playerNoGenerator;

    @Override
    public ScheduleEventContextVo getEventContext(Long eventId) {
        BasketTournamentSeason season = eventTeamSupport.requireSeason(eventId);
        BasketTournament tournament = tournamentMapper.selectById(season.getTournamentId());
        ScheduleEventContextVo vo = new ScheduleEventContextVo();
        vo.setEventId(eventId);
        vo.setSeasonId(eventId);
        vo.setSeasonName(season.getSeasonName());
        if (tournament != null) {
            vo.setTournamentName(tournament.getTournamentName());
        }
        String platformCode = eventTeamSupport.resolvePlatformCode(eventId);
        vo.setPlatformCode(platformCode);
        vo.setPlatformDisplayName(GameTerminalSupport.formatDisplayName(platformCode));
        vo.setEventTeamCount(eventTeamSupport.loadActiveSnapshots(eventId).size());
        vo.setExistingMatchCount(matchMapper.countBySeasonId(eventId));
        vo.setHasPublishedSchedule(matchMapper.countPublishedBySeasonId(eventId) > 0);
        BasketScheduleGenerateBatch latest = batchMapper.selectLatestByEventId(eventId);
        if (latest != null) {
            vo.setLatestBatchId(latest.getId());
            vo.setLatestBatchPublishStatus(latest.getPublishStatus());
            vo.setHasDraftSchedule(ScheduleBatchPublishStatusEnum.DRAFT.getCode() == latest.getPublishStatus());
        } else {
            vo.setHasDraftSchedule(false);
        }
        return vo;
    }

    @Override
    public ScheduleGeneratePreviewVo preview(ScheduleGenerateReq req) {
        validateGenerateRequest(req);
        BasketScheduleRule rule = requireRule(req.getRuleId());
        List<EventTeamScheduleSnapshot> teams = eventTeamSupport.loadActiveSnapshots(req.getEventId());
        BasketTournamentSeason season = eventTeamSupport.requireSeason(req.getEventId());
        BasketTournament tournament = tournamentMapper.selectById(season.getTournamentId());

        ScheduleGeneratePreviewVo vo = new ScheduleGeneratePreviewVo();
        vo.setEventId(req.getEventId());
        vo.setSeasonName(season.getSeasonName());
        if (tournament != null) {
            vo.setTournamentName(tournament.getTournamentName());
        }
        vo.setPlatformCode(eventTeamSupport.resolvePlatformCode(req.getEventId()));
        vo.setEventTeamCount(teams.size());
        vo.setExpectedMatchCount(calcPairCount(teams.size()));
        vo.setRuleName(rule.getRuleName());
        vo.setRuleCode(rule.getRuleCode());
        vo.setRuleVersion(rule.getRuleVersion());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ScheduleGenerateResultVo generateDraftSchedule(ScheduleGenerateReq req) {
        validateGenerateRequest(req);
        Long eventId = req.getEventId();
        assertNoPublishedSchedule(eventId);

        int existingCount = matchMapper.countBySeasonId(eventId);
        if (existingCount > 0) {
            if (!Boolean.TRUE.equals(req.getForceRegenerate())) {
                throw new ServiceException("当前赛事已有赛程记录，请先删除草稿后再重新生成");
            }
            deleteDraftInternal(eventId, "重新生成前删除旧草稿");
        }

        BasketScheduleRule rule = requireRule(req.getRuleId());
        List<EventTeamScheduleSnapshot> teams = eventTeamSupport.loadActiveSnapshots(eventId);
        if (teams.size() < 2) {
            throw new ServiceException("正式参赛球队不足 2 支，无法生成赛程");
        }

        Long stageId = resolveStageId(req.getStageId(), eventId);
        String platformCode = eventTeamSupport.resolvePlatformCode(eventId);
        ScheduleRuleContext context = buildContext(req, rule, teams, stageId, platformCode);
        ScheduleRuleStrategy strategy = strategyFactory.getStrategy(rule.getRuleCode(), rule.getRuleVersion());
        List<MatchPlan> plans = strategy.generate(context);
        validateGeneratedPlans(eventId, plans);

        Date now = new Date();
        BasketScheduleGenerateBatch batch = new BasketScheduleGenerateBatch();
        batch.setBatchNo(playerNoGenerator.generateBizNo("SB"));
        batch.setEventId(eventId);
        batch.setRuleId(rule.getId());
        batch.setRuleCode(rule.getRuleCode());
        batch.setRuleVersion(rule.getRuleVersion());
        batch.setEventTeamCount(teams.size());
        batch.setMatchCount(plans.size());
        batch.setGenerateStatus(ScheduleGenerateStatusEnum.SUCCESS.getCode());
        batch.setPublishStatus(ScheduleBatchPublishStatusEnum.DRAFT.getCode());
        batch.setGeneratedBy(SecurityUtils.getUserId());
        batch.setGeneratedTime(now);
        batch.setRemark(req.getRemark());
        batch.setCreateTime(now);
        batch.setUpdateTime(now);
        batch.setIsDelete(NOT_DELETED);
        batchMapper.insert(batch);

        for (MatchPlan plan : plans) {
            persistMatch(eventId, stageId, platformCode, rule, batch.getId(), plan, now);
        }

        auditSupport.logBatchAction(batch.getId(), eventId, ScheduleAuditActionTypeEnum.GENERATE_DRAFT,
                "自动生成草稿赛程", req.getRemark(), null);

        ScheduleGenerateResultVo result = new ScheduleGenerateResultVo();
        result.setBatchId(batch.getId());
        result.setBatchNo(batch.getBatchNo());
        result.setMatchCount(plans.size());
        result.setEventTeamCount(teams.size());
        return result;
    }

    @Override
    public ScheduleGenerateResultVo generateDraft(ScheduleGenerateReq req) {
        return generateDraftSchedule(req);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(SchedulePublishReq req) {
        Long eventId = req.getEventId();
        eventTeamSupport.requireSeason(eventId);
        BasketScheduleGenerateBatch batch = resolveBatch(req);
        if (ScheduleBatchPublishStatusEnum.PUBLISHED.getCode() == batch.getPublishStatus()) {
            throw new ServiceException("赛程已发布，请勿重复发布");
        }
        Date now = new Date();
        batchMapper.updatePublishStatus(batch.getId(), ScheduleBatchPublishStatusEnum.PUBLISHED.getCode(), now);
        matchMapper.updatePublishStatusByBatchId(batch.getId(), MatchPublishStatusEnum.PUBLISHED.getCode(), 1);
        auditSupport.logBatchAction(batch.getId(), eventId, ScheduleAuditActionTypeEnum.PUBLISH,
                "发布赛程", req.getReason(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unpublish(SchedulePublishReq req) {
        Long eventId = req.getEventId();
        eventTeamSupport.requireSeason(eventId);
        BasketScheduleGenerateBatch batch = resolveBatch(req);
        if (ScheduleBatchPublishStatusEnum.PUBLISHED.getCode() != batch.getPublishStatus()) {
            throw new ServiceException("当前批次未发布，无法下架");
        }
        Date now = new Date();
        batchMapper.updatePublishStatus(batch.getId(), ScheduleBatchPublishStatusEnum.UNPUBLISHED.getCode(), now);
        matchMapper.updatePublishStatusByBatchId(batch.getId(), MatchPublishStatusEnum.UNPUBLISHED.getCode(), 0);
        auditSupport.logBatchAction(batch.getId(), eventId, ScheduleAuditActionTypeEnum.UNPUBLISH,
                "下架赛程", req.getReason(), null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteDraft(Long eventId, String reason) {
        eventTeamSupport.requireSeason(eventId);
        if (matchMapper.countPublishedBySeasonId(eventId) > 0) {
            throw new ServiceException("已有已发布赛程，不能删除草稿");
        }
        deleteDraftInternal(eventId, reason);
    }

    private void deleteDraftInternal(Long eventId, String reason) {
        matchMapper.softDeleteDraftBySeasonId(eventId);
        batchMapper.softDeleteDraftByEventId(eventId);
        auditSupport.logBatchAction(null, eventId, ScheduleAuditActionTypeEnum.DELETE_DRAFT,
                "删除草稿赛程", reason, null);
    }

    private void validateGenerateRequest(ScheduleGenerateReq req) {
        if (req == null || req.getEventId() == null) {
            throw new ServiceException("请选择赛事");
        }
        if (req.getRuleId() == null) {
            throw new ServiceException("请选择赛程规则");
        }
        eventTeamSupport.requireSeason(req.getEventId());
    }

    private void assertNoPublishedSchedule(Long eventId) {
        if (batchMapper.countPublishedByEventId(eventId) > 0) {
            throw new ServiceException("当前赛事已有已发布赛程，不允许重新生成覆盖");
        }
    }

    private BasketScheduleRule requireRule(Long ruleId) {
        BasketScheduleRule rule = ruleMapper.selectById(ruleId);
        if (rule == null || rule.getEnabled() == null || rule.getEnabled() != 1) {
            throw new ServiceException("赛程规则不存在或未启用");
        }
        return rule;
    }

    private Long resolveStageId(Long stageId, Long eventId) {
        if (stageId != null) {
            BasketTournamentStage stage = stageMapper.selectById(stageId);
            if (stage == null || !eventId.equals(stage.getSeasonId())) {
                throw new ServiceException("阶段不属于当前赛事");
            }
            return stageId;
        }
        List<com.zhs.basket.vo.StageListVo> stages = stageMapper.selectList(buildStageQuery(eventId));
        if (stages.isEmpty()) {
            throw new ServiceException("请先配置赛事阶段");
        }
        return stages.get(0).getId();
    }

    private com.zhs.basket.query.StageQuery buildStageQuery(Long eventId) {
        com.zhs.basket.query.StageQuery query = new com.zhs.basket.query.StageQuery();
        query.setSeasonId(eventId);
        return query;
    }

    private ScheduleRuleContext buildContext(ScheduleGenerateReq req, BasketScheduleRule rule,
            List<EventTeamScheduleSnapshot> teams, Long stageId, String platformCode) {
        ScheduleRuleContext context = new ScheduleRuleContext();
        context.setEventId(req.getEventId());
        context.setSeasonId(req.getEventId());
        context.setStageId(stageId);
        context.setPlatformCode(platformCode);
        context.setRuleId(rule.getId());
        context.setRuleCode(rule.getRuleCode());
        context.setRuleVersion(rule.getRuleVersion());
        context.setEventTeams(teams);
        context.setWeekdays(weekdayMapper.selectByRuleId(rule.getId()).stream()
                .map(BasketScheduleRuleWeekday::getWeekday).collect(Collectors.toList()));
        context.setTimeSlots(timeSlotMapper.selectByRuleId(rule.getId()).stream()
                .map(BasketScheduleRuleTimeSlot::getStartTime).collect(Collectors.toList()));
        Date baseDate = req.getBaseDate();
        if (baseDate == null) {
            BasketTournamentSeason season = seasonMapper.selectById(req.getEventId());
            baseDate = season.getSeasonStartTime() != null ? season.getSeasonStartTime() : new Date();
        }
        context.setBaseDate(baseDate);
        return context;
    }

    private void validateGeneratedPlans(Long eventId, List<MatchPlan> plans) {
        Set<String> pairKeys = new HashSet<>();
        Map<Long, Set<Long>> teamsByTime = new HashMap<>();
        for (MatchPlan plan : plans) {
            if (plan.getHomeEventTeamId().equals(plan.getAwayEventTeamId())) {
                throw new ServiceException("生成结果存在主客队相同比赛");
            }
            String pairKey = canonicalEventTeamPairKey(plan.getHomeEventTeamId(), plan.getAwayEventTeamId());
            if (!pairKeys.add(pairKey)) {
                throw new ServiceException("生成结果存在重复对阵");
            }
            if (matchMapper.countEventTeamPair(eventId, plan.getHomeEventTeamId(), plan.getAwayEventTeamId(), null) > 0) {
                throw new ServiceException("生成结果与已有比赛重复");
            }
            assertNoBatchTimeConflict(teamsByTime, plan);
            assertNoTimeConflict(null, plan.getHomeEventTeamId(), plan.getMatchTime());
            assertNoTimeConflict(null, plan.getAwayEventTeamId(), plan.getMatchTime());
        }
    }

    private String canonicalEventTeamPairKey(Long teamA, Long teamB) {
        long min = Math.min(teamA, teamB);
        long max = Math.max(teamA, teamB);
        return min + ":" + max;
    }

    private void assertNoBatchTimeConflict(Map<Long, Set<Long>> teamsByTime, MatchPlan plan) {
        if (plan.getMatchTime() == null) {
            return;
        }
        long timeKey = plan.getMatchTime().getTime();
        Set<Long> teamsAtTime = teamsByTime.computeIfAbsent(timeKey, key -> new HashSet<>());
        if (!teamsAtTime.add(plan.getHomeEventTeamId()) || !teamsAtTime.add(plan.getAwayEventTeamId())) {
            throw new ServiceException("同一时间点存在重复参赛球队");
        }
    }

    private void assertNoTimeConflict(Long excludeMatchId, Long eventTeamId, Date matchTime) {
        if (matchMapper.countEventTeamTimeConflict(excludeMatchId, eventTeamId, matchTime) > 0) {
            throw new ServiceException("同一时间点存在重复参赛球队");
        }
    }

    private void persistMatch(Long eventId, Long stageId, String platformCode, BasketScheduleRule rule,
            Long batchId, MatchPlan plan, Date now) {
        BasketMatch match = new BasketMatch();
        match.setMatchNo(playerNoGenerator.generateBizNo("M"));
        match.setScheduleBatchId(batchId);
        match.setSeasonId(eventId);
        match.setPlatformCode(platformCode);
        match.setRuleId(rule.getId());
        match.setRuleCode(rule.getRuleCode());
        match.setRuleVersion(rule.getRuleVersion());
        match.setStageId(stageId);
        match.setRoundNo(plan.getRoundNo());
        match.setHomeTeamId(plan.getHomeTeamId());
        match.setAwayTeamId(plan.getAwayTeamId());
        match.setHomeEventTeamId(plan.getHomeEventTeamId());
        match.setAwayEventTeamId(plan.getAwayEventTeamId());
        match.setHomeTeamNameSnapshot(plan.getHomeTeamNameSnapshot());
        match.setAwayTeamNameSnapshot(plan.getAwayTeamNameSnapshot());
        match.setHomeTeamLogoSnapshot(plan.getHomeTeamLogoSnapshot());
        match.setAwayTeamLogoSnapshot(plan.getAwayTeamLogoSnapshot());
        match.setScheduledStartTime(plan.getMatchTime());
        match.setMatchStatus(MatchStatusEnum.SCHEDULED.getCode());
        match.setHomeScore(0);
        match.setAwayScore(0);
        match.setOvertimeCount(0);
        match.setDataStatus(MatchDataStatusEnum.DRAFT.getCode());
        match.setPublicStatus(0);
        match.setPublishStatus(MatchPublishStatusEnum.DRAFT.getCode());
        match.setSortNo(plan.getSortNo());
        match.setCreateTime(now);
        match.setUpdateTime(now);
        match.setIsDelete(NOT_DELETED);
        matchMapper.insertScheduleMatch(match);
        matchTeamSupport.persistMatchTeams(match);
    }

    private BasketScheduleGenerateBatch resolveBatch(SchedulePublishReq req) {
        if (req.getBatchId() != null) {
            BasketScheduleGenerateBatch batch = batchMapper.selectById(req.getBatchId());
            if (batch == null || !req.getEventId().equals(batch.getEventId())) {
                throw new ServiceException("赛程批次不存在");
            }
            return batch;
        }
        BasketScheduleGenerateBatch batch = batchMapper.selectLatestByEventId(req.getEventId());
        if (batch == null) {
            throw new ServiceException("当前赛事没有可操作的赛程批次");
        }
        return batch;
    }

    private int calcPairCount(int teamCount) {
        return teamCount * (teamCount - 1) / 2;
    }
}
