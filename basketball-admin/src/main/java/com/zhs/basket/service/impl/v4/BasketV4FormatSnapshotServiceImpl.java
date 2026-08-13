package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.*;
import com.zhs.basket.dto.v4.BasketDivisionFormatPlanApplyReq;
import com.zhs.basket.dto.v4.BasketDivisionStageOrderReq;
import com.zhs.basket.dto.v4.BasketDivisionStageSaveReq;
import com.zhs.basket.dto.v4.BasketDivisionTieBreakRuleSaveReq;
import com.zhs.basket.enums.v4.*;
import com.zhs.basket.mapper.v4.*;
import com.zhs.basket.service.v4.IBasketV4FormatSnapshotService;
import com.zhs.basket.support.v4.DivisionFormatPlanValidator;
import com.zhs.basket.support.v4.DivisionFormatSnapshotFactory;
import com.zhs.basket.support.v4.DivisionRuleMutableSpecification;
import com.zhs.basket.support.v4.FormatSnapshotReadySpecification;
import com.zhs.basket.support.v4.StageFormatSpecification;
import com.zhs.basket.support.v4.V4FormatDisplaySupport;
import com.zhs.basket.vo.v4.BasketDivisionFormatSnapshotVo;
import com.zhs.basket.vo.v4.BasketDivisionStageVo;
import com.zhs.basket.vo.v4.BasketDivisionTieBreakRuleVo;
import com.zhs.basket.vo.v4.FormatPlanValidationResultVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.StringUtils;
import com.zhs.common.utils.bean.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BasketV4FormatSnapshotServiceImpl implements IBasketV4FormatSnapshotService {

    @Autowired
    private BasketFormatTemplateStageMapper templateStageMapper;
    @Autowired
    private BasketFormatTemplateTieBreakRuleMapper templateTieBreakRuleMapper;
    @Autowired
    private BasketDivisionFormatSnapshotMapper snapshotMapper;
    @Autowired
    private BasketDivisionStageMapper divisionStageMapper;
    @Autowired
    private BasketDivisionTieBreakRuleMapper divisionTieBreakRuleMapper;
    @Autowired
    private BasketDivisionMapper divisionMapper;
    @Autowired
    private DivisionFormatSnapshotFactory snapshotFactory;
    @Autowired
    private DivisionRuleMutableSpecification ruleMutableSpecification;
    @Autowired
    private BasketV4FormatTemplateServiceImpl templateService;
    @Autowired
    private BasketSeasonMapper seasonMapper;
    @Autowired
    private DivisionFormatPlanValidator formatPlanValidator;

    private static final List<String> DEFAULT_TIE_BREAK_CODES = List.of(
            "WIN_COUNT", "HEAD_TO_HEAD", "POINT_DIFFERENCE", "TOTAL_POINTS", "MANUAL");

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createDraftSnapshot(Long divisionId, Long templateId) {
        BasketDivision division = requireDivision(divisionId);
        BasketFormatTemplate template = templateService.requireEnabledTemplate(templateId);
        BasketSeason season = requireSeason(division.getSeasonId());
        templateService.assertTemplateBelongsToSeries(template, season.getSeriesId());
        BasketDivisionFormatSnapshot existing = snapshotMapper.selectActiveByDivisionId(divisionId);
        if (existing != null) {
            throw new ServiceException("Division 已存在当前规则快照，请使用重建");
        }
        return doCreateSnapshot(division, template, null);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void rebuildDraftSnapshot(Long divisionId, Long templateId) {
        BasketDivision division = requireDivision(divisionId);
        BasketDivisionFormatSnapshot existing = snapshotMapper.selectActiveByDivisionId(divisionId);
        ruleMutableSpecification.assertCanRebuild(division, existing);
        Long supersededId = null;
        if (existing != null) {
            if (SnapshotStatusEnum.fromCode(existing.getSnapshotStatus()).isLockedOrBeyond()) {
                throw new ServiceException("已锁定规则快照不允许重建");
            }
            supersededId = existing.getId();
            supersedeSnapshot(existing);
        }
        BasketFormatTemplate template = templateService.requireEnabledTemplate(templateId);
        BasketSeason season = requireSeason(division.getSeasonId());
        templateService.assertTemplateBelongsToSeries(template, season.getSeriesId());
        Long snapshotId = doCreateSnapshot(division, template, supersededId);
        division.setFormatTemplateId(templateId);
        division.setFormatSnapshotId(snapshotId);
        division.setUpdateTime(new Date());
        divisionMapper.updateById(division);
    }

    @Override
    public BasketDivisionFormatSnapshotVo getDivisionSnapshot(Long divisionId) {
        BasketDivision division = requireDivision(divisionId);
        BasketDivisionFormatSnapshot snapshot = snapshotMapper.selectActiveByDivisionId(divisionId);
        if (snapshot == null) {
            return null;
        }
        BasketDivisionFormatSnapshotVo vo = toSnapshotVo(snapshot, division);
        List<BasketDivisionTieBreakRule> allRules = divisionTieBreakRuleMapper.selectBySnapshotId(snapshot.getId());
        Map<Long, List<BasketDivisionTieBreakRule>> rulesByStage = allRules.stream()
                .collect(Collectors.groupingBy(BasketDivisionTieBreakRule::getStageId));
        vo.setStages(divisionStageMapper.selectBySnapshotId(snapshot.getId()).stream().map(s -> {
            BasketDivisionStageVo stageVo = new BasketDivisionStageVo();
            BeanUtils.copyBeanProp(stageVo, s);
            V4FormatDisplaySupport.enrichDivisionStageVo(stageVo);
            List<BasketDivisionTieBreakRule> stageRules = rulesByStage.getOrDefault(s.getId(), Collections.emptyList());
            stageVo.setTieBreakRules(stageRules.stream().map(this::toTieBreakVo).collect(Collectors.toList()));
            return stageVo;
        }).collect(Collectors.toList()));
        vo.setFormatSummary(V4FormatDisplaySupport.buildFormatSummary(vo));
        boolean mutable = ruleMutableSpecification.isMutable(division, snapshot);
        vo.setMutable(mutable);
        vo.setLockable(SnapshotStatusEnum.DRAFT.getCode().equals(snapshot.getSnapshotStatus()));
        vo.setLocked(SnapshotStatusEnum.fromCode(snapshot.getSnapshotStatus()).isLockedOrBeyond());
        return vo;
    }

    @Override
    public void validateSnapshotReady(Long divisionId) {
        BasketDivision division = requireDivision(divisionId);
        BasketDivisionFormatSnapshot snapshot = requireSnapshot(divisionId);
        if (!SnapshotStatusEnum.DRAFT.getCode().equals(snapshot.getSnapshotStatus())) {
            throw new ServiceException("仅草稿状态规则快照可校验");
        }
        validateDivisionBasics(division);
        List<BasketDivisionStage> stages = divisionStageMapper.selectBySnapshotId(snapshot.getId());
        List<BasketDivisionTieBreakRule> tieBreakRules = divisionTieBreakRuleMapper.selectBySnapshotId(snapshot.getId());
        new FormatSnapshotReadySpecification().validate(stages, tieBreakRules);
        normalizeStageOrders(stages);
    }

    @Override
    public FormatPlanValidationResultVo validateFormatPlan(BasketDivisionFormatPlanApplyReq req) {
        return formatPlanValidator.validate(req);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BasketDivisionFormatSnapshotVo applyFormatPlan(Long divisionId, BasketDivisionFormatPlanApplyReq req) {
        FormatPlanValidationResultVo validation = formatPlanValidator.validate(req);
        if (!validation.isValid()) {
            throw new ServiceException(String.join("\n", validation.getErrors()));
        }
        BasketDivision division = requireDivision(divisionId);
        FormatPlanCodeEnum planCode = FormatPlanCodeEnum.fromCode(req.getPlanCode());
        BasketSeason season = requireSeason(division.getSeasonId());
        BasketFormatTemplate template = templateService.requireEnabledTemplateByCode(
                season.getSeriesId(), planCode.getBaseTemplateCode());
        templateService.assertTemplateBelongsToSeries(template, season.getSeriesId());

        BasketDivisionFormatSnapshot existing = snapshotMapper.selectActiveByDivisionId(divisionId);
        ruleMutableSpecification.assertCanRebuild(division, existing);
        Long supersededId = null;
        if (existing != null) {
            if (SnapshotStatusEnum.fromCode(existing.getSnapshotStatus()).isLockedOrBeyond()) {
                throw new ServiceException("已锁定规则快照不允许应用赛制方案");
            }
            supersededId = existing.getId();
            supersedeSnapshot(existing);
        }

        int nextVersion = snapshotMapper.selectMaxVersionByDivisionId(divisionId) + 1;
        BasketDivisionFormatSnapshot snapshot = snapshotFactory.buildSnapshotHeader(
                divisionId, template, nextVersion, supersededId);
        snapshotMapper.insert(snapshot);

        List<BasketDivisionStage> stages = buildFormatPlanStages(divisionId, snapshot.getId(), planCode, req);
        Map<String, Long> stageCodeToId = new HashMap<>();
        for (BasketDivisionStage stage : stages) {
            divisionStageMapper.insert(stage);
            stageCodeToId.put(stage.getStageCode(), stage.getId());
        }
        insertDefaultTieBreakRules(divisionId, snapshot.getId(), stages, stageCodeToId);

        division.setFormatTemplateId(template.getId());
        division.setFormatSnapshotId(snapshot.getId());
        division.setUpdateTime(new Date());
        divisionMapper.updateById(division);

        validateSnapshotReady(divisionId);
        return getDivisionSnapshot(divisionId);
    }

    private List<BasketDivisionStage> buildFormatPlanStages(Long divisionId, Long snapshotId,
                                                            FormatPlanCodeEnum planCode,
                                                            BasketDivisionFormatPlanApplyReq req) {
        Date now = new Date();
        int winPoints = req.getWinPoints() == null ? 2 : req.getWinPoints();
        int lossPoints = req.getLossPoints() == null ? 1 : req.getLossPoints();
        int cycles = req.getRoundRobinLegs() == null ? 1 : req.getRoundRobinLegs();
        String pairingRule = StringUtils.isEmpty(req.getEliminationPairingRule())
                ? "HIGH_VS_LOW" : req.getEliminationPairingRule().trim();
        PairingRuleEnum.fromCode(pairingRule);

        BasketDivisionStage first = new BasketDivisionStage();
        first.setSnapshotId(snapshotId);
        first.setDivisionId(divisionId);
        first.setStageOrder(1);
        first.setBestOf(1);
        first.setWinPoints(winPoints);
        first.setLossPoints(lossPoints);
        first.setPairingRule("NONE");
        first.setRequiresManualConfirmation(0);
        first.setStageStatus(DivisionStageStatusEnum.DRAFT.getCode());
        first.setExecutionStatus(StageExecutionStatusEnum.DRAFT.getCode());
        first.setExecutionVersion(0);
        first.setCreateTime(now);
        first.setUpdateTime(now);
        first.setIsDelete(0);

        BasketDivisionStage second = new BasketDivisionStage();
        second.setSnapshotId(snapshotId);
        second.setDivisionId(divisionId);
        second.setStageCode("KNOCKOUT");
        second.setStageName("淘汰赛");
        second.setStageOrder(2);
        second.setStageType(StageTypeEnum.KNOCKOUT.getCode());
        second.setOrganizationMode(OrganizationModeEnum.UNGROUPED.getCode());
        second.setCompetitionFormat(CompetitionFormatEnum.SINGLE_ELIMINATION.getCode());
        second.setGroupingMode(GroupingModeEnum.NONE.getCode());
        second.setBestOf(1);
        second.setAdvancementType(AdvancementTypeEnum.NONE.getCode());
        second.setPairingRule(pairingRule);
        second.setRequiresManualConfirmation(0);
        second.setStageStatus(DivisionStageStatusEnum.DRAFT.getCode());
        second.setExecutionStatus(StageExecutionStatusEnum.DRAFT.getCode());
        second.setExecutionVersion(0);
        second.setCreateTime(now);
        second.setUpdateTime(now);
        second.setIsDelete(0);

        if (planCode == FormatPlanCodeEnum.GROUP_TO_ELIM) {
            first.setStageCode("GROUP");
            first.setStageName("小组赛");
            first.setStageType(StageTypeEnum.GROUP.getCode());
            first.setOrganizationMode(OrganizationModeEnum.GROUPED.getCode());
            first.setCompetitionFormat(CompetitionFormatEnum.ROUND_ROBIN.getCode());
            first.setGroupingMode(GroupingModeEnum.MANUAL.getCode());
            first.setGroupCount(req.getGroupCount());
            first.setRoundRobinCycles(cycles);
            first.setAdvancementType(AdvancementTypeEnum.GROUP_TOP_N.getCode());
            first.setAdvancementCount(req.getQualifiersPerGroup());
            first.setNextStageCode(second.getStageCode());
        } else {
            first.setStageCode("REGULAR");
            first.setStageName("常规赛");
            first.setStageType(StageTypeEnum.REGULAR.getCode());
            first.setOrganizationMode(OrganizationModeEnum.UNGROUPED.getCode());
            first.setCompetitionFormat(CompetitionFormatEnum.ROUND_ROBIN.getCode());
            first.setGroupingMode(GroupingModeEnum.NONE.getCode());
            first.setRoundRobinCycles(cycles);
            first.setAdvancementType(AdvancementTypeEnum.OVERALL_TOP_N.getCode());
            first.setAdvancementCount(req.getOverallAdvanceCount());
            first.setNextStageCode(second.getStageCode());
        }
        return List.of(first, second);
    }

    private void insertDefaultTieBreakRules(Long divisionId, Long snapshotId,
                                            List<BasketDivisionStage> stages,
                                            Map<String, Long> stageCodeToId) {
        Date now = new Date();
        for (BasketDivisionStage stage : stages) {
            if (!CompetitionFormatEnum.ROUND_ROBIN.getCode().equals(stage.getCompetitionFormat())) {
                continue;
            }
            Long stageId = stageCodeToId.get(stage.getStageCode());
            if (stageId == null) {
                continue;
            }
            int priority = 1;
            for (String ruleCode : DEFAULT_TIE_BREAK_CODES) {
                TieBreakRuleEnum.fromCode(ruleCode);
                BasketDivisionTieBreakRule rule = new BasketDivisionTieBreakRule();
                rule.setSnapshotId(snapshotId);
                rule.setDivisionId(divisionId);
                rule.setStageId(stageId);
                rule.setRuleCode(ruleCode);
                rule.setPriorityNo(priority++);
                rule.setEnabled(1);
                rule.setCreateTime(now);
                rule.setUpdateTime(now);
                rule.setIsDelete(0);
                divisionTieBreakRuleMapper.insert(rule);
            }
        }
    }

    /**
     * 锁定当前草稿快照。锁定后该 Snapshot 即正式 Rule Version Vn，禁止再改核心规则。
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void lockSnapshot(Long divisionId, Long operatorId) {
        BasketDivisionFormatSnapshot snapshot = requireSnapshot(divisionId);
        if (SnapshotStatusEnum.fromCode(snapshot.getSnapshotStatus()).isLockedOrBeyond()) {
            return;
        }
        validateSnapshotReady(divisionId);
        if (!SnapshotStatusEnum.DRAFT.getCode().equals(snapshot.getSnapshotStatus())) {
            throw new ServiceException("规则快照已锁定或不可锁定");
        }
        snapshot.setSnapshotStatus(SnapshotStatusEnum.LOCKED.getCode());
        snapshot.setLockedTime(new Date());
        snapshot.setLockedBy(operatorId);
        snapshot.setUpdateTime(new Date());
        if (snapshotMapper.updateById(snapshot) != 1) {
            throw new ServiceException("锁定规则快照失败");
        }
        List<BasketDivisionStage> stages = divisionStageMapper.selectBySnapshotId(snapshot.getId());
        Date now = new Date();
        for (BasketDivisionStage stage : stages) {
            stage.setStageStatus(DivisionStageStatusEnum.READY.getCode());
            stage.setUpdateTime(now);
            divisionStageMapper.updateById(stage);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long addStage(Long divisionId, BasketDivisionStageSaveReq req) {
        BasketDivision division = requireDivision(divisionId);
        BasketDivisionFormatSnapshot snapshot = requireSnapshot(divisionId);
        ruleMutableSpecification.assertCanEditRules(division, snapshot);
        validateStageReq(req);
        List<BasketDivisionStage> stages = divisionStageMapper.selectBySnapshotId(snapshot.getId());
        assertStageCodeUnique(stages, req.getStageCode(), null);
        int nextOrder = stages.stream().mapToInt(BasketDivisionStage::getStageOrder).max().orElse(0) + 1;
        Date now = new Date();
        BasketDivisionStage stage = new BasketDivisionStage();
        applyStageReq(stage, req);
        stage.setStageType(StageTypeEnum.CUSTOM.getCode());
        stage.setSnapshotId(snapshot.getId());
        stage.setDivisionId(divisionId);
        stage.setStageOrder(req.getStageOrder() != null ? req.getStageOrder() : nextOrder);
        stage.setStageStatus(DivisionStageStatusEnum.DRAFT.getCode());
        stage.setExecutionStatus(StageExecutionStatusEnum.DRAFT.getCode());
        stage.setExecutionVersion(0);
        stage.setCreateTime(now);
        stage.setUpdateTime(now);
        stage.setIsDelete(0);
        divisionStageMapper.insert(stage);
        renumberStageOrders(snapshot.getId());
        return stage.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStage(Long divisionId, Long stageId, BasketDivisionStageSaveReq req) {
        BasketDivision division = requireDivision(divisionId);
        BasketDivisionFormatSnapshot snapshot = requireSnapshot(divisionId);
        ruleMutableSpecification.assertCanEditRules(division, snapshot);
        validateStageReq(req);
        BasketDivisionStage stage = requireStage(stageId, snapshot.getId());
        List<BasketDivisionStage> stages = divisionStageMapper.selectBySnapshotId(snapshot.getId());
        assertStageCodeUnique(stages, req.getStageCode(), stageId);
        applyStageReq(stage, req);
        boolean orderChanged = req.getStageOrder() != null && !req.getStageOrder().equals(stage.getStageOrder());
        if (orderChanged) {
            for (BasketDivisionStage s : stages) {
                divisionStageMapper.updateStageOrder(s.getId(), 100000 + s.getId().intValue());
            }
            stage.setStageOrder(req.getStageOrder());
        }
        stage.setUpdateTime(new Date());
        if (divisionStageMapper.updateById(stage) != 1) {
            throw new ServiceException("更新阶段失败");
        }
        renumberStageOrders(snapshot.getId());
        // 编辑过程中仅校验 nextStage 引用存在；完整链路与循环检测交给 validateSnapshotReady
        assertNextStageReferencesExist(snapshot.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteStage(Long divisionId, Long stageId) {
        BasketDivision division = requireDivision(divisionId);
        BasketDivisionFormatSnapshot snapshot = requireSnapshot(divisionId);
        ruleMutableSpecification.assertCanEditRules(division, snapshot);
        BasketDivisionStage stage = requireStage(stageId, snapshot.getId());
        List<BasketDivisionStage> stages = divisionStageMapper.selectBySnapshotId(snapshot.getId());
        for (BasketDivisionStage other : stages) {
            if (stage.getStageCode().equals(other.getNextStageCode())) {
                throw new ServiceException("阶段「" + stage.getStageName() + "」被「"
                        + other.getStageName() + "」引用为下一阶段，无法删除");
            }
        }
        divisionTieBreakRuleMapper.softDeleteByStageId(stageId);
        if (divisionStageMapper.softDeleteById(stageId) != 1) {
            throw new ServiceException("删除阶段失败");
        }
        renumberStageOrders(snapshot.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reorderStages(Long divisionId, BasketDivisionStageOrderReq req) {
        BasketDivision division = requireDivision(divisionId);
        BasketDivisionFormatSnapshot snapshot = requireSnapshot(divisionId);
        ruleMutableSpecification.assertCanEditRules(division, snapshot);
        if (req == null || req.getStageIds() == null || req.getStageIds().isEmpty()) {
            throw new ServiceException("阶段顺序不能为空");
        }
        List<BasketDivisionStage> stages = divisionStageMapper.selectBySnapshotId(snapshot.getId());
        if (stages.size() != req.getStageIds().size()) {
            throw new ServiceException("阶段顺序列表不完整");
        }
        Set<Long> existing = stages.stream().map(BasketDivisionStage::getId).collect(Collectors.toSet());
        // 先写临时大序号避免唯一冲突
        int temp = 1000;
        for (Long stageId : req.getStageIds()) {
            if (!existing.contains(stageId)) {
                throw new ServiceException("阶段不属于当前快照：" + stageId);
            }
            divisionStageMapper.updateStageOrder(stageId, temp++);
        }
        int order = 1;
        for (Long stageId : req.getStageIds()) {
            divisionStageMapper.updateStageOrder(stageId, order++);
        }
        // 最后阶段清空 nextStageCode 校验交由 ready 规格处理；这里仅保证顺序连续
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTieBreakRules(Long divisionId, BasketDivisionTieBreakRuleSaveReq req) {
        BasketDivision division = requireDivision(divisionId);
        BasketDivisionFormatSnapshot snapshot = requireSnapshot(divisionId);
        ruleMutableSpecification.assertCanEditRules(division, snapshot);
        if (req == null || req.getStageId() == null) {
            throw new ServiceException("stageId 不能为空");
        }
        BasketDivisionStage stage = requireStage(req.getStageId(), snapshot.getId());
        List<BasketDivisionTieBreakRuleSaveReq.Item> items =
                req.getRules() == null ? Collections.emptyList() : req.getRules();
        Set<String> codes = new HashSet<>();
        for (BasketDivisionTieBreakRuleSaveReq.Item item : items) {
            if (item == null || StringUtils.isEmpty(item.getRuleCode())) {
                throw new ServiceException("同分规则编码不能为空");
            }
            TieBreakRuleEnum.fromCode(item.getRuleCode());
            if (!codes.add(item.getRuleCode())) {
                throw new ServiceException("同一阶段同分规则不能重复：" + item.getRuleCode());
            }
        }
        // MANUAL 应放末尾
        boolean sawManual = false;
        List<BasketDivisionTieBreakRuleSaveReq.Item> sorted = items.stream()
                .sorted(Comparator.comparing(i -> i.getPriorityNo() == null ? Integer.MAX_VALUE : i.getPriorityNo()))
                .collect(Collectors.toList());
        for (BasketDivisionTieBreakRuleSaveReq.Item item : sorted) {
            if (TieBreakRuleEnum.MANUAL.getCode().equals(item.getRuleCode())) {
                sawManual = true;
            } else if (sawManual) {
                throw new ServiceException("MANUAL 同分规则必须放在末尾");
            }
        }
        divisionTieBreakRuleMapper.softDeleteByStageId(stage.getId());
        Date now = new Date();
        int priority = 1;
        for (BasketDivisionTieBreakRuleSaveReq.Item item : sorted) {
            BasketDivisionTieBreakRule rule = new BasketDivisionTieBreakRule();
            rule.setSnapshotId(snapshot.getId());
            rule.setDivisionId(divisionId);
            rule.setStageId(stage.getId());
            rule.setRuleCode(item.getRuleCode());
            rule.setPriorityNo(priority++);
            rule.setEnabled(item.getEnabled() == null ? 1 : item.getEnabled());
            rule.setCreateTime(now);
            rule.setUpdateTime(now);
            rule.setIsDelete(0);
            divisionTieBreakRuleMapper.insert(rule);
        }
    }

    private Long doCreateSnapshot(BasketDivision division, BasketFormatTemplate template, Long supersededSnapshotId) {
        List<BasketFormatTemplateStage> templateStages = templateStageMapper.selectByTemplateId(template.getId());
        if (templateStages.isEmpty()) {
            throw new ServiceException("赛制模板缺少阶段定义");
        }
        int nextVersion = snapshotMapper.selectMaxVersionByDivisionId(division.getId()) + 1;
        BasketDivisionFormatSnapshot snapshot = snapshotFactory.buildSnapshotHeader(
                division.getId(), template, nextVersion, supersededSnapshotId);
        snapshotMapper.insert(snapshot);
        List<BasketDivisionStage> stages = snapshotFactory.buildStages(division.getId(), snapshot.getId(), templateStages);
        Map<String, Long> stageCodeToId = new HashMap<>();
        for (BasketDivisionStage stage : stages) {
            divisionStageMapper.insert(stage);
            stageCodeToId.put(stage.getStageCode(), stage.getId());
        }
        List<BasketFormatTemplateTieBreakRule> templateRules = templateTieBreakRuleMapper.selectByTemplateId(template.getId());
        List<BasketDivisionTieBreakRule> rules = snapshotFactory.buildTieBreakRules(
                division.getId(), snapshot.getId(), templateRules, stageCodeToId);
        for (BasketDivisionTieBreakRule rule : rules) {
            divisionTieBreakRuleMapper.insert(rule);
        }
        division.setFormatTemplateId(template.getId());
        division.setFormatSnapshotId(snapshot.getId());
        division.setUpdateTime(new Date());
        divisionMapper.updateById(division);
        return snapshot.getId();
    }

    private void validateStageReq(BasketDivisionStageSaveReq req) {
        if (req == null) {
            throw new ServiceException("阶段参数不能为空");
        }
        if (StringUtils.isEmpty(req.getStageCode()) || StringUtils.isEmpty(req.getStageName())) {
            throw new ServiceException("阶段编码和名称不能为空");
        }
        StageFormatSpecification.requireExplicitOrganizationMode(req.getOrganizationMode());
        OrganizationModeEnum.fromCode(req.getOrganizationMode());
        StageTypeEnum.fromCode(req.getStageType());
        CompetitionFormatEnum format = CompetitionFormatEnum.fromCode(req.getCompetitionFormat());
        if (format == CompetitionFormatEnum.MANUAL) {
            throw new ServiceException("当前版本自定义赛制不支持 MANUAL 比赛方式");
        }
        GroupingModeEnum mode = GroupingModeEnum.fromCode(req.getGroupingMode());
        AdvancementTypeEnum adv = AdvancementTypeEnum.fromCode(req.getAdvancementType());
        if (adv == AdvancementTypeEnum.SERIES_WINNER) {
            throw new ServiceException("当前版本暂不支持 SERIES_WINNER（系列赛胜者）晋级方式，请使用 WINNER_ADVANCE");
        }
        PairingRuleEnum.fromCode(req.getPairingRule());
        FormatSnapshotReadySpecification.validateBestOfForSave(req.getBestOf());

        BasketDivisionStage probe = new BasketDivisionStage();
        probe.setStageName(req.getStageName());
        probe.setOrganizationMode(req.getOrganizationMode());
        probe.setGroupingMode(req.getGroupingMode());
        probe.setGroupCount(req.getGroupCount());
        probe.setCompetitionFormat(req.getCompetitionFormat());
        probe.setAdvancementType(req.getAdvancementType());
        probe.setAdvancementCount(req.getAdvancementCount());
        probe.setRoundRobinCycles(req.getRoundRobinCycles());
        probe.setWinPoints(req.getWinPoints());
        probe.setLossPoints(req.getLossPoints());
        StageFormatSpecification.validateOrganizationAndGrouping(probe);
        StageFormatSpecification.validateCompetitionFormat(probe, true);
        StageFormatSpecification.validateAllowedCombination(probe);
        StageFormatSpecification.validateAdvancementCount(probe);
        boolean isLastForReq = StringUtils.isEmpty(req.getNextStageCode());
        StageFormatSpecification.validateChainAdvancement(probe, isLastForReq);
        StageFormatSpecification.validateRoundRobinRules(probe, isLastForReq);
        StageFormatSpecification.validateEliminationRules(probe, isLastForReq);

        if (CompetitionFormatEnum.ROUND_ROBIN.getCode().equals(req.getCompetitionFormat())) {
            if (req.getRoundRobinCycles() == null || req.getRoundRobinCycles() <= 0) {
                throw new ServiceException("循环赛必须配置 roundRobinCycles");
            }
            if (req.getWinPoints() == null || req.getLossPoints() == null) {
                throw new ServiceException("循环赛必须配置胜负积分");
            }
        }
        if (CompetitionFormatEnum.SINGLE_ELIMINATION.getCode().equals(req.getCompetitionFormat())
                && adv == AdvancementTypeEnum.WINNER_ADVANCE
                && req.getAdvancementCount() != null && req.getAdvancementCount() > 0) {
            throw new ServiceException("WINNER_ADVANCE 不需要配置 advancementCount");
        }
        if (adv == AdvancementTypeEnum.NONE) {
            if (req.getAdvancementCount() != null && req.getAdvancementCount() > 0) {
                throw new ServiceException("无晋级时 advancementCount 必须为空或0");
            }
            req.setAdvancementCount(null);
        } else if (adv.requiresAdvancementCount()
                && (req.getAdvancementCount() == null || req.getAdvancementCount() <= 0)) {
            throw new ServiceException("当前晋级类型必须配置 advancementCount");
        }
    }

    private void applyStageReq(BasketDivisionStage stage, BasketDivisionStageSaveReq req) {
        stage.setStageCode(req.getStageCode().trim());
        stage.setStageName(req.getStageName().trim());
        stage.setStageType(req.getStageType());
        stage.setOrganizationMode(req.getOrganizationMode());
        stage.setCompetitionFormat(req.getCompetitionFormat());
        stage.setGroupingMode(req.getGroupingMode());
        stage.setGroupCount(req.getGroupCount());
        stage.setRoundRobinCycles(req.getRoundRobinCycles());
        stage.setBestOf(req.getBestOf());
        stage.setWinPoints(req.getWinPoints());
        stage.setLossPoints(req.getLossPoints());
        stage.setForfeitWinPoints(req.getForfeitWinPoints());
        stage.setForfeitLossPoints(req.getForfeitLossPoints());
        stage.setAdvancementType(req.getAdvancementType());
        stage.setAdvancementCount(req.getAdvancementCount());
        stage.setPairingRule(req.getPairingRule());
        stage.setNextStageCode(StringUtils.isEmpty(req.getNextStageCode()) ? null : req.getNextStageCode().trim());
        stage.setRequiresManualConfirmation(req.getRequiresManualConfirmation() == null ? 0 : req.getRequiresManualConfirmation());
    }

    private void assertStageCodeUnique(List<BasketDivisionStage> stages, String stageCode, Long excludeId) {
        for (BasketDivisionStage stage : stages) {
            if (stage.getStageCode().equals(stageCode)
                    && (excludeId == null || !excludeId.equals(stage.getId()))) {
                throw new ServiceException("阶段编码已存在：" + stageCode);
            }
        }
    }

    private void renumberStageOrders(Long snapshotId) {
        List<BasketDivisionStage> stages = divisionStageMapper.selectBySnapshotId(snapshotId);
        int temp = 1000;
        for (BasketDivisionStage stage : stages) {
            divisionStageMapper.updateStageOrder(stage.getId(), temp++);
        }
        int order = 1;
        for (BasketDivisionStage stage : stages) {
            divisionStageMapper.updateStageOrder(stage.getId(), order++);
        }
    }

    private void normalizeStageOrders(List<BasketDivisionStage> stages) {
        for (int i = 0; i < stages.size(); i++) {
            if (!Integer.valueOf(i + 1).equals(stages.get(i).getStageOrder())) {
                throw new ServiceException("阶段顺序必须为连续正整数");
            }
        }
    }

    private void assertNextStageReferencesExist(Long snapshotId) {
        List<BasketDivisionStage> stages = divisionStageMapper.selectBySnapshotId(snapshotId);
        Set<String> codes = stages.stream().map(BasketDivisionStage::getStageCode).collect(Collectors.toSet());
        for (BasketDivisionStage stage : stages) {
            if (StringUtils.isNotEmpty(stage.getNextStageCode()) && !codes.contains(stage.getNextStageCode())) {
                throw new ServiceException("下一阶段编码不存在：" + stage.getNextStageCode());
            }
        }
    }

    private void assertNextStageLinks(Long snapshotId) {
        List<BasketDivisionStage> stages = divisionStageMapper.selectBySnapshotId(snapshotId);
        Set<String> codes = stages.stream().map(BasketDivisionStage::getStageCode).collect(Collectors.toSet());
        Map<String, String> nextMap = new HashMap<>();
        for (BasketDivisionStage stage : stages) {
            if (StringUtils.isNotEmpty(stage.getNextStageCode())) {
                if (!codes.contains(stage.getNextStageCode())) {
                    throw new ServiceException("下一阶段编码不存在：" + stage.getNextStageCode());
                }
                nextMap.put(stage.getStageCode(), stage.getNextStageCode());
            }
        }
        for (BasketDivisionStage stage : stages) {
            Set<String> visited = new HashSet<>();
            String current = stage.getStageCode();
            while (nextMap.containsKey(current)) {
                if (!visited.add(current)) {
                    throw new ServiceException("阶段链路存在循环：" + current);
                }
                current = nextMap.get(current);
            }
        }
    }

    private void validateDivisionBasics(BasketDivision division) {
        if (StringUtils.isEmpty(division.getPlatformCode())
                || StringUtils.isEmpty(division.getGameMode())
                || StringUtils.isEmpty(division.getRegistrationMode())) {
            throw new ServiceException("Division 平台、比赛模式、报名方式不完整");
        }
        try {
            GameModeEnum.requireCode(division.getGameMode()).toRosterLimit()
                    .validateCounts(division.getLineupMinCount(), division.getLineupMaxCount());
        } catch (Exception ex) {
            throw new ServiceException("人数规则与比赛模式不一致：" + ex.getMessage());
        }
    }

    private BasketDivisionFormatSnapshotVo toSnapshotVo(BasketDivisionFormatSnapshot snapshot, BasketDivision division) {
        BasketDivisionFormatSnapshotVo vo = new BasketDivisionFormatSnapshotVo();
        vo.setSnapshotId(snapshot.getId());
        vo.setDivisionId(division.getId());
        vo.setSourceTemplateId(snapshot.getSourceTemplateId());
        vo.setSourceTemplateCode(snapshot.getSourceTemplateCode());
        vo.setSourceTemplateName(snapshot.getSourceTemplateName());
        vo.setSourceTemplateVersion(snapshot.getSourceTemplateVersion());
        vo.setSnapshotVersion(snapshot.getSnapshotVersion());
        vo.setSnapshotStatus(snapshot.getSnapshotStatus());
        vo.setSnapshotStatusName(V4FormatDisplaySupport.snapshotStatusLabel(snapshot.getSnapshotStatus()));
        vo.setIsCurrent(snapshot.getIsCurrent());
        vo.setRuleVersionId(snapshot.getId());
        vo.setRuleVersionNo(snapshot.getSnapshotVersion());
        vo.setRuleVersionStatus(snapshot.getSnapshotStatus());
        return vo;
    }

    private BasketDivisionTieBreakRuleVo toTieBreakVo(BasketDivisionTieBreakRule rule) {
        BasketDivisionTieBreakRuleVo vo = new BasketDivisionTieBreakRuleVo();
        vo.setId(rule.getId());
        vo.setStageId(rule.getStageId());
        vo.setRuleCode(rule.getRuleCode());
        vo.setRuleCodeName(V4FormatDisplaySupport.tieBreakRuleLabel(rule.getRuleCode()));
        vo.setPriorityNo(rule.getPriorityNo());
        vo.setEnabled(rule.getEnabled());
        return vo;
    }

    private BasketDivision requireDivision(Long divisionId) {
        BasketDivision division = divisionMapper.selectById(divisionId);
        if (division == null) {
            throw new ServiceException("Division 不存在");
        }
        return division;
    }

    private BasketSeason requireSeason(Long seasonId) {
        BasketSeason season = seasonMapper.selectById(seasonId);
        if (season == null) {
            throw new ServiceException("Season 不存在");
        }
        return season;
    }

    private BasketDivisionFormatSnapshot requireSnapshot(Long divisionId) {
        BasketDivisionFormatSnapshot snapshot = snapshotMapper.selectActiveByDivisionId(divisionId);
        if (snapshot == null) {
            throw new ServiceException("Division 缺少规则快照");
        }
        return snapshot;
    }

    private BasketDivisionStage requireStage(Long stageId, Long snapshotId) {
        BasketDivisionStage stage = divisionStageMapper.selectById(stageId);
        if (stage == null || !snapshotId.equals(stage.getSnapshotId())) {
            throw new ServiceException("阶段不存在或不属于当前快照");
        }
        return stage;
    }

    private void supersedeSnapshot(BasketDivisionFormatSnapshot existing) {
        snapshotMapper.clearActiveUniqueKey(existing.getId());
        snapshotMapper.markSuperseded(existing.getId());
        divisionStageMapper.softDeleteBySnapshotId(existing.getId());
        divisionTieBreakRuleMapper.softDeleteBySnapshotId(existing.getId());
    }
}
