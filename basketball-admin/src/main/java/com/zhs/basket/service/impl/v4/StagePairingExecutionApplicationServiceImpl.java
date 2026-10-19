package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketDivisionStageTeam;
import com.zhs.basket.domain.v4.BasketDivisionGroup;
import com.zhs.basket.domain.v4.BasketDivisionGroupingBatch;
import com.zhs.basket.domain.v4.BasketEventTeam;
import com.zhs.basket.domain.v4.BasketMatchSeries;
import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.domain.v4.BasketStagePairingEntry;
import com.zhs.basket.domain.v4.BasketStagePairingExecution;
import com.zhs.basket.dto.v4.StageExternalRankingReq;
import com.zhs.basket.dto.v4.StageManualSlotReq;
import com.zhs.basket.dto.v4.StagePairingSeedReq;
import com.zhs.basket.enums.v4.ByeRuleEnum;
import com.zhs.basket.enums.v4.DrawRuleEnum;
import com.zhs.basket.enums.v4.GroupingStatusEnum;
import com.zhs.basket.enums.v4.PairingExecutionStatusEnum;
import com.zhs.basket.enums.v4.StageTeamEntryTypeEnum;
import com.zhs.basket.enums.v4.StageTeamQualificationStatusEnum;
import com.zhs.basket.enums.v4.PairingRuleEnum;
import com.zhs.basket.enums.v4.ParticipantResolutionStatusEnum;
import com.zhs.basket.enums.v4.ParticipantSourceTypeEnum;
import com.zhs.basket.enums.v4.PublishStatusEnum;
import com.zhs.basket.enums.v4.StageExecutionStatusEnum;
import com.zhs.basket.mapper.v4.BasketDivisionGroupMapper;
import com.zhs.basket.mapper.v4.BasketDivisionGroupingBatchMapper;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageTeamMapper;
import com.zhs.basket.mapper.v4.BasketEventTeamMapper;
import com.zhs.basket.mapper.v4.BasketMatchSeriesMapper;
import com.zhs.basket.mapper.v4.BasketMatchSubmissionMapper;
import com.zhs.basket.mapper.v4.BasketScheduleBatchMapper;
import com.zhs.basket.mapper.v4.BasketStagePairingEntryMapper;
import com.zhs.basket.mapper.v4.BasketStagePairingExecutionMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.req.AuditFieldInput;
import com.zhs.basket.service.v4.IBasketV4StructureVersionService;
import com.zhs.basket.service.v4.IPlaceholderResolutionApplicationService;
import com.zhs.basket.service.v4.IStagePairingExecutionApplicationService;
import com.zhs.basket.support.BasketBusinessAuditWriter;
import com.zhs.basket.support.v4.bracket.BracketParticipant;
import com.zhs.basket.support.v4.bracket.BracketPreviewBuilder;
import com.zhs.basket.support.v4.bracket.GroupCrossPlacementRequest;
import com.zhs.basket.support.v4.bracket.GroupCrossPlacementStrategy;
import com.zhs.basket.support.v4.bracket.GroupLayout;
import com.zhs.basket.support.v4.bracket.GroupRankPlacementSupport;
import com.zhs.basket.support.v4.bracket.ParticipantSourceLabels;
import com.zhs.basket.support.v4.bracket.StructureParticipantStatus;
import com.zhs.basket.support.v4.bracket.BracketSlot;
import com.zhs.basket.support.v4.bracket.BracketSlotPlan;
import com.zhs.basket.support.v4.bracket.ByeAllocationRequest;
import com.zhs.basket.support.v4.bracket.ByeAllocationStrategyFactory;
import com.zhs.basket.support.v4.bracket.EliminationCapacity;
import com.zhs.basket.support.v4.bracket.SeededBracketPlacement;
import com.zhs.basket.support.v4.roster.TeamRosterSupport;
import com.zhs.basket.support.v4.version.StructureVersionContext;
import com.zhs.basket.vo.v4.BracketPreviewVo;
import com.zhs.basket.vo.v4.StagePairingEntryVo;
import com.zhs.basket.vo.v4.StagePairingExecutionVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

@Service
public class StagePairingExecutionApplicationServiceImpl implements IStagePairingExecutionApplicationService {

    private static final int NOT_DELETED = 0;

    @Autowired
    private BasketDivisionMapper divisionMapper;
    @Autowired
    private BasketDivisionStageMapper stageMapper;
    @Autowired
    private BasketDivisionStageTeamMapper stageTeamMapper;
    @Autowired
    private BasketEventTeamMapper eventTeamMapper;
    @Autowired
    private BasketStagePairingExecutionMapper executionMapper;
    @Autowired
    private BasketStagePairingEntryMapper entryMapper;
    @Autowired
    private BasketScheduleBatchMapper scheduleBatchMapper;
    @Autowired
    private BasketV4MatchMapper matchMapper;
    @Autowired
    private BasketMatchSeriesMapper matchSeriesMapper;
    @Autowired
    private BasketMatchSubmissionMapper submissionMapper;
    @Autowired
    private IBasketV4StructureVersionService structureVersionService;
    @Autowired
    private ByeAllocationStrategyFactory byeAllocationStrategyFactory;
    @Autowired
    private BasketBusinessAuditWriter auditWriter;
    @Autowired
    private TeamRosterSupport teamRosterSupport;
    @Autowired
    private BasketDivisionGroupingBatchMapper groupingBatchMapper;
    @Autowired
    private BasketDivisionGroupMapper groupMapper;
    @Autowired
    private GroupCrossPlacementStrategy groupCrossPlacementStrategy;
    @Autowired
    @Lazy
    private IPlaceholderResolutionApplicationService placeholderResolutionApplicationService;

    @Override
    public StagePairingExecutionVo getCurrent(Long stageId) {
        BasketDivisionStage stage = requireStage(stageId);
        BasketStagePairingExecution execution = executionMapper.selectCurrentByStageId(stageId);
        if (execution == null) {
            return emptyWorkspace(stage);
        }
        return toVo(stage, execution, entryMapper.selectByExecutionId(execution.getId()));
    }

