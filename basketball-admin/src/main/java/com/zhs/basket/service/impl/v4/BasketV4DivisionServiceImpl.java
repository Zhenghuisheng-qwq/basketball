package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionFormatSnapshot;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketFormatTemplate;
import com.zhs.basket.domain.v4.BasketSeason;
import com.zhs.basket.domain.v4.RosterLimit;
import com.zhs.basket.dto.v4.BasketDivisionCreateReq;
import com.zhs.basket.dto.v4.BasketDivisionUpdateReq;
import com.zhs.basket.enums.v4.*;
import com.zhs.basket.mapper.v4.BasketDivisionFormatSnapshotMapper;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageMapper;
import com.zhs.basket.mapper.v4.BasketEventTeamMapper;
import com.zhs.basket.mapper.v4.BasketSeasonMapper;
import com.zhs.basket.mapper.v4.BasketV4MatchMapper;
import com.zhs.basket.query.v4.BasketDivisionQuery;
import com.zhs.basket.service.v4.IBasketV4DivisionClosingService;
import com.zhs.basket.service.v4.IBasketV4DivisionService;
import com.zhs.basket.service.v4.IBasketV4FormatSnapshotService;
import com.zhs.basket.service.v4.IBasketV4SeasonService;
import com.zhs.basket.support.v4.CaptainGrantEventSyncSupport;
import com.zhs.basket.support.v4.BestOfCapabilityGate;
import com.zhs.basket.support.v4.DivisionSafeRecoverySupport;
import com.zhs.basket.support.v4.V4PublicReadCachePublisher;
import com.zhs.basket.support.v4.DivisionRuleMutableSpecification;
import com.zhs.basket.support.v4.V4FormatDisplaySupport;
import com.zhs.basket.vo.v4.BasketDivisionFormatSnapshotVo;
import com.zhs.basket.vo.v4.BasketDivisionListVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.StringUtils;
import com.zhs.common.utils.bean.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class BasketV4DivisionServiceImpl implements IBasketV4DivisionService {

    private static final Integer NOT_DELETED = 0;
    private static final int MAX_TEAM_COUNT_LIMIT = 512;

    @Autowired
    private BasketDivisionMapper divisionMapper;
    @Autowired
    private BasketSeasonMapper seasonMapper;
    @Autowired
    private IBasketV4FormatSnapshotService formatSnapshotService;
    @Autowired
    private BasketV4FormatTemplateServiceImpl formatTemplateService;
    @Autowired
    private DivisionRuleMutableSpecification ruleMutableSpecification;
    @Autowired
    private BasketEventTeamMapper eventTeamMapper;
    @Autowired
    private BasketDivisionStageMapper stageMapper;
    @Autowired
    private BasketDivisionFormatSnapshotMapper snapshotMapper;
    @Autowired
    private IBasketV4DivisionClosingService divisionClosingService;
    @Autowired
    private IBasketV4SeasonService seasonService;
    @Autowired
    private DivisionSafeRecoverySupport safeRecoverySupport;
    @Autowired
    private V4PublicReadCachePublisher publicReadCachePublisher;
    @Autowired
    private BasketV4MatchMapper matchMapper;
    @Autowired
    private CaptainGrantEventSyncSupport captainGrantEventSyncSupport;

    @Override
    public List<BasketDivisionListVo> selectList(BasketDivisionQuery query) {
        List<BasketDivisionListVo> rows = divisionMapper.selectVoList(query);
        rows.forEach(this::enrichDivisionVo);
        return rows;
    }

    @Override
    public BasketDivisionListVo selectById(Long id) {
        BasketDivisionListVo vo = requireDivisionVo(id);
        enrichDivisionVo(vo);
        BasketDivisionFormatSnapshotVo snapshot = formatSnapshotService.getDivisionSnapshot(id);
        if (snapshot != null) {
            vo.setFormatSummary(snapshot.getFormatSummary());
        }
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(BasketDivisionCreateReq req) {
        validateCreateReq(req);
        BasketSeason season = requireSeason(req.getSeasonId());
        GameModeEnum gameMode = GameModeEnum.requireCode(req.getGameMode());
        RosterLimit rosterLimit = RosterLimit.resolve(gameMode);

        Long templateId = resolveTemplateId(req.getFormatTemplateId(), season.getSeriesId());

        BasketDivision entity = new BasketDivision();
        BeanUtils.copyBeanProp(entity, req);
        entity.setDivisionName(req.getDivisionName().trim());
        if (req.getDivisionNameEn() != null) {
            entity.setDivisionNameEn(req.getDivisionNameEn().trim());
        }
        entity.setRegistrationMode(resolveRegistrationMode(req.getRegistrationMode()));
        entity.setFormatType(StringUtils.isNotEmpty(req.getFormatType())
                ? req.getFormatType() : FormatTypeEnum.REGULAR_ROUND_ROBIN.getCode());
        entity.setGameMode(gameMode.getCode());
        entity.setLineupMinCount(rosterLimit.getMinCount());
        entity.setLineupMaxCount(rosterLimit.getMaxCount());
        entity.setFormatTemplateId(templateId);
        entity.setDivisionStatus(DivisionStatusEnum.DRAFT.getCode());
        entity.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        entity.setCreateTime(new Date());
        entity.setUpdateTime(new Date());
        entity.setIsDelete(NOT_DELETED);
        normalizeDivisionName(entity);
        validateDivisionTimes(entity);
        validateDivisionTimesWithinSeason(entity);
        assertUniqueDivisionCombo(req.getSeasonId(), entity.getPlatformCode(),
                entity.getGameMode(), entity.getRegistrationMode(), null);
        divisionMapper.insert(entity);

        formatSnapshotService.createDraftSnapshot(entity.getId(), templateId);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, BasketDivisionUpdateReq req) {
        BasketDivision division = requireDivision(id);
        Date previousMatchEnd = division.getMatchEndTime();
        boolean draft = DivisionStatusEnum.DRAFT.getCode().equals(division.getDivisionStatus());
        boolean registrationOpen = DivisionStatusEnum.REGISTRATION_OPEN.getCode().equals(division.getDivisionStatus());
        boolean registrationClosed = DivisionStatusEnum.REGISTRATION_CLOSED.getCode().equals(division.getDivisionStatus());
        boolean scheduled = DivisionStatusEnum.SCHEDULED.getCode().equals(division.getDivisionStatus());

        if (!draft && !registrationOpen && !registrationClosed && !scheduled) {
            throw new ServiceException("当前状态不允许编辑 Division");
        }

        if (draft) {
            applyDraftUpdate(division, req);
            normalizeDivisionName(division);
        } else if (scheduled) {
            applyScheduledDateUpdate(division, req);
            normalizeDivisionName(division);
        } else {
            applyLimitedUpdate(division, req);
        }

        validateLineupByGameMode(division);
        validateMaxTeamCount(division);
        validateDivisionTimes(division);
        validateDivisionTimesWithinSeason(division);
        assertUniqueDivisionCombo(division.getSeasonId(), division.getPlatformCode(),
                division.getGameMode(), division.getRegistrationMode(), id);
        division.setUpdateTime(new Date());
        if (divisionMapper.updateById(division) != 1) {
            throw new ServiceException("更新 Division 失败");
        }
        captainGrantEventSyncSupport.onDivisionMatchEndChanged(id, previousMatchEnd, division.getMatchEndTime());
        publicReadCachePublisher.divisionMutation(id, division.getSeasonId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void openRegistration(Long id) {
        BasketDivision division = requireDivision(id);
        if (DivisionStatusEnum.REGISTRATION_OPEN.getCode().equals(division.getDivisionStatus())) {
            return;
        }
        BasketSeason season = requireSeason(division.getSeasonId());
        if (!PublishStatusEnum.PUBLISHED.getCode().equals(season.getPublishStatus())) {
            throw new ServiceException("Season 必须先发布");
        }
        if (!isDivisionBasicComplete(division)) {
            throw new ServiceException("Division 基础信息不完整，无法开放报名");
        }
        validateDivisionTimes(division);
        if (division.getMaxTeamCount() == null || division.getMaxTeamCount() <= 0) {
            throw new ServiceException("队伍上限无效，无法开放报名");
        }
        validateLineupByGameMode(division);
        BasketDivisionFormatSnapshotVo snapshot = formatSnapshotService.getDivisionSnapshot(id);
        if (snapshot == null) {
            throw new ServiceException("Division 缺少规则快照，无法开放报名");
        }
        if (!SnapshotStatusEnum.LOCKED.getCode().equals(snapshot.getSnapshotStatus()) && !Boolean.TRUE.equals(snapshot.getLocked())) {
            throw new ServiceException("规则快照未锁定，请先锁定规则后再开放报名");
        }
        if (snapshot.getStages() == null || snapshot.getStages().isEmpty()) {
            throw new ServiceException("规则快照缺少有效阶段，无法开放报名");
        }
        BestOfCapabilityGate.assertCanOpenRegistration(stageMapper.selectByDivisionId(id));

        division.setDivisionStatus(DivisionStatusEnum.REGISTRATION_OPEN.getCode());
        division.setUpdateTime(new Date());
        if (divisionMapper.updateById(division) != 1) {
            throw new ServiceException("开放 Division 报名失败");
        }
        publicReadCachePublisher.divisionMutation(id, division.getSeasonId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeRegistration(Long id) {
        BasketDivision division = requireDivision(id);
        if (!DivisionStatusEnum.REGISTRATION_OPEN.getCode().equals(division.getDivisionStatus())) {
            throw new ServiceException("仅报名中状态可关闭报名");
        }
        division.setDivisionStatus(DivisionStatusEnum.REGISTRATION_CLOSED.getCode());
        division.setUpdateTime(new Date());
        if (divisionMapper.updateById(division) != 1) {
            throw new ServiceException("关闭 Division 报名失败");
        }
        publicReadCachePublisher.divisionMutation(id, division.getSeasonId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finish(Long id, String reason) {
        divisionClosingService.finishDivision(id, reason);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        BasketDivision division = requireDivision(id);
        safeRecoverySupport.validateDivisionCanDelete(division);
        safeRecoverySupport.softDeleteFormatConfig(division.getId());
        if (divisionMapper.softDelete(id) != 1) {
            throw new ServiceException("删除 Division 失败");
        }
        seasonService.refreshAggregatedStatus(division.getSeasonId());
        publicReadCachePublisher.divisionMutation(id, division.getSeasonId());
    }

    private void applyDraftUpdate(BasketDivision division, BasketDivisionUpdateReq req) {
        if (req.getPlatformCode() != null && !req.getPlatformCode().equals(division.getPlatformCode())) {
            ruleMutableSpecification.assertCanChangeKeyFields(division);
            validateWritablePlatformCode(req.getPlatformCode());
            division.setPlatformCode(req.getPlatformCode());
        }
        if (req.getDivisionName() != null) {
            division.setDivisionName(req.getDivisionName().trim());
        }
        if (req.getDivisionNameEn() != null) {
            division.setDivisionNameEn(req.getDivisionNameEn().trim());
        }
        if (req.getRegistrationMode() != null && !req.getRegistrationMode().equals(division.getRegistrationMode())) {
            ruleMutableSpecification.assertCanChangeKeyFields(division);
            validateRegistrationModeWritable(req.getRegistrationMode());
            division.setRegistrationMode(req.getRegistrationMode());
        }
        if (req.getFormatType() != null && !req.getFormatType().equals(division.getFormatType())) {
            ruleMutableSpecification.assertCanChangeKeyFields(division);
            division.setFormatType(req.getFormatType());
        }
        if (req.getGameMode() != null && !req.getGameMode().equals(division.getGameMode())) {
            ruleMutableSpecification.assertCanChangeGameMode(division);
            GameModeEnum gameMode = GameModeEnum.requireCode(req.getGameMode());
            RosterLimit limit = RosterLimit.resolve(gameMode);
            division.setGameMode(gameMode.getCode());
            division.setLineupMinCount(limit.getMinCount());
            division.setLineupMaxCount(limit.getMaxCount());
        }
        if (req.getFormatTemplateId() != null && !req.getFormatTemplateId().equals(division.getFormatTemplateId())) {
            BasketDivisionFormatSnapshot current = snapshotMapper.selectActiveByDivisionId(division.getId());
            Long aligned = ruleMutableSpecification.resolveFormatTemplateIdForUpdate(req.getFormatTemplateId(), current);
            if (current != null && current.getSourceTemplateId() != null) {
                division.setFormatTemplateId(aligned);
            } else {
                ruleMutableSpecification.assertCanChangeKeyFields(division);
                formatSnapshotService.rebuildDraftSnapshot(division.getId(), req.getFormatTemplateId());
                BasketDivision refreshed = requireDivision(division.getId());
                division.setFormatTemplateId(refreshed.getFormatTemplateId());
                division.setFormatSnapshotId(refreshed.getFormatSnapshotId());
            }
        }
        if (req.getMaxTeamCount() != null) {
            division.setMaxTeamCount(req.getMaxTeamCount());
        }
        if (req.getMaxPersonCount() != null) {
            division.setMaxPersonCount(req.getMaxPersonCount());
        }
        if (req.getRegistrationStartTime() != null) {
            division.setRegistrationStartTime(req.getRegistrationStartTime());
        }
        if (req.getRegistrationEndTime() != null) {
            division.setRegistrationEndTime(req.getRegistrationEndTime());
        }
        if (req.getMatchStartTime() != null) {
            division.setMatchStartTime(req.getMatchStartTime());
        }
        if (req.getMatchEndTime() != null) {
            division.setMatchEndTime(req.getMatchEndTime());
        }
        if (req.getSortOrder() != null) {
            division.setSortOrder(req.getSortOrder());
        }
    }

    /**
     * 已排赛但比赛尚未正式开始时，只允许调整赛区日期和展示名称。
     * 赛制仍走重新发布，不在这里换模板。
     */
    private void applyScheduledDateUpdate(BasketDivision division, BasketDivisionUpdateReq req) {
        if (matchMapper.countStartedByDivisionId(division.getId()) > 0) {
            throw new ServiceException("已有正式开赛、完赛或弃权记录，不能修改赛区日期");
        }
        rejectIfChanged(req.getPlatformCode(), division.getPlatformCode(), "已排赛后不允许修改平台");
        rejectIfChanged(req.getGameMode(), division.getGameMode(), "已排赛后不允许修改比赛模式");
        rejectIfChanged(req.getRegistrationMode(), division.getRegistrationMode(), "已排赛后不允许修改报名方式");
        rejectIfChanged(req.getFormatType(), division.getFormatType(), "已排赛后不允许在日期编辑中修改赛制类型");
        if (req.getFormatTemplateId() != null && !req.getFormatTemplateId().equals(division.getFormatTemplateId())) {
            throw new ServiceException("已排赛后不允许在日期编辑中更换赛制模板");
        }
        rejectIfChanged(req.getLineupMinCount(), division.getLineupMinCount(), "已排赛后不允许修改阵容人数");
        rejectIfChanged(req.getLineupMaxCount(), division.getLineupMaxCount(), "已排赛后不允许修改阵容人数");
        rejectIfChanged(req.getMaxTeamCount(), division.getMaxTeamCount(), "已排赛后不允许修改队伍上限");
        rejectIfChanged(req.getMaxPersonCount(), division.getMaxPersonCount(), "已排赛后不允许修改人数上限");
        if (req.getDivisionName() != null) {
            division.setDivisionName(req.getDivisionName().trim());
        }
        if (req.getDivisionNameEn() != null) {
            division.setDivisionNameEn(req.getDivisionNameEn().trim());
        }
        if (req.getRegistrationStartTime() != null) {
            division.setRegistrationStartTime(req.getRegistrationStartTime());
        }
        if (req.getRegistrationEndTime() != null) {
            division.setRegistrationEndTime(req.getRegistrationEndTime());
        }
        if (req.getMatchStartTime() != null) {
            division.setMatchStartTime(req.getMatchStartTime());
        }
        if (req.getMatchEndTime() != null) {
            division.setMatchEndTime(req.getMatchEndTime());
        }
        if (req.getSortOrder() != null) {
            division.setSortOrder(req.getSortOrder());
        }
    }

    private void rejectIfChanged(Object requested, Object current, String message) {
        if (requested != null && !requested.equals(current)) {
            throw new ServiceException(message);
        }
    }

    private void applyLimitedUpdate(BasketDivision division, BasketDivisionUpdateReq req) {
        if (req.getPlatformCode() != null && !req.getPlatformCode().equals(division.getPlatformCode())) {
            throw new ServiceException("报名开放后不允许修改 platformCode");
        }
        if (req.getGameMode() != null && !req.getGameMode().equals(division.getGameMode())) {
            throw new ServiceException("报名开放后不允许修改 gameMode");
        }
        if (req.getRegistrationMode() != null && !req.getRegistrationMode().equals(division.getRegistrationMode())) {
            throw new ServiceException("报名开放后不允许修改 registrationMode");
        }
        if (req.getFormatTemplateId() != null && !req.getFormatTemplateId().equals(division.getFormatTemplateId())) {
            BasketDivisionFormatSnapshot current = snapshotMapper.selectActiveByDivisionId(division.getId());
            if (current != null && current.getSourceTemplateId() != null) {
                division.setFormatTemplateId(current.getSourceTemplateId());
            } else {
                throw new ServiceException("已锁定规则后不允许更换模板");
            }
        }
        if (req.getLineupMinCount() != null && !req.getLineupMinCount().equals(division.getLineupMinCount())) {
            throw new ServiceException("报名开放后不允许修改 lineupMinCount");
        }
        if (req.getLineupMaxCount() != null && !req.getLineupMaxCount().equals(division.getLineupMaxCount())) {
            throw new ServiceException("报名开放后不允许修改 lineupMaxCount");
        }
        if (req.getDivisionName() != null) {
            division.setDivisionName(req.getDivisionName().trim());
        }
        if (req.getDivisionNameEn() != null) {
            division.setDivisionNameEn(req.getDivisionNameEn().trim());
        }
        if (req.getMaxTeamCount() != null) {
            division.setMaxTeamCount(req.getMaxTeamCount());
        }
        if (req.getMaxPersonCount() != null) {
            division.setMaxPersonCount(req.getMaxPersonCount());
        }
        if (req.getRegistrationStartTime() != null) {
            division.setRegistrationStartTime(req.getRegistrationStartTime());
        }
        if (req.getRegistrationEndTime() != null) {
            division.setRegistrationEndTime(req.getRegistrationEndTime());
        }
        if (req.getMatchStartTime() != null) {
            division.setMatchStartTime(req.getMatchStartTime());
        }
        if (req.getMatchEndTime() != null) {
            division.setMatchEndTime(req.getMatchEndTime());
        }
        if (req.getSortOrder() != null) {
            division.setSortOrder(req.getSortOrder());
        }
    }

    private void validateCreateReq(BasketDivisionCreateReq req) {
        if (req.getSeasonId() == null) {
            throw new ServiceException("seasonId 不能为空");
        }
        if (StringUtils.isEmpty(req.getPlatformCode())) {
            throw new ServiceException("platformCode 不能为空");
        }
        validateWritablePlatformCode(req.getPlatformCode());
        if (StringUtils.isEmpty(req.getGameMode())) {
            throw new ServiceException("gameMode 不能为空");
        }
        GameModeEnum.requireCode(req.getGameMode());
        if (StringUtils.isEmpty(req.getDivisionName())) {
            throw new ServiceException("divisionName 不能为空");
        }
        String trimmedName = req.getDivisionName().trim();
        if (trimmedName.isEmpty()) {
            throw new ServiceException("divisionName 不能为空");
        }
        validateDivisionName(trimmedName);
        if (StringUtils.isEmpty(req.getRegistrationMode())) {
            throw new ServiceException("registrationMode 不能为空");
        }
        validateRegistrationModeWritable(req.getRegistrationMode());
        if (req.getMaxTeamCount() == null || req.getMaxTeamCount() <= 0) {
            throw new ServiceException("maxTeamCount 必须为正整数");
        }
        if (req.getMaxTeamCount() > MAX_TEAM_COUNT_LIMIT) {
            throw new ServiceException("maxTeamCount 不能超过 " + MAX_TEAM_COUNT_LIMIT);
        }
    }

    private void validateWritablePlatformCode(String platformCode) {
        if (!PlatformCodeEnum.isWritableCode(platformCode)) {
            throw new ServiceException("platformCode 仅允许 PC 或 PS_XBOX，历史值 PS/XBOX 不可用于新建或修改");
        }
    }

    private void validateRegistrationModeWritable(String mode) {
        if (!RegistrationModeEnum.TEAM.getCode().equals(mode)
                && !RegistrationModeEnum.PERSONAL.getCode().equals(mode)) {
            throw new ServiceException("registrationMode 仅支持 TEAM 或 PERSONAL");
        }
    }

    private String resolveRegistrationMode(String reqMode) {
        if (StringUtils.isNotEmpty(reqMode)) {
            return reqMode;
        }
        return RegistrationModeEnum.TEAM.getCode();
    }

    private Long resolveTemplateId(Long reqTemplateId, Long seriesId) {
        if (reqTemplateId != null) {
            BasketFormatTemplate template = formatTemplateService.requireEnabledTemplate(reqTemplateId);
            formatTemplateService.assertTemplateBelongsToSeries(template, seriesId);
            return reqTemplateId;
        }
        return formatTemplateService.resolveDefaultTemplate(seriesId).getId();
    }

    private void validateLineupByGameMode(BasketDivision division) {
        RosterLimit limit = RosterLimit.resolve(division.getGameMode());
        try {
            limit.validateCounts(division.getLineupMinCount(), division.getLineupMaxCount());
        } catch (IllegalArgumentException ex) {
            throw new ServiceException(ex.getMessage());
        }
    }

    private void validateDivisionTimes(BasketDivision division) {
        if (division.getRegistrationStartTime() != null && division.getRegistrationEndTime() != null) {
            if (!division.getRegistrationStartTime().before(division.getRegistrationEndTime())) {
                throw new ServiceException("报名开始时间必须早于报名结束时间");
            }
        }
        if (division.getRegistrationEndTime() != null && division.getMatchStartTime() != null) {
            if (division.getRegistrationEndTime().after(division.getMatchStartTime())) {
                throw new ServiceException("报名结束时间不得晚于比赛开始时间");
            }
        }
        if (division.getMatchStartTime() != null && division.getMatchEndTime() != null) {
            if (!division.getMatchStartTime().before(division.getMatchEndTime())) {
                throw new ServiceException("比赛开始时间必须早于比赛结束时间");
            }
        }
    }

    private void validateDivisionTimesWithinSeason(BasketDivision division) {
        if (division.getSeasonId() == null) {
            return;
        }
        BasketSeason season = seasonMapper.selectById(division.getSeasonId());
        if (season == null || season.getSeasonStartTime() == null || season.getSeasonEndTime() == null) {
            return;
        }
        Date seasonStart = season.getSeasonStartTime();
        Date seasonEnd = season.getSeasonEndTime();
        if (division.getRegistrationStartTime() != null && division.getRegistrationStartTime().before(seasonStart)) {
            throw new ServiceException("报名开始时间不能早于赛季展示周期开始时间");
        }
        if (division.getMatchEndTime() != null && division.getMatchEndTime().after(seasonEnd)) {
            throw new ServiceException("比赛结束时间不能晚于赛季展示周期结束时间");
        }
    }

    private void validateDivisionName(String name) {
        if (name.matches("^\\d+$")) {
            throw new ServiceException("赛区名称不能为纯数字");
        }
    }

    private void normalizeDivisionName(BasketDivision division) {
        if (division.getDivisionName() != null) {
            division.setDivisionName(division.getDivisionName().trim());
            validateDivisionName(division.getDivisionName());
        }
    }

    private void validateMaxTeamCount(BasketDivision division) {
        if (division.getMaxTeamCount() == null || division.getMaxTeamCount() <= 0) {
            throw new ServiceException("队伍上限必须为正整数");
        }
        if (division.getMaxTeamCount() > MAX_TEAM_COUNT_LIMIT) {
            throw new ServiceException("队伍上限不能超过 " + MAX_TEAM_COUNT_LIMIT);
        }
        if (division.getId() != null) {
            int existingTeams = eventTeamMapper.countActiveByDivisionId(division.getId());
            if (division.getMaxTeamCount() < existingTeams) {
                throw new ServiceException("队伍上限不能小于已有参赛球队数（" + existingTeams + "）");
            }
        }
    }

    private void assertUniqueDivisionCombo(Long seasonId, String platformCode, String gameMode,
                                           String registrationMode, Long excludeId) {
        BasketDivision existing = divisionMapper.selectBySeasonCombo(
                seasonId, platformCode, gameMode, registrationMode, excludeId);
        if (existing != null) {
            throw new ServiceException(String.format("当前赛季已存在 %s · %s · %s 赛区。",
                    V4FormatDisplaySupport.platformLabel(platformCode),
                    V4FormatDisplaySupport.gameModeLabel(gameMode),
                    V4FormatDisplaySupport.registrationModeLabel(registrationMode)));
        }
    }

    private void enrichDivisionVo(BasketDivisionListVo vo) {
        if (vo == null) {
            return;
        }
        vo.setPlatformName(V4FormatDisplaySupport.platformLabel(vo.getPlatformCode()));
        vo.setGameModeName(V4FormatDisplaySupport.gameModeLabel(vo.getGameMode()));
        vo.setRegistrationModeName(V4FormatDisplaySupport.registrationModeLabel(vo.getRegistrationMode()));
        if (vo.getFormatSnapshotStatus() != null) {
            vo.setFormatSnapshotStatus(vo.getFormatSnapshotStatus());
        }
    }

    private BasketSeason requireSeason(Long seasonId) {
        BasketSeason season = seasonMapper.selectById(seasonId);
        if (season == null) {
            throw new ServiceException("Season 不存在");
        }
        return season;
    }

    private BasketDivision requireDivision(Long id) {
        if (id == null) {
            throw new ServiceException("Division ID 不能为空");
        }
        BasketDivision division = divisionMapper.selectById(id);
        if (division == null) {
            throw new ServiceException("Division 不存在");
        }
        return division;
    }

    private BasketDivisionListVo requireDivisionVo(Long id) {
        BasketDivisionQuery query = new BasketDivisionQuery();
        query.setId(id);
        List<BasketDivisionListVo> rows = divisionMapper.selectVoList(query);
        if (rows.isEmpty()) {
            throw new ServiceException("Division 不存在");
        }
        return rows.get(0);
    }

    private boolean isDivisionBasicComplete(BasketDivision division) {
        return division != null
                && StringUtils.isNotEmpty(division.getPlatformCode())
                && StringUtils.isNotEmpty(division.getGameMode())
                && StringUtils.isNotEmpty(division.getDivisionName())
                && StringUtils.isNotEmpty(division.getRegistrationMode())
                && division.getFormatTemplateId() != null
                && division.getFormatSnapshotId() != null
                && division.getMaxTeamCount() != null && division.getMaxTeamCount() > 0
                && division.getLineupMinCount() != null
                && division.getLineupMaxCount() != null
                && division.getRegistrationStartTime() != null
                && division.getRegistrationEndTime() != null
                && division.getMatchStartTime() != null
                && division.getMatchEndTime() != null;
    }
}
