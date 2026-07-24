package com.zhs.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.request.team.TeamInvitationCreateReq;
import com.zhs.request.team.TeamInvitationRespondReq;
import com.zhs.response.team.TeamInvitationItemResp;

public interface ITeamInvitationBusinessService {

    TeamInvitationItemResp createInvitation(String teamId, TeamInvitationCreateReq req);

    IPage<TeamInvitationItemResp> pageTeamInvitations(String teamId, Integer currentPage, Integer pageSize, Integer status);

    IPage<TeamInvitationItemResp> pageMyInvitations(Integer currentPage, Integer pageSize, Integer status);

    TeamInvitationItemResp respond(String invitationId, TeamInvitationRespondReq req);
}
