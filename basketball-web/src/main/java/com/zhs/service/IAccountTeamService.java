package com.zhs.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.request.team.TeamCreateReq;
import com.zhs.request.team.TeamLogoBindReq;
import com.zhs.request.team.TeamUpdateReq;
import com.zhs.response.team.InheritableTeamSummaryResp;
import com.zhs.response.team.JoinableTeamPageResp;
import com.zhs.response.team.TeamDetailResp;
import com.zhs.response.team.TeamSummaryResp;

import java.util.List;

public interface IAccountTeamService {

    IPage<TeamSummaryResp> pageMine(Integer currentPage, Integer pageSize);

    JoinableTeamPageResp pageJoinable(Integer currentPage, Integer pageSize, String platformCode, String keyword);

    List<InheritableTeamSummaryResp> listInheritableTeams();

    TeamDetailResp bindGrantToTeam(String teamId);

    TeamDetailResp create(TeamCreateReq req, String clientIp);

    TeamDetailResp getDetail(String teamId);

    TeamDetailResp update(String teamId, TeamUpdateReq req, String clientIp);

    TeamDetailResp bindLogo(String teamId, TeamLogoBindReq req, String clientIp);
}
