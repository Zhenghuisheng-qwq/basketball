package com.zhs.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.request.team.TeamMemberUpdateReq;
import com.zhs.response.team.CandidatePlayerItemResp;
import com.zhs.response.team.CandidatePlayerPageResp;
import com.zhs.response.team.TeamMemberDetailResp;
import com.zhs.response.team.TeamMemberItemResp;

import java.util.List;

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

    /**
     * 本队历史球员。生涯记录仍在这支球队上；已加入本届其他球队的不能再邀请。
     */
    List<CandidatePlayerItemResp> listHistoricalTeammates(String teamId);
}
