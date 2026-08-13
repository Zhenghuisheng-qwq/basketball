package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionGroupTeam;
import com.zhs.basket.domain.v4.BasketDivisionGroupingBatch;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketDivisionStageQualification;
import com.zhs.basket.domain.v4.BasketEventTeam;
import com.zhs.basket.domain.v4.BasketMatchSeries;
import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.domain.v4.BasketStagePairingEntry;
import com.zhs.basket.domain.v4.BasketStagePairingExecution;
import com.zhs.basket.enums.v4.GroupingStatusEnum;
import com.zhs.basket.enums.v4.PairingExecutionStatusEnum;
import com.zhs.basket.enums.v4.ParticipantResolutionStatusEnum;
import com.zhs.basket.enums.v4.ParticipantSourceTypeEnum;
import com.zhs.basket.enums.v4.PublishStatusEnum;
import com.zhs.basket.enums.v4.QualificationRecordStatusEnum;
import com.zhs.basket.mapper.v4.BasketDivisionGroupTeamMapper;
import com.zhs.basket.mapper.v4.BasketDivisionGroupingBatchMapper;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageQualificationMapper;
import com.zhs.basket.mapper.v4.BasketMatchSeriesMapper;
import com.zhs.basket.mapper.v4.BasketScheduleBatchMapper;
import com.zhs.basket.mapper.v4.BasketStagePairingEntryMapper;
import com.zhs.basket.mapper.v4.BasketStagePairingExecutionMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.req.AuditFieldInput;
import com.zhs.basket.service.v4.IPlaceholderResolutionApplicationService;
import com.zhs.basket.service.v4.IStagePairingExecutionApplicationService;
import com.zhs.basket.support.BasketBusinessAuditWriter;
import com.zhs.basket.support.v4.bracket.BracketSlot;
import com.zhs.basket.support.v4.bracket.GroupRankParticipantResolver;
import com.zhs.basket.support.v4.bracket.ParticipantResolveContext;
import com.zhs.basket.support.v4.bracket.ParticipantSourceResolverRegistry;
import com.zhs.basket.support.v4.bracket.StructureParticipantStatus;
import com.zhs.basket.support.v4.roster.TeamRosterSupport;
import com.zhs.basket.support.v4.stage.MatchSeriesSupport;
import com.zhs.basket.support.v4.stage.StageExecutionContext;
import com.zhs.basket.vo.v4.StagePairingExecutionVo;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cup/knockout placeholder resolution use-case. Not a CupEngine.
 * Qualification Confirm is not this service; this only maps confirmed GROUP_TOP_N onto fixed slots.
 */
@Service
public class PlaceholderResolutionApplicationServiceImpl implements IPlaceholderResolutionApplicationService {

