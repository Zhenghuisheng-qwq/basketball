package com.zhs.basket.support.v4.leaderboard;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketLeaderboardEntry;
import com.zhs.basket.domain.v4.BasketLeaderboardPolicy;
import com.zhs.basket.domain.v4.BasketLeaderboardPolicyAudit;
import com.zhs.basket.domain.v4.BasketLeaderboardRun;
import com.zhs.basket.domain.v4.BasketLeaderboardSnapshot;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageMapper;
import com.zhs.basket.mapper.v4.BasketLeaderboardMapper;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class LeaderboardApplicationService {

    private static final Logger log = LoggerFactory.getLogger(LeaderboardApplicationService.class);

    @Autowired
    private BasketLeaderboardMapper leaderboardMapper;
    @Autowired
    private BasketDivisionMapper divisionMapper;
    @Autowired
    private BasketDivisionStageMapper stageMapper;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PlatformTransactionManager transactionManager;

    public Map<String, Object> scopes(Long divisionId) {
        requireDivision(divisionId);
        List<BasketDivisionStage> stages = stageMapper.selectByDivisionId(divisionId);
        List<Long> stageIds = new ArrayList<>();
        List<Map<String, Object>> rows = new ArrayList<>();
        for (BasketDivisionStage stage : stages) {
            if (stage.getId() == null || Integer.valueOf(1).equals(stage.getIsDelete())) {
                continue;
            }
            stageIds.add(stage.getId());
            rows.add(scopeRow(divisionId, LeaderboardScopes.stage(stage.getId()), stage));
        }
        rows.add(scopeRow(divisionId, LeaderboardScopes.OVERALL, null));
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("divisionId", divisionId);
        body.put("stageIds", stageIds);
        body.put("scopes", rows);
        return body;
    }

    @Transactional(rollbackFor = Exception.class)
    public BasketLeaderboardPolicy createDraft(Long divisionId, String scopeKey) {
        divisionMapper.selectByIdForUpdate(divisionId);
        ScopeRef scope = resolve(divisionId, scopeKey);
        if (leaderboardMapper.selectOpenDraft(divisionId, scope.key) != null) {
            throw new ServiceException("该范围已有草稿");
        }
        BasketLeaderboardPolicy active = leaderboardMapper.selectActive(divisionId, scope.key);
        LeaderboardConfig config = active == null
                ? LeaderboardTemplates.forStage(scope.stage, stageIds(divisionId))
                : readConfig(active.getConfigJson());
        if (LeaderboardScopes.isOverall(scope.key) && (config.getOverallStageIds() == null || config.getOverallStageIds().isEmpty())) {
            config.setOverallStageIds(stageIds(divisionId));
        }
        Date now = new Date();
        BasketLeaderboardPolicy policy = new BasketLeaderboardPolicy();
        policy.setDivisionId(divisionId);
        policy.setScopeKey(scope.key);
        policy.setStageId(scope.stage == null ? null : scope.stage.getId());
        policy.setVersionNo(leaderboardMapper.selectMaxVersion(divisionId, scope.key) + 1);
        policy.setLifecycle("DRAFT");
        policy.setTemplateCode(config.getTemplateCode());
        policy.setRankingAudience(config.getRankingAudience());
        policy.setConfigJson(writeConfig(config));
        policy.setBasedOnPolicyId(active == null ? null : active.getId());
        policy.setExpectedLock(0);
        policy.setDraftScopeKey(scope.key);
        policy.setCreateBy(currentUserId());
        policy.setCreateTime(now);
        policy.setUpdateBy(currentUserId());
        policy.setUpdateTime(now);
        leaderboardMapper.insertPolicy(policy);
        audit(policy, "SAVE", null, policy.getConfigJson(), active == null ? "新建草稿" : "基于生效版本修订");
        return leaderboardMapper.selectPolicyById(policy.getId());
    }

    @Transactional(rollbackFor = Exception.class)
    public BasketLeaderboardPolicy saveDraft(Long divisionId, Long draftId, Integer expectedLock, LeaderboardConfig config) {
        BasketLeaderboardPolicy draft = requireDraft(divisionId, draftId);
        if (expectedLock == null || !expectedLock.equals(draft.getExpectedLock())) {
            throw new ServiceException("规则已被其他人修改，请刷新后重试");
        }
        ScopeRef scope = resolve(divisionId, draft.getScopeKey());
        List<String> errors = LeaderboardValidator.validate(config, scope.stage, false);
        if (!errors.isEmpty()) {
            throw new ServiceException(String.join("；", errors));
        }
        String before = draft.getConfigJson();
        draft.setConfigJson(writeConfig(config));
        draft.setTemplateCode(config.getTemplateCode());
        draft.setRankingAudience(config.getRankingAudience());
        draft.setUpdateBy(currentUserId());
        draft.setUpdateTime(new Date());
        if (leaderboardMapper.updateDraft(draft) != 1) {
            throw new ServiceException("规则已被其他人修改，请刷新后重试");
        }
        BasketLeaderboardPolicy saved = leaderboardMapper.selectPolicyById(draftId);
        audit(saved, "SAVE", before, saved.getConfigJson(), "保存草稿");
        return saved;
    }

    @Transactional(rollbackFor = Exception.class)
    public BasketLeaderboardPolicy loadTemplate(Long divisionId, Long draftId, Integer expectedLock) {
        BasketLeaderboardPolicy draft = requireDraft(divisionId, draftId);
        ScopeRef scope = resolve(divisionId, draft.getScopeKey());
        LeaderboardConfig config = LeaderboardTemplates.forStage(scope.stage, stageIds(divisionId));
        return saveDraft(divisionId, draftId, expectedLock, config);
    }

    @Transactional(rollbackFor = Exception.class)
    public BasketLeaderboardPolicy validateDraft(Long divisionId, Long draftId, Integer expectedLock) {
        BasketLeaderboardPolicy draft = requireDraft(divisionId, draftId);
        if (expectedLock == null || !expectedLock.equals(draft.getExpectedLock())) {
            throw new ServiceException("规则已被其他人修改，请刷新后重试");
        }
        ScopeRef scope = resolve(divisionId, draft.getScopeKey());
        List<String> errors = LeaderboardValidator.validate(readConfig(draft.getConfigJson()), scope.stage, true);
        if (!errors.isEmpty()) {
            throw new ServiceException(String.join("；", errors));
        }
        draft.setLifecycle("READY");
        draft.setDraftScopeKey(draft.getScopeKey());
        draft.setActiveScopeKey(null);
        draft.setUpdateBy(currentUserId());
        draft.setUpdateTime(new Date());
        if (leaderboardMapper.updateLifecycle(draft) != 1) {
            throw new ServiceException("规则已被其他人修改，请刷新后重试");
        }
        BasketLeaderboardPolicy saved = leaderboardMapper.selectPolicyById(draftId);
        audit(saved, "VALIDATE", null, saved.getConfigJson(), "校验通过");
        return saved;
    }

    @Transactional(rollbackFor = Exception.class)
    public BasketLeaderboardPolicy reopenDraft(Long divisionId, Long draftId, Integer expectedLock) {
        BasketLeaderboardPolicy draft = requireDraft(divisionId, draftId);
        if (!"READY".equals(draft.getLifecycle())) {
            throw new ServiceException("只有待发布草稿可以退回编辑");
        }
        if (expectedLock == null || !expectedLock.equals(draft.getExpectedLock())) {
            throw new ServiceException("规则已被其他人修改，请刷新后重试");
        }
        draft.setLifecycle("DRAFT");
        draft.setDraftScopeKey(draft.getScopeKey());
        draft.setActiveScopeKey(null);
        draft.setUpdateBy(currentUserId());
        draft.setUpdateTime(new Date());
        if (leaderboardMapper.updateLifecycle(draft) != 1) {
            throw new ServiceException("规则已被其他人修改，请刷新后重试");
        }
        return leaderboardMapper.selectPolicyById(draftId);
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> preview(Long divisionId, Long draftId) {
        BasketLeaderboardPolicy draft = requireDraft(divisionId, draftId);
        LeaderboardConfig config = readConfig(draft.getConfigJson());
        List<LeaderboardSubject> subjects = compute(divisionId, draft.getScopeKey(), config);
        Long runId = persistRun(draft, "PREVIEW", subjects, null);
        Map<String, Object> body = summary(draft, subjects, runId);
        BasketLeaderboardPolicy active = leaderboardMapper.selectActive(divisionId, draft.getScopeKey());
        if (active != null) {
            body.put("diff", diff(config, readConfig(active.getConfigJson()), subjects,
                    compute(divisionId, active.getScopeKey(), readConfig(active.getConfigJson()))));
        }
        audit(draft, "PREVIEW", null, null, "试算 run=" + runId);
        return body;
    }

    public List<BasketLeaderboardEntry> eligibility(Long divisionId, Long runId, String subjectType, String status) {
        requireDivision(divisionId);
        List<BasketLeaderboardEntry> rows = leaderboardMapper.selectEntriesByRun(runId, subjectType);
        if (rows.isEmpty() || !divisionId.equals(rows.get(0).getDivisionId())) {
            throw new ServiceException("试算结果不存在");
        }
        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            return rows;
        }
        boolean eligible = "ELIGIBLE".equalsIgnoreCase(status);
        List<BasketLeaderboardEntry> filtered = new ArrayList<>();
        for (BasketLeaderboardEntry row : rows) {
            boolean rowEligible = Integer.valueOf(1).equals(row.getEligible());
            if (rowEligible == eligible) {
                filtered.add(row);
            }
        }
        return filtered;
    }

    @Transactional(rollbackFor = Exception.class)
    public Map<String, Object> publish(Long divisionId, Long draftId, Integer expectedActiveVersion, Integer expectedLock, String reason) {
        divisionMapper.selectByIdForUpdate(divisionId);
        BasketLeaderboardPolicy draft = requireDraft(divisionId, draftId);
        if (!"READY".equals(draft.getLifecycle())) {
            throw new ServiceException("请先校验通过再发布");
        }
        if (expectedLock == null || !expectedLock.equals(draft.getExpectedLock())) {
            throw new ServiceException("规则已被其他人修改，请刷新后重试");
        }
        BasketLeaderboardPolicy active = leaderboardMapper.selectActive(divisionId, draft.getScopeKey());
        int currentVersion = active == null ? 0 : active.getVersionNo();
        int expected = expectedActiveVersion == null ? 0 : expectedActiveVersion;
        if (currentVersion != expected) {
            throw new ServiceException("生效版本已变化，请刷新后重试");
        }
        ScopeRef scope = resolve(divisionId, draft.getScopeKey());
        LeaderboardConfig config = readConfig(draft.getConfigJson());
        List<String> errors = LeaderboardValidator.validate(config, scope.stage, true);
        if (!errors.isEmpty()) {
            throw new ServiceException(String.join("；", errors));
        }
        List<LeaderboardSubject> subjects = compute(divisionId, draft.getScopeKey(), config);
        String revision = sourceRevision(divisionId);
        Long runId = persistRun(draft, "PUBLISH", subjects, revision);
        switchPublic(draft, runId, revision);
        leaderboardMapper.clearActive(divisionId, draft.getScopeKey(), draft.getId());
        activate(draft, reason);
        audit(draft, "PUBLISH", active == null ? null : active.getConfigJson(), draft.getConfigJson(), reason);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("policy", leaderboardMapper.selectPolicyById(draft.getId()));
        body.put("snapshot", leaderboardMapper.selectPublicSnapshot(divisionId, draft.getScopeKey()));
        body.put("runId", runId);
        return body;
    }

    @Transactional(rollbackFor = Exception.class)
    public BasketLeaderboardPolicy archive(Long divisionId, String scopeKey, Integer expectedLock, String reason) {
        divisionMapper.selectByIdForUpdate(divisionId);
        BasketLeaderboardPolicy active = leaderboardMapper.selectActive(divisionId, scopeKey);
        if (active == null) {
            throw new ServiceException("没有可归档的生效规则");
        }
        if (expectedLock == null || !expectedLock.equals(active.getExpectedLock())) {
            throw new ServiceException("规则已被其他人修改，请刷新后重试");
        }
        if (reason == null || reason.isBlank()) {
            throw new ServiceException("归档需要填写说明");
        }
        active.setLifecycle("ARCHIVED");
        active.setDraftScopeKey(null);
        active.setActiveScopeKey(null);
        active.setPublishNote(reason.trim());
        active.setUpdateBy(currentUserId());
        active.setUpdateTime(new Date());
        if (leaderboardMapper.updateLifecycle(active) != 1) {
            throw new ServiceException("规则已被其他人修改，请刷新后重试");
        }
        BasketLeaderboardPolicy saved = leaderboardMapper.selectPolicyById(active.getId());
        audit(saved, "ARCHIVE", null, null, reason.trim());
        return saved;
    }

    public void scheduleRecalc(Long divisionId, Long stageId) {
        if (divisionId == null) {
            return;
        }
        Runnable job = () -> {
            try {
                recalculate(divisionId, stageId);
            } catch (Exception ex) {
                log.warn("排行榜重算失败 divisionId={} stageId={}", divisionId, stageId, ex);
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    job.run();
                }
            });
            return;
        }
        job.run();
    }

    /**
     * 每个范围单独提交。失败只记一条失败作业，已经公开的快照保持不动。
     * 不用方法上的 REQUIRES_NEW：审核回调是同类自调用，注解不会生效。
     */
    public void recalculate(Long divisionId, Long stageId) {
        List<BasketLeaderboardPolicy> policies = leaderboardMapper.selectPolicies(divisionId);
        for (BasketLeaderboardPolicy policy : policies) {
            if (!"ACTIVE".equals(policy.getLifecycle()) || !affects(policy, stageId)) {
                continue;
            }
            try {
                requiresNew().executeWithoutResult(status -> recalculateOne(policy.getId()));
            } catch (Exception ex) {
                log.warn("排行榜范围重算失败 policyId={}", policy.getId(), ex);
                try {
                    requiresNew().executeWithoutResult(status -> recordFailed(policy, ex));
                } catch (Exception logEx) {
                    log.warn("排行榜失败记录没有写入 policyId={}", policy.getId(), logEx);
                }
            }
        }
    }

    public List<BasketLeaderboardRun> runs(Long divisionId, String scopeKey) {
        requireDivision(divisionId);
        return leaderboardMapper.selectRuns(divisionId, scopeKey);
    }

    public List<BasketLeaderboardPolicyAudit> audits(Long divisionId) {
        requireDivision(divisionId);
        return leaderboardMapper.selectAudits(divisionId);
    }

    @Transactional(rollbackFor = Exception.class)
    public void updateFeatured(Long divisionId, String reason) {
        if (divisionId != null) {
            requireDivision(divisionId);
        }
        if (leaderboardMapper.selectSetting() == null) {
            throw new ServiceException("排行设置表尚未初始化");
        }
        leaderboardMapper.updateFeatured(divisionId, currentUserId(), new Date());
        BasketLeaderboardPolicyAudit audit = new BasketLeaderboardPolicyAudit();
        audit.setDivisionId(divisionId == null ? 0L : divisionId);
        audit.setAction("FEATURE");
        audit.setOperatorId(currentUserId());
        audit.setOperatorName(currentUserName());
        audit.setReason(reason);
        audit.setAfterJson(divisionId == null ? null : String.valueOf(divisionId));
        audit.setCreateTime(new Date());
        leaderboardMapper.insertAudit(audit);
    }

    public Long featuredDivisionId() {
        var setting = leaderboardMapper.selectSetting();
        return setting == null ? null : setting.getFeaturedDivisionId();
    }

    private void recalculateOne(Long policyId) {
        BasketLeaderboardPolicy policy = leaderboardMapper.selectPolicyById(policyId);
        if (policy == null || !"ACTIVE".equals(policy.getLifecycle())) {
            return;
        }
        divisionMapper.selectByIdForUpdate(policy.getDivisionId());
        String revision = sourceRevision(policy.getDivisionId());
        BasketLeaderboardSnapshot current = leaderboardMapper.selectPublicSnapshot(policy.getDivisionId(), policy.getScopeKey());
        if (current != null && revision.equals(current.getSourceRevision())) {
            return;
        }
        LeaderboardConfig config = readConfig(policy.getConfigJson());
        List<LeaderboardSubject> subjects = compute(policy.getDivisionId(), policy.getScopeKey(), config);
        Long runId = persistRun(policy, "RECALC", subjects, revision);
        switchPublic(policy, runId, revision);
        audit(policy, "RECALC", null, null, "审核后重算 run=" + runId);
    }

    private void recordFailed(BasketLeaderboardPolicy policy, Exception ex) {
        BasketLeaderboardRun failed = new BasketLeaderboardRun();
        failed.setDivisionId(policy.getDivisionId());
        failed.setScopeKey(policy.getScopeKey());
        failed.setPolicyId(policy.getId());
        failed.setPurpose("RECALC");
        failed.setState("FAILED");
        failed.setErrorMessage(trim(ex.getMessage()));
        failed.setStartedTime(new Date());
        failed.setFinishedTime(new Date());
        leaderboardMapper.insertRun(failed);
    }

    private TransactionTemplate requiresNew() {
        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        return template;
    }

    private boolean affects(BasketLeaderboardPolicy policy, Long stageId) {
        if (stageId == null) {
            return true;
        }
        if (LeaderboardScopes.isOverall(policy.getScopeKey())) {
            LeaderboardConfig config = readConfig(policy.getConfigJson());
            return config.getOverallStageIds() != null && config.getOverallStageIds().contains(stageId);
        }
        return stageId.equals(LeaderboardScopes.stageId(policy.getScopeKey()));
    }

    private void switchPublic(BasketLeaderboardPolicy policy, Long runId, String revision) {
        BasketLeaderboardSnapshot snapshot = new BasketLeaderboardSnapshot();
        snapshot.setRunId(runId);
        snapshot.setDivisionId(policy.getDivisionId());
        snapshot.setScopeKey(policy.getScopeKey());
        snapshot.setPolicyId(policy.getId());
        snapshot.setSourceRevision(revision);
        snapshot.setIsPublic(0);
        leaderboardMapper.insertSnapshot(snapshot);
        leaderboardMapper.bindSnapshot(runId, snapshot.getId());
        leaderboardMapper.clearPublic(policy.getDivisionId(), policy.getScopeKey());
        leaderboardMapper.markPublic(snapshot.getId(), policy.getScopeKey(), new Date());
    }

    private void activate(BasketLeaderboardPolicy draft, String reason) {
        draft.setLifecycle("ACTIVE");
        draft.setDraftScopeKey(null);
        draft.setActiveScopeKey(draft.getScopeKey());
        draft.setPublishNote(reason);
        draft.setUpdateBy(currentUserId());
        draft.setUpdateTime(new Date());
        if (leaderboardMapper.updateLifecycle(draft) != 1) {
            throw new ServiceException("规则已被其他人修改，请刷新后重试");
        }
    }

    private Long persistRun(BasketLeaderboardPolicy policy, String purpose, List<LeaderboardSubject> subjects, String revision) {
        BasketLeaderboardRun run = new BasketLeaderboardRun();
        run.setDivisionId(policy.getDivisionId());
        run.setScopeKey(policy.getScopeKey());
        run.setPolicyId(policy.getId());
        run.setPurpose(purpose);
        run.setSourceRevision(revision == null ? sourceRevision(policy.getDivisionId()) : revision);
        run.setState("SUCCESS");
        run.setStartedTime(new Date());
        run.setFinishedTime(new Date());
        leaderboardMapper.insertRun(run);
        List<BasketLeaderboardEntry> rows = new ArrayList<>();
        for (LeaderboardSubject subject : subjects) {
            rows.add(toEntry(policy, run.getId(), subject));
        }
        if (!rows.isEmpty()) {
            leaderboardMapper.insertEntries(rows);
        }
        return run.getId();
    }

    private BasketLeaderboardEntry toEntry(BasketLeaderboardPolicy policy, Long runId, LeaderboardSubject subject) {
        BasketLeaderboardEntry row = new BasketLeaderboardEntry();
        row.setRunId(runId);
        row.setDivisionId(policy.getDivisionId());
        row.setScopeKey(policy.getScopeKey());
        row.setSubjectType(subject.getSubjectType());
        row.setEventTeamId(subject.getEventTeamId());
        row.setEventPlayerId(subject.getEventPlayerId() == null ? 0L : subject.getEventPlayerId());
        row.setEligible(subject.isEligible() ? 1 : 0);
        row.setFailedConditions(subject.getFailedConditions().isEmpty() ? null : String.join(",", subject.getFailedConditions()));
        row.setValidGames(subject.getValidGames());
        row.setPlayedRounds(subject.getPlayedRounds());
        row.setPlayedSeries(subject.getPlayedSeries());
        row.setDueFixtureRate(subject.getDueFixtureRate());
        row.setRequiredGames(subject.getRequiredGames());
        row.setRankNo(subject.getRankNo());
        row.setPoints(subject.getPoints());
        row.setRebound(subject.getRebound());
        row.setAssist(subject.getAssist());
        row.setSteal(subject.getSteal());
        row.setBlock(subject.getBlock());
        row.setTurnover(subject.getTurnover());
        row.setWins(subject.getWins());
        row.setLosses(subject.getLosses());
        row.setFieldGoalMade(subject.getFieldGoalMade());
        row.setFieldGoalAttempt(subject.getFieldGoalAttempt());
        row.setThreePointMade(subject.getThreePointMade());
        row.setThreePointAttempt(subject.getThreePointAttempt());
        row.setFreeThrowMade(subject.getFreeThrowMade());
        row.setFreeThrowAttempt(subject.getFreeThrowAttempt());
        return row;
    }

    private List<LeaderboardSubject> compute(Long divisionId, String scopeKey, LeaderboardConfig config) {
        Set<Long> stageIds = new HashSet<>();
        if (LeaderboardScopes.isOverall(scopeKey)) {
            if (config.getOverallStageIds() != null) {
                stageIds.addAll(config.getOverallStageIds());
            }
        } else {
            Long stageId = LeaderboardScopes.stageId(scopeKey);
            if (stageId != null) {
                stageIds.add(stageId);
            }
        }
        List<LeaderboardSubject> subjects = LeaderboardAggregator.aggregate(
                leaderboardMapper.selectPublishedMatches(divisionId),
                leaderboardMapper.selectTeamBoxes(divisionId),
                leaderboardMapper.selectPlayerBoxes(divisionId),
                stageIds,
                new Date());
        LeaderboardEligibilityEngine.apply(subjects, config);
        List<LeaderboardSubject> teams = new ArrayList<>();
        List<LeaderboardSubject> players = new ArrayList<>();
        for (LeaderboardSubject subject : subjects) {
            if (LeaderboardSubject.TEAM.equals(subject.getSubjectType())) {
                teams.add(subject);
            } else {
                players.add(subject);
            }
        }
        LeaderboardRanker.rank(teams, "PTS", "average", config);
        LeaderboardRanker.rank(players, "PTS", "average", config);
        return subjects;
    }

    private Map<String, Object> summary(BasketLeaderboardPolicy policy, List<LeaderboardSubject> subjects, Long runId) {
        int teamEligible = 0;
        int teamAll = 0;
        int playerEligible = 0;
        int playerAll = 0;
        List<LeaderboardSubject> top = new ArrayList<>();
        for (LeaderboardSubject subject : subjects) {
            if (LeaderboardSubject.TEAM.equals(subject.getSubjectType())) {
                teamAll++;
                if (subject.isEligible()) {
                    teamEligible++;
                    if (subject.getRankNo() != null && subject.getRankNo() <= 10) {
                        top.add(subject);
                    }
                }
            } else {
                playerAll++;
                if (subject.isEligible()) {
                    playerEligible++;
                }
            }
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("runId", runId);
        body.put("policyId", policy.getId());
        body.put("scopeKey", policy.getScopeKey());
        body.put("teamCount", teamAll);
        body.put("teamEligible", teamEligible);
        body.put("playerCount", playerAll);
        body.put("playerEligible", playerEligible);
        body.put("topTeams", top);
        body.put("emptyReason", teamEligible == 0 ? "NO_ELIGIBLE_SUBJECTS" : null);
        return body;
    }

    private Map<String, Object> diff(LeaderboardConfig draft, LeaderboardConfig active,
                                     List<LeaderboardSubject> draftSubjects, List<LeaderboardSubject> activeSubjects) {
        List<String> changes = new ArrayList<>();
        compare(changes, "球队最低局数", draft.getTeam().getMinimumTeamGames(), active.getTeam().getMinimumTeamGames());
        compare(changes, "球队轮次", draft.getTeam().getMinimumTeamPlayedRounds(), active.getTeam().getMinimumTeamPlayedRounds());
        compare(changes, "球队系列赛", draft.getTeam().getMinimumTeamPlayedSeries(), active.getTeam().getMinimumTeamPlayedSeries());
        compare(changes, "履行率", draft.getTeam().getMinimumDueFixtureRate(), active.getTeam().getMinimumDueFixtureRate());
        compare(changes, "球员最低局数", draft.getPlayer().getMinimumPlayerGames(), active.getPlayer().getMinimumPlayerGames());
        compare(changes, "球员出场比例", draft.getPlayer().getMinimumPlayerTeamRate(), active.getPlayer().getMinimumPlayerTeamRate());
        Set<Long> draftTeams = eligibleTeams(draftSubjects);
        Set<Long> activeTeams = eligibleTeams(activeSubjects);
        List<Long> entered = new ArrayList<>(draftTeams);
        entered.removeAll(activeTeams);
        List<Long> dropped = new ArrayList<>(activeTeams);
        dropped.removeAll(draftTeams);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("changes", changes);
        body.put("enteredTeamIds", entered);
        body.put("droppedTeamIds", dropped);
        return body;
    }

    private Set<Long> eligibleTeams(List<LeaderboardSubject> subjects) {
        Set<Long> ids = new HashSet<>();
        for (LeaderboardSubject subject : subjects) {
            if (LeaderboardSubject.TEAM.equals(subject.getSubjectType()) && subject.isEligible()) {
                ids.add(subject.getEventTeamId());
            }
        }
        return ids;
    }

    private void compare(List<String> changes, String label, Object left, Object right) {
        String l = left == null ? "空" : String.valueOf(left);
        String r = right == null ? "空" : String.valueOf(right);
        if (!l.equals(r)) {
            changes.add(label + "从 " + r + " 改为 " + l);
        }
    }

    private String sourceRevision(Long divisionId) {
        int count = leaderboardMapper.countTeamStats(divisionId);
        Date max = leaderboardMapper.maxTeamStatUpdateTime(divisionId);
        int matches = leaderboardMapper.selectPublishedMatches(divisionId).size();
        return count + ":" + (max == null ? 0 : max.getTime()) + ":" + matches;
    }

    private Map<String, Object> scopeRow(Long divisionId, String scopeKey, BasketDivisionStage stage) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("scopeKey", scopeKey);
        row.put("stageId", stage == null ? null : stage.getId());
        row.put("stageName", stage == null ? "全赛事" : stage.getStageName());
        row.put("stageType", stage == null ? LeaderboardScopes.OVERALL : stage.getStageType());
        row.put("competitionFormat", stage == null ? null : stage.getCompetitionFormat());
        row.put("bestOf", stage == null ? null : stage.getBestOf());
        row.put("doubleElimination", LeaderboardTemplates.blocksReady(stage));
        row.put("roundLimitsForbidden", LeaderboardTemplates.roundLimitsForbidden(stage));
        row.put("draft", leaderboardMapper.selectOpenDraft(divisionId, scopeKey));
        row.put("active", leaderboardMapper.selectActive(divisionId, scopeKey));
        return row;
    }

    private ScopeRef resolve(Long divisionId, String scopeKey) {
        requireDivision(divisionId);
        if (scopeKey == null || scopeKey.isBlank()) {
            throw new ServiceException("请选择统计范围");
        }
        if (LeaderboardScopes.isOverall(scopeKey)) {
            return new ScopeRef(LeaderboardScopes.OVERALL, null);
        }
        Long stageId = LeaderboardScopes.stageId(scopeKey);
        BasketDivisionStage stage = stageId == null ? null : stageMapper.selectById(stageId);
        if (stage == null || !divisionId.equals(stage.getDivisionId()) || Integer.valueOf(1).equals(stage.getIsDelete())) {
            throw new ServiceException("统计范围不属于该赛区");
        }
        return new ScopeRef(LeaderboardScopes.stage(stage.getId()), stage);
    }

    private List<Long> stageIds(Long divisionId) {
        List<Long> ids = new ArrayList<>();
        for (BasketDivisionStage stage : stageMapper.selectByDivisionId(divisionId)) {
            if (stage.getId() != null && !Integer.valueOf(1).equals(stage.getIsDelete())) {
                ids.add(stage.getId());
            }
        }
        return ids;
    }

    private BasketLeaderboardPolicy requireDraft(Long divisionId, Long draftId) {
        BasketLeaderboardPolicy draft = leaderboardMapper.selectPolicyById(draftId);
        if (draft == null || !divisionId.equals(draft.getDivisionId())) {
            throw new ServiceException("草稿不存在");
        }
        if (!"DRAFT".equals(draft.getLifecycle()) && !"READY".equals(draft.getLifecycle())) {
            throw new ServiceException("已发布规则不能直接修改");
        }
        return draft;
    }

    private void requireDivision(Long divisionId) {
        BasketDivision division = divisionMapper.selectById(divisionId);
        if (division == null || Integer.valueOf(1).equals(division.getIsDelete())) {
            throw new ServiceException("赛区不存在");
        }
    }

    private void audit(BasketLeaderboardPolicy policy, String action, String before, String after, String reason) {
        BasketLeaderboardPolicyAudit row = new BasketLeaderboardPolicyAudit();
        row.setPolicyId(policy.getId());
        row.setDivisionId(policy.getDivisionId());
        row.setScopeKey(policy.getScopeKey());
        row.setAction(action);
        row.setOperatorId(currentUserId());
        row.setOperatorName(currentUserName());
        row.setBeforeJson(before);
        row.setAfterJson(after);
        row.setReason(reason);
        row.setCreateTime(new Date());
        leaderboardMapper.insertAudit(row);
    }

    private LeaderboardConfig readConfig(String json) {
        try {
            return objectMapper.readValue(json, LeaderboardConfig.class);
        } catch (JsonProcessingException ex) {
            throw new ServiceException("规则配置无法解析");
        }
    }

    private String writeConfig(LeaderboardConfig config) {
        try {
            return objectMapper.writeValueAsString(config);
        } catch (JsonProcessingException ex) {
            throw new ServiceException("规则配置无法保存");
        }
    }

    private Long currentUserId() {
        try {
            return SecurityUtils.getUserId();
        } catch (Exception ex) {
            return null;
        }
    }

    private String currentUserName() {
        try {
            return SecurityUtils.getUsername();
        } catch (Exception ex) {
            return null;
        }
    }

    private String trim(String message) {
        if (message == null) {
            return "重算失败";
        }
        return message.length() > 900 ? message.substring(0, 900) : message;
    }

    private record ScopeRef(String key, BasketDivisionStage stage) {
    }
}
