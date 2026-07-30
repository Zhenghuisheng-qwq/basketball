package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketCompetitionSeason;
import com.zhs.basket.enums.CompetitionSeasonStatusEnum;
import com.zhs.basket.mapper.BasketCompetitionSeasonMapper;
import com.zhs.basket.query.CompetitionSeasonQuery;
import com.zhs.basket.req.CompetitionSeasonCreateReq;
import com.zhs.basket.req.CompetitionSeasonUpdateReq;
import com.zhs.basket.service.IBasketCompetitionSeasonService;
import com.zhs.basket.support.PlayerNoGenerator;
import com.zhs.basket.vo.CompetitionSeasonDetailVo;
import com.zhs.basket.vo.CompetitionSeasonListVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.bean.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BasketCompetitionSeasonServiceImpl implements IBasketCompetitionSeasonService {

    private static final Integer NOT_DELETED = 0;

    @Autowired
    private BasketCompetitionSeasonMapper competitionSeasonMapper;
    @Autowired
    private PlayerNoGenerator playerNoGenerator;

    @Override
    public List<CompetitionSeasonListVo> selectList(CompetitionSeasonQuery query) {
        return competitionSeasonMapper.selectList(query.getSeasonName(), query.getSeasonYear(), query.getSeasonStatus())
                .stream()
                .map(this::toListVo)
                .collect(Collectors.toList());
    }

    @Override
    public CompetitionSeasonDetailVo selectById(Long id) {
        BasketCompetitionSeason season = requireSeason(id);
        CompetitionSeasonDetailVo vo = new CompetitionSeasonDetailVo();
        BeanUtils.copyBeanProp(vo, season);
        enrichVo(vo, season);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(CompetitionSeasonCreateReq req) {
        validateTimes(req.getStartTime(), req.getEndTime());
        assertUniqueYearAndName(req.getSeasonYear(), req.getSeasonName(), null);
        Date now = new Date();
        BasketCompetitionSeason season = new BasketCompetitionSeason();
        BeanUtils.copyBeanProp(season, req);
        season.setSeasonNo(playerNoGenerator.generateBizNo("CS"));
        season.setSeasonStatus(req.getSeasonStatus() != null
                ? req.getSeasonStatus() : CompetitionSeasonStatusEnum.PREPARE.getCode());
        season.setPublicStatus(req.getPublicStatus() != null ? req.getPublicStatus() : 0);
        season.setCreateTime(now);
        season.setUpdateTime(now);
        season.setIsDelete(NOT_DELETED);
        competitionSeasonMapper.insert(season);
        return season.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, CompetitionSeasonUpdateReq req) {
        BasketCompetitionSeason season = requireSeason(id);
        if (CompetitionSeasonStatusEnum.ARCHIVED.getCode() == season.getSeasonStatus()) {
            throw new ServiceException("已归档赛季不允许修改");
        }
        validateTimes(req.getStartTime(), req.getEndTime());
        assertUniqueYearAndName(req.getSeasonYear(), req.getSeasonName(), id);
        BeanUtils.copyBeanProp(season, req);
        if (competitionSeasonMapper.updateById(season) != 1) {
            throw new ServiceException("更新失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        requireSeason(id);
        if (competitionSeasonMapper.countTournamentsById(id) > 0) {
            throw new ServiceException("该赛季下已有关联赛事，不能删除");
        }
        if (competitionSeasonMapper.softDelete(id) != 1) {
            throw new ServiceException("删除失败");
        }
    }

    @Override
    public void requireExists(Long id) {
        requireSeason(id);
    }

    private BasketCompetitionSeason requireSeason(Long id) {
        if (id == null) {
            throw new ServiceException("赛季不存在");
        }
        BasketCompetitionSeason season = competitionSeasonMapper.selectById(id);
        if (season == null) {
            throw new ServiceException("赛季不存在");
        }
        return season;
    }

    private void validateTimes(Date startTime, Date endTime) {
        if (startTime == null || endTime == null) {
            throw new ServiceException("赛季开始时间和结束时间不能为空");
        }
        if (!startTime.before(endTime)) {
            throw new ServiceException("赛季开始时间必须早于结束时间");
        }
    }

    private void assertUniqueYearAndName(Integer seasonYear, String seasonName, Long excludeId) {
        if (seasonYear == null || !StringUtils.hasText(seasonName)) {
            throw new ServiceException("赛季年份和赛季名称不能为空");
        }
        if (competitionSeasonMapper.countByYearAndName(seasonYear, seasonName.trim(), excludeId) > 0) {
            throw new ServiceException("同一年份下已存在同名赛季，请勿重复创建");
        }
    }

    private CompetitionSeasonListVo toListVo(BasketCompetitionSeason season) {
        CompetitionSeasonListVo vo = new CompetitionSeasonListVo();
        BeanUtils.copyBeanProp(vo, season);
        enrichVo(vo, season);
        return vo;
    }

    private void enrichVo(CompetitionSeasonListVo vo, BasketCompetitionSeason season) {
        vo.setSeasonStatusName(CompetitionSeasonStatusEnum.fromCode(season.getSeasonStatus()).getDescription());
        vo.setTournamentCount(competitionSeasonMapper.countTournamentsById(season.getId()));
    }
}