    @Autowired
    private BasketDivisionMapper divisionMapper;
    @Autowired
    private BasketDivisionStageMapper stageMapper;
    @Autowired
    private BasketStagePairingExecutionMapper executionMapper;
    @Autowired
    private BasketStagePairingEntryMapper entryMapper;
    @Autowired
    private BasketDivisionStageQualificationMapper qualificationMapper;
    @Autowired
    private BasketDivisionGroupingBatchMapper groupingBatchMapper;
    @Autowired
    private BasketDivisionGroupTeamMapper groupTeamMapper;
    @Autowired
    private BasketScheduleBatchMapper scheduleBatchMapper;
    @Autowired
    private BasketMatchSeriesMapper matchSeriesMapper;
    @Autowired
    private BasketV4MatchMapper matchMapper;
    @Autowired
    private TeamRosterSupport teamRosterSupport;
    @Autowired
    private ParticipantSourceResolverRegistry resolverRegistry;
    @Autowired
    private MatchSeriesSupport matchSeriesSupport;
    @Autowired
    private BasketBusinessAuditWriter auditWriter;
    @Autowired
    @Lazy
    private IStagePairingExecutionApplicationService pairingExecutionApplicationService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public StagePairingExecutionVo resolve(Long knockoutStageId) {
        BasketDivisionStage stage = requireStage(knockoutStageId);
        BasketDivision division = divisionMapper.selectByIdForUpdate(stage.getDivisionId());
        if (division == null) {
            throw new ServiceException("Division 不存在");
        }
        BasketScheduleBatch published = scheduleBatchMapper.selectPublishedByStageId(stage.getId());
        if (published != null && PublishStatusEnum.PUBLISHED.getCode().equals(published.getPublishStatus())) {
            throw new ServiceException("当前淘汰赛段已发布，需先撤回后调整");
        }
        if (matchMapper.countFinishedByStageId(stage.getId()) > 0) {
            throw new ServiceException("淘汰赛已产生正式结果，禁止重新解析占位");
        }

        BasketStagePairingExecution execution = executionMapper.selectCurrentByStageIdForUpdate(stage.getId());
        if (execution == null || !PairingExecutionStatusEnum.CONFIRMED.getCode().equals(execution.getStatus())) {
            throw new ServiceException("需要已确认的 PairingExecution 才能解析占位");
        }
        List<BasketStagePairingEntry> entries = entryMapper.selectByExecutionId(execution.getId());
        if (entries.isEmpty()) {
            throw new ServiceException("PairingExecution 没有签位");
        }

        Long sourceStageId = execution.getSourceStageId();
        if (sourceStageId == null) {
            sourceStageId = findPreviousStage(stage) == null ? null : findPreviousStage(stage).getId();
        }
        if (sourceStageId == null) {
            throw new ServiceException("缺少小组赛来源阶段，无法解析 GROUP_RANK");
        }
        List<BasketDivisionStageQualification> confirmed = qualificationMapper.selectConfirmedByStageId(sourceStageId);
        if (confirmed.isEmpty()) {
            throw new ServiceException("需要已确认的 GROUP_TOP_N 晋级才能解析占位");
        }

        if (ParticipantResolutionStatusEnum.RESOLVED.getCode().equals(execution.getResolutionStatus())
                && !StructureParticipantStatus.hasUnresolvedPlaceholders(entries)
                && sameQualificationBinding(entries, confirmed)) {
            return pairingExecutionApplicationService.getCurrent(stage.getId());
        }
        if (ParticipantResolutionStatusEnum.RESOLVED.getCode().equals(execution.getResolutionStatus())
                && !sameQualificationBinding(entries, confirmed)) {
            throw new ServiceException("占位已按历史晋级确认解析，不能原地覆盖，请走结构失效后重新生成");
        }

        ParticipantResolveContext context = buildContext(division, stage, sourceStageId, confirmed);
        Map<Long, Long> resolvedByEntryId = new HashMap<>();
        Map<Long, Long> qualificationByEntryId = new HashMap<>();
        Set<Long> resolvedTeams = new HashSet<>();
        for (BasketStagePairingEntry entry : entries) {
            BracketSlot slot = toSlot(entry);
            if (Integer.valueOf(1).equals(entry.getIsBye())
                    || ParticipantSourceTypeEnum.BYE.getCode().equals(entry.getSourceType())) {
                if (entry.getResolvedEventTeamId() != null) {
                    throw new ServiceException("BYE 不能绑定 resolved EventTeam");
                }
                continue;
            }
            Long teamId = resolverRegistry.resolve(entry.getSourceType()).resolve(slot, context);
            if (ParticipantSourceTypeEnum.GROUP_RANK.getCode().equals(entry.getSourceType())) {
                if (teamId == null) {
                    throw new ServiceException("GROUP_RANK 解析结果不能为空");
                }
                if (!resolvedTeams.add(teamId)) {
                    throw new ServiceException("同一 EventTeam 不能解析到两个晋级名次");
                }
                ParticipantResolveContext.ConfirmedQualification qualification = context.getConfirmedByGroupRank()
                        .get(new ParticipantResolveContext.GroupRankKey(
                                entry.getSourceStageId(), entry.getSourceGroupId(), entry.getSourceRank()));
                qualificationByEntryId.put(entry.getId(), qualification.qualificationId());
            }
            resolvedByEntryId.put(entry.getId(), teamId);
        }
        for (BasketStagePairingEntry entry : entries) {
            if (Integer.valueOf(1).equals(entry.getIsBye())
                    || ParticipantSourceTypeEnum.BYE.getCode().equals(entry.getSourceType())) {
                continue;
            }
            if (!resolvedByEntryId.containsKey(entry.getId())) {
                throw new ServiceException("占位解析不完整，已整体拒绝");
            }
        }

        for (BasketStagePairingEntry entry : entries) {
            if (!resolvedByEntryId.containsKey(entry.getId())) {
                continue;
            }
            entry.setResolvedEventTeamId(resolvedByEntryId.get(entry.getId()));
            entry.setResolutionStatus(ParticipantResolutionStatusEnum.RESOLVED.getCode());
            entry.setQualificationId(qualificationByEntryId.get(entry.getId()));
            if (entryMapper.updateResolution(entry) != 1) {
                throw new ServiceException("写入占位解析失败");
            }
        }

        applyResolvedTeamsToSeries(stage, entries);
        execution.setResolutionStatus(ParticipantResolutionStatusEnum.RESOLVED.getCode());
        execution.setQualificationStageId(sourceStageId);
        executionMapper.updateById(execution);
        auditWriter.writeAudit("V4_STAGE", "PAIRING_EXECUTION", execution.getId(),
                "PLACEHOLDER_RESOLVE", "解析淘汰占位为真实 EventTeam", null, List.of(
                        field("stageId", String.valueOf(stage.getId())),
                        field("qualificationStageId", String.valueOf(sourceStageId))));
        return pairingExecutionApplicationService.getCurrent(stage.getId());
    }

