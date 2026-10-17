package com.zhs.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.request.team.TeamMemberUpdateReq;
import com.zhs.response.team.CandidatePlayerPageResp;
import com.zhs.response.team.TeamMemberDetailResp;
import com.zhs.response.team.TeamMemberItemResp;

public interface ITeamMemberBusinessService {

    IPage<TeamMemberItemResp> pageMembers(String teamId, Integer currentPage, Integer pageSize);

    IPage<TeamMemberItemResp> pageMembers(String teamId, Integer currentPage, Integer pageSize, Boolean includeHistory);

    /**
     * 队内成员私有详情：队长可看本队成员，普通成员仅可看自己。
     */
    TeamMemberDetailResp getMemberDetail(String teamId, String teamMemberId);

    TeamMemberItemResp updateMember(String teamId, String teamMemberId, TeamMemberUpdateReq req);

    boolean removeMember(String teamId, String teamMemberId, String clientIp);

    void leave(String teamId, String clientIp);

    CandidatePlayerPageResp pageCandidatePlayers(String teamId, Integer currentPage, Integer pageSize, String keyword);
}
