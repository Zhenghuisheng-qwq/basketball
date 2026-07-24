package com.zhs.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.request.team.TeamJoinApplicationCreateReq;
import com.zhs.request.team.TeamJoinApplicationProcessReq;
import com.zhs.response.team.TeamJoinApplicationItemResp;
import com.zhs.service.ITeamJoinApplicationBusinessService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(value = "球队加入申请")
@Slf4j
public class TeamJoinApplicationBusinessController {

    @Resource
    private ITeamJoinApplicationBusinessService teamJoinApplicationBusinessService;

    @ApiOperation("提交加入球队申请")
    @PostMapping("/basket/team/{teamId}/join-application")
    public R<TeamJoinApplicationItemResp> createJoinApplication(@PathVariable String teamId,
                                                                @RequestBody TeamJoinApplicationCreateReq req) {
        return R.ok(teamJoinApplicationBusinessService.createJoinApplication(teamId, req), "申请提交成功");
    }

    @ApiOperation("我的加入申请分页")
    @GetMapping("/basket/team/join-application/mine/page")
    public R<IPage<TeamJoinApplicationItemResp>> pageMyApplications(@RequestParam(required = false) Integer currentPage,
                                                                    @RequestParam(required = false) Integer pageSize,
                                                                    @RequestParam(required = false) Integer status) {
        return R.ok(teamJoinApplicationBusinessService.pageMyApplications(currentPage, pageSize, status));
    }

    @ApiOperation("取消加入申请")
    @PutMapping("/basket/team/join-application/{applicationId}/cancel")
    public R<Void> cancel(@PathVariable String applicationId) {
        teamJoinApplicationBusinessService.cancel(applicationId);
        return R.ok(null, "申请已取消");
    }

    @ApiOperation("球队加入申请分页")
    @GetMapping("/basket/team/{teamId}/join-application/page")
    public R<IPage<TeamJoinApplicationItemResp>> pageTeamApplications(@PathVariable String teamId,
                                                                      @RequestParam(required = false) Integer currentPage,
                                                                      @RequestParam(required = false) Integer pageSize,
                                                                      @RequestParam(required = false) Integer status) {
        return R.ok(teamJoinApplicationBusinessService.pageTeamApplications(teamId, currentPage, pageSize, status));
    }

    @ApiOperation("处理加入申请")
    @PutMapping("/basket/team/join-application/{applicationId}/process")
    public R<TeamJoinApplicationItemResp> process(@PathVariable String applicationId,
                                                  @RequestBody TeamJoinApplicationProcessReq req) {
        return R.ok(teamJoinApplicationBusinessService.process(applicationId, req), "申请处理成功");
    }
}
