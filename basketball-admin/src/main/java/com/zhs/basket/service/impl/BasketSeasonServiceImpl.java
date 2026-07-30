package com.zhs.basket.service.impl;

import com.zhs.basket.constant.CnpaRosterConstants;
import com.zhs.basket.domain.BasketTournamentSeason;
import com.zhs.basket.enums.SeasonStatusEnum;
import com.zhs.basket.mapper.BasketTournamentSeasonMapper;
import com.zhs.basket.req.SeasonCreateReq;
import com.zhs.basket.req.SeasonUpdateReq;
import com.zhs.basket.service.IBasketCaptainQualificationService;
import com.zhs.basket.service.IBasketSeasonService;
import com.zhs.basket.support.PlayerNoGenerator;
import com.zhs.basket.vo.SeasonDetailVo;
import com.zhs.basket.vo.SeasonListVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.bean.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class BasketSeasonServiceImpl implements IBasketSeasonService {

    private static final Integer NOT_DELETED = 0;

    @Autowired private BasketTournamentSeasonMapper seasonMapper;
    @Autowired private PlayerNoGenerator playerNoGenerator;
    @Autowired private IBasketCaptainQualificationService captainQualificationService;

    @Override
    public List<SeasonListVo> selectByTournamentId(Long tournamentId) {
        return seasonMapper.selectByTournamentId(tournamentId).stream().map(this::toListVo).collect(Collectors.toList());
    }

    @Override
    public SeasonDetailVo selectById(Long seasonId) {
        BasketTournamentSeason season = requireSeason(seasonId);
        SeasonDetailVo vo = new SeasonDetailVo();
        BeanUtils.copyBeanProp(vo, season);
        vo.setSeasonStatusName(SeasonStatusEnum.fromCode(season.getSeasonStatus()).getDescription());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(Long tournamentId, SeasonCreateReq req) {
        validateSeasonTimes(req.getRegistrationStartTime(), req.getRegistrationEndTime(),
                req.getSeasonStartTime(), req.getSeasonEndTime(), req.getMinRosterCount(), req.getMaxRosterCount());
        BasketTournamentSeason season = new BasketTournamentSeason();
        BeanUtils.copyBeanProp(season, req);
        season.setTournamentId(tournamentId);
        season.setSeasonNo(playerNoGenerator.generateBizNo("SN"));
        season.setSeasonStatus(SeasonStatusEnum.PREPARE.getCode());
        season.setIsDelete(NOT_DELETED);
        seasonMapper.insert(season);
        return season.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long seasonId, SeasonUpdateReq req) {
        BasketTournamentSeason season = requireSeason(seasonId);
        validateSeasonTimes(req.getRegistrationStartTime(), req.getRegistrationEndTime(),
                req.getSeasonStartTime(), req.getSeasonEndTime(), req.getMinRosterCount(), req.getMaxRosterCount());
        BeanUtils.copyBeanProp(season, req);
        if (seasonMapper.updateById(season) != 1) {
            throw new ServiceException("更新失败");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long seasonId) {
        requireSeason(seasonId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void openRegistration(Long seasonId) {
        changeStatus(seasonId, Arrays.asList(SeasonStatusEnum.PREPARE.getCode(), SeasonStatusEnum.REGISTRATION_CLOSED.getCode()),
                SeasonStatusEnum.REGISTRATION_OPEN);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void closeRegistration(Long seasonId) {
        changeStatus(seasonId, Arrays.asList(SeasonStatusEnum.REGISTRATION_OPEN.getCode()),
                SeasonStatusEnum.REGISTRATION_CLOSED);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void start(Long seasonId) {
        changeStatus(seasonId, Arrays.asList(SeasonStatusEnum.REGISTRATION_OPEN.getCode(), SeasonStatusEnum.REGISTRATION_CLOSED.getCode()),
                SeasonStatusEnum.IN_PROGRESS);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void finish(Long seasonId) {
        changeStatus(seasonId, Arrays.asList(SeasonStatusEnum.IN_PROGRESS.getCode()),
                SeasonStatusEnum.FINISHED);
        captainQualificationService.expireBySeasonFinish(seasonId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long seasonId) {
        if (seasonMapper.softDelete(seasonId) != 1) {
            throw new ServiceException("删除失败");
        }
    }

    private void changeStatus(Long seasonId, java.util.List<Integer> from, SeasonStatusEnum to) {
        int rows = seasonMapper.updateStatusIfMatch(seasonId, from, to.getCode());
        if (rows != 1) {
            throw new ServiceException("状态变更失败");
        }
    }

    private void validateSeasonTimes(Date regStart, Date regEnd, Date seasonStart, Date seasonEnd,
                                     Integer minRoster, Integer maxRoster) {
        if (regStart == null || regEnd == null || seasonStart == null || seasonEnd == null) {
            throw new ServiceException("时间字段不能为空");
        }
        if (!regStart.before(regEnd)) {
            throw new ServiceException("报名开始时间必须早于报名结束时间");
        }
        if (regEnd.after(seasonStart)) {
            throw new ServiceException("报名结束时间不能晚于赛季开始时间");
        }
        if (!seasonStart.before(seasonEnd)) {
            throw new ServiceException("赛季开始时间必须早于赛季结束时间");
        }
        if (minRoster == null || minRoster < CnpaRosterConstants.MIN_ROSTER_COUNT) {
            throw new ServiceException("最小阵容人数不能少于 " + CnpaRosterConstants.MIN_ROSTER_COUNT);
        }
        if (maxRoster == null || maxRoster > CnpaRosterConstants.MAX_ROSTER_COUNT) {
            throw new ServiceException("最大阵容人数不能超过 " + CnpaRosterConstants.MAX_ROSTER_COUNT);
        }
        if (maxRoster != null && maxRoster < minRoster) {
            throw new ServiceException("最大 roster 人数不能小于最小人数");
        }
    }

    private BasketTournamentSeason requireSeason(Long id) {
        BasketTournamentSeason season = seasonMapper.selectById(id);
        if (season == null) {
            throw new ServiceException("赛季不存在");
        }
        return season;
    }

    private SeasonListVo toListVo(BasketTournamentSeason season) {
        SeasonListVo vo = new SeasonListVo();
        BeanUtils.copyBeanProp(vo, season);
        vo.setSeasonStatusName(SeasonStatusEnum.fromCode(season.getSeasonStatus()).getDescription());
        return vo;
    }
}