    private void applyResolvedTeamsToSeries(BasketDivisionStage stage, List<BasketStagePairingEntry> entries) {
        List<BasketMatchSeries> seriesList = matchSeriesMapper.selectByStageId(stage.getId());
        Map<String, BasketMatchSeries> byRoundSlot = new HashMap<>();
        for (BasketMatchSeries series : seriesList) {
            byRoundSlot.put(series.getRoundNo() + ":" + series.getBracketSlot(), series);
        }
        BasketScheduleBatch batch = stage.getScheduleBatchId() == null
                ? null : scheduleBatchMapper.selectById(stage.getScheduleBatchId());
        StageExecutionContext context = new StageExecutionContext();
        context.setDivision(divisionMapper.selectById(stage.getDivisionId()));
        context.setStage(stage);

        int pairCount = entries.size() / 2;
        Long[] firstRoundWinners = new Long[pairCount];
        boolean[] byeAdvance = new boolean[pairCount];
        for (int i = 0; i < pairCount; i++) {
            BasketStagePairingEntry home = entries.get(i * 2);
            BasketStagePairingEntry away = entries.get(i * 2 + 1);
            boolean homeBye = Integer.valueOf(1).equals(home.getIsBye());
            boolean awayBye = Integer.valueOf(1).equals(away.getIsBye());
            if (homeBye) {
                byeAdvance[i] = true;
                firstRoundWinners[i] = away.getResolvedEventTeamId();
                continue;
            }
            if (awayBye) {
                byeAdvance[i] = true;
                firstRoundWinners[i] = home.getResolvedEventTeamId();
                continue;
            }
            BasketMatchSeries series = byRoundSlot.get("1:" + i);
            if (series == null) {
                continue;
            }
            matchSeriesSupport.ensureGames(context, batch, series,
                    home.getResolvedEventTeamId(), away.getResolvedEventTeamId());
        }

        fillLaterRounds(byRoundSlot, firstRoundWinners, byeAdvance, context, batch);
    }

    private void fillLaterRounds(Map<String, BasketMatchSeries> byRoundSlot,
                                 Long[] previousWinners,
                                 boolean[] previousBye,
                                 StageExecutionContext context,
                                 BasketScheduleBatch batch) {
        int roundNo = 2;
        Long[] winners = previousWinners;
        boolean[] bye = previousBye;
        while (winners.length >= 2) {
            int pairCount = winners.length / 2;
            Long[] nextWinners = new Long[pairCount];
            boolean[] nextBye = new boolean[pairCount];
            for (int slot = 0; slot < pairCount; slot++) {
                Long homeId = bye[slot * 2] ? winners[slot * 2] : null;
                Long awayId = bye[slot * 2 + 1] ? winners[slot * 2 + 1] : null;
                BasketMatchSeries series = byRoundSlot.get(roundNo + ":" + slot);
                if (series == null) {
                    continue;
                }
                if (homeId != null) {
                    series.setHomeEventTeamId(homeId);
                }
                if (awayId != null) {
                    series.setAwayEventTeamId(awayId);
                }
                if (series.getHomeEventTeamId() != null && series.getAwayEventTeamId() != null) {
                    matchSeriesSupport.ensureGames(context, batch, series,
                            series.getHomeEventTeamId(), series.getAwayEventTeamId());
                } else {
                    matchSeriesMapper.updateById(series);
                }
            }
            winners = nextWinners;
            bye = nextBye;
            roundNo++;
        }
    }

