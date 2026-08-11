package com.zhs.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.request.team.TeamCreateReq;
import com.zhs.request.team.TeamLogoBindReq;
import com.zhs.request.team.TeamUpdateReq;
import com.zhs.response.team.InheritableTeamSummaryResp;
import com.zhs.response.team.JoinableTeamPageResp;
import com.zhs.response.team.TeamDetailResp;
import com.zhs.response.team.TeamSummaryResp;

import java.util.List;

public interface IAccountTeamService {

    IPage<TeamSummaryResp> pageMine(Integer currentPage, Integer pageSize);

    /**
     * @param scope current | history；空则默认 current（兼容旧调用）
     */
    IPage<TeamSummaryResp> pageMine(Integer currentPage, Integer pageSize, String scope);

    JoinableTeamPageResp pageJoinable(Integer currentPage, Integer pageSize, String platformCode, String keyword);

    List<InheritableTeamSummaryResp> listInheritableTeams();

    TeamDetailResp bindGrantToTeam(String teamId);

    TeamDetailResp create(TeamCreateReq req, String clientIp);

    TeamDetailResp createForPersonalFormation(Long drawTeamId, TeamCreateReq req, String clientIp);

    TeamDetailResp getDetail(String teamId);

    TeamDetailResp update(String teamId, TeamUpdateReq req, String clientIp);

    TeamDetailResp bindLogo(String teamId, TeamLogoBindReq req, String clientIp);

    /**
     * 提交 / 重新提交球队认证：DRAFT|REJECTED → PENDING。
     * 已认证球队请走资料变更审核，不可调用本接口。
     */
    TeamDetailResp submitCertification(String teamId, String clientIp);
}
