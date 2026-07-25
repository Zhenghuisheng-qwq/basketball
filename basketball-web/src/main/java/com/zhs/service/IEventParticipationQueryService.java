package com.zhs.service;

import com.zhs.response.participation.EventRosterMemberResp;
import com.zhs.response.participation.EventTeamDetailResp;
import com.zhs.response.participation.EventTeamSummaryResp;
import com.zhs.response.participation.RosterChangeHistoryItemResp;

import java.util.List;

public interface IEventParticipationQueryService {

    List<EventTeamSummaryResp> getMyEventTeams(String seasonId);

    EventTeamDetailResp getEventTeamDetail(String eventTeamId);

    List<EventRosterMemberResp> getEventRoster(String eventTeamId);

    List<RosterChangeHistoryItemResp> getRosterHistory(String seasonId, String playerId, String eventTeamId);
}
