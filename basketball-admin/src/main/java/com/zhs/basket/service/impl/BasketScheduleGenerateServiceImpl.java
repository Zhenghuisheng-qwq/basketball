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
import com.zhs.basket.support.BasketScheduleMessageHelper;
import com.zhs.basket.support.ScheduleMatchTeamSupport;
import com.zhs.basket.support.ScheduleMatchValidationSupport;
import com.zhs.basket.vo.ScheduleEventContextVo;
import com.zhs.basket.vo.ScheduleGeneratePreviewVo;
import com.zhs.basket.vo.ScheduleGenerateResultVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class BasketScheduleGenerateServiceImpl implements IBasketScheduleGenerateService {

    private static final Logger log = LoggerFactory.getLogger(BasketScheduleGenerateServiceImpl.class);
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
    @Autowired private BasketTeamSeasonProfileMapper eventTeamMapper;
    @Autowired private BasketTournamentRegistrationMapper registrationMapper;
    @Autowired private ScheduleRuleStrategyFactory strategyFactory;
    @Autowired private ScheduleEventTeamSupport eventTeamSupport;
    @Autowired private ScheduleAuditSupport auditSupport;
    @Autowired private ScheduleMatchValidationSupport validationSupport;
    @Autowired private PlayerNoGenerator playerNoGenerator;
    @Autowired private BasketScheduleMessageHelper scheduleMessageHelper;
    @Autowired @Lazy private IBasketScheduleGenerateService scheduleGenerateService;

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
        // 正式参赛口径：参赛中 + 已审核 + 阵容已锁定（与可生成赛程一致）
        int eligibleCount = eventTeamSupport.countEligibleTeams(eventId);
        int lockedCount = eventTeamSupport.countLockedActiveTeams(eventId);
        vo.setEventTeamCount(eligibleCount);
        vo.setLockedTeamCount(lockedCount);
        vo.setExpectedMatchCount(calcPairCount(eligibleCount));
        vo.setExpectedRoundCount(calcRoundCount(eligibleCount));
        vo.setExistingMatchCount(matchMapper.countBySeasonId(eventId));
        int deletableDraftCount = matchMapper.countDeletableDraftBySeasonId(eventId);
        int legacyCount = matchMapper.countLegacyUnpublishedBySeasonId(eventId);
        int batchCount = batchMapper.countByEventId(eventId);
        vo.setDeletableDraftMatchCount(deletableDraftCount);
        vo.setLegacyUnpublishedMatchCount(legacyCount);
        vo.setHasLegacyUnpublishedMatches(legacyCount > 0);
        vo.setHasPublishedSchedule(matchMapper.countPublishedBySeasonId(eventId) > 0);
        BasketScheduleGenerateBatch latest = batchMapper.selectLatestByEventId(eventId);
        if (latest != null) {
            vo.setLatestBatchId(latest.getId());
            vo.setLatestBatchPublishStatus(latest.getPublishStatus());
            int ps = latest.getPublishStatus() != null ? latest.getPublishStatus() : ScheduleBatchPublishStatusEnum.DRAFT.getCode();
            vo.setHasDraftSchedule(ps == ScheduleBatchPublishStatusEnum.DRAFT.getCode()
                    || ps == ScheduleBatchPublishStatusEnum.UNPUBLISHED.getCode());
        } else {
            vo.setHasDraftSchedule(deletableDraftCount > 0);
        }
        boolean hasUnpublishedMatches = deletableDraftCount > 0
                && matchMapper.countUnpublishedScheduleBySeasonId(eventId) > 0;
        if (!Boolean.TRUE.equals(vo.getHasDraftSchedule()) && hasUnpublishedMatches) {
            vo.setHasDraftSchedule(true);
        }
        vo.setCanGenerateSchedule(Boolean.FALSE.equals(vo.getHasPublishedSchedule()) && eligibleCount >= 2);
        vo.setCanDeleteRegeneratableDraft(Boolean.FALSE.equals(vo.getHasPublishedSchedule())
                && (Boolean.TRUE.equals(vo.getHasDraftSchedule()) || deletableDraftCount > 0 || batchCount > 0));
        vo.setScheduleStatusText(resolveScheduleStatusText(vo, hasUnpublishedMatches));
        return vo;
    }

    private String resolveScheduleStatusText(ScheduleEventContextVo vo, boolean hasUnpublishedMatches) {
        if (Boolean.TRUE.equals(vo.getHasPublishedSchedule())) {
            return "已发布";
        }
        if (vo.getLatestBatchPublishStatus() != null
                && vo.getLatestBatchPublishStatus() == ScheduleBatchPublishStatusEnum.UNPUBLISHED.getCode()) {
            return "已下架";
        }
        if (hasUnpublishedMatches) {
            return "已下架";
        }
        if (Boolean.TRUE.equals(vo.getHasDraftSchedule())
                || (vo.getDeletableDraftMatchCount() != null && vo.getDeletableDraftMatchCount() > 0)) {
            return "草稿";
        }
        if (Boolean.TRUE.equals(vo.getHasLegacyUnpublishedMatches())) {
            return "未生成（存在历史脏数据）";
        }
        return "未生成";
    }

    @Override
    public ScheduleGeneratePreviewVo preview(ScheduleGenerateReq req) {
        validateGenerateRequest(req);
        BasketScheduleRule rule = requireRule(req.getRuleId());
        List<EventTeamScheduleSnapshot> teams = eventTeamSupport.loadEligibleSnapshots(req.getEventId());
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
        vo.setLockedTeamCount(eventTeamSupport.countLockedActiveTeams(req.getEventId()));
        vo.setExpectedMatchCount(calcPairCount(teams.size()));
        vo.setExpectedRoundCount(calcRoundCount(teams.size()));
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
        // 赛事维度悲观锁，防止并发生成重复批次/比赛
        seasonMapper.selectByIdForUpdate(eventId);
        assertNoPublishedSchedule(eventId);

        BasketScheduleGenerateBatch latestBatch = batchMapper.selectLatestByEventId(eventId);
        boolean hasUnpublishedBatch = latestBatch != null
                && ScheduleBatchPublishStatusEnum.UNPUBLISHED.getCode() == latestBatch.getPublishStatus();
        int deletableDraftCount = matchMapper.countDeletableDraftBySeasonId(eventId);
        int batchCount = batchMapper.countByEventId(eventId);
        int activeValidCount = matchMapper.countActiveValidScheduleBySeasonId(eventId);

        // 仅当存在有效草稿（非下架）且未确认覆盖时才拦截；已下架可直接重新生成
        if (activeValidCount > 0 && !hasUnpublishedBatch && !Boolean.TRUE.equals(req.getForceRegenerate())) {
            throw new ServiceException("当前已存在草稿赛程，请先查看或删除草稿后重新生成");
        }
        if (batchCount > 0 || deletableDraftCount > 0) {
            deleteDraftInternal(eventId, hasUnpublishedBatch ? "下架后重新生成" : "重新生成前删除旧草稿");
        }

        BasketScheduleRule rule = requireRule(req.getRuleId());
        List<EventTeamScheduleSnapshot> teams = eventTeamSupport.loadEligibleSnapshots(eventId);
        if (teams.size() < 2) {
            throw new ServiceException("正式参赛球队不足 2 支，无法生成赛程");
        }
        int activeCount = eventTeamMapper.countActiveBySeasonId(eventId);
        int lockedCount = eventTeamSupport.countLockedActiveTeams(eventId);
        if (lockedCount < activeCount) {
            throw new ServiceException("仍有正式赛事球队阵容未锁定，请先完成阵容锁定后再生成赛程");
        }

        Long stageId = resolveStageId(req.getStageId(), eventId);
        String platformCode = eventTeamSupport.resolvePlatformCode(eventId);
        ScheduleRuleContext context = buildContext(req, rule, teams, stageId, platformCode);
        ScheduleRuleStrategy strategy = strategyFactory.getStrategy(rule.getRuleCode(), rule.getRuleVersion());
        List<MatchPlan> plans = strategy.generate(context);
        validateGeneratedPlans(eventId, plans);
        assertGeneratedMatchTimes(rule, plans);

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
        List<BasketMatch> matches = matchMapper.selectByBatchId(batch.getId());
        validateBeforePublish(eventId, batch, matches, Boolean.TRUE.equals(req.getAllowIncomplete()));
        Date now = new Date();
        batchMapper.updatePublishStatus(batch.getId(), ScheduleBatchPublishStatusEnum.PUBLISHED.getCode(), now);
        matchMapper.updatePublishStatusByBatchId(batch.getId(), MatchPublishStatusEnum.PUBLISHED.getCode(), 1);
        auditSupport.logBatchAction(batch.getId(), eventId, ScheduleAuditActionTypeEnum.PUBLISH,
                "发布赛程", req.getReason(), null);
        Long stageId = matches.isEmpty() ? null : matches.get(0).getStageId();
        scheduleMessageHelper.notifySchedulePublished(batch, stageId, SecurityUtils.getUserId());
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
        int deletable = matchMapper.countDeletableDraftBySeasonId(eventId);
        int batchCount = batchMapper.countByEventId(eventId);
        BasketScheduleGenerateBatch latest = batchMapper.selectLatestByEventId(eventId);
        boolean hasUnpublishedBatch = latest != null
                && ScheduleBatchPublishStatusEnum.UNPUBLISHED.getCode() == latest.getPublishStatus();
        if (deletable <= 0 && batchCount <= 0 && !hasUnpublishedBatch) {
            throw new ServiceException("当前没有可删除的草稿赛程；已结束/已取消的历史比赛不会被删除");
        }
        deleteDraftInternal(eventId, reason);
    }

    @Override
    public int autoGenerateEligibleSchedules() {
        Date now = new Date();
        BasketScheduleRule defaultRule = ruleMapper.selectByCodeAndVersion(
                ScheduleRuleCodeEnum.REGULAR_ROUND_ROBIN.getCode(),
                ScheduleRuleVersionEnum.V2.getCode());
        if (defaultRule == null || defaultRule.getEnabled() == null || defaultRule.getEnabled() != 1) {
            log.warn("未找到启用的 REGULAR_ROUND_ROBIN V2 规则，跳过自动排赛");
            return 0;
        }
        List<Long> seasonIds = seasonMapper.selectRegistrationClosedSeasonIds(now);
        int success = 0;
        for (Long seasonId : seasonIds) {
            if (!shouldAutoGenerate(seasonId, now)) {
                continue;
            }
            try {
                ScheduleGenerateReq req = new ScheduleGenerateReq();
                req.setEventId(seasonId);
                req.setRuleId(defaultRule.getId());
                req.setRemark("报名截止后自动生成草稿赛程");
                scheduleGenerateService.generateDraftSchedule(req);
                success++;
            } catch (Exception ex) {
                log.warn("赛季 {} 自动草稿赛程生成失败: {}", seasonId, ex.getMessage());
            }
        }
        return success;
    }

    private boolean shouldAutoGenerate(Long seasonId, Date now) {
        BasketTournamentSeason season = seasonMapper.selectById(seasonId);
        if (season == null || season.getRegistrationEndTime() == null) {
            return false;
        }
        if (season.getRegistrationEndTime().after(now)) {
            return false;
        }
        if (batchMapper.countPublishedByEventId(seasonId) > 0) {
            return false;
        }
        if (batchMapper.countByEventId(seasonId) > 0) {
            return false;
        }
        if (matchMapper.countDeletableDraftBySeasonId(seasonId) > 0) {
            return false;
        }
        if (eventTeamMapper.countEligibleForScheduleBySeasonId(seasonId) < 2) {
            return false;
        }
        if (registrationMapper.countPendingAuditBySeasonId(seasonId) > 0) {
            return false;
        }
        return true;
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
        if (batchMapper.countPublishedByEventId(eventId) > 0
                || matchMapper.countPublishedBySeasonId(eventId) > 0) {
            throw new ServiceException("当前赛程已发布，不能重复生成。如需调整，请使用赛程调整功能");
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
        context.setSlotCapacity(rule.getSlotCapacity());
        // V2 强制使用晚场多 1 场的均分策略，避免 DB 配置残留 FILL_FIRST
        if (ScheduleRuleVersionEnum.V2.getCode().equalsIgnoreCase(rule.getRuleVersion())) {
            context.setAllocationMode(ScheduleAllocationModeEnum.EVEN_SPLIT_WITH_LATE_EXTRA.getCode());
        } else {
            context.setAllocationMode(rule.getAllocationMode());
        }
        Date baseDate = req.getBaseDate();
        if (baseDate == null) {
            BasketTournamentSeason season = seasonMapper.selectById(req.getEventId());
            baseDate = season.getSeasonStartTime() != null ? season.getSeasonStartTime() : new Date();
        }
        context.setBaseDate(baseDate);
        return context;
    }

    private void assertGeneratedMatchTimes(BasketScheduleRule rule, List<MatchPlan> plans) {
        BasketMatch probe = new BasketMatch();
        probe.setRuleId(rule.getId());
        probe.setRuleCode(rule.getRuleCode());
        probe.setRuleVersion(rule.getRuleVersion());
        for (MatchPlan plan : plans) {
            validationSupport.assertMatchTimeByRule(probe, plan.getMatchTime());
        }
    }

    private void validateBeforePublish(Long eventId, BasketScheduleGenerateBatch batch,
            List<BasketMatch> matches, boolean allowIncomplete) {
        if (matches == null || matches.isEmpty()) {
            throw new ServiceException("当前批次没有可发布的比赛");
        }
        Set<Long> eligibleIds = eventTeamSupport.loadEligibleSnapshots(eventId).stream()
                .map(EventTeamScheduleSnapshot::getEventTeamId).collect(Collectors.toSet());
        Set<String> pairKeys = new HashSet<>();
        Map<String, Set<Long>> teamsByDay = new HashMap<>();
        for (BasketMatch match : matches) {
            if (match.getHomeEventTeamId() == null || match.getAwayEventTeamId() == null) {
                throw new ServiceException("存在缺少主队或客队的比赛，无法发布");
            }
            if (match.getHomeEventTeamId().equals(match.getAwayEventTeamId())) {
                throw new ServiceException("存在主队等于客队的比赛，无法发布");
            }
            if (!eligibleIds.contains(match.getHomeEventTeamId())
                    || !eligibleIds.contains(match.getAwayEventTeamId())) {
                throw new ServiceException("存在非当前赛事正式参赛球队的对阵，无法发布");
            }
            String pairKey = canonicalEventTeamPairKey(match.getHomeEventTeamId(), match.getAwayEventTeamId());
            if (!pairKeys.add(pairKey)) {
                throw new ServiceException("存在重复对阵，无法发布");
            }
            validationSupport.assertMatchTimeByRule(match, match.getScheduledStartTime());
            if (match.getScheduledStartTime() != null) {
                String dayKey = validationSupport.toLocalDate(match.getScheduledStartTime()).toString();
                Set<Long> dayTeams = teamsByDay.computeIfAbsent(dayKey, key -> new HashSet<>());
                if (!dayTeams.add(match.getHomeEventTeamId()) || !dayTeams.add(match.getAwayEventTeamId())) {
                    throw new ServiceException("同一球队同一比赛日最多一场，无法发布");
                }
            }
        }
        int expected = calcPairCount(eligibleIds.size());
        if (matches.size() != expected && !allowIncomplete) {
            throw new ServiceException(String.format(
                    "草稿比赛数量为 %d，预计总场次为 %d。如需不完整发布请勾选允许不完整发布",
                    matches.size(), expected));
        }
    }

    private void validateGeneratedPlans(Long eventId, List<MatchPlan> plans) {
        Set<String> pairKeys = new HashSet<>();
        Map<Long, Set<Long>> teamsByTime = new HashMap<>();
        Map<Integer, Set<Long>> teamsByRound = new HashMap<>();
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
            if (plan.getRoundNo() != null) {
                Set<Long> roundTeams = teamsByRound.computeIfAbsent(plan.getRoundNo(), key -> new HashSet<>());
                if (!roundTeams.add(plan.getHomeEventTeamId()) || !roundTeams.add(plan.getAwayEventTeamId())) {
                    throw new ServiceException("同一轮次存在重复参赛球队");
                }
            }
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

    private int calcRoundCount(int teamCount) {
        if (teamCount < 2) {
            return 0;
        }
        return teamCount % 2 == 0 ? teamCount - 1 : teamCount;
    }
}
