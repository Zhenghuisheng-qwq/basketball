package com.zhs.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.request.account.MyEventsSeasonPageQuery;
import com.zhs.request.team.TeamRegistrationDraftReq;
import com.zhs.request.team.TeamRegistrationRemindReq;
import com.zhs.request.team.TeamRegistrationRosterReq;
import com.zhs.request.team.TeamRegistrationUpdateReq;
import com.zhs.request.team.TeamRegistrationWithdrawReq;
import com.zhs.response.account.CompetitionSeasonOptionResp;
import com.zhs.response.account.MyEventsContextResp;
import com.zhs.response.team.AvailableSeasonItemResp;
import com.zhs.response.team.AvailableSeasonPageResp;
import com.zhs.response.team.RegistrationRosterCandidateItemResp;
import com.zhs.response.team.SeasonPlatformItemResp;
import com.zhs.response.team.TeamEventRecordItemResp;
import com.zhs.response.team.TeamRegistrationAuditItemResp;
import com.zhs.response.team.TeamRegistrationDetailResp;
import com.zhs.response.team.TeamRegistrationItemResp;

import java.util.List;

public interface ITeamRegistrationBusinessService {

    MyEventsContextResp getMyEventsContext();

    AvailableSeasonPageResp pageMyEventsSeasons(MyEventsSeasonPageQuery query);

    List<CompetitionSeasonOptionResp> listMyEventsCompetitionSeasonOptions();

    AvailableSeasonPageResp pageAvailableSeasons(String teamId, Integer currentPage, Integer pageSize);

    AvailableSeasonPageResp pageMemberAvailableSeasons(String teamId, Integer currentPage, Integer pageSize);

    IPage<TeamEventRecordItemResp> pageMemberTeamEvents(String teamId, Integer currentPage, Integer pageSize);

    void remindCaptainToRegister(String teamId, TeamRegistrationRemindReq req);

    IPage<TeamRegistrationItemResp> pageRegistrations(String teamId, Integer currentPage, Integer pageSize, Integer status);

    TeamRegistrationDetailResp createDraft(String teamId, TeamRegistrationDraftReq req);

    TeamRegistrationDetailResp getDetail(String registrationId);

    TeamRegistrationDetailResp getMemberDetail(String registrationId);

    TeamRegistrationDetailResp update(String registrationId, TeamRegistrationUpdateReq req);

    TeamRegistrationDetailResp saveRoster(String registrationId, TeamRegistrationRosterReq req);

    TeamRegistrationDetailResp submit(String registrationId);

    TeamRegistrationDetailResp withdraw(String registrationId, TeamRegistrationWithdrawReq req);

    void deleteDraft(String registrationId);

    IPage<TeamRegistrationAuditItemResp> pageAuditRecords(String registrationId, Integer currentPage, Integer pageSize);

    List<SeasonPlatformItemResp> listSeasonPlatforms(String seasonId);

    List<RegistrationRosterCandidateItemResp> listRosterCandidates(String registrationId);
}
