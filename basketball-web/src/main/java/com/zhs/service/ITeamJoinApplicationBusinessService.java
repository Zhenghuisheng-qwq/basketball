package com.zhs.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.request.team.TeamJoinApplicationCreateReq;
import com.zhs.request.team.TeamJoinApplicationProcessReq;
import com.zhs.response.team.TeamJoinApplicationItemResp;

public interface ITeamJoinApplicationBusinessService {

    TeamJoinApplicationItemResp createJoinApplication(String teamId, TeamJoinApplicationCreateReq req);

    IPage<TeamJoinApplicationItemResp> pageMyApplications(Integer currentPage, Integer pageSize, Integer status);

    void cancel(String applicationId);

    IPage<TeamJoinApplicationItemResp> pageTeamApplications(String teamId, Integer currentPage, Integer pageSize, Integer status);

    TeamJoinApplicationItemResp process(String applicationId, TeamJoinApplicationProcessReq req);
}
