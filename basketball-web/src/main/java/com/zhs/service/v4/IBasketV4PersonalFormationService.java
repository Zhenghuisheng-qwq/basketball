package com.zhs.service.v4;

import com.zhs.request.v4.V4PersonalFormationBindTeamReq;
import com.zhs.request.team.TeamCreateReq;
import com.zhs.response.team.TeamDetailResp;
import com.zhs.response.v4.V4PersonalFormationAvailableTeamResp;
import com.zhs.response.v4.V4PersonalFormationTaskResp;

import java.util.List;

public interface IBasketV4PersonalFormationService {
    List<V4PersonalFormationTaskResp> listFormationTasks();

    V4PersonalFormationTaskResp getFormationDetail(Long drawTeamId);

    List<V4PersonalFormationAvailableTeamResp> listAvailableTeams(Long drawTeamId);

    void bindLongTermTeam(V4PersonalFormationBindTeamReq req);

    TeamDetailResp createLongTermTeam(Long drawTeamId, TeamCreateReq req, String clientIp);

    Long confirmFormation(Long drawTeamId);
}
