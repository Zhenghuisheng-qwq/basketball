package com.zhs.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.request.team.TeamCreateReq;
import com.zhs.request.team.TeamLogoBindReq;
import com.zhs.request.team.TeamUpdateReq;
import com.zhs.response.team.JoinableTeamPageResp;
import com.zhs.response.team.TeamDetailResp;
import com.zhs.response.team.TeamSummaryResp;

public interface IAccountTeamService {

    IPage<TeamSummaryResp> pageMine(Integer currentPage, Integer pageSize);

    JoinableTeamPageResp pageJoinable(Integer currentPage, Integer pageSize, String keyword);

    TeamDetailResp create(TeamCreateReq req, String clientIp);

    TeamDetailResp getDetail(String teamId);

    TeamDetailResp update(String teamId, TeamUpdateReq req, String clientIp);

    TeamDetailResp bindLogo(String teamId, TeamLogoBindReq req, String clientIp);
}
