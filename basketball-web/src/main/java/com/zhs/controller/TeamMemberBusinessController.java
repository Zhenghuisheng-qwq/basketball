package com.zhs.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.request.team.TeamMemberUpdateReq;
import com.zhs.response.team.CandidatePlayerItemResp;
import com.zhs.response.team.TeamMemberItemResp;
import com.zhs.service.ITeamMemberBusinessService;
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

    @ApiOperation("球队成员分页")
    @GetMapping("/{teamId}/member/page")
    public R<IPage<TeamMemberItemResp>> pageMembers(@PathVariable String teamId,
                                                    @RequestParam(required = false) Integer currentPage,
                                                    @RequestParam(required = false) Integer pageSize) {
        return R.ok(teamMemberBusinessService.pageMembers(teamId, currentPage, pageSize));
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
        teamMemberBusinessService.removeMember(teamId, teamMemberId, resolveClientIp(request));
        return R.ok(null, "成员移除成功");
    }

    @ApiOperation("主动离队")
    @PostMapping("/{teamId}/leave")
    public R<Void> leave(@PathVariable String teamId, HttpServletRequest request) {
        teamMemberBusinessService.leave(teamId, resolveClientIp(request));
        return R.ok(null, "离队成功");
    }

    @ApiOperation("候选球员分页")
    @GetMapping("/{teamId}/candidate-player/page")
    public R<IPage<CandidatePlayerItemResp>> pageCandidatePlayers(@PathVariable String teamId,
                                                                   @RequestParam(required = false) Integer currentPage,
                                                                   @RequestParam(required = false) Integer pageSize,
                                                                   @RequestParam(required = false) String keyword) {
        return R.ok(teamMemberBusinessService.pageCandidatePlayers(teamId, currentPage, pageSize, keyword));
    }

    private String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}
