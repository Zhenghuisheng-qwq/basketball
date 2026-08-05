package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketSeason;
import com.zhs.basket.domain.v4.BasketSeries;
import com.zhs.basket.dto.v4.BasketSeasonCreateReq;
import com.zhs.basket.dto.v4.BasketSeasonUpdateReq;
import com.zhs.basket.enums.v4.DivisionStatusEnum;
import com.zhs.basket.enums.v4.PublishStatusEnum;
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
        boolean archived = SeasonStatusEnum.ARCHIVED.getCode().equals(season.getSeasonStatus());

        if (archived) {
            throw new ServiceException("已归档赛季不可编辑");
        }

        if (divisionCount > 0) {
            rejectKeyFieldChange(season.getSeriesId(), req.getSeriesId(), "已有平台赛区，不允许修改赛事系列");
            rejectKeyFieldChange(season.getSeasonYear(), req.getSeasonYear(), "已有平台赛区，不允许修改赛季年份");
            rejectKeyFieldChange(season.getSeasonName(), req.getSeasonName(), "已有平台赛区，不允许修改赛季名称");
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
            if (req.getSeasonStartTime() != null) {
                season.setSeasonStartTime(req.getSeasonStartTime());
            }
            if (req.getSeasonEndTime() != null) {
                season.setSeasonEndTime(req.getSeasonEndTime());
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
            if (req.getSeasonStartTime() != null) {
                season.setSeasonStartTime(req.getSeasonStartTime());
            }
            if (req.getSeasonEndTime() != null) {
                season.setSeasonEndTime(req.getSeasonEndTime());
            }
        }

        validateSeasonPeriod(season.getSeasonStartTime(), season.getSeasonEndTime());
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
        String publishStatus = season.getPublishStatus();
        if (PublishStatusEnum.PUBLISHED.getCode().equals(publishStatus)) {
            return;
        }
        if (!PublishStatusEnum.DRAFT.getCode().equals(publishStatus)
                && !PublishStatusEnum.OFFLINE.getCode().equals(publishStatus)) {
            throw new ServiceException("仅草稿或已下线 Season 可发布");
        }
        validatePublishable(season);

        season.setPublishStatus(PublishStatusEnum.PUBLISHED.getCode());
        season.setSeasonStatus(resolveSeasonStatusOnPublish(id));
        season.setUpdateTime(new Date());
        if (seasonMapper.updateById(season) != 1) {
            throw new ServiceException("发布 Season 失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long id) {
        BasketSeason season = requireSeason(id);
        if (!PublishStatusEnum.PUBLISHED.getCode().equals(season.getPublishStatus())) {
            throw new ServiceException("仅已发布 Season 可下线");
        }
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
            throw new ServiceException("存在平台赛区时不允许删除 Season，请先处理赛区");
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
        if (req.getSeasonYear() < 2020 || req.getSeasonYear() > 2099) {
            throw new ServiceException("seasonYear 需在 2020-2099 范围内");
        }
        if (StringUtils.isEmpty(req.getSeasonName())) {
            throw new ServiceException("seasonName 不能为空");
        }
        if (req.getSeasonName().length() > 128) {
            throw new ServiceException("seasonName 长度不能超过 128");
        }
        if (StringUtils.isEmpty(req.getDisplayTitle())) {
            throw new ServiceException("displayTitle 不能为空");
        }
        if (req.getDisplayTitle().length() > 128) {
            throw new ServiceException("displayTitle 长度不能超过 128");
        }
        if (req.getDescription() != null && req.getDescription().length() > 2000) {
            throw new ServiceException("description 长度不能超过 2000");
        }
        validateSeasonPeriod(req.getSeasonStartTime(), req.getSeasonEndTime());
    }

    private void validatePublishable(BasketSeason season) {
        if (season.getSeriesId() == null || season.getSeasonYear() == null
                || StringUtils.isEmpty(season.getSeasonName())
                || StringUtils.isEmpty(season.getDisplayTitle())) {
            throw new ServiceException("发布前请完善赛季基础资料");
        }
        validateSeasonPeriod(season.getSeasonStartTime(), season.getSeasonEndTime());
    }

    private void validateSeasonPeriod(Date start, Date end) {
        if (start != null && end != null && start.after(end)) {
            throw new ServiceException("赛季结束时间必须晚于赛季开始时间");
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

    /**
     * 赛季业务状态按下属 Division 聚合；无 Division 或未开放报名时保持未开始。
     */
    private String resolveSeasonStatusOnPublish(Long seasonId) {
        List<BasketDivision> divisions = divisionMapper.selectBySeasonId(seasonId);
        if (divisions.isEmpty()) {
            return SeasonStatusEnum.NOT_STARTED.getCode();
        }
        boolean anyRegistrationOpen = divisions.stream()
                .anyMatch(d -> DivisionStatusEnum.REGISTRATION_OPEN.getCode().equals(d.getDivisionStatus()));
        if (anyRegistrationOpen) {
            return SeasonStatusEnum.REGISTRATION_OPEN.getCode();
        }
        boolean anyPlaying = divisions.stream()
                .anyMatch(d -> DivisionStatusEnum.PLAYING.getCode().equals(d.getDivisionStatus()));
        if (anyPlaying) {
            return SeasonStatusEnum.PLAYING.getCode();
        }
        boolean allFinished = divisions.stream()
                .allMatch(d -> DivisionStatusEnum.FINISHED.getCode().equals(d.getDivisionStatus())
                        || DivisionStatusEnum.ARCHIVED.getCode().equals(d.getDivisionStatus()));
        if (allFinished) {
            return SeasonStatusEnum.FINISHED.getCode();
        }
        boolean anyRegistrationClosed = divisions.stream()
                .anyMatch(d -> DivisionStatusEnum.REGISTRATION_CLOSED.getCode().equals(d.getDivisionStatus())
                        || DivisionStatusEnum.SCHEDULED.getCode().equals(d.getDivisionStatus()));
        if (anyRegistrationClosed) {
            return SeasonStatusEnum.REGISTRATION_CLOSED.getCode();
        }
        return SeasonStatusEnum.NOT_STARTED.getCode();
    }

    private void rejectKeyFieldChange(Object oldVal, Object newVal, String message) {
        if (newVal != null && !newVal.equals(oldVal)) {
            throw new ServiceException(message);
        }
    }
}
