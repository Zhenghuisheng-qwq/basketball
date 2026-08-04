package com.zhs.basket.service.impl;

import com.zhs.basket.domain.BasketCompetitionSeason;
import com.zhs.basket.domain.BasketTournament;
import com.zhs.basket.domain.BasketTournamentSeason;
import com.zhs.basket.enums.TournamentRegistrationWindowStatusEnum;
import com.zhs.basket.enums.TournamentStatusEnum;
import com.zhs.basket.mapper.BasketCompetitionSeasonMapper;
import com.zhs.basket.mapper.BasketTournamentMapper;
import com.zhs.basket.mapper.BasketTournamentSeasonMapper;
import com.zhs.basket.query.TournamentQuery;
import com.zhs.basket.service.IBasketTournamentService;
import com.zhs.basket.support.TournamentRegistrationWindowSupport;
import com.zhs.basket.vo.TournamentDetailVo;
import com.zhs.basket.vo.TournamentListVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.PageListUtils;
import com.zhs.common.utils.bean.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;

@Service
public class BasketTournamentServiceImpl implements IBasketTournamentService {

    @Autowired
    private BasketTournamentMapper tournamentMapper;
    @Autowired
    private BasketCompetitionSeasonMapper competitionSeasonMapper;
    @Autowired
    private BasketTournamentSeasonMapper tournamentSeasonMapper;

    @Override
    public List<TournamentListVo> selectList(TournamentQuery query) {
        return PageListUtils.map(tournamentMapper.selectList(query), this::toListVo);
    }

    @Override
    public TournamentDetailVo selectById(Long id) {
        BasketTournament tournament = requireTournament(id);
        TournamentDetailVo vo = new TournamentDetailVo();
        BeanUtils.copyBeanProp(vo, tournament);
        enrichVo(vo, tournament);
        return vo;
    }

    private BasketTournament requireTournament(Long id) {
        BasketTournament tournament = tournamentMapper.selectById(id);
        if (tournament == null) {
            throw new ServiceException("赛事不存在");
        }
        return tournament;
    }

    private TournamentListVo toListVo(BasketTournament tournament) {
        TournamentListVo vo = new TournamentListVo();
        BeanUtils.copyBeanProp(vo, tournament);
        enrichVo(vo, tournament);
        return vo;
    }

    private void enrichVo(TournamentListVo vo, BasketTournament tournament) {
        vo.setTournamentStatusName(TournamentStatusEnum.fromCode(tournament.getTournamentStatus()).getDescription());
        TournamentRegistrationWindowStatusEnum registrationWindowStatus = TournamentRegistrationWindowSupport.resolve(
                tournament.getRegistrationStartTime(), tournament.getRegistrationEndTime(), new Date());
        vo.setRegistrationWindowStatus(registrationWindowStatus.getCode());
        vo.setRegistrationWindowStatusName(registrationWindowStatus.getDescription());
        if (tournament.getCompetitionSeasonId() != null) {
            BasketCompetitionSeason competitionSeason = competitionSeasonMapper.selectById(tournament.getCompetitionSeasonId());
            if (competitionSeason != null) {
                vo.setCompetitionSeasonName(formatCompetitionSeasonDisplay(competitionSeason));
                vo.setCompetitionSeasonYear(competitionSeason.getSeasonYear());
            }
        }
        vo.setTournamentTypeName(resolveTournamentTypeName(tournament.getTournamentType()));
        BasketTournamentSeason operationalSeason = tournamentSeasonMapper.selectPrimaryByTournamentId(tournament.getId());
        if (operationalSeason != null) {
            vo.setOperationalSeasonId(operationalSeason.getId());
        }
    }

    private String formatCompetitionSeasonDisplay(BasketCompetitionSeason season) {
        if (season.getSeasonYear() != null && season.getSeasonName() != null) {
            return season.getSeasonYear() + " " + season.getSeasonName();
        }
        if (season.getSeasonName() != null) {
            return season.getSeasonName();
        }
        return season.getSeasonYear() != null ? String.valueOf(season.getSeasonYear()) : "";
    }

    private String resolveTournamentTypeName(String tournamentType) {
        if (tournamentType == null) {
            return "";
        }
        return switch (tournamentType) {
            case "LEAGUE" -> "联赛";
            case "CUP" -> "杯赛";
            case "FRIENDLY" -> "友谊赛";
            default -> tournamentType;
        };
    }
}