    @Override
    public BracketPreviewVo previewBracket(Long stageId) {
        StagePairingExecutionVo current = getCurrent(stageId);
        if (current.getBracketPreview() != null) {
            return current.getBracketPreview();
        }
        BasketDivisionStage stage = requireStage(stageId);
        if (isGroupRankPlacement(stage)) {
            throw new ServiceException("请先生成 GROUP_CROSS / MANUAL 占位");
        }
        List<BasketDivisionStageTeam> teams = requireFormalStageTeams(stage);
        BracketSlotPlan plan = SeededBracketPlacement.placeBySeed(toParticipants(teams, Map.of()));
        return BracketPreviewBuilder.build(stage, plan, teamNames(teams));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StagePairingExecutionVo saveAdminSeeds(Long stageId, StagePairingSeedReq req) {
        BasketDivisionStage stage = requireStage(stageId);
        lockDivision(stage.getDivisionId());
        assertMutable(stage);
        List<BasketDivisionStageTeam> teams = requireFormalStageTeams(stage);
        Set<Long> formalIds = teamIds(teams);
        List<Long> seedIds = req == null ? List.of() : req.getSeedEventTeamIds();
        int maxSeeds = Math.min(4, teams.size());
        if (seedIds.size() > maxSeeds) {
            throw new ServiceException("管理员种子最多 " + maxSeeds + " 支");
        }
        Set<Long> seen = new HashSet<>();
        for (Long teamId : seedIds) {
            if (teamId == null || !formalIds.contains(teamId)) {
                throw new ServiceException("种子队必须来自当前 Locked Roster 的 FORMAL EventTeam");
            }
            if (!seen.add(teamId)) {
                throw new ServiceException("种子不能重复");
            }
        }
        BasketStagePairingExecution execution = requireDraftExecution(stage);
        Map<Long, Integer> ranking = loadRanking(execution);
        for (int i = 0; i < seedIds.size(); i++) {
            ranking.put(seedIds.get(i), i + 1);
        }
        persistParticipants(execution, teams, ranking, seedIds);
        audit("SEED_SET", stage, execution, "设置管理员种子");
        return getCurrent(stageId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StagePairingExecutionVo saveExternalRanking(Long stageId, StageExternalRankingReq req) {
        BasketDivisionStage stage = requireStage(stageId);
        lockDivision(stage.getDivisionId());
        assertMutable(stage);
        List<BasketDivisionStageTeam> teams = requireFormalStageTeams(stage);
        Set<Long> formalIds = teamIds(teams);
        Map<Long, Integer> ranking = new HashMap<>();
        Set<Integer> usedRank = new HashSet<>();
        if (req != null && req.getItems() != null) {
            for (StageExternalRankingReq.Item item : req.getItems()) {
                if (item.getEventTeamId() == null || !formalIds.contains(item.getEventTeamId())) {
                    throw new ServiceException("外部排名只能使用当前 FORMAL EventTeam");
                }
                if (item.getRankingNo() == null || item.getRankingNo() <= 0) {
                    throw new ServiceException("ranking_no 必须为正整数");
                }
                if (!usedRank.add(item.getRankingNo())) {
                    throw new ServiceException("ranking_no 不能重复");
                }
                ranking.put(item.getEventTeamId(), item.getRankingNo());
            }
        }
        if (ranking.size() != teams.size()) {
            throw new ServiceException("外部排名必须覆盖全部 FORMAL 参赛队");
        }
        BasketStagePairingExecution execution = requireDraftExecution(stage);
        persistParticipants(execution, teams, ranking, List.of());
        audit("EXTERNAL_RANKING", stage, execution, "设置外部排名");
        return getCurrent(stageId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StagePairingExecutionVo generateRandomDraw(Long stageId) {
        BasketDivisionStage stage = requireStage(stageId);
        lockDivision(stage.getDivisionId());
        assertMutable(stage);
        List<BasketDivisionStageTeam> teams = requireFormalStageTeams(stage);
        BasketStagePairingExecution execution = requireDraftExecution(stage);
        Map<Long, Integer> ranking = loadRanking(execution);
        List<Long> adminSeeds = loadAdminSeedOrder(execution);
        long randomSeed = execution.getRandomSeed() != null
                ? execution.getRandomSeed() : ThreadLocalRandom.current().nextLong();
        List<BracketParticipant> participants = assignDrawSeeds(teams, ranking, adminSeeds, randomSeed);
        EliminationCapacity capacity = EliminationCapacity.of(teams.size());
        Set<Long> byeTeamIds = byeAllocationStrategyFactory.resolve(stage.getByeRule())
                .allocateByeTeamIds(participants, capacity, ByeAllocationRequest.builder()
                        .randomSeed(randomSeed)
                        .build());
        BracketSlotPlan plan = byeTeamIds.isEmpty()
                ? SeededBracketPlacement.placeBySeed(participants)
                : SeededBracketPlacement.placeWithByeTeams(participants, byeTeamIds);
        replaceEntries(execution, plan);
        execution.setRandomSeed(randomSeed);
        execution.setGeneratedTime(new Date());
        execution.setStatus(PairingExecutionStatusEnum.DRAFT.getCode());
        execution.setOperatorId(operatorId());
        executionMapper.updateById(execution);
        audit("RANDOM_DRAW", stage, execution, "生成随机抽签");
        return getCurrent(stageId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StagePairingExecutionVo confirm(Long stageId) {
        BasketDivisionStage stage = requireStage(stageId);
        lockDivision(stage.getDivisionId());
        assertMutable(stage);
        BasketStagePairingExecution execution = executionMapper.selectCurrentByStageIdForUpdate(stageId);
        if (execution == null) {
            throw new ServiceException("没有可确认的抽签执行");
        }
        if (PairingExecutionStatusEnum.CONFIRMED.getCode().equals(execution.getStatus())) {
            return getCurrent(stageId);
        }
        List<BasketStagePairingEntry> entries = entryMapper.selectByExecutionId(execution.getId());
        if (isGroupRankPlacement(stage)) {
            validateGroupRankSlots(stage, entries);
        } else {
            validateCompleteSlots(stage, requireFormalStageTeams(stage), entries);
        }
        Date now = new Date();
        execution.setStatus(PairingExecutionStatusEnum.CONFIRMED.getCode());
        execution.setConfirmedTime(now);
        execution.setConfirmedBy(operatorId());
        execution.setIsCurrent(1);
        execution.setCurrentUniqueKey("STAGE:" + stageId);
        executionMapper.updateById(execution);
        audit("DRAW_CONFIRM", stage, execution, "确认抽签");
        return getCurrent(stageId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StagePairingExecutionVo saveManualSlots(Long stageId, StageManualSlotReq req) {
        BasketDivisionStage stage = requireStage(stageId);
        lockDivision(stage.getDivisionId());
        assertMutable(stage);
        if (isGroupRankPlacement(stage)) {
            return saveManualGroupRankSlots(stage, req);
        }
        List<BasketDivisionStageTeam> teams = requireFormalStageTeams(stage);
        EliminationCapacity capacity = EliminationCapacity.of(teams.size());
        if (req == null || req.getSlots() == null || req.getSlots().size() != capacity.getCapacity()) {
            throw new ServiceException("签位数量必须等于 bracket capacity " + capacity.getCapacity());
        }
        List<BracketSlot> slots = new ArrayList<>();
        Set<Long> usedTeams = new HashSet<>();
        int byeCount = 0;
        for (StageManualSlotReq.Slot item : req.getSlots()) {
            if (item.getSlotNo() == null) {
                throw new ServiceException("slot_no 不能为空");
            }
            boolean bye = Boolean.TRUE.equals(item.getBye()) || item.getEventTeamId() == null;
            if (bye) {
                if (item.getEventTeamId() != null) {
                    throw new ServiceException("BYE 签位不能绑定球队");
                }
                slots.add(BracketSlot.bye(item.getSlotNo()));
                byeCount++;
            } else {
                if (!usedTeams.add(item.getEventTeamId())) {
                    throw new ServiceException("每支 FORMAL 球队只能出现一次");
                }
                slots.add(BracketSlot.team(item.getSlotNo(), item.getEventTeamId(), null, null, null));
            }
        }
        if (usedTeams.size() != teams.size()) {
            throw new ServiceException("手动签位必须包含每支 FORMAL 球队恰好一次");
        }
        if (byeCount != capacity.getByeCount()) {
            throw new ServiceException("BYE 数量必须等于 capacity - 参赛队数");
        }
        slots.sort(Comparator.comparingInt(BracketSlot::getSlotNo));
        BasketStagePairingExecution execution = requireDraftExecution(stage);
        BracketSlotPlan plan = SeededBracketPlacement.fromSlots(slots, teams.size());
        replaceEntries(execution, plan);
        execution.setGeneratedTime(new Date());
        execution.setOperatorId(operatorId());
        executionMapper.updateById(execution);
        audit("MANUAL_SLOT", stage, execution, "保存手动签位");
        return getCurrent(stageId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StagePairingExecutionVo generateGroupCross(Long stageId) {
        BasketDivisionStage stage = requireStage(stageId);
        lockDivision(stage.getDivisionId());
        assertMutable(stage);
        if (!PairingRuleEnum.GROUP_CROSS.getCode().equals(stage.getPairingRule())) {
            throw new ServiceException("当前阶段 pairing_rule 不是 GROUP_CROSS");
        }
        BasketDivisionStage previous = findPreviousStage(stage);
        if (!GroupRankPlacementSupport.usesGroupRankPlacement(stage, previous)) {
            throw new ServiceException("GROUP_CROSS 需要上一阶段为分组循环赛");
        }
        BasketDivisionGroupingBatch grouping = groupingBatchMapper.selectActiveByDivisionAndStage(
                stage.getDivisionId(), previous.getId());
        if (grouping == null || !GroupingStatusEnum.LOCKED.getCode().equals(grouping.getGroupingStatus())) {
            throw new ServiceException("请先锁定小组分配，再生成淘汰占位结构");
        }
        List<BasketDivisionGroup> groups = groupMapper.selectByBatchId(grouping.getId());
        int advance = previous.getAdvancementCount() == null ? 0 : previous.getAdvancementCount();
        List<GroupLayout> layouts = new ArrayList<>();
        for (BasketDivisionGroup group : groups) {
            layouts.add(new GroupLayout(group.getId(), group.getGroupCode(), group.getGroupOrder()));
        }
        BracketSlotPlan plan = groupCrossPlacementStrategy.place(GroupCrossPlacementRequest.builder()
                .sourceStageId(previous.getId())
                .advancePerGroup(advance)
                .groups(layouts)
                .build());
        BasketStagePairingExecution execution = requireDraftExecution(stage);
        execution.setPlacementRule(PairingRuleEnum.GROUP_CROSS.getCode());
        execution.setSourceStageId(previous.getId());
        execution.setResolutionStatus(ParticipantResolutionStatusEnum.PENDING.getCode());
        replaceEntries(execution, plan);
        execution.setGeneratedTime(new Date());
        execution.setOperatorId(operatorId());
        executionMapper.updateById(execution);
        audit("GROUP_CROSS_GENERATE", stage, execution, "生成 GROUP_CROSS 淘汰占位");
        return getCurrent(stageId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StagePairingExecutionVo resolvePlaceholders(Long stageId) {
        return placeholderResolutionApplicationService.resolve(stageId);
    }

    private StagePairingExecutionVo saveManualGroupRankSlots(BasketDivisionStage stage, StageManualSlotReq req) {
        BasketDivisionStage previous = findPreviousStage(stage);
        BasketDivisionGroupingBatch grouping = groupingBatchMapper.selectActiveByDivisionAndStage(
                stage.getDivisionId(), previous.getId());
        if (grouping == null || !GroupingStatusEnum.LOCKED.getCode().equals(grouping.getGroupingStatus())) {
            throw new ServiceException("请先锁定小组分配");
        }
        List<BasketDivisionGroup> groups = groupMapper.selectByBatchId(grouping.getId());
        int advance = previous.getAdvancementCount() == null ? 0 : previous.getAdvancementCount();
        int qualifierCount = groups.size() * advance;
        EliminationCapacity capacity = EliminationCapacity.of(qualifierCount);
        if (req == null || req.getSlots() == null || req.getSlots().size() != capacity.getCapacity()) {
            throw new ServiceException("签位数量必须等于 bracket capacity " + capacity.getCapacity());
        }
        Map<Long, BasketDivisionGroup> groupById = new HashMap<>();
        for (BasketDivisionGroup group : groups) {
            groupById.put(group.getId(), group);
        }
        List<BracketSlot> slots = new ArrayList<>();
        Set<String> usedSources = new HashSet<>();
        int byeCount = 0;
        for (StageManualSlotReq.Slot item : req.getSlots()) {
            if (item.getSlotNo() == null) {
                throw new ServiceException("slot_no 不能为空");
            }
            boolean bye = Boolean.TRUE.equals(item.getBye())
                    || ParticipantSourceTypeEnum.BYE.getCode().equals(item.getSourceType());
            if (bye) {
                if (item.getSourceGroupId() != null || item.getEventTeamId() != null) {
                    throw new ServiceException("BYE 不能绑定小组名次或球队");
                }
                slots.add(BracketSlot.bye(item.getSlotNo()));
                byeCount++;
                continue;
            }
            if (item.getEventTeamId() != null) {
                throw new ServiceException("小组未结束前 MANUAL 只能填 GROUP_RANK 占位，不能填真实球队");
            }
            if (item.getSourceGroupId() == null || item.getSourceRank() == null) {
                throw new ServiceException("MANUAL 占位必须指定 sourceGroupId 与 sourceRank");
            }
            BasketDivisionGroup group = groupById.get(item.getSourceGroupId());
            if (group == null) {
                throw new ServiceException("source group 不合法");
            }
            if (item.getSourceRank() < 1 || item.getSourceRank() > advance) {
                throw new ServiceException("source rank 不合法");
            }
            String key = group.getId() + ":" + item.getSourceRank();
            if (!usedSources.add(key)) {
                throw new ServiceException("GROUP_RANK 来源不能重复: "
                        + ParticipantSourceLabels.displayCode(group.getGroupCode(), item.getSourceRank()));
            }
            slots.add(BracketSlot.groupRank(item.getSlotNo(), previous.getId(), group.getId(),
                    group.getGroupCode(), item.getSourceRank()));
        }
        if (usedSources.size() != qualifierCount) {
            throw new ServiceException("MANUAL 必须覆盖每个小组名次来源恰好一次");
        }
        if (byeCount != capacity.getByeCount()) {
            throw new ServiceException("BYE 数量必须等于 capacity - 晋级数");
        }
        slots.sort(Comparator.comparingInt(BracketSlot::getSlotNo));
        BracketSlotPlan plan = SeededBracketPlacement.fromSlots(slots, qualifierCount);
        BasketStagePairingExecution execution = requireDraftExecution(stage);
        execution.setPlacementRule(PairingRuleEnum.MANUAL.getCode());
        execution.setSourceStageId(previous.getId());
        execution.setResolutionStatus(ParticipantResolutionStatusEnum.PENDING.getCode());
        replaceEntries(execution, plan);
        execution.setGeneratedTime(new Date());
        execution.setOperatorId(operatorId());
        executionMapper.updateById(execution);
        audit("MANUAL_PLACEHOLDER_CONFIRM", stage, execution, "保存 MANUAL 小组名次占位");
        return getCurrent(stage.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StagePairingExecutionVo redraw(Long stageId) {
        BasketDivisionStage stage = requireStage(stageId);
        lockDivision(stage.getDivisionId());
        assertMutable(stage);
        BasketStagePairingExecution current = executionMapper.selectCurrentByStageIdForUpdate(stageId);
        discardUnpublishedSchedule(stage);
        if (current != null) {
            executionMapper.markSuperseded(current.getId());
        }
        BasketStagePairingExecution created = newDraft(stage);
        audit("REDRAW", stage, created, "重新抽签，旧执行保留为 SUPERSEDED");
        if (DrawRuleEnum.MANUAL_SLOT.getCode().equals(stage.getDrawRule())) {
            return getCurrent(stageId);
        }
        return generateRandomDraw(stageId);
    }

    public BracketSlotPlan loadConfirmedPlan(BasketDivisionStage stage) {
        BasketStagePairingExecution execution = executionMapper.selectCurrentByStageId(stage.getId());
        if (execution == null || !PairingExecutionStatusEnum.CONFIRMED.getCode().equals(execution.getStatus())) {
            throw new ServiceException("当前抽签尚未确认，无法生成结构");
        }
        List<BasketStagePairingEntry> entries = entryMapper.selectByExecutionId(execution.getId());
        Map<Long, String> groupCodes = loadGroupCodes(entries);
        List<BracketSlot> slots = new ArrayList<>();
        int participantCount = 0;
        for (BasketStagePairingEntry entry : entries) {
            BracketSlot slot = toBracketSlot(entry);
            if (entry.getSourceGroupId() != null) {
                slot.setSourceGroupCode(groupCodes.get(entry.getSourceGroupId()));
                slot.setDisplayCode(ParticipantSourceLabels.displayCode(slot.getSourceGroupCode(), entry.getSourceRank()));
                slot.setDisplayName(ParticipantSourceLabels.displayName(slot.getSourceGroupCode(), entry.getSourceRank()));
            }
            slots.add(slot);
            if (!Integer.valueOf(1).equals(entry.getIsBye())) {
                participantCount++;
            }
        }
        return SeededBracketPlacement.fromSlots(slots, participantCount);
    }

    private void assertMutable(BasketDivisionStage stage) {
        BasketScheduleBatch published = scheduleBatchMapper.selectPublishedByStageId(stage.getId());
        if (published != null && PublishStatusEnum.PUBLISHED.getCode().equals(published.getPublishStatus())) {
            throw new ServiceException("当前赛段已发布，需要先撤回后再调整");
        }
        if (matchMapper.countFinishedByStageId(stage.getId()) > 0) {
            throw new ServiceException("赛事已产生正式比赛结果，不能重建结构");
        }
        BasketStagePairingExecution current = executionMapper.selectCurrentByStageId(stage.getId());
        if (current != null && PairingExecutionStatusEnum.CONFIRMED.getCode().equals(current.getStatus())
                && scheduleBatchMapper.selectActiveByStageId(stage.getId()) != null
                && matchMapper.countFinishedByStageId(stage.getId()) > 0) {
            throw new ServiceException("赛事已产生正式比赛结果，不能重建结构");
        }
    }

    /**
     * 未发布、未开赛的草稿赛程在重新抽签时直接作废。已发布或已有正式结果的不删。
     */
    private void discardUnpublishedSchedule(BasketDivisionStage stage) {
        if (scheduleBatchMapper.selectPublishedByStageId(stage.getId()) != null) {
            throw new ServiceException("当前赛段已发布，需要先撤回后再调整");
        }
        if (matchMapper.countFinishedByStageId(stage.getId()) > 0) {
            throw new ServiceException("赛事已产生正式比赛结果，不能重建结构");
        }
        java.util.Set<Long> batchIds = new LinkedHashSet<>();
        if (stage.getScheduleBatchId() != null) {
            batchIds.add(stage.getScheduleBatchId());
        }
        for (BasketMatchSeries series : matchSeriesMapper.selectByStageId(stage.getId())) {
            if (series.getScheduleBatchId() != null) {
                batchIds.add(series.getScheduleBatchId());
            }
        }
        boolean removed = false;
        for (Long batchId : batchIds) {
            BasketScheduleBatch batch = scheduleBatchMapper.selectById(batchId);
            if (batch == null || Integer.valueOf(1).equals(batch.getIsDelete())) {
                continue;
            }
            if (PublishStatusEnum.PUBLISHED.getCode().equals(batch.getPublishStatus())) {
                throw new ServiceException("当前赛段已发布，需要先撤回后再调整");
            }
            if (submissionMapper.countByScheduleBatchId(batchId) > 0) {
                throw new ServiceException("该赛程已有比赛提交记录，无法作废");
            }
            matchMapper.softDeleteDraftByBatchId(batchId);
            matchSeriesMapper.softDeleteDraftByBatchId(batchId);
            if (scheduleBatchMapper.softDelete(batchId) != 1) {
                throw new ServiceException("作废赛程批次失败");
            }
            removed = true;
        }
        if (removed || stage.getScheduleBatchId() != null) {
            stage.setScheduleBatchId(null);
            stage.setExecutionStatus(StageExecutionStatusEnum.READY.getCode());
            stage.setUpdateTime(new Date());
            if (stageMapper.updateExecutionStatus(stage) != 1) {
                throw new ServiceException("更新阶段执行状态失败");
            }
        }
    }

    private BasketStagePairingExecution requireDraftExecution(BasketDivisionStage stage) {
        BasketStagePairingExecution current = executionMapper.selectCurrentByStageId(stage.getId());
        if (current != null && PairingExecutionStatusEnum.CONFIRMED.getCode().equals(current.getStatus())) {
            discardUnpublishedSchedule(stage);
            executionMapper.markSuperseded(current.getId());
            return newDraft(stage);
        }
        if (current != null) {
            return current;
        }
        return newDraft(stage);
    }

    private BasketStagePairingExecution newDraft(BasketDivisionStage stage) {
        StructureVersionContext version = structureVersionService.resolveForGenerate(stage.getDivisionId());
        Integer maxNo = executionMapper.selectMaxExecutionNo(stage.getId());
        int nextNo = maxNo == null ? 1 : maxNo + 1;
        Date now = new Date();
        executionMapper.clearCurrentByStageId(stage.getId());
        BasketStagePairingExecution execution = new BasketStagePairingExecution();
        execution.setDivisionId(stage.getDivisionId());
        execution.setStageId(stage.getId());
        execution.setRuleVersionId(version.getRuleVersionId());
        execution.setRosterVersionId(version.getRosterVersionId());
        execution.setExecutionNo(nextNo);
        execution.setSeedRule(stage.getSeedRule());
        execution.setDrawRule(stage.getDrawRule());
        execution.setByeRule(stage.getByeRule() == null ? ByeRuleEnum.SEED_PRIORITY.getCode() : stage.getByeRule());
        execution.setStatus(PairingExecutionStatusEnum.DRAFT.getCode());
        execution.setOperatorId(operatorId());
        execution.setIsCurrent(1);
        execution.setCurrentUniqueKey("STAGE:" + stage.getId());
        execution.setCreateTime(now);
        execution.setUpdateTime(now);
        execution.setIsDelete(NOT_DELETED);
        if (executionMapper.insert(execution) != 1) {
            throw new ServiceException("创建抽签执行失败");
        }
        return execution;
    }

    private void persistParticipants(BasketStagePairingExecution execution,
                                     List<BasketDivisionStageTeam> teams,
                                     Map<Long, Integer> ranking,
                                     List<Long> adminSeeds) {
        Map<Long, Integer> seedNo = new HashMap<>();
        for (int i = 0; i < adminSeeds.size(); i++) {
            seedNo.put(adminSeeds.get(i), i + 1);
        }
        List<BracketParticipant> participants = toParticipants(teams, ranking);
        for (BracketParticipant participant : participants) {
            if (seedNo.containsKey(participant.getEventTeamId())) {
                participant.setSeedNo(seedNo.get(participant.getEventTeamId()));
            } else if (participant.getRankingNo() != null) {
                participant.setSeedNo(participant.getRankingNo());
            }
        }
        BracketSlotPlan plan = SeededBracketPlacement.placeBySeed(participants);
        replaceEntries(execution, plan);
    }

    private List<BracketParticipant> assignDrawSeeds(List<BasketDivisionStageTeam> teams,
                                                     Map<Long, Integer> ranking,
                                                     List<Long> adminSeeds,
                                                     long randomSeed) {
        Set<Long> seeded = new LinkedHashSet<>(adminSeeds);
        List<BasketDivisionStageTeam> rest = teams.stream()
                .filter(team -> !seeded.contains(team.getEventTeamId()))
                .sorted(Comparator.comparing(BasketDivisionStageTeam::getEventTeamId))
                .collect(Collectors.toCollection(ArrayList::new));
        Collections.shuffle(rest, new Random(randomSeed));
        List<BracketParticipant> participants = new ArrayList<>();
        int seed = 1;
        for (Long teamId : adminSeeds) {
            BracketParticipant participant = new BracketParticipant();
            participant.setEventTeamId(teamId);
            participant.setSeedNo(seed);
            participant.setRankingNo(ranking.getOrDefault(teamId, seed));
            participants.add(participant);
            seed++;
        }
        for (BasketDivisionStageTeam team : rest) {
            BracketParticipant participant = new BracketParticipant();
            participant.setEventTeamId(team.getEventTeamId());
            participant.setSeedNo(seed);
            participant.setRankingNo(ranking.getOrDefault(team.getEventTeamId(), seed));
            participant.setSourceRank(team.getSourceRank());
            participants.add(participant);
            seed++;
        }
        if (participants.size() != teams.size()) {
            throw new ServiceException("抽签参赛队数量不一致");
        }
        return participants;
    }

    private void replaceEntries(BasketStagePairingExecution execution, BracketSlotPlan plan) {
        entryMapper.deleteByExecutionId(execution.getId());
        Date now = new Date();
        List<BasketStagePairingEntry> rows = new ArrayList<>();
        for (BracketSlot slot : plan.getSlots()) {
            BasketStagePairingEntry entry = new BasketStagePairingEntry();
            entry.setExecutionId(execution.getId());
            entry.setSlotNo(slot.getSlotNo());
            entry.setEventTeamId(slot.isBye() ? null : slot.getEventTeamId());
            entry.setSeedNo(slot.getSeedNo());
            entry.setRankingNo(slot.getRankingNo());
            entry.setIsBye(slot.isBye() ? 1 : 0);
            if (slot.isBye()) {
                entry.setSourceType(ParticipantSourceTypeEnum.BYE.getCode());
                entry.setResolutionStatus(ParticipantResolutionStatusEnum.BYE.getCode());
            } else if (ParticipantSourceTypeEnum.GROUP_RANK.getCode().equals(slot.getSourceType())) {
                entry.setSourceType(ParticipantSourceTypeEnum.GROUP_RANK.getCode());
                entry.setSourceStageId(slot.getSourceStageId());
                entry.setSourceGroupId(slot.getSourceGroupId());
                entry.setSourceRank(slot.getSourceRank());
                entry.setResolvedEventTeamId(slot.getResolvedEventTeamId());
                entry.setResolutionStatus(slot.getResolutionStatus() != null
                        ? slot.getResolutionStatus() : ParticipantResolutionStatusEnum.PENDING.getCode());
                entry.setGroupRankKey(execution.getId() + ":" + slot.getSourceStageId() + ":"
                        + slot.getSourceGroupId() + ":" + slot.getSourceRank());
            } else {
                entry.setSourceType(ParticipantSourceTypeEnum.DIRECT_EVENT_TEAM.getCode());
                entry.setResolutionStatus(ParticipantResolutionStatusEnum.RESOLVED.getCode());
                entry.setResolvedEventTeamId(slot.getEventTeamId());
            }
            entry.setCreateTime(now);
            entry.setUpdateTime(now);
            entry.setIsDelete(NOT_DELETED);
            rows.add(entry);
        }
        if (!rows.isEmpty() && entryMapper.batchInsert(rows) != rows.size()) {
            throw new ServiceException("保存抽签签位失败");
        }
    }

    private void validateCompleteSlots(BasketDivisionStage stage,
                                       List<BasketDivisionStageTeam> teams,
                                       List<BasketStagePairingEntry> entries) {
        EliminationCapacity capacity = EliminationCapacity.of(teams.size());
        if (entries.size() != capacity.getCapacity()) {
            throw new ServiceException("签位未填完，无法确认");
        }
        Set<Long> used = new HashSet<>();
        int bye = 0;
        Set<Integer> slots = new HashSet<>();
        Set<Integer> seeds = new HashSet<>();
        for (BasketStagePairingEntry entry : entries) {
            if (!slots.add(entry.getSlotNo())) {
                throw new ServiceException("slot_no 同一次执行必须唯一");
            }
            if (Integer.valueOf(1).equals(entry.getIsBye())) {
                if (entry.getEventTeamId() != null) {
                    throw new ServiceException("BYE Entry 不能绑定 event_team_id");
                }
                bye++;
            } else {
                if (entry.getEventTeamId() == null) {
                    throw new ServiceException("非 BYE Entry 必须绑定 event_team_id");
                }
                if (!used.add(entry.getEventTeamId())) {
                    throw new ServiceException("同一抽签中每支球队只能出现一次");
                }
            }
            if (entry.getSeedNo() != null && !seeds.add(entry.getSeedNo())) {
                throw new ServiceException("seed_no 同一次执行必须唯一");
            }
        }
        if (used.size() != teams.size() || bye != capacity.getByeCount()) {
            throw new ServiceException("签位未填完或 BYE 数量不正确");
        }
    }

    private StagePairingExecutionVo toVo(BasketDivisionStage stage,
                                         BasketStagePairingExecution execution,
                                         List<BasketStagePairingEntry> entries) {
        List<BasketDivisionStageTeam> teams = stageTeamMapper.selectByStageId(stage.getId());
        Map<Long, String> names = teamNames(teams);
        StagePairingExecutionVo vo = new StagePairingExecutionVo();
        vo.setExecutionId(execution.getId());
        vo.setStageId(stage.getId());
        vo.setExecutionNo(execution.getExecutionNo());
        vo.setSeedRule(execution.getSeedRule());
        vo.setDrawRule(execution.getDrawRule());
        vo.setByeRule(execution.getByeRule());
        vo.setPlacementRule(execution.getPlacementRule());
        vo.setResolutionStatus(execution.getResolutionStatus());
        vo.setSourceStageId(execution.getSourceStageId());
        vo.setHasUnresolvedPlaceholders(StructureParticipantStatus.hasUnresolvedPlaceholders(entries));
        vo.setParticipantResolved(StructureParticipantStatus.isParticipantResolved(entries));
        vo.setStatus(execution.getStatus());
        vo.setRandomSeed(execution.getRandomSeed());
        vo.setGeneratedTime(execution.getGeneratedTime());
        vo.setConfirmedTime(execution.getConfirmedTime());
        int participantCount = (int) entries.stream().filter(e -> !Integer.valueOf(1).equals(e.getIsBye())).count();
        if (participantCount >= 2) {
            applyCapacity(vo, participantCount);
        } else if (teams.size() >= 2) {
            applyCapacity(vo, teams.size());
        } else {
            applyFormalRosterCapacity(vo, stage);
        }
        Map<Long, String> groupCodes = loadGroupCodes(entries);
        for (BasketStagePairingEntry entry : entries) {
            StagePairingEntryVo item = new StagePairingEntryVo();
            item.setSlotNo(entry.getSlotNo());
            item.setEventTeamId(entry.getResolvedEventTeamId() != null ? entry.getResolvedEventTeamId() : entry.getEventTeamId());
            item.setSeedNo(entry.getSeedNo());
            item.setRankingNo(entry.getRankingNo());
            item.setBye(Integer.valueOf(1).equals(entry.getIsBye()));
            item.setSourceType(entry.getSourceType());
            item.setSourceStageId(entry.getSourceStageId());
            item.setSourceGroupId(entry.getSourceGroupId());
            item.setSourceRank(entry.getSourceRank());
            item.setResolutionStatus(entry.getResolutionStatus());
            item.setResolvedEventTeamId(entry.getResolvedEventTeamId());
            if (entry.getSourceGroupId() != null) {
                item.setSourceGroupCode(groupCodes.get(entry.getSourceGroupId()));
                item.setDisplayCode(ParticipantSourceLabels.displayCode(item.getSourceGroupCode(), entry.getSourceRank()));
                item.setDisplayName(ParticipantSourceLabels.displayName(item.getSourceGroupCode(), entry.getSourceRank()));
            }
            Long teamId = item.getResolvedEventTeamId() != null ? item.getResolvedEventTeamId() : entry.getEventTeamId();
            if (teamId != null && !names.containsKey(teamId)) {
                BasketEventTeam eventTeam = eventTeamMapper.selectById(teamId);
                if (eventTeam != null) {
                    names.put(teamId, eventTeam.getTeamNameSnapshot());
                }
            }
            if (teamId != null) {
                item.setResolvedEventTeamName(names.get(teamId));
                item.setTeamName(item.getDisplayName() != null
                        ? item.getDisplayName() + " · " + names.getOrDefault(teamId, String.valueOf(teamId))
                        : names.get(teamId));
            } else if (Boolean.TRUE.equals(item.getBye())) {
                item.setTeamName("BYE");
            } else if (item.getDisplayName() != null) {
                item.setTeamName(item.getDisplayName());
            }
            vo.getEntries().add(item);
        }
        int previewCount = vo.getParticipantCount() != null ? vo.getParticipantCount() : 0;
        if (previewCount >= 2 && entries.size() >= 2) {
            List<BracketSlot> slots = new ArrayList<>();
            for (BasketStagePairingEntry entry : entries) {
                BracketSlot slot = toBracketSlot(entry);
                if (entry.getSourceGroupId() != null) {
                    slot.setSourceGroupCode(groupCodes.get(entry.getSourceGroupId()));
                    slot.setDisplayCode(ParticipantSourceLabels.displayCode(slot.getSourceGroupCode(), entry.getSourceRank()));
                    slot.setDisplayName(ParticipantSourceLabels.displayName(slot.getSourceGroupCode(), entry.getSourceRank()));
                }
                slots.add(slot);
            }
            try {
                BracketSlotPlan plan = SeededBracketPlacement.fromSlots(slots, previewCount);
                Map<Long, String> previewNames = new HashMap<>(names);
                for (BasketStagePairingEntry entry : entries) {
                    if (entry.getResolvedEventTeamId() != null) {
                        BasketEventTeam team = eventTeamMapper.selectById(entry.getResolvedEventTeamId());
                        if (team != null) {
                            previewNames.put(entry.getResolvedEventTeamId(), team.getTeamNameSnapshot());
                        }
                    }
                }
                vo.setBracketPreview(BracketPreviewBuilder.build(stage, plan, previewNames));
            } catch (RuntimeException ignored) {
                // incomplete draft slots are shown as entries only
            }
        }
        return vo;
    }

    private StagePairingExecutionVo emptyWorkspace(BasketDivisionStage stage) {
        StagePairingExecutionVo vo = new StagePairingExecutionVo();
        vo.setStageId(stage.getId());
        vo.setSeedRule(stage.getSeedRule());
        vo.setDrawRule(stage.getDrawRule());
        vo.setByeRule(stage.getByeRule());
        vo.setStatus(PairingExecutionStatusEnum.DRAFT.getCode());
        List<BasketDivisionStageTeam> teams = stageTeamMapper.selectByStageId(stage.getId());
        if (teams.size() >= 2) {
            applyCapacity(vo, teams.size());
        } else {
            applyFormalRosterCapacity(vo, stage);
        }
        return vo;
    }

    private void applyFormalRosterCapacity(StagePairingExecutionVo vo, BasketDivisionStage stage) {
        try {
            int formalCount = teamRosterSupport.selectFormalEventTeams(stage.getDivisionId()).size();
            if (formalCount >= 2) {
                applyCapacity(vo, formalCount);
            } else {
                vo.setParticipantCount(formalCount);
            }
        } catch (ServiceException ignored) {
            vo.setParticipantCount(0);
        }
    }

    private void applyCapacity(StagePairingExecutionVo vo, int participantCount) {
        EliminationCapacity capacity = EliminationCapacity.of(participantCount);
        vo.setParticipantCount(capacity.getParticipantCount());
        vo.setBracketCapacity(capacity.getCapacity());
        vo.setByeCount(capacity.getByeCount());
        vo.setFirstRoundActual(capacity.getFirstRoundActual());
        vo.setTotalActualSeries(capacity.getTotalActualSeries());
    }

    private Map<Long, Integer> loadRanking(BasketStagePairingExecution execution) {
        Map<Long, Integer> ranking = new HashMap<>();
        for (BasketStagePairingEntry entry : entryMapper.selectByExecutionId(execution.getId())) {
            if (entry.getEventTeamId() != null && entry.getRankingNo() != null) {
                ranking.put(entry.getEventTeamId(), entry.getRankingNo());
            } else if (entry.getEventTeamId() != null && entry.getSeedNo() != null) {
                ranking.put(entry.getEventTeamId(), entry.getSeedNo());
            }
        }
        return ranking;
    }

    private List<Long> loadAdminSeedOrder(BasketStagePairingExecution execution) {
        return entryMapper.selectByExecutionId(execution.getId()).stream()
                .filter(entry -> entry.getEventTeamId() != null && entry.getSeedNo() != null && entry.getSeedNo() <= 4)
                .sorted(Comparator.comparing(BasketStagePairingEntry::getSeedNo))
                .map(BasketStagePairingEntry::getEventTeamId)
                .distinct()
                .collect(Collectors.toList());
    }

    private List<BracketParticipant> toParticipants(List<BasketDivisionStageTeam> teams, Map<Long, Integer> ranking) {
        List<BracketParticipant> participants = new ArrayList<>();
        for (BasketDivisionStageTeam team : teams) {
            BracketParticipant participant = new BracketParticipant();
            participant.setEventTeamId(team.getEventTeamId());
            Integer rank = ranking.getOrDefault(team.getEventTeamId(),
                    team.getSourceRank() != null ? team.getSourceRank() : team.getSeedNo());
            participant.setSeedNo(rank);
            participant.setRankingNo(rank);
            participant.setSourceRank(team.getSourceRank());
            participant.setSourceType(team.getEntryType());
            participants.add(participant);
        }
        return participants;
    }

    private boolean isGroupRankPlacement(BasketDivisionStage stage) {
        return GroupRankPlacementSupport.usesGroupRankPlacement(stage, findPreviousStage(stage));
    }

    private BasketDivisionStage findPreviousStage(BasketDivisionStage stage) {
        List<BasketDivisionStage> stages = stageMapper.selectByDivisionId(stage.getDivisionId());
        BasketDivisionStage previous = null;
        for (BasketDivisionStage item : stages) {
            if (item.getStageOrder() != null && stage.getStageOrder() != null
                    && item.getStageOrder() < stage.getStageOrder()) {
                if (previous == null || item.getStageOrder() > previous.getStageOrder()) {
                    previous = item;
                }
            }
        }
        return previous;
    }

    private void validateGroupRankSlots(BasketDivisionStage stage, List<BasketStagePairingEntry> entries) {
        BasketDivisionStage previous = findPreviousStage(stage);
        if (previous == null || previous.getAdvancementCount() == null) {
            throw new ServiceException("缺少小组晋级配置");
        }
        BasketDivisionGroupingBatch grouping = groupingBatchMapper.selectActiveByDivisionAndStage(
                stage.getDivisionId(), previous.getId());
        if (grouping == null) {
            throw new ServiceException("缺少已锁定小组");
        }
        int qualifierCount = groupMapper.selectByBatchId(grouping.getId()).size() * previous.getAdvancementCount();
        EliminationCapacity capacity = EliminationCapacity.of(qualifierCount);
        if (entries.size() != capacity.getCapacity()) {
            throw new ServiceException("签位未填完，无法确认");
        }
        Set<String> sources = new HashSet<>();
        int bye = 0;
        Set<Integer> slots = new HashSet<>();
        for (BasketStagePairingEntry entry : entries) {
            if (!slots.add(entry.getSlotNo())) {
                throw new ServiceException("slot_no 同一次执行必须唯一");
            }
            if (Integer.valueOf(1).equals(entry.getIsBye())
                    || ParticipantSourceTypeEnum.BYE.getCode().equals(entry.getSourceType())) {
                if (entry.getResolvedEventTeamId() != null || entry.getEventTeamId() != null) {
                    throw new ServiceException("BYE Entry 不能绑定 event_team_id");
                }
                bye++;
                continue;
            }
            if (!ParticipantSourceTypeEnum.GROUP_RANK.getCode().equals(entry.getSourceType())) {
                throw new ServiceException("GROUP_CROSS / MANUAL 占位必须使用 GROUP_RANK 来源");
            }
            if (entry.getSourceGroupId() == null || entry.getSourceRank() == null) {
                throw new ServiceException("GROUP_RANK 必须包含 source_group_id 与 source_rank");
            }
            String key = entry.getSourceGroupId() + ":" + entry.getSourceRank();
            if (!sources.add(key)) {
                throw new ServiceException("GROUP_RANK 来源不能重复");
            }
        }
        if (sources.size() != qualifierCount || bye != capacity.getByeCount()) {
            throw new ServiceException("占位来源未填完或 BYE 数量不正确");
        }
    }

    private BracketSlot toBracketSlot(BasketStagePairingEntry entry) {
        if (Integer.valueOf(1).equals(entry.getIsBye())
                || ParticipantSourceTypeEnum.BYE.getCode().equals(entry.getSourceType())) {
            return BracketSlot.bye(entry.getSlotNo());
        }
        if (ParticipantSourceTypeEnum.GROUP_RANK.getCode().equals(entry.getSourceType())) {
            BracketSlot slot = BracketSlot.groupRank(entry.getSlotNo(), entry.getSourceStageId(),
                    entry.getSourceGroupId(), null,
                    entry.getSourceRank() == null ? 0 : entry.getSourceRank());
            slot.setResolvedEventTeamId(entry.getResolvedEventTeamId());
            slot.setResolutionStatus(entry.getResolutionStatus());
            if (entry.getResolvedEventTeamId() != null) {
                slot.setEventTeamId(entry.getResolvedEventTeamId());
            }
            return slot;
        }
        return BracketSlot.team(entry.getSlotNo(), entry.getEventTeamId(),
                entry.getSeedNo(), entry.getRankingNo(), entry.getSourceRank());
    }

    private Map<Long, String> loadGroupCodes(List<BasketStagePairingEntry> entries) {
        Map<Long, String> codes = new HashMap<>();
        for (BasketStagePairingEntry entry : entries) {
            if (entry.getSourceGroupId() == null || codes.containsKey(entry.getSourceGroupId())) {
                continue;
            }
            BasketDivisionGroup group = groupMapper.selectById(entry.getSourceGroupId());
            if (group != null) {
                codes.put(group.getId(), group.getGroupCode());
            }
        }
        return codes;
    }

    private List<BasketDivisionStageTeam> requireFormalStageTeams(BasketDivisionStage stage) {
        List<BasketEventTeam> formalTeams = teamRosterSupport.selectFormalEventTeams(stage.getDivisionId());
        Set<Long> formalIds = formalTeams.stream().map(BasketEventTeam::getId).collect(Collectors.toSet());
        List<BasketDivisionStageTeam> teams = stageTeamMapper.selectByStageId(stage.getId()).stream()
                .filter(team -> team.getEventTeamId() != null && formalIds.contains(team.getEventTeamId()))
                .collect(Collectors.toList());
        if (teams.size() < 2 && findPreviousStage(stage) == null && formalTeams.size() >= 2) {
            teams = initFirstStageTeamsFromFormal(stage, formalTeams);
        }
        if (teams.size() < 2) {
            throw new ServiceException("当前锁定名单没有足够的正式参赛球队，请返回报名与参赛名单检查名单分配");
        }
        return teams;
    }

    private List<BasketDivisionStageTeam> initFirstStageTeamsFromFormal(BasketDivisionStage stage,
                                                                        List<BasketEventTeam> formalTeams) {
        Date now = new Date();
        List<BasketDivisionStageTeam> toInsert = new ArrayList<>();
        int seed = 1;
        for (BasketEventTeam eventTeam : formalTeams) {
            BasketDivisionStageTeam team = new BasketDivisionStageTeam();
            team.setDivisionId(stage.getDivisionId());
            team.setSnapshotId(stage.getSnapshotId());
            team.setStageId(stage.getId());
            team.setEventTeamId(eventTeam.getId());
            team.setSeedNo(seed++);
            team.setEntryType(StageTeamEntryTypeEnum.INITIAL.getCode());
            team.setQualificationStatus(StageTeamQualificationStatusEnum.PENDING.getCode());
            team.setActiveUniqueKey(stage.getId() + "_" + eventTeam.getId());
            team.setCreateTime(now);
            team.setUpdateTime(now);
            team.setIsDelete(NOT_DELETED);
            toInsert.add(team);
        }
        if (stageTeamMapper.batchInsert(toInsert) != toInsert.size()) {
            throw new ServiceException("初始化阶段参赛队伍失败");
        }
        return toInsert;
    }

    private Map<Long, String> teamNames(List<BasketDivisionStageTeam> teams) {
        Map<Long, String> names = new HashMap<>();
        for (BasketDivisionStageTeam team : teams) {
            if (team.getEventTeamId() == null) {
                continue;
            }
            BasketEventTeam eventTeam = eventTeamMapper.selectById(team.getEventTeamId());
            if (eventTeam != null) {
                names.put(team.getEventTeamId(), eventTeam.getTeamNameSnapshot());
            }
        }
        return names;
    }

    private Set<Long> teamIds(List<BasketDivisionStageTeam> teams) {
        return teams.stream().map(BasketDivisionStageTeam::getEventTeamId).collect(Collectors.toSet());
    }

    private void audit(String action, BasketDivisionStage stage, BasketStagePairingExecution execution, String summary) {
        List<AuditFieldInput> fields = new ArrayList<>();
        fields.add(field("stageId", String.valueOf(stage.getId())));
        fields.add(field("ruleVersionId", execution.getRuleVersionId() == null ? null : String.valueOf(execution.getRuleVersionId())));
        fields.add(field("rosterVersionId", execution.getRosterVersionId() == null ? null : String.valueOf(execution.getRosterVersionId())));
        fields.add(field("executionNo", String.valueOf(execution.getExecutionNo())));
        fields.add(field("randomSeed", execution.getRandomSeed() == null ? null : String.valueOf(execution.getRandomSeed())));
        auditWriter.writeAudit("V4_STAGE", "PAIRING_EXECUTION", execution.getId(), action, summary, null, fields);
    }

    private AuditFieldInput field(String name, String value) {
        AuditFieldInput field = new AuditFieldInput();
        field.setFieldName(name);
        field.setAfterValue(value);
        return field;
    }

    private BasketDivisionStage requireStage(Long stageId) {
        BasketDivisionStage stage = stageMapper.selectById(stageId);
        if (stage == null) {
            throw new ServiceException("阶段不存在");
        }
        return stage;
    }

    private void lockDivision(Long divisionId) {
        BasketDivision division = divisionMapper.selectByIdForUpdate(divisionId);
        if (division == null) {
            throw new ServiceException("Division 不存在");
        }
    }

    private Long operatorId() {
        try {
            return SecurityUtils.getUserId();
        } catch (Exception ex) {
            return 0L;
        }
    }
}
