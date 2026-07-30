package com.zhs.basket.service.impl;

import com.zhs.basket.constant.CnpaRosterConstants;
import com.zhs.basket.domain.BasketCompetitionSeason;
import com.zhs.basket.domain.BasketTournament;
import com.zhs.basket.domain.BasketTournamentSeason;
import com.zhs.basket.enums.SeasonStatusEnum;
import com.zhs.basket.enums.TournamentRegistrationWindowStatusEnum;
import com.zhs.basket.enums.TournamentStatusEnum;
import com.zhs.basket.mapper.BasketCompetitionSeasonMapper;
import com.zhs.basket.mapper.BasketTournamentMapper;
import com.zhs.basket.mapper.BasketTournamentRegistrationMapper;
import com.zhs.basket.mapper.BasketTournamentSeasonMapper;
import com.zhs.basket.query.TournamentQuery;
import com.zhs.basket.req.TournamentCreateReq;
import com.zhs.basket.req.TournamentUpdateReq;
import com.zhs.basket.service.IBasketCompetitionSeasonService;
import com.zhs.basket.service.IBasketTournamentService;
import com.zhs.basket.mapper.BasketTournamentSeasonPlatformMapper;
import com.zhs.basket.support.GameTerminalSupport;
import com.zhs.basket.support.PlayerNoGenerator;
import com.zhs.basket.support.TournamentRegistrationWindowSupport;
import com.zhs.basket.vo.TournamentDetailVo;
import com.zhs.basket.vo.TournamentListVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.bean.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class BasketTournamentServiceImpl implements IBasketTournamentService {

    private static final Integer NOT_DELETED = 0;

    @Autowired
    private BasketTournamentMapper tournamentMapper;
    @Autowired
    private BasketCompetitionSeasonMapper competitionSeasonMapper;
    @Autowired
    private BasketTournamentSeasonMapper tournamentSeasonMapper;
    @Autowired
    private BasketTournamentRegistrationMapper registrationMapper;
    @Autowired
    private IBasketCompetitionSeasonService competitionSeasonService;
    @Autowired
    private BasketTournamentSeasonPlatformMapper seasonPlatformMapper;
    @Autowired
    private PlayerNoGenerator playerNoGenerator;

    @Override
    public List<TournamentListVo> selectList(TournamentQuery query) {
        return tournamentMapper.selectList(query).stream().map(this::toListVo).collect(Collectors.toList());
    }

    @Override
    public TournamentDetailVo selectById(Long id) {
        BasketTournament tournament = requireTournament(id);
        TournamentDetailVo vo = new TournamentDetailVo();
        BeanUtils.copyBeanProp(vo, tournament);
        enrichVo(vo, tournament);
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long create(TournamentCreateReq req) {
        if (req.getCompetitionSeasonId() == null) {
            throw new ServiceException("请先选择所属赛季");
        }
        competitionSeasonService.requireExists(req.getCompetitionSeasonId());
        validateTournamentTimes(req.getRegistrationStartTime(), req.getRegistrationEndTime(),
                req.getCompetitionStartTime(), req.getCompetitionEndTime());
        GameTerminalSupport.requireTournamentTerminal(req.getGameTerminal());
        assertUniqueSeasonAndName(req.getCompetitionSeasonId(), req.getTournamentName(), null);

        Date now = new Date();
        BasketTournament tournament = new BasketTournament();
        BeanUtils.copyBeanProp(tournament, req);
        tournament.setGameTerminal(GameTerminalSupport.normalize(req.getGameTerminal()));
        tournament.setTournamentNo(playerNoGenerator.generateBizNo("TN"));
        tournament.setTournamentStatus(TournamentStatusEnum.DRAFT.getCode());
        tournament.setPublicStatus(req.getPublicStatus() != null ? req.getPublicStatus() : 0);
        tournament.setDisplayOrder(req.getDisplayOrder() != null ? req.getDisplayOrder() : 0);
        tournament.setCreateTime(now);
        tournament.setUpdateTime(now);
        tournament.setIsDelete(NOT_DELETED);
        if (tournament.getShortName() == null || tournament.getShortName().isBlank()) {
            tournament.setShortName(tournament.getTournamentName());
        }
        if (tournament.getTournamentType() == null || tournament.getTournamentType().isBlank()) {
            tournament.setTournamentType("LEAGUE");
        }
        if (tournament.getFormatCode() == null || tournament.getFormatCode().isBlank()) {
            tournament.setFormatCode("5V5");
        }
        tournamentMapper.insert(tournament);
        BasketTournamentSeason operationalSeason = createOperationalSeason(tournament, req, now);
        syncEventPlatform(operationalSeason.getId(), tournament.getGameTerminal());
        return tournament.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long id, TournamentUpdateReq req) {
        BasketTournament tournament = requireTournament(id);
        validateTournamentTimes(req.getRegistrationStartTime(), req.getRegistrationEndTime(),
                req.getCompetitionStartTime(), req.getCompetitionEndTime());
        if (req.getGameTerminal() != null) {
            GameTerminalSupport.requireTournamentTerminal(req.getGameTerminal());
        }

        if (req.getCompetitionSeasonId() != null
                && !Objects.equals(req.getCompetitionSeasonId(), tournament.getCompetitionSeasonId())) {
            assertCanChangeCompetitionSeason(tournament);
            competitionSeasonService.requireExists(req.getCompetitionSeasonId());
        }

        Long targetSeasonId = req.getCompetitionSeasonId() != null
                ? req.getCompetitionSeasonId() : tournament.getCompetitionSeasonId();
        String targetName = StringUtils.hasText(req.getTournamentName())
                ? req.getTournamentName() : tournament.getTournamentName();
        assertUniqueSeasonAndName(targetSeasonId, targetName, id);

        BeanUtils.copyBeanProp(tournament, req);
        if (req.getGameTerminal() != null) {
            tournament.setGameTerminal(GameTerminalSupport.normalize(req.getGameTerminal()));
        }
        if (tournamentMapper.updateById(tournament) != 1) {
            throw new ServiceException("更新失败");
        }

        syncOperationalSeason(tournament);
        BasketTournamentSeason operationalSeason = tournamentSeasonMapper.selectPrimaryByTournamentId(tournament.getId());
        if (operationalSeason != null) {
            syncEventPlatform(operationalSeason.getId(), tournament.getGameTerminal());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void publish(Long id) {
        BasketTournament tournament = requireTournament(id);
        int rows = tournamentMapper.updateStatusIfMatch(id,
                Arrays.asList(TournamentStatusEnum.DRAFT.getCode(), TournamentStatusEnum.DISABLED.getCode()),
                TournamentStatusEnum.ACTIVE.getCode());
        assertSingleRow(rows, "发布失败");
        tournament.setTournamentStatus(TournamentStatusEnum.ACTIVE.getCode());
        syncOperationalSeason(tournament);
    }

    private void syncOperationalSeason(BasketTournament tournament) {
        BasketTournamentSeason operationalSeason = tournamentSeasonMapper.selectPrimaryByTournamentId(tournament.getId());
        if (operationalSeason == null) {
            return;
        }
        operationalSeason.setRegistrationStartTime(tournament.getRegistrationStartTime());
        operationalSeason.setRegistrationEndTime(tournament.getRegistrationEndTime());
        operationalSeason.setSeasonStartTime(tournament.getCompetitionStartTime());
        operationalSeason.setSeasonEndTime(tournament.getCompetitionEndTime());
        operationalSeason.setSeasonName(tournament.getTournamentName());
        syncSeasonRegistrationStatus(operationalSeason, tournament.getTournamentStatus());
        tournamentSeasonMapper.updateById(operationalSeason);
    }

    private void syncSeasonRegistrationStatus(BasketTournamentSeason season, Integer tournamentStatus) {
        if (season.getRegistrationStartTime() == null || season.getRegistrationEndTime() == null) {
            return;
        }
        Date now = new Date();
        if (!Objects.equals(TournamentStatusEnum.ACTIVE.getCode(), tournamentStatus)) {
            return;
        }
        if (now.before(season.getRegistrationStartTime())) {
            if (Objects.equals(SeasonStatusEnum.REGISTRATION_OPEN.getCode(), season.getSeasonStatus())) {
                season.setSeasonStatus(SeasonStatusEnum.PREPARE.getCode());
            }
            return;
        }
        if (now.after(season.getRegistrationEndTime())) {
            if (Objects.equals(SeasonStatusEnum.REGISTRATION_OPEN.getCode(), season.getSeasonStatus())) {
                season.setSeasonStatus(SeasonStatusEnum.REGISTRATION_CLOSED.getCode());
            }
            return;
        }
        season.setSeasonStatus(SeasonStatusEnum.REGISTRATION_OPEN.getCode());
    }

    private void openOperationalSeasonRegistration(Long tournamentId) {
        BasketTournament tournament = requireTournament(tournamentId);
        syncOperationalSeason(tournament);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unpublish(Long id) {
        int rows = tournamentMapper.updateStatusIfMatch(id,
                Arrays.asList(TournamentStatusEnum.ACTIVE.getCode()),
                TournamentStatusEnum.DISABLED.getCode());
        assertSingleRow(rows, "下架失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void close(Long id) {
        int rows = tournamentMapper.updateStatusIfMatch(id,
                Arrays.asList(TournamentStatusEnum.ACTIVE.getCode(), TournamentStatusEnum.DRAFT.getCode()),
                TournamentStatusEnum.ENDED.getCode());
        assertSingleRow(rows, "关闭失败");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Long id) {
        BasketTournament tournament = requireTournament(id);
        if (!Objects.equals(TournamentStatusEnum.DRAFT.getCode(), tournament.getTournamentStatus())) {
            throw new ServiceException("仅草稿赛事允许删除");
        }
        if (registrationMapper.countByTournamentId(id) > 0) {
            throw new ServiceException("赛事已有报名数据，不能删除");
        }
        if (tournamentMapper.softDelete(id) != 1) {
            throw new ServiceException("删除失败");
        }
    }

    private BasketTournamentSeason createOperationalSeason(BasketTournament tournament, TournamentCreateReq req, Date now) {
        BasketCompetitionSeason competitionSeason = competitionSeasonMapper.selectById(tournament.getCompetitionSeasonId());
        BasketTournamentSeason season = new BasketTournamentSeason();
        season.setSeasonNo(playerNoGenerator.generateBizNo("SN"));
        season.setTournamentId(tournament.getId());
        season.setSeasonName(tournament.getTournamentName());
        season.setSeasonYear(competitionSeason != null ? competitionSeason.getSeasonYear() : null);
        season.setRegistrationStartTime(req.getRegistrationStartTime());
        season.setRegistrationEndTime(req.getRegistrationEndTime());
        season.setSeasonStartTime(req.getCompetitionStartTime());
        season.setSeasonEndTime(req.getCompetitionEndTime());
        season.setMaxTeamCount(16);
        season.setMinRosterCount(CnpaRosterConstants.MIN_ROSTER_COUNT);
        season.setMaxRosterCount(CnpaRosterConstants.MAX_ROSTER_COUNT);
        season.setAllowTeamRegistration(1);
        season.setAllowOpenRegistration(0);
        season.setSeasonStatus(SeasonStatusEnum.PREPARE.getCode());
        season.setPublicStatus(tournament.getPublicStatus());
        season.setDescription(tournament.getDescription());
        season.setCreateTime(now);
        season.setUpdateTime(now);
        season.setIsDelete(NOT_DELETED);
        tournamentSeasonMapper.insert(season);
        return season;
    }

    private void syncEventPlatform(Long seasonId, String gameTerminal) {
        if (seasonId == null) {
            return;
        }
        String platformCode = GameTerminalSupport.normalize(gameTerminal);
        if (platformCode == null) {
            return;
        }
        seasonPlatformMapper.upsertOpenPlatform(seasonId, platformCode);
    }

    private String formatCompetitionSeasonDisplay(BasketCompetitionSeason competitionSeason) {
        if (competitionSeason == null) {
            return null;
        }
        if (competitionSeason.getSeasonName() != null && competitionSeason.getSeasonName().contains("赛季")) {
            return competitionSeason.getSeasonName();
        }
        if (competitionSeason.getSeasonYear() != null) {
            return competitionSeason.getSeasonYear() + "赛季";
        }
        return competitionSeason.getSeasonName();
    }

    private String resolveTournamentTypeName(String tournamentType) {
        if (tournamentType == null || tournamentType.isBlank()) {
            return null;
        }
        return switch (tournamentType.trim().toUpperCase()) {
            case "REGULAR_SEASON", "REGULAR" -> "常规赛";
            case "PLAYOFFS", "PLAYOFF" -> "季后赛";
            case "ALL_STAR", "ALLSTAR" -> "全明星赛";
            case "LEAGUE" -> "联赛";
            default -> tournamentType;
        };
    }

    private void assertCanChangeCompetitionSeason(BasketTournament tournament) {
        if (Objects.equals(TournamentStatusEnum.ACTIVE.getCode(), tournament.getTournamentStatus())) {
            throw new ServiceException("已发布赛事不允许修改所属赛季");
        }
        if (Objects.equals(TournamentStatusEnum.ENDED.getCode(), tournament.getTournamentStatus())) {
            throw new ServiceException("已关闭赛事不允许修改所属赛季");
        }
        if (registrationMapper.countByTournamentId(tournament.getId()) > 0) {
            throw new ServiceException("赛事已有报名数据，不允许修改所属赛季");
        }
    }

    private void validateTournamentTimes(Date registrationStart, Date registrationEnd,
                                       Date competitionStart, Date competitionEnd) {
        if (registrationStart == null || registrationEnd == null || competitionStart == null || competitionEnd == null) {
            throw new ServiceException("报名和比赛时间不能为空");
        }
        if (!registrationStart.before(registrationEnd)) {
            throw new ServiceException("报名开始时间必须早于报名结束时间");
        }
        if (registrationEnd.after(competitionStart)) {
            throw new ServiceException("报名结束时间不能晚于比赛开始时间");
        }
        if (!competitionStart.before(competitionEnd)) {
            throw new ServiceException("比赛开始时间必须早于比赛结束时间");
        }
    }

    private void assertUniqueSeasonAndName(Long competitionSeasonId, String tournamentName, Long excludeId) {
        if (competitionSeasonId == null || !StringUtils.hasText(tournamentName)) {
            throw new ServiceException("所属赛季和赛事名称不能为空");
        }
        if (tournamentMapper.countBySeasonAndName(competitionSeasonId, tournamentName.trim(), excludeId) > 0) {
            throw new ServiceException("该赛季下已存在同名赛事，请勿重复创建");
        }
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

    private void assertSingleRow(int rows, String message) {
        if (rows != 1) {
            throw new ServiceException(message);
        }
    }
}
