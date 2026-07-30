package com.zhs.basket.support;

import com.zhs.basket.domain.BasketMatch;
import com.zhs.basket.domain.BasketMatchTeam;
import com.zhs.basket.enums.TeamSideEnum;
import com.zhs.basket.mapper.BasketMatchTeamMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Component
public class ScheduleMatchTeamSupport {

    private static final Integer NOT_DELETED = 0;

    @Autowired private BasketMatchTeamMapper matchTeamMapper;

    public void persistMatchTeams(BasketMatch match) {
        List<BasketMatchTeam> teams = buildMatchTeams(match);
        matchTeamMapper.batchInsert(teams);
    }

    public void replaceMatchTeams(BasketMatch match) {
        matchTeamMapper.softDeleteByMatchId(match.getId());
        persistMatchTeams(match);
    }

    private List<BasketMatchTeam> buildMatchTeams(BasketMatch match) {
        List<BasketMatchTeam> teams = new ArrayList<>(2);
        teams.add(buildMatchTeam(match, match.getHomeTeamId(), TeamSideEnum.HOME.getCode()));
        teams.add(buildMatchTeam(match, match.getAwayTeamId(), TeamSideEnum.AWAY.getCode()));
        return teams;
    }

    private BasketMatchTeam buildMatchTeam(BasketMatch match, Long teamId, String sideType) {
        Date now = new Date();
        BasketMatchTeam team = new BasketMatchTeam();
        team.setMatchId(match.getId());
        team.setTeamId(teamId);
        team.setSideType(sideType);
        team.setScore(0);
        team.setIsWinner(0);
        team.setTimeoutCount(0);
        team.setFoulsTotal(0);
        team.setCreateTime(now);
        team.setUpdateTime(now);
        team.setIsDelete(NOT_DELETED);
        return team;
    }
}
