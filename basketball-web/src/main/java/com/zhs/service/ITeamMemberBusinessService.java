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
