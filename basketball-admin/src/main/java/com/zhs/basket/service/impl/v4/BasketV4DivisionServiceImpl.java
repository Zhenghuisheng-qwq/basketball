package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketSeason;
import com.zhs.basket.dto.v4.BasketDivisionCreateReq;
import com.zhs.basket.dto.v4.BasketDivisionUpdateReq;
import com.zhs.basket.enums.v4.DivisionStatusEnum;
import com.zhs.basket.enums.v4.FormatTypeEnum;
import com.zhs.basket.enums.v4.PlatformCodeEnum;
import com.zhs.basket.enums.v4.PublishStatusEnum;
import com.zhs.basket.enums.v4.RegistrationModeEnum;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketSeasonMapper;
import com.zhs.basket.query.v4.BasketDivisionQuery;
import com.zhs.basket.service.v4.IBasketV4DivisionService;
import com.zhs.basket.vo.v4.BasketDivisionListVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.StringUtils;
import com.zhs.common.utils.bean.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class BasketV4DivisionServiceImpl implements IBasketV4DivisionService {

    private static final Integer NOT_DELETED = 0;
    private static final int DEFAULT_LINEUP_MIN = 5;
    private static final int DEFAULT_LINEUP_MAX = 8;

    @Autowired
    private BasketDivisionMapper divisionMapper;
    @Autowired
    private BasketSeasonMapper seasonMapper;

    @Override
    public List<BasketDivisionListVo> selectList(BasketDivisionQuery query) {
        return divisionMapper.selectVoList(query);
    }

    @Override
    public BasketDivisionListVo selectById(Long id) {
        requireDivision(id);
        BasketDivisionQuery query = new BasketDivisionQuery();
        query.setId(id);
        List<BasketDivisionListVo> rows = divisionMapper.selectVoList(query);
        if (rows.isEmpty()) {
            throw new ServiceException("Division 不存在");
        }
        return rows.get(0);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(BasketDivisionCreateReq req) {
        validateCreateReq(req);
        BasketSeason season = requireSeason(req.getSeasonId());

        BasketDivision entity = new BasketDivision();
        BeanUtils.copyBeanProp(entity, req);
        entity.setRegistrationMode(RegistrationModeEnum.TEAM.getCode());
        entity.setFormatType(FormatTypeEnum.REGULAR_ROUND_ROBIN.getCode());
        entity.setLineupMinCount(req.getLineupMinCount() != null ? req.getLineupMinCount() : DEFAULT_LINEUP_MIN);
        entity.setLineupMaxCount(req.getLineupMaxCount() != null ? req.getLineupMaxCount() : DEFAULT_LINEUP_MAX);
        inheritSeasonTimesIfBlank(entity, season);
        entity.setDivisionStatus(DivisionStatusEnum.DRAFT.getCode());
        entity.setSortOrder(req.getSortOrder() != null ? req.getSortOrder() : 0);
        entity.setCreateTime(new Date());
        entity.setUpdateTime(new Date());
        entity.setIsDelete(NOT_DELETED);
        validateLineup(entity.getLineupMinCount(), entity.getLineupMaxCount());
        validateDivisionTimes(entity);
        assertUniquePlatform(req.getSeasonId(), req.getPlatformCode(), null);
        divisionMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, BasketDivisionUpdateReq req) {
        BasketDivision division = requireDivision(id);
        boolean draft = DivisionStatusEnum.DRAFT.getCode().equals(division.getDivisionStatus());
        boolean registrationOpen = DivisionStatusEnum.REGISTRATION_OPEN.getCode().equals(division.getDivisionStatus());

        if (!draft && !registrationOpen && !DivisionStatusEnum.REGISTRATION_CLOSED.getCode().equals(division.getDivisionStatus())) {
            throw new ServiceException("当前状态不允许编辑 Division");
        }

        if (draft) {
            applyDraftUpdate(division, req);
        } else {
            applyLimitedUpdate(division, req);
        }

        validateLineup(division.getLineupMinCount(), division.getLineupMaxCount());
        validateDivisionTimes(division);
        assertUniquePlatform(division.getSeasonId(), division.getPlatformCode(), id);
        division.setUpdateTime(new Date());
        if (divisionMapper.updateById(division) != 1) {
            throw new ServiceException("更新 Division 失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void openRegistration(Long id) {
        BasketDivision division = requireDivision(id);
        BasketSeason season = requireSeason(division.getSeasonId());
        if (!PublishStatusEnum.PUBLISHED.getCode().equals(season.getPublishStatus())) {
            throw new ServiceException("Season 必须先发布");
        }
        if (!isDivisionBasicComplete(division)) {
            throw new ServiceException("Division 基础信息不完整，无法开放报名");
        }
        division.setDivisionStatus(DivisionStatusEnum.REGISTRATION_OPEN.getCode());
        division.setUpdateTime(new Date());
        if (divisionMapper.updateById(division) != 1) {
            throw new ServiceException("开放 Division 报名失败");
        }
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
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        BasketDivision division = requireDivision(id);
        if (!DivisionStatusEnum.DRAFT.getCode().equals(division.getDivisionStatus())) {
            throw new ServiceException("仅草稿状态 Division 可删除");
        }
        if (divisionMapper.softDelete(id) != 1) {
            throw new ServiceException("删除 Division 失败");
        }
    }

    private void applyDraftUpdate(BasketDivision division, BasketDivisionUpdateReq req) {
        if (req.getPlatformCode() != null) {
            validatePlatformCode(req.getPlatformCode());
            division.setPlatformCode(req.getPlatformCode());
        }
        if (req.getDivisionName() != null) {
            division.setDivisionName(req.getDivisionName());
        }
        if (req.getRegistrationMode() != null) {
            validateRegistrationMode(req.getRegistrationMode());
            division.setRegistrationMode(req.getRegistrationMode());
        }
        if (req.getFormatType() != null) {
            validateFormatType(req.getFormatType());
            division.setFormatType(req.getFormatType());
        }
        if (req.getMaxTeamCount() != null) {
            division.setMaxTeamCount(req.getMaxTeamCount());
        }
        if (req.getMaxPersonCount() != null) {
            division.setMaxPersonCount(req.getMaxPersonCount());
        }
        if (req.getLineupMinCount() != null) {
            division.setLineupMinCount(req.getLineupMinCount());
        }
        if (req.getLineupMaxCount() != null) {
            division.setLineupMaxCount(req.getLineupMaxCount());
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

    private void applyLimitedUpdate(BasketDivision division, BasketDivisionUpdateReq req) {
        if (req.getPlatformCode() != null && !req.getPlatformCode().equals(division.getPlatformCode())) {
            throw new ServiceException("报名开放后不允许修改 platformCode");
        }
        if (req.getRegistrationMode() != null && !req.getRegistrationMode().equals(division.getRegistrationMode())) {
            throw new ServiceException("报名开放后不允许修改 registrationMode");
        }
        if (req.getFormatType() != null && !req.getFormatType().equals(division.getFormatType())) {
            throw new ServiceException("报名开放后不允许修改 formatType");
        }
        if (req.getLineupMinCount() != null && !req.getLineupMinCount().equals(division.getLineupMinCount())) {
            throw new ServiceException("报名开放后不允许修改 lineupMinCount");
        }
        if (req.getLineupMaxCount() != null && !req.getLineupMaxCount().equals(division.getLineupMaxCount())) {
            throw new ServiceException("报名开放后不允许修改 lineupMaxCount");
        }
        if (req.getDivisionName() != null) {
            division.setDivisionName(req.getDivisionName());
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
        validatePlatformCode(req.getPlatformCode());
        if (StringUtils.isEmpty(req.getDivisionName())) {
            throw new ServiceException("divisionName 不能为空");
        }
        validateRegistrationMode(req.getRegistrationMode());
        validateFormatType(req.getFormatType());
        if (req.getMaxTeamCount() == null || req.getMaxTeamCount() <= 0) {
            throw new ServiceException("maxTeamCount 必须大于 0");
        }
    }

    private void validatePlatformCode(String platformCode) {
        Set<String> allowed = new HashSet<>(Arrays.asList(
                PlatformCodeEnum.PC.getCode(),
                PlatformCodeEnum.PS.getCode(),
                PlatformCodeEnum.XBOX.getCode()));
        if (!allowed.contains(platformCode)) {
            throw new ServiceException("platformCode 只能为 PC / PS / XBOX");
        }
    }

    private void validateRegistrationMode(String mode) {
        if (StringUtils.isEmpty(mode)) {
            return;
        }
        if (RegistrationModeEnum.PERSONAL.getCode().equals(mode)) {
            throw new ServiceException("PERSONAL 个人报名暂未开放");
        }
        if (!RegistrationModeEnum.TEAM.getCode().equals(mode)) {
            throw new ServiceException("registrationMode 当前仅支持 TEAM");
        }
    }

    private void validateFormatType(String formatType) {
        if (StringUtils.isEmpty(formatType)) {
            return;
        }
        if (!FormatTypeEnum.REGULAR_ROUND_ROBIN.getCode().equals(formatType)) {
            throw new ServiceException("formatType 当前仅支持 REGULAR_ROUND_ROBIN");
        }
    }

    private void validateLineup(Integer min, Integer max) {
        if (min == null || min < DEFAULT_LINEUP_MIN) {
            throw new ServiceException("lineupMinCount 不能小于 5");
        }
        if (max == null || max > DEFAULT_LINEUP_MAX) {
            throw new ServiceException("lineupMaxCount 不能大于 8");
        }
        if (min > max) {
            throw new ServiceException("lineupMinCount 不能大于 lineupMaxCount");
        }
    }

    private void validateDivisionTimes(BasketDivision division) {
        if (division.getRegistrationStartTime() != null && division.getRegistrationEndTime() != null
                && division.getRegistrationStartTime().after(division.getRegistrationEndTime())) {
            throw new ServiceException("报名开始时间不得晚于报名结束时间");
        }
        if (division.getMatchStartTime() != null && division.getMatchEndTime() != null
                && division.getMatchStartTime().after(division.getMatchEndTime())) {
            throw new ServiceException("比赛开始时间不得晚于比赛结束时间");
        }
    }

    private void inheritSeasonTimesIfBlank(BasketDivision division, BasketSeason season) {
        if (division.getRegistrationStartTime() == null) {
            division.setRegistrationStartTime(season.getRegistrationStartTime());
        }
        if (division.getRegistrationEndTime() == null) {
            division.setRegistrationEndTime(season.getRegistrationEndTime());
        }
        if (division.getMatchStartTime() == null) {
            division.setMatchStartTime(season.getMatchStartTime());
        }
        if (division.getMatchEndTime() == null) {
            division.setMatchEndTime(season.getMatchEndTime());
        }
    }

    private void assertUniquePlatform(Long seasonId, String platformCode, Long excludeId) {
        BasketDivision existing = divisionMapper.selectBySeasonPlatform(seasonId, platformCode, excludeId);
        if (existing != null) {
            throw new ServiceException("同一 Season 下不能重复创建同平台 Division");
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

    private boolean isDivisionBasicComplete(BasketDivision division) {
        return division != null
                && StringUtils.isNotEmpty(division.getPlatformCode())
                && StringUtils.isNotEmpty(division.getDivisionName())
                && StringUtils.isNotEmpty(division.getRegistrationMode())
                && StringUtils.isNotEmpty(division.getFormatType())
                && division.getMaxTeamCount() != null && division.getMaxTeamCount() > 0
                && division.getLineupMinCount() != null
                && division.getLineupMaxCount() != null
                && division.getRegistrationStartTime() != null
                && division.getRegistrationEndTime() != null
                && division.getMatchStartTime() != null
                && division.getMatchEndTime() != null;
    }
}
