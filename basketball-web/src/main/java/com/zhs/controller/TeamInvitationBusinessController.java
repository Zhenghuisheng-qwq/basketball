package com.zhs.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.request.team.TeamInvitationCreateReq;
import com.zhs.request.team.TeamInvitationRespondReq;
import com.zhs.response.team.TeamInvitationItemResp;
import com.zhs.service.ITeamInvitationBusinessService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(value = "球队邀请")
@Slf4j
public class TeamInvitationBusinessController {

    @Resource
    private ITeamInvitationBusinessService teamInvitationBusinessService;

    @ApiOperation("创建球队邀请")
    @PostMapping("/basket/team/{teamId}/invitation")
    public R<TeamInvitationItemResp> createInvitation(@PathVariable String teamId,
                                                      @RequestBody TeamInvitationCreateReq req) {
        return R.ok(teamInvitationBusinessService.createInvitation(teamId, req), "邀请发送成功");
    }

    @ApiOperation("球队邀请分页")
    @GetMapping("/basket/team/{teamId}/invitation/page")
    public R<IPage<TeamInvitationItemResp>> pageTeamInvitations(@PathVariable String teamId,
                                                                @RequestParam(required = false) Integer currentPage,
                                                                @RequestParam(required = false) Integer pageSize,
                                                                @RequestParam(required = false) Integer status) {
        return R.ok(teamInvitationBusinessService.pageTeamInvitations(teamId, currentPage, pageSize, status));
    }

    @ApiOperation("我的邀请分页")
    @GetMapping("/basket/team/invitation/mine/page")
    public R<IPage<TeamInvitationItemResp>> pageMyInvitations(@RequestParam(required = false) Integer currentPage,
                                                              @RequestParam(required = false) Integer pageSize,
                                                              @RequestParam(required = false) Integer status) {
        return R.ok(teamInvitationBusinessService.pageMyInvitations(currentPage, pageSize, status));
    }

    @ApiOperation("响应球队邀请")
    @PutMapping("/basket/team/invitation/{invitationId}/respond")
    public R<TeamInvitationItemResp> respond(@PathVariable String invitationId,
                                             @RequestBody TeamInvitationRespondReq req) {
        return R.ok(teamInvitationBusinessService.respond(invitationId, req), "邀请响应成功");
    }
}
