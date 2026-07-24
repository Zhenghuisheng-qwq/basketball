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
