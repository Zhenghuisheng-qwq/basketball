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

    /**
     * 当前队长在目标赛事下可以继承的原球队。只包含本人仍是队长的球队。
     *
     * @param divisionId 要带去的赛事；为空且仅有一条未绑定授权时自动使用该赛事
     */
    List<InheritableTeamSummaryResp> listInheritableTeams(Long divisionId);

    /**
     * 把本届未使用的队长授权绑到原球队。只继承球队，不带队员。
     * 荣誉和历史仍挂在原球队主键上；当前名单只有队长。老球员按赛季结束归档，生涯还在这支球队上，之后由队长邀请。
     */
    TeamDetailResp bindGrantToTeam(String teamId, Long divisionId, String clientIp);

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
