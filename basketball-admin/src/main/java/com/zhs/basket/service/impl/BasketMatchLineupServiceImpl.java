package com.zhs.basket.service.impl;

import com.zhs.basket.constant.BasketMatchConstants;
import com.zhs.basket.domain.BasketDisciplineRecord;
import com.zhs.basket.domain.BasketMatch;
import com.zhs.basket.domain.BasketMatchRoster;
import com.zhs.basket.domain.BasketTournamentRegistrationPlayer;
import com.zhs.basket.enums.MatchChangeTypeEnum;
import com.zhs.basket.enums.MatchStatusEnum;
import com.zhs.basket.enums.RosterRoleEnum;
import com.zhs.basket.mapper.BasketDisciplineRecordMapper;
import com.zhs.basket.mapper.BasketMatchRosterMapper;
import com.zhs.basket.mapper.BasketTeamMapper;
import com.zhs.basket.mapper.BasketTournamentRegistrationPlayerMapper;
import com.zhs.basket.req.MatchLineupSaveReq;
import com.zhs.basket.service.IBasketMatchLineupService;
import com.zhs.basket.service.IBasketMatchValidationService;
import com.zhs.basket.vo.AvailableMatchPlayerVo;
import com.zhs.basket.vo.MatchLineupPlayerVo;
import com.zhs.basket.vo.MatchLineupVo;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class BasketMatchLineupServiceImpl implements IBasketMatchLineupService {

    private static final Integer NOT_DELETED = 0;
    private static final int PLAY_STATUS_ACTIVE = 1;

    @Autowired private IBasketMatchValidationService validationService;
    @Autowired private BasketMatchRosterMapper rosterMapper;
    @Autowired private BasketTournamentRegistrationPlayerMapper registrationPlayerMapper;
    @Autowired private BasketDisciplineRecordMapper disciplineRecordMapper;
    @Autowired private BasketTeamMapper teamMapper;

    @Override
    public MatchLineupVo selectLineup(Long matchId, Long teamId) {
        BasketMatch match = validationService.requireMatch(matchId);
        assertTeamInMatch(match, teamId);
        MatchLineupVo vo = new MatchLineupVo();
        vo.setMatchId(matchId);
        vo.setTeamId(teamId);
        if (teamMapper.selectById(teamId) != null) {
            vo.setTeamName(teamMapper.selectById(teamId).getTeamName());
        }
        vo.setPlayers(rosterMapper.selectVoByMatchAndTeam(matchId, teamId));
        return vo;
    }

    @Override
    public List<AvailableMatchPlayerVo> selectAvailablePlayers(Long matchId, Long teamId) {
        BasketMatch match = validationService.requireMatch(matchId);
        assertTeamInMatch(match, teamId);
        return registrationPlayerMapper.selectAvailableBySeasonAndTeam(match.getSeasonId(), teamId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveLineup(Long matchId, Long teamId, MatchLineupSaveReq req) {
        BasketMatch match = validationService.requireMatch(matchId);
        assertTeamInMatch(match, teamId);
        if (!MatchStatusEnum.fromCode(match.getMatchStatus()).canEnterLineup()) {
            throw new ServiceException("当前状态不可编辑出场名单");
        }
        if (req == null || CollectionUtils.isEmpty(req.getPlayers())) {
            throw new ServiceException("出场名单不能为空");
        }

        Set<String> jerseyNos = new HashSet<>();
        List<Long> memberIds = new ArrayList<>();
        List<BasketMatchRoster> rosterList = new ArrayList<>();

        for (MatchLineupSaveReq.LineupPlayerItem item : req.getPlayers()) {
            if (item.getRegistrationPlayerId() == null) {
                throw new ServiceException("只能从报名球员中选择");
            }
            BasketTournamentRegistrationPlayer regPlayer = registrationPlayerMapper.selectById(item.getRegistrationPlayerId());
            if (regPlayer == null || !teamId.equals(regPlayer.getTeamId())
                    || !match.getSeasonId().equals(regPlayer.getSeasonId())) {
                throw new ServiceException("报名球员无效");
            }
            if (regPlayer.getValidationStatus() == null
                    || regPlayer.getValidationStatus() != BasketMatchConstants.REGISTRATION_PLAYER_PASS) {
                throw new ServiceException("报名球员校验未通过");
            }
            String jersey = item.getJerseyNo() != null ? item.getJerseyNo().trim() : regPlayer.getJerseyNo();
            if (StringUtils.isEmpty(jersey) || !jerseyNos.add(jersey)) {
                throw new ServiceException("球衣号码不能为空且不能重复");
            }
            int starting = item.getIsStarting() != null && item.getIsStarting() == 1 ? 1 : 0;
            int captain = item.getCaptainFlag() != null && item.getCaptainFlag() == 1 ? 1 : 0;

            BasketMatchRoster roster = new BasketMatchRoster();
            roster.setMatchId(matchId);
            roster.setSeasonId(match.getSeasonId());
            roster.setTeamId(teamId);
            roster.setPlayerId(regPlayer.getPlayerId());
            roster.setRegistrationPlayerId(regPlayer.getId());
            roster.setJerseyNo(jersey);
            roster.setRosterRole(StringUtils.isNotEmpty(item.getRosterRole()) ? item.getRosterRole() : RosterRoleEnum.PLAYER.getCode());
            roster.setIsStarting(starting);
            roster.setPositionCode(item.getPositionCode());
            roster.setPlayStatus(PLAY_STATUS_ACTIVE);
            roster.setCaptainFlag(captain);
            roster.setSortNo(item.getSortNo() != null ? item.getSortNo() : 0);
            roster.setIsDelete(NOT_DELETED);
            rosterList.add(roster);
            if (regPlayer.getMemberId() != null) {
                memberIds.add(regPlayer.getMemberId());
            }
        }

        Date now = new Date();
        if (!memberIds.isEmpty()) {
            Set<Long> banned = disciplineRecordMapper.selectActiveBySeasonAndMembers(match.getSeasonId(), memberIds, now)
                    .stream().map(BasketDisciplineRecord::getMemberId).collect(Collectors.toSet());
            if (!banned.isEmpty()) {
                throw new ServiceException("出场名单包含处于禁赛期的球员");
            }
        }

        rosterMapper.softDeleteByMatchAndTeam(matchId, teamId);
        rosterMapper.batchInsert(rosterList);
        validationService.writeStatusLog(match, match.getMatchStatus(), MatchChangeTypeEnum.LINEUP_SAVE, "保存出场名单");
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void confirmLineup(Long matchId, Long teamId) {
        BasketMatch match = validationService.requireMatch(matchId);
        assertTeamInMatch(match, teamId);
        if (rosterMapper.countStartersByMatchAndTeam(matchId, teamId) != BasketMatchConstants.STARTER_COUNT) {
            throw new ServiceException("须指定5名首发球员");
        }
        if (rosterMapper.countCaptainsByMatchAndTeam(matchId, teamId) != BasketMatchConstants.CAPTAIN_COUNT) {
            throw new ServiceException("须指定1名场上队长");
        }
        validationService.writeStatusLog(match, match.getMatchStatus(), MatchChangeTypeEnum.LINEUP_CONFIRM, "确认出场名单");
    }

    private void assertTeamInMatch(BasketMatch match, Long teamId) {
        if (!teamId.equals(match.getHomeTeamId()) && !teamId.equals(match.getAwayTeamId())) {
            throw new ServiceException("球队不属于该比赛");
        }
    }
}
