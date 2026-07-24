#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import os

ROOT = r"d:\code\dz\basketball\scaffolding-v2\basketball-web\src\main\java\com\zhs"

def w(rel, content):
    path = os.path.join(ROOT, rel.replace("/", os.sep))
    os.makedirs(os.path.dirname(path), exist_ok=True)
    with open(path, "w", encoding="utf-8", newline="\n") as f:
        f.write(content.strip() + "\n")

# Due to size, write service interfaces and controllers; impls as separate files via Write tool after

w("service/ITeamMemberBusinessService.java", """
package com.zhs.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.request.team.TeamMemberUpdateReq;
import com.zhs.response.team.CandidatePlayerItemResp;
import com.zhs.response.team.TeamMemberItemResp;

public interface ITeamMemberBusinessService {

    IPage<TeamMemberItemResp> pageMembers(String teamId, Integer currentPage, Integer pageSize);

    TeamMemberItemResp updateMember(String teamId, String teamMemberId, TeamMemberUpdateReq req);

    void removeMember(String teamId, String teamMemberId, String clientIp);

    void leave(String teamId, String clientIp);

    IPage<CandidatePlayerItemResp> pageCandidatePlayers(String teamId, Integer currentPage, Integer pageSize, String keyword);
}
""")

w("service/ITeamInvitationBusinessService.java", """
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
""")

w("service/ITeamJoinApplicationBusinessService.java", """
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
""")

w("service/ITeamRegistrationBusinessService.java", """
package com.zhs.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.request.team.TeamRegistrationDraftReq;
import com.zhs.request.team.TeamRegistrationRosterReq;
import com.zhs.request.team.TeamRegistrationUpdateReq;
import com.zhs.request.team.TeamRegistrationWithdrawReq;
import com.zhs.response.team.AvailableSeasonItemResp;
import com.zhs.response.team.TeamRegistrationAuditItemResp;
import com.zhs.response.team.TeamRegistrationDetailResp;
import com.zhs.response.team.TeamRegistrationItemResp;

public interface ITeamRegistrationBusinessService {

    IPage<AvailableSeasonItemResp> pageAvailableSeasons(String teamId, Integer currentPage, Integer pageSize);

    IPage<TeamRegistrationItemResp> pageRegistrations(String teamId, Integer currentPage, Integer pageSize, Integer status);

    TeamRegistrationDetailResp createDraft(String teamId, TeamRegistrationDraftReq req);

    TeamRegistrationDetailResp getDetail(String registrationId);

    TeamRegistrationDetailResp update(String registrationId, TeamRegistrationUpdateReq req);

    TeamRegistrationDetailResp saveRoster(String registrationId, TeamRegistrationRosterReq req);

    TeamRegistrationDetailResp submit(String registrationId);

    TeamRegistrationDetailResp withdraw(String registrationId, TeamRegistrationWithdrawReq req);

    void deleteDraft(String registrationId);

    IPage<TeamRegistrationAuditItemResp> pageAuditRecords(String registrationId, Integer currentPage, Integer pageSize);
}
""")

print("Interfaces created")
