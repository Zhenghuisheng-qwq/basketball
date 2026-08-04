package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketSeason;
import com.zhs.basket.domain.v4.BasketSeries;
import com.zhs.basket.dto.v4.BasketSeasonCreateReq;
import com.zhs.basket.dto.v4.BasketSeasonUpdateReq;
import com.zhs.basket.enums.v4.FormatTypeEnum;
import com.zhs.basket.enums.v4.PublishStatusEnum;
import com.zhs.basket.enums.v4.RegistrationModeEnum;
import com.zhs.basket.enums.v4.SeasonStatusEnum;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketSeasonMapper;
import com.zhs.basket.mapper.v4.BasketSeriesMapper;
import com.zhs.basket.query.v4.BasketDivisionQuery;
import com.zhs.basket.query.v4.BasketSeasonQuery;
import com.zhs.basket.service.v4.IBasketV4SeasonService;
import com.zhs.basket.vo.v4.BasketSeasonDetailVo;
import com.zhs.basket.vo.v4.BasketSeasonListVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.StringUtils;
import com.zhs.common.utils.bean.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class BasketV4SeasonServiceImpl implements IBasketV4SeasonService {

    private static final Integer NOT_DELETED = 0;

    @Autowired
    private BasketSeasonMapper seasonMapper;
    @Autowired
    private BasketSeriesMapper seriesMapper;
    @Autowired
    private BasketDivisionMapper divisionMapper;

    @Override
    public List<BasketSeasonListVo> selectList(BasketSeasonQuery query) {
        return seasonMapper.selectVoList(query);
    }

    @Override
    public BasketSeasonDetailVo selectById(Long id) {
        requireSeason(id);
        BasketSeasonQuery query = new BasketSeasonQuery();
        query.setId(id);
        List<BasketSeasonListVo> rows = seasonMapper.selectVoList(query);
        if (rows.isEmpty()) {
            throw new ServiceException("Season 不存在");
        }
        BasketSeasonDetailVo vo = new BasketSeasonDetailVo();
        BeanUtils.copyBeanProp(vo, rows.get(0));
        BasketDivisionQuery divisionQuery = new BasketDivisionQuery();
        divisionQuery.setSeasonId(id);
        vo.setDivisions(divisionMapper.selectVoList(divisionQuery));
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(BasketSeasonCreateReq req) {
        validateCreateReq(req);
        requireEnabledSeries(req.getSeriesId());
        assertUniqueSeason(req.getSeriesId(), req.getSeasonYear(), req.getSeasonName(), null);

        BasketSeason entity = new BasketSeason();
        BeanUtils.copyBeanProp(entity, req);
        if (StringUtils.isEmpty(entity.getRegistrationMode())) {
            entity.setRegistrationMode(RegistrationModeEnum.TEAM.getCode());
        }
        if (StringUtils.isEmpty(entity.getFormatType())) {
            entity.setFormatType(FormatTypeEnum.REGULAR_ROUND_ROBIN.getCode());
        }
        entity.setPublishStatus(PublishStatusEnum.DRAFT.getCode());
        entity.setSeasonStatus(SeasonStatusEnum.NOT_STARTED.getCode());
        entity.setCreateTime(new Date());
        entity.setUpdateTime(new Date());
        entity.setIsDelete(NOT_DELETED);
        seasonMapper.insert(entity);
        return entity.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, BasketSeasonUpdateReq req) {
        BasketSeason season = requireSeason(id);
        int divisionCount = seasonMapper.countDivisionBySeasonId(id);
        boolean published = PublishStatusEnum.PUBLISHED.getCode().equals(season.getPublishStatus());

        if (divisionCount > 0) {
            rejectKeyFieldChange(season.getSeriesId(), req.getSeriesId(), "已有 Division，不允许修改 Series");
            rejectKeyFieldChange(season.getSeasonYear(), req.getSeasonYear(), "已有 Division，不允许修改赛季年份");
            rejectKeyFieldChange(season.getSeasonName(), req.getSeasonName(), "已有 Division，不允许修改赛季名称");
            rejectKeyFieldChange(season.getRegistrationMode(), req.getRegistrationMode(), "已有 Division，不允许修改报名方式");
            rejectKeyFieldChange(season.getFormatType(), req.getFormatType(), "已有 Division，不允许修改赛制");
        }

        if (published) {
            if (req.getDescription() != null) {
                season.setDescription(req.getDescription());
            }
            if (req.getDisplayTitle() != null) {
                season.setDisplayTitle(req.getDisplayTitle());
            }
            if (req.getCoverFileId() != null) {
                season.setCoverFileId(req.getCoverFileId());
            }
            if (req.getBannerFileId() != null) {
                season.setBannerFileId(req.getBannerFileId());
            }
        } else {
            if (req.getSeriesId() != null) {
                requireEnabledSeries(req.getSeriesId());
                season.setSeriesId(req.getSeriesId());
            }
            if (req.getSeasonYear() != null) {
                season.setSeasonYear(req.getSeasonYear());
            }
            if (req.getSeasonName() != null) {
                season.setSeasonName(req.getSeasonName());
            }
            if (req.getDisplayTitle() != null) {
                season.setDisplayTitle(req.getDisplayTitle());
            }
            if (req.getDescription() != null) {
                season.setDescription(req.getDescription());
            }
            if (req.getCoverFileId() != null) {
                season.setCoverFileId(req.getCoverFileId());
            }
            if (req.getBannerFileId() != null) {
                season.setBannerFileId(req.getBannerFileId());
            }
            if (req.getRegistrationMode() != null) {
                validateRegistrationMode(req.getRegistrationMode());
                season.setRegistrationMode(req.getRegistrationMode());
            }
            if (req.getFormatType() != null) {
                validateFormatType(req.getFormatType());
                season.setFormatType(req.getFormatType());
            }
            if (req.getRegistrationStartTime() != null) {
                season.setRegistrationStartTime(req.getRegistrationStartTime());
            }
            if (req.getRegistrationEndTime() != null) {
                season.setRegistrationEndTime(req.getRegistrationEndTime());
            }
            if (req.getMatchStartTime() != null) {
                season.setMatchStartTime(req.getMatchStartTime());
            }
            if (req.getMatchEndTime() != null) {
                season.setMatchEndTime(req.getMatchEndTime());
            }
        }

        validateSeasonTimes(season.getRegistrationStartTime(), season.getRegistrationEndTime(),
                season.getMatchStartTime(), season.getMatchEndTime());
        assertUniqueSeason(season.getSeriesId(), season.getSeasonYear(), season.getSeasonName(), id);
        season.setUpdateTime(new Date());
        if (seasonMapper.updateById(season) != 1) {
            throw new ServiceException("更新 Season 失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        BasketSeason season = requireSeason(id);
        if (!PublishStatusEnum.DRAFT.getCode().equals(season.getPublishStatus())) {
            throw new ServiceException("仅草稿状态 Season 可发布");
        }
        List<BasketDivision> divisions = divisionMapper.selectBySeasonId(id);
        if (divisions.isEmpty()) {
            throw new ServiceException("发布 Season 前至少创建一个 Division");
        }
        boolean hasComplete = divisions.stream().anyMatch(this::isDivisionBasicComplete);
        if (!hasComplete) {
            throw new ServiceException("至少一个 Division 基础信息完整后才能发布 Season");
        }

        season.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
        season.setSeasonStatus(resolveSeasonStatusOnPublish(season));
        season.setUpdateTime(new Date());
        if (seasonMapper.updateById(season) != 1) {
            throw new ServiceException("发布 Season 失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long id) {
        BasketSeason season = requireSeason(id);
        season.setPublishStatus(PublishStatusEnum.OFFLINE.getCode());
        season.setUpdateTime(new Date());
        if (seasonMapper.updateById(season) != 1) {
            throw new ServiceException("下线 Season 失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        BasketSeason season = requireSeason(id);
        if (!PublishStatusEnum.DRAFT.getCode().equals(season.getPublishStatus())) {
            throw new ServiceException("仅草稿状态 Season 可删除");
        }
        if (seasonMapper.countDivisionBySeasonId(id) > 0) {
            throw new ServiceException("存在 Division 时不允许删除 Season，请先处理 Division");
        }
        if (seasonMapper.softDelete(id) != 1) {
            throw new ServiceException("删除 Season 失败");
        }
    }

    private void validateCreateReq(BasketSeasonCreateReq req) {
        if (req.getSeriesId() == null) {
            throw new ServiceException("seriesId 不能为空");
        }
        if (req.getSeasonYear() == null) {
            throw new ServiceException("seasonYear 不能为空");
        }
        if (StringUtils.isEmpty(req.getSeasonName())) {
            throw new ServiceException("seasonName 不能为空");
        }
        if (StringUtils.isEmpty(req.getDisplayTitle())) {
            throw new ServiceException("displayTitle 不能为空");
        }
        validateRegistrationMode(req.getRegistrationMode());
        validateFormatType(req.getFormatType());
        validateSeasonTimes(req.getRegistrationStartTime(), req.getRegistrationEndTime(),
                req.getMatchStartTime(), req.getMatchEndTime());
    }

    private void validateRegistrationMode(String mode) {
        if (StringUtils.isEmpty(mode)) {
            return;
        }
        if (!RegistrationModeEnum.TEAM.getCode().equals(mode)
                && !RegistrationModeEnum.PERSONAL.getCode().equals(mode)) {
            throw new ServiceException("registrationMode 仅支持 TEAM 或 PERSONAL");
        }
    }

    /**
     * Season.format_type 为过渡字段，仅作创建 Division 时的默认参考，不是最终执行规则。
     */
    private void validateFormatType(String formatType) {
        if (StringUtils.isEmpty(formatType)) {
            return;
        }
        if (!FormatTypeEnum.REGULAR_ROUND_ROBIN.getCode().equals(formatType)) {
            throw new ServiceException("formatType 当前仅支持 REGULAR_ROUND_ROBIN");
        }
    }

    private void validateSeasonTimes(Date regStart, Date regEnd, Date matchStart, Date matchEnd) {
        if (regStart != null && regEnd != null && regStart.after(regEnd)) {
            throw new ServiceException("报名开始时间不得晚于报名结束时间");
        }
        if (matchStart != null && matchEnd != null && matchStart.after(matchEnd)) {
            throw new ServiceException("比赛开始时间不得晚于比赛结束时间");
        }
    }

    private BasketSeries requireEnabledSeries(Long seriesId) {
        BasketSeries series = seriesMapper.selectById(seriesId);
        if (series == null) {
            throw new ServiceException("Series 不存在");
        }
        if (series.getEnabled() == null || series.getEnabled() != 1) {
            throw new ServiceException("Series 未启用");
        }
        return series;
    }

    private BasketSeason requireSeason(Long id) {
        if (id == null) {
            throw new ServiceException("Season ID 不能为空");
        }
        BasketSeason season = seasonMapper.selectById(id);
        if (season == null) {
            throw new ServiceException("Season 不存在");
        }
        return season;
    }

    private void assertUniqueSeason(Long seriesId, Integer seasonYear, String seasonName, Long excludeId) {
        if (seriesId == null || seasonYear == null || StringUtils.isEmpty(seasonName)) {
            return;
        }
        BasketSeason existing = seasonMapper.selectBySeriesYearName(seriesId, seasonYear, seasonName, excludeId);
        if (existing != null) {
            throw new ServiceException("同一 Series + 年份 + 赛季名称已存在");
        }
    }

    private String resolveSeasonStatusOnPublish(BasketSeason season) {
        Date now = new Date();
        Date regStart = season.getRegistrationStartTime();
        Date regEnd = season.getRegistrationEndTime();
        if (regStart != null && regEnd != null && !now.before(regStart) && !now.after(regEnd)) {
            return SeasonStatusEnum.REGISTRATION_OPEN.getCode();
        }
        if (regEnd != null && now.after(regEnd)) {
            return SeasonStatusEnum.REGISTRATION_CLOSED.getCode();
        }
        return SeasonStatusEnum.NOT_STARTED.getCode();
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

    private void rejectKeyFieldChange(Object oldVal, Object newVal, String message) {
        if (newVal != null && !newVal.equals(oldVal)) {
            throw new ServiceException(message);
        }
    }
}
