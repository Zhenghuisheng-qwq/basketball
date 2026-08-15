package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionFormatSnapshot;
import com.zhs.basket.domain.v4.BasketDivisionRosterVersion;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketScheduleBatch;
import com.zhs.basket.domain.v4.BasketSeason;
import com.zhs.basket.domain.v4.BasketSeries;
import com.zhs.basket.domain.v4.BasketStagePublishRecord;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;
import com.zhs.basket.enums.v4.PublishStatusEnum;
import com.zhs.basket.enums.v4.StageExecutionStatusEnum;
import com.zhs.basket.enums.v4.StructureStatusEnum;
import com.zhs.basket.mapper.v4.BasketDivisionFormatSnapshotMapper;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketDivisionRosterVersionMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageMapper;
import com.zhs.basket.mapper.v4.BasketScheduleBatchMapper;
import com.zhs.basket.mapper.v4.BasketSeasonMapper;
import com.zhs.basket.mapper.v4.BasketSeriesMapper;
import com.zhs.basket.mapper.v4.BasketStagePublishRecordMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.query.v4.BasketDivisionQuery;
import com.zhs.basket.service.v4.IDivisionOperationOverviewQueryService;
import com.zhs.basket.service.v4.IDivisionStageExecutionApplicationService;
import com.zhs.basket.service.v4.IStagePairingExecutionApplicationService;
import com.zhs.basket.service.v4.IStageScheduleApplicationService;
import com.zhs.basket.support.v4.DivisionOperationCapabilitySupport;
import com.zhs.basket.support.v4.stage.StructureGenerationPrerequisiteValidator;
import com.zhs.basket.support.v4.DivisionRuleMutableSpecification;
import com.zhs.basket.support.v4.DoubleEliminationCapabilityGate;
import com.zhs.basket.support.v4.V4AdminDisplaySupport;
import com.zhs.basket.support.v4.V4FormatDisplaySupport;
import com.zhs.basket.support.v4.version.RuleVersionSupport;
import com.zhs.basket.support.v4.version.RuleVersionView;
import com.zhs.basket.vo.v4.BasketDivisionListVo;
import com.zhs.basket.vo.v4.BasketDivisionStageExecutionListVo;
import com.zhs.basket.vo.v4.DivisionContextVo;
import com.zhs.basket.vo.v4.DivisionOperationCapabilities;
import com.zhs.basket.vo.v4.DivisionSelectorItemVo;
import com.zhs.basket.vo.v4.DivisionWorkflowOverviewVo;
import com.zhs.basket.vo.v4.RosterSummaryVo;
import com.zhs.basket.vo.v4.RosterVersionHistoryItemVo;
import com.zhs.basket.vo.v4.RuleSummaryVo;
import com.zhs.basket.vo.v4.StageOperationCapabilities;
import com.zhs.basket.vo.v4.StagePairingExecutionVo;
import com.zhs.basket.vo.v4.StageScheduleSummaryVo;
import com.zhs.basket.vo.v4.StageWorkflowSummaryVo;
import com.zhs.common.exception.ServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class DivisionOperationOverviewQueryServiceImpl implements IDivisionOperationOverviewQueryService {

    @Autowired
    private BasketDivisionMapper divisionMapper;
    @Autowired
    private BasketSeasonMapper seasonMapper;
    @Autowired
    private BasketSeriesMapper seriesMapper;
    @Autowired
    private BasketDivisionFormatSnapshotMapper snapshotMapper;
    @Autowired
    private BasketDivisionRosterVersionMapper rosterVersionMapper;
    @Autowired
    private BasketScheduleBatchMapper scheduleBatchMapper;
    @Autowired
    private BasketDivisionStageMapper stageMapper;
    @Autowired
    private BasketStagePublishRecordMapper publishRecordMapper;
    @Autowired
    private BasketV4MatchMapper matchMapper;
    @Autowired
    private IDivisionStageExecutionApplicationService stageExecutionApplicationService;
    @Autowired
    private IStageScheduleApplicationService stageScheduleApplicationService;
    @Autowired
    private IStagePairingExecutionApplicationService pairingExecutionApplicationService;
    @Autowired
    private DivisionRuleMutableSpecification ruleMutableSpecification;
    @Autowired
    private StructureGenerationPrerequisiteValidator structureGenerationPrerequisiteValidator;

    @Override
    public List<DivisionSelectorItemVo> listSelector(BasketDivisionQuery query, String entry) {
        List<BasketDivisionListVo> rows = divisionMapper.selectVoList(query == null ? new BasketDivisionQuery() : query);
        List<Long> ids = rows.stream().map(BasketDivisionListVo::getId).filter(Objects::nonNull).toList();
        Map<Long, BasketDivisionRosterVersion> rosterMap = new HashMap<>();
        if (!ids.isEmpty()) {
            for (BasketDivisionRosterVersion version : rosterVersionMapper.selectCurrentByDivisionIds(ids)) {
                rosterMap.put(version.getDivisionId(), version);
            }
        }
        Map<Long, List<BasketScheduleBatch>> batchMap = new HashMap<>();
        if (!ids.isEmpty()) {
            for (BasketScheduleBatch batch : scheduleBatchMapper.selectByDivisionIds(ids)) {
                batchMap.computeIfAbsent(batch.getDivisionId(), k -> new ArrayList<>()).add(batch);
            }
        }
        List<DivisionSelectorItemVo> result = new ArrayList<>();
        for (BasketDivisionListVo row : rows) {
            result.add(toSelectorItem(row, rosterMap.get(row.getId()), batchMap.getOrDefault(row.getId(), List.of()), entry));
        }
        return result;
    }

    @Override
    public DivisionWorkflowOverviewVo getOverview(Long divisionId) {
        BasketDivision division = divisionMapper.selectById(divisionId);
        if (division == null) {
            throw new ServiceException("Division 不存在");
        }
        BasketSeason season = seasonMapper.selectById(division.getSeasonId());
        BasketSeries series = season == null ? null : seriesMapper.selectById(season.getSeriesId());
        BasketDivisionFormatSnapshot snapshot = snapshotMapper.selectActiveByDivisionId(divisionId);
        RuleVersionView ruleView = RuleVersionSupport.fromSnapshot(snapshot);

        List<BasketDivisionStage> stages = snapshot == null
                ? stageMapper.selectByDivisionId(divisionId)
                : stageMapper.selectBySnapshotId(snapshot.getId());
        boolean hasDoubleElim = stages.stream().anyMatch(DoubleEliminationCapabilityGate::isDoubleElimination);
        boolean ruleMutable = ruleMutableSpecification.isMutable(division, snapshot);
        String workflowMode = snapshot == null ? null : snapshot.getRosterWorkflowMode();
        String ruleStatus = snapshot == null ? null : snapshot.getSnapshotStatus();
        List<BasketDivisionRosterVersion> rosterVersions = rosterVersionMapper.selectByDivisionId(divisionId);
        BasketDivisionRosterVersion currentRoster = rosterVersions.stream()
                .filter(v -> v.getIsCurrent() != null && v.getIsCurrent() == 1)
                .findFirst()
                .orElse(null);
        String rosterStatus = currentRoster == null ? "DRAFT" : currentRoster.getStatus();

        DivisionWorkflowOverviewVo vo = new DivisionWorkflowOverviewVo();
        vo.setContext(toContext(division, season, series, snapshot));
        vo.setRule(toRuleSummary(snapshot, ruleView));
        vo.setRoster(toRosterSummary(currentRoster, rosterVersions));

        DivisionOperationCapabilities caps = DivisionOperationCapabilitySupport.divisionCapabilities(
                workflowMode, ruleStatus, rosterStatus, ruleMutable, hasDoubleElim);
        List<BasketDivisionStageExecutionListVo> stageRows = stageExecutionApplicationService.listStages(divisionId);
        List<StageWorkflowSummaryVo> stageSummaries = new ArrayList<>();
        boolean anyCanEditSchedule = false;
        boolean anyCanPublish = false;
        boolean anyCanWithdraw = false;
        boolean anyCanGenerate = false;
        for (BasketDivisionStageExecutionListVo stageRow : stageRows) {
            StageWorkflowSummaryVo stageVo = toStageSummary(division, snapshot, currentRoster, stageRow, stages);
            stageSummaries.add(stageVo);
            StageOperationCapabilities sc = stageVo.getCapabilities();
            if (sc != null) {
                anyCanEditSchedule = anyCanEditSchedule || sc.isCanEditSchedule();
                anyCanPublish = anyCanPublish || sc.isCanPublish();
                anyCanWithdraw = anyCanWithdraw || sc.isCanWithdraw();
                anyCanGenerate = anyCanGenerate || sc.isCanGenerateStructure();
            }
        }
        caps.setCanEditSchedule(anyCanEditSchedule);
        caps.setCanPublishStage(anyCanPublish);
        caps.setCanWithdrawStage(anyCanWithdraw);
        if (anyCanGenerate) {
            caps.setCanGenerateStructure(true);
        }
        vo.setCapabilities(caps);
        vo.setStages(stageSummaries);
        vo.setWorkflowHint(resolveDivisionHint(caps, stageSummaries));
        boolean legacy = !DivisionOperationCapabilitySupport.isVersioned(workflowMode);
        vo.setLegacy(legacy);
        vo.setReadOnly(legacy);
        return vo;
    }

    private DivisionSelectorItemVo toSelectorItem(BasketDivisionListVo row,
                                                  BasketDivisionRosterVersion roster,
                                                  List<BasketScheduleBatch> batches,
                                                  String entry) {
        DivisionSelectorItemVo item = new DivisionSelectorItemVo();
        item.setDivisionId(row.getId());
        item.setDivisionName(row.getDivisionName());
        item.setSeasonId(row.getSeasonId());
        item.setSeasonTitle(row.getSeasonTitle());
        item.setSeriesCode(row.getSeriesCode());
        item.setSeriesName(row.getSeriesName());
        item.setPlatformCode(row.getPlatformCode());
        item.setGameMode(row.getGameMode());
        item.setRegistrationMode(row.getRegistrationMode());
        item.setTemplateType(row.getFormatTemplateType());
        item.setTemplateCode(row.getFormatTemplateCode());
        item.setRuleStatus(row.getFormatSnapshotStatus());
        item.setRuleStatusName(V4AdminDisplaySupport.ruleStatusLabel(row.getFormatSnapshotStatus()));
        String rosterStatus = roster == null ? "DRAFT" : roster.getStatus();
        item.setRosterStatus(rosterStatus);
        item.setRosterStatusName(V4AdminDisplaySupport.rosterStatusLabel(rosterStatus));
        item.setRosterWorkflowMode(row.getRosterWorkflowMode());
        String publish = batches.stream()
                .map(BasketScheduleBatch::getPublishStatus)
                .filter(PublishStatusEnum.PUBLISHED.getCode()::equals)
                .findFirst()
                .orElse(batches.stream().map(BasketScheduleBatch::getPublishStatus).filter(Objects::nonNull).findFirst().orElse("DRAFT"));
        item.setPublishStatus(publish);
        item.setPublishStatusName(V4AdminDisplaySupport.publishStatusLabel(publish));
        long stale = batches.stream().filter(b -> StructureStatusEnum.STALE.getCode().equals(b.getStructureStatus())).count();
        long active = batches.stream().filter(b -> StructureStatusEnum.ACTIVE.getCode().equals(b.getStructureStatus())).count();
        item.setStructureSummary(stale > 0 ? "结构已失效" : (active > 0 ? "当前结构已生成" : "尚未生成"));
        long published = batches.stream().filter(b -> PublishStatusEnum.PUBLISHED.getCode().equals(b.getPublishStatus())).count();
        item.setScheduleSummary(published > 0 ? published + " 个赛段已发布" : (active > 0 ? "已有结构待排期/发布" : "尚未排期"));
        String block = DivisionOperationCapabilitySupport.selectorBlockReason(entry, row.getRosterWorkflowMode(), row.getFormatSnapshotStatus());
        item.setBlockReason(block);
        item.setSelectable(block == null);
        return item;
    }

    private DivisionContextVo toContext(BasketDivision division, BasketSeason season, BasketSeries series,
                                        BasketDivisionFormatSnapshot snapshot) {
        DivisionContextVo ctx = new DivisionContextVo();
        ctx.setDivisionId(division.getId());
        ctx.setDivisionName(division.getDivisionName());
        ctx.setSeasonId(division.getSeasonId());
        ctx.setSeasonTitle(season == null ? null : (season.getDisplayTitle() == null ? season.getSeasonName() : season.getDisplayTitle()));
        ctx.setSeriesId(season == null ? null : season.getSeriesId());
        ctx.setSeriesCode(series == null ? null : series.getSeriesCode());
        ctx.setSeriesName(series == null ? null : series.getSeriesName());
        ctx.setPlatformCode(division.getPlatformCode());
        ctx.setPlatformName(V4FormatDisplaySupport.platformLabel(division.getPlatformCode()));
        ctx.setGameMode(division.getGameMode());
        ctx.setGameModeName(V4FormatDisplaySupport.gameModeLabel(division.getGameMode()));
        ctx.setRegistrationMode(division.getRegistrationMode());
        ctx.setRegistrationModeName(V4FormatDisplaySupport.registrationModeLabel(division.getRegistrationMode()));
        if (snapshot != null) {
            ctx.setTemplateType(snapshot.getSourceTemplateType());
            ctx.setTemplateCode(snapshot.getSourceTemplateCode());
            ctx.setTemplateName(snapshot.getSourceTemplateName());
            ctx.setRosterWorkflowMode(snapshot.getRosterWorkflowMode());
            ctx.setRosterWorkflowModeName(V4AdminDisplaySupport.workflowModeLabel(snapshot.getRosterWorkflowMode()));
        }
        ctx.setDivisionStatus(division.getDivisionStatus());
        ctx.setDivisionStatusName(division.getDivisionStatus());
        return ctx;
    }

    private RuleSummaryVo toRuleSummary(BasketDivisionFormatSnapshot snapshot, RuleVersionView view) {
        RuleSummaryVo vo = new RuleSummaryVo();
        if (snapshot == null) {
            vo.setRuleStatus(null);
            vo.setRuleStatusName(V4AdminDisplaySupport.ruleStatusLabel(null));
            vo.setLocked(false);
            return vo;
        }
        vo.setRuleVersionId(snapshot.getId());
        vo.setRuleVersionNo(view != null ? view.getRuleVersionNo() : RuleVersionSupport.productRuleVersionNo(snapshot));
        vo.setRuleStatus(snapshot.getSnapshotStatus());
        vo.setRuleStatusName(V4AdminDisplaySupport.ruleStatusLabel(snapshot.getSnapshotStatus()));
        vo.setLocked(view != null && view.isLocked());
        vo.setTemplateType(snapshot.getSourceTemplateType());
        vo.setTemplateCode(snapshot.getSourceTemplateCode());
        return vo;
    }

    private RosterSummaryVo toRosterSummary(BasketDivisionRosterVersion current, List<BasketDivisionRosterVersion> versions) {
        RosterSummaryVo vo = new RosterSummaryVo();
        if (current == null) {
            vo.setRosterStatus("DRAFT");
            vo.setRosterStatusName(V4AdminDisplaySupport.rosterStatusLabel("DRAFT"));
            vo.setLocked(false);
        } else {
            vo.setRosterStatus(current.getStatus());
            vo.setRosterStatusName(V4AdminDisplaySupport.rosterStatusLabel(current.getStatus()));
            vo.setCurrentRosterVersionId(current.getId());
            vo.setCurrentRosterVersionNo(current.getVersionNo());
            vo.setFormalCount(current.getFormalCount());
            vo.setCandidateCount(current.getCandidateCount());
            vo.setExcludedCount(current.getExcludedCount());
            vo.setLocked("LOCKED".equals(current.getStatus()));
        }
        for (BasketDivisionRosterVersion version : versions) {
            RosterVersionHistoryItemVo item = new RosterVersionHistoryItemVo();
            item.setRosterVersionId(version.getId());
            item.setVersionNo(version.getVersionNo());
            item.setStatus(version.getStatus());
            item.setStatusName(V4AdminDisplaySupport.rosterStatusLabel(version.getStatus()));
            item.setIsCurrent(version.getIsCurrent());
            item.setFormalCount(version.getFormalCount());
            item.setCandidateCount(version.getCandidateCount());
            item.setExcludedCount(version.getExcludedCount());
            item.setCreateTime(version.getCreateTime());
            item.setLockedTime(version.getLockedTime());
            item.setLockedBy(version.getLockedBy());
            vo.getVersions().add(item);
        }
        return vo;
    }

    private StageWorkflowSummaryVo toStageSummary(BasketDivision division,
                                                  BasketDivisionFormatSnapshot snapshot,
                                                  BasketDivisionRosterVersion currentRoster,
                                                  BasketDivisionStageExecutionListVo row,
                                                  List<BasketDivisionStage> allStages) {
        StageScheduleSummaryVo schedule = stageScheduleApplicationService.summary(row.getStageId());
        StagePairingExecutionVo pairing = null;
        boolean pairingRequired = needsPairing(row);
        if (pairingRequired) {
            pairing = pairingExecutionApplicationService.getCurrent(row.getStageId());
        }
        BasketStagePublishRecord publishRecord = publishRecordMapper.selectCurrentByStageId(row.getStageId());
        int finished = matchMapper.countFinishedByStageId(row.getStageId());
        boolean doubleElim = DoubleEliminationCapabilityGate.isDoubleElimination(row.getCompetitionFormat());
        boolean ruleLocked = DivisionOperationCapabilitySupport.isRuleLocked(snapshot == null ? null : snapshot.getSnapshotStatus());
        boolean versioned = DivisionOperationCapabilitySupport.isVersioned(snapshot == null ? null : snapshot.getRosterWorkflowMode());
        boolean rosterReady = !versioned || (currentRoster != null && "LOCKED".equals(currentRoster.getStatus()));
        String pairingBlock = pairingRequired
                ? structureGenerationPrerequisiteValidator.pairingBlockReason(
                row.getSeedRule(), row.getDrawRule(), row.getPairingRule(), pairing)
                : null;
        boolean pairingOk = pairingBlock == null;
        boolean qualificationReady = qualificationReady(row, allStages);
        StageOperationCapabilities sc = DivisionOperationCapabilitySupport.stageCapabilities(
                ruleLocked,
                rosterReady,
                row.getStructureStatus(),
                row.getPublishStatus(),
                schedule.getUnscheduledUnits(),
                schedule.isCanRecommend(),
                pairingOk,
                pairingBlock,
                qualificationReady,
                doubleElim);

        StageWorkflowSummaryVo vo = new StageWorkflowSummaryVo();
        vo.setStageId(row.getStageId());
        vo.setStageCode(row.getStageCode());
        vo.setStageName(row.getStageName());
        vo.setStageOrder(row.getStageOrder());
        vo.setStageType(row.getStageType());
        vo.setStageTypeName(row.getStageTypeName());
        vo.setCompetitionFormat(row.getCompetitionFormat());
        vo.setCompetitionFormatName(V4AdminDisplaySupport.competitionFormatLabel(row.getCompetitionFormat()));
        vo.setParticipantSource(participantSource(row, allStages));
        vo.setParticipantCount(row.getParticipantCount());
        vo.setStructureStatus(row.getStructureStatus());
        vo.setStructureStatusName(V4AdminDisplaySupport.structureStatusLabel(row.getStructureStatus()));
        vo.setExecutionVersion(row.getExecutionVersion());
        String pairingStatus = pairingRequired ? (pairing == null ? "PENDING" : pairing.getStatus()) : "NOT_REQUIRED";
        vo.setPairingStatus(pairingStatus);
        vo.setPairingStatusName(V4AdminDisplaySupport.pairingStatusLabel(pairingStatus));
        vo.setQualificationStatus(row.getExecutionStatus());
        vo.setQualificationStatusName(qualificationLabel(row));
        vo.setScheduledUnits(schedule.getScheduledUnits());
        vo.setTotalUnits(schedule.getTotalUnits());
        vo.setUnresolvedPlaceholders(schedule.getUnresolvedUnits());
        vo.setResolvedPlaceholders(schedule.getResolvedUnits());
        vo.setPublishStatus(row.getPublishStatus());
        vo.setPublishStatusName(V4AdminDisplaySupport.publishStatusLabel(row.getPublishStatus()));
        vo.setPublishNo(publishRecord == null ? null : publishRecord.getPublishNo());
        vo.setScheduleRevision(row.getScheduleRevision());
        vo.setFinishedCount(finished);
        vo.setResultTotal(schedule.getTotalUnits());
        vo.setCapabilities(sc);
        if ("待生成".equals(sc.getBlockReason()) && sc.isCanGenerateStructure()) {
            vo.setWorkflowHint("抽签已确认，可以生成结构");
        } else {
            vo.setWorkflowHint(sc.getBlockReason() == null ? resolveStageHint(vo) : sc.getBlockReason());
        }
        return vo;
    }

    private boolean needsPairing(BasketDivisionStageExecutionListVo row) {
        if (!CompetitionFormatEnum.SINGLE_ELIMINATION.getCode().equals(row.getCompetitionFormat())) {
            return false;
        }
        String draw = row.getDrawRule();
        String seed = row.getSeedRule();
        String pairing = row.getPairingRule();
        return "RANDOM_CONFIRM".equals(draw) || "MANUAL_SLOT".equals(draw)
                || "ADMIN_SEED".equals(seed) || "EXTERNAL_RANKING".equals(seed)
                || "GROUP_CROSS".equals(pairing) || "MANUAL".equals(pairing);
    }

    private boolean qualificationReady(BasketDivisionStageExecutionListVo row, List<BasketDivisionStage> allStages) {
        BasketDivisionStage current = allStages.stream()
                .filter(s -> Objects.equals(s.getId(), row.getStageId()))
                .findFirst()
                .orElse(null);
        if (current == null || current.getStageOrder() == null || current.getStageOrder() <= 1) {
            return true;
        }
        BasketDivisionStage previous = allStages.stream()
                .filter(s -> s.getStageOrder() != null && s.getStageOrder() < current.getStageOrder())
                .max((a, b) -> Integer.compare(a.getStageOrder(), b.getStageOrder()))
                .orElse(null);
        if (previous == null) {
            return true;
        }
        String status = previous.getExecutionStatus();
        if (StageExecutionStatusEnum.COMPLETED.getCode().equals(status)) {
            return true;
        }
        return previous.getConfirmedTime() != null;
    }

    private String participantSource(BasketDivisionStageExecutionListVo row, List<BasketDivisionStage> allStages) {
        if (row.getStageOrder() == null || row.getStageOrder() <= 1) {
            return "本赛区正式参赛名单";
        }
        BasketDivisionStage previous = allStages.stream()
                .filter(s -> s.getStageOrder() != null && s.getStageOrder() < row.getStageOrder())
                .max((a, b) -> Integer.compare(a.getStageOrder(), b.getStageOrder()))
                .orElse(null);
        if (previous == null) {
            return "上一比赛阶段晋级队伍";
        }
        return "「" + previous.getStageName() + "」晋级队伍";
    }

    private String qualificationLabel(BasketDivisionStageExecutionListVo row) {
        String status = row.getExecutionStatus();
        if (StageExecutionStatusEnum.RESULT_PENDING_CONFIRM.getCode().equals(status)) {
            return "等待排名确认";
        }
        if (StageExecutionStatusEnum.COMPLETED.getCode().equals(status) || row.getConfirmedTime() != null) {
            return "晋级已确认";
        }
        if (StageExecutionStatusEnum.RUNNING.getCode().equals(status)) {
            return "比赛进行中";
        }
        return row.getExecutionStatusName();
    }

    private String resolveStageHint(StageWorkflowSummaryVo vo) {
        if (PublishStatusEnum.PUBLISHED.getCode().equals(vo.getPublishStatus())) {
            return "已发布";
        }
        if (vo.getTotalUnits() != null && vo.getScheduledUnits() != null
                && vo.getTotalUnits() > 0 && Objects.equals(vo.getScheduledUnits(), vo.getTotalUnits())) {
            return "排期完成，可发布";
        }
        return "进行中";
    }

    private String resolveDivisionHint(DivisionOperationCapabilities caps, List<StageWorkflowSummaryVo> stages) {
        if (caps.getBlockReason() != null) {
            return caps.getBlockReason();
        }
        return stages.stream()
                .map(StageWorkflowSummaryVo::getWorkflowHint)
                .filter(Objects::nonNull)
                .collect(Collectors.joining("；"));
    }
}
