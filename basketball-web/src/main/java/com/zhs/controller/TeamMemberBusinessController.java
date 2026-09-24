package com.zhs.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.request.team.TeamMemberUpdateReq;
import com.zhs.response.team.CandidatePlayerPageResp;
import com.zhs.response.team.TeamMemberDetailResp;
import com.zhs.response.team.TeamMemberItemResp;
import com.zhs.service.ITeamMemberBusinessService;
import com.zhs.support.ClientIpResolver;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/basket/team")
@Api(value = "球队成员")
@Slf4j
public class TeamMemberBusinessController {

    @Resource
    private ITeamMemberBusinessService teamMemberBusinessService;
    @Resource
    private ClientIpResolver clientIpResolver;

    @ApiOperation("球队成员分页（历史球队自动含 LEFT；includeHistory=true 强制含历史）")
    @GetMapping("/{teamId}/member/page")
    public R<IPage<TeamMemberItemResp>> pageMembers(@PathVariable String teamId,
                                                    @RequestParam(required = false) Integer currentPage,
                                                    @RequestParam(required = false) Integer pageSize,
                                                    @RequestParam(required = false) Boolean includeHistory) {
        return R.ok(teamMemberBusinessService.pageMembers(teamId, currentPage, pageSize, includeHistory));
    }

    @ApiOperation("队内成员私有详情（队长可看本队成员，成员仅可看自己）")
    @GetMapping("/{teamId}/member/{teamMemberId}/detail")
    public R<TeamMemberDetailResp> getMemberDetail(@PathVariable String teamId,
                                                   @PathVariable String teamMemberId) {
        return R.ok(teamMemberBusinessService.getMemberDetail(teamId, teamMemberId));
    }

    @ApiOperation("更新球队成员")
    @PutMapping("/{teamId}/member/{teamMemberId}")
    public R<TeamMemberItemResp> updateMember(@PathVariable String teamId,
                                              @PathVariable String teamMemberId,
                                              @RequestBody TeamMemberUpdateReq req) {
        return R.ok(teamMemberBusinessService.updateMember(teamId, teamMemberId, req), "成员更新成功");
    }

    @ApiOperation("移除球队成员")
    @DeleteMapping("/{teamId}/member/{teamMemberId}")
    public R<Void> removeMember(@PathVariable String teamId,
                                @PathVariable String teamMemberId,
                                HttpServletRequest request) {
        teamMemberBusinessService.removeMember(teamId, teamMemberId, clientIpResolver.resolve(request));
        return R.ok(null, "成员移除成功");
    }

    @ApiOperation("主动离队")
    @PostMapping("/{teamId}/leave")
    public R<Void> leave(@PathVariable String teamId, HttpServletRequest request) {
        teamMemberBusinessService.leave(teamId, clientIpResolver.resolve(request));
        return R.ok(null, "离队成功");
    }

    @ApiOperation("候选球员分页")
    @GetMapping("/{teamId}/candidate-player/page")
    public R<CandidatePlayerPageResp> pageCandidatePlayers(@PathVariable String teamId,
                                                                   @RequestParam(required = false) Integer currentPage,
                                                                   @RequestParam(required = false) Integer pageSize,
                                                                   @RequestParam(required = false) String keyword) {
        return R.ok(teamMemberBusinessService.pageCandidatePlayers(teamId, currentPage, pageSize, keyword));
    }
}