    private ParticipantResolveContext buildContext(BasketDivision division,
                                                   BasketDivisionStage knockoutStage,
                                                   Long sourceStageId,
                                                   List<BasketDivisionStageQualification> confirmed) {
        Map<ParticipantResolveContext.GroupRankKey, ParticipantResolveContext.ConfirmedQualification> byRank = new HashMap<>();
        Set<Long> seenTeams = new HashSet<>();
        for (BasketDivisionStageQualification row : confirmed) {
            if (!QualificationRecordStatusEnum.CONFIRMED.getCode().equals(row.getQualificationStatus())) {
                continue;
            }
            if (row.getEventTeamId() != null && !seenTeams.add(row.getEventTeamId())) {
                throw new ServiceException("已确认晋级存在重复 EventTeam");
            }
            ParticipantResolveContext.GroupRankKey key = new ParticipantResolveContext.GroupRankKey(
                    sourceStageId, row.getGroupId(), row.getSourceRank());
            if (byRank.put(key, new ParticipantResolveContext.ConfirmedQualification(
                    row.getId(), row.getEventTeamId(), row.getGroupId(), row.getSourceRank())) != null) {
                throw new ServiceException("已确认晋级存在重复小组名次");
            }
        }
        Map<Long, Long> teamGroup = new HashMap<>();
        BasketDivisionGroupingBatch grouping = groupingBatchMapper.selectActiveByDivisionAndStage(
                division.getId(), sourceStageId);
        if (grouping == null || !GroupingStatusEnum.LOCKED.getCode().equals(grouping.getGroupingStatus())) {
            throw new ServiceException("小组分配未锁定，无法校验解析球队所属小组");
        }
        for (BasketDivisionGroupTeam assignment : groupTeamMapper.selectByBatchId(grouping.getId())) {
            teamGroup.put(assignment.getEventTeamId(), assignment.getGroupId());
        }
        Set<Long> formalIds = new HashSet<>();
        for (BasketEventTeam team : teamRosterSupport.selectFormalEventTeams(division.getId())) {
            formalIds.add(team.getId());
        }
        ParticipantResolveContext context = new ParticipantResolveContext();
        context.setDivisionId(division.getId());
        context.setKnockoutStageId(knockoutStage.getId());
        context.setSourceStageId(sourceStageId);
        context.setConfirmedByGroupRank(byRank);
        context.setEventTeamGroupId(teamGroup);
        context.setFormalEventTeamIds(formalIds);
        return context;
    }

    private boolean sameQualificationBinding(List<BasketStagePairingEntry> entries,
                                             List<BasketDivisionStageQualification> confirmed) {
        Set<Long> bound = new HashSet<>();
        for (BasketStagePairingEntry entry : entries) {
            if (entry.getQualificationId() != null) {
                bound.add(entry.getQualificationId());
            }
        }
        if (bound.isEmpty()) {
            return false;
        }
        Set<Long> confirmedIds = new HashSet<>();
        for (BasketDivisionStageQualification row : confirmed) {
            confirmedIds.add(row.getId());
        }
        return bound.equals(confirmedIds);
    }

    private BracketSlot toSlot(BasketStagePairingEntry entry) {
        if (Integer.valueOf(1).equals(entry.getIsBye())) {
            return BracketSlot.bye(entry.getSlotNo());
        }
        if (ParticipantSourceTypeEnum.GROUP_RANK.getCode().equals(entry.getSourceType())) {
            BracketSlot slot = BracketSlot.groupRank(entry.getSlotNo(), entry.getSourceStageId(),
                    entry.getSourceGroupId(), null, entry.getSourceRank() == null ? 0 : entry.getSourceRank());
            slot.setResolvedEventTeamId(entry.getResolvedEventTeamId());
            slot.setResolutionStatus(entry.getResolutionStatus());
            return slot;
        }
        return BracketSlot.team(entry.getSlotNo(), entry.getEventTeamId(), entry.getSeedNo(),
                entry.getRankingNo(), entry.getSourceRank());
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

    private BasketDivisionStage requireStage(Long stageId) {
        BasketDivisionStage stage = stageMapper.selectById(stageId);
        if (stage == null) {
            throw new ServiceException("阶段不存在");
        }
        return stage;
    }

    private AuditFieldInput field(String name, String value) {
        AuditFieldInput field = new AuditFieldInput();
        field.setFieldName(name);
        field.setAfterValue(value);
        return field;
    }
}
