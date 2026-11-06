package com.zhs.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.request.team.TeamCreateReq;
import com.zhs.request.team.TeamLogoBindReq;
import com.zhs.request.team.TeamUpdateReq;
import com.zhs.response.team.JoinableTeamPageResp;
import com.zhs.response.team.TeamDetailResp;
import com.zhs.response.team.TeamSummaryResp;
import com.zhs.service.IAccountTeamService;
import com.zhs.support.ClientIpResolver;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
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
@RequestMapping("/basket/team")
@Api(value = "球队管理")
@Slf4j
public class AccountTeamController {

    @Resource
    private IAccountTeamService accountTeamService;
    @Resource
    private ClientIpResolver clientIpResolver;

    @ApiOperation("我的球队列表（scope=current|history，默认 current）")
    @GetMapping("/mine")
    public R<IPage<TeamSummaryResp>> pageMine(@RequestParam(required = false) Integer currentPage,
                                              @RequestParam(required = false) Integer pageSize,
                                              @RequestParam(required = false) String scope) {
        return R.ok(accountTeamService.pageMine(currentPage, pageSize, scope));
    }

    @ApiOperation("可加入球队分页")
    @GetMapping("/joinable/page")
    public R<JoinableTeamPageResp> pageJoinable(@RequestParam(required = false) Integer currentPage,
                                                  @RequestParam(required = false) Integer pageSize,
                                                  @RequestParam(required = false) String platformCode,
                                                  @RequestParam(required = false) String keyword) {
        return R.ok(accountTeamService.pageJoinable(currentPage, pageSize, platformCode, keyword));
    }

    @ApiOperation("可继承历史球队列表")
    @GetMapping("/inheritable/list")
    public R<java.util.List<com.zhs.response.team.InheritableTeamSummaryResp>> listInheritableTeams(
            @RequestParam(required = false) Long divisionId) {
        return R.ok(accountTeamService.listInheritableTeams(divisionId));
    }

    @ApiOperation("原队长继承历史球队到本届赛事")
    @PostMapping("/captain/grant/bind-team")
    public R<TeamDetailResp> bindGrantToTeam(@RequestBody com.zhs.request.team.CaptainGrantBindTeamReq req,
                                              HttpServletRequest request) {
        if (req == null || req.getTeamId() == null || req.getTeamId().isBlank()) {
            return R.fail("球队主键不能为空");
        }
        return R.ok(accountTeamService.bindGrantToTeam(
                req.getTeamId(), req.getDivisionId(), clientIpResolver.resolve(request)), "已继承原球队");
    }

    @ApiOperation("创建球队")
    @PostMapping("/create")
    public R<TeamDetailResp> create(@RequestBody TeamCreateReq req, HttpServletRequest request) {
        return R.ok(accountTeamService.create(req, clientIpResolver.resolve(request)), "球队创建成功");
    }

    @ApiOperation("球队详情")
    @GetMapping("/{teamId}")
    public R<TeamDetailResp> getDetail(@PathVariable String teamId) {
        return R.ok(accountTeamService.getDetail(teamId));
    }

    @ApiOperation("更新球队")
    @PutMapping("/{teamId}")
    public R<TeamDetailResp> update(@PathVariable String teamId,
                                     @RequestBody TeamUpdateReq req,
                                     HttpServletRequest request) {
        return R.ok(accountTeamService.update(teamId, req, clientIpResolver.resolve(request)), "球队更新成功");
    }

    @ApiOperation("绑定球队 Logo")
    @PostMapping("/{teamId}/logo")
    public R<TeamDetailResp> bindLogo(@PathVariable String teamId,
                                      @RequestBody TeamLogoBindReq req,
                                      HttpServletRequest request) {
        return R.ok(accountTeamService.bindLogo(teamId, req, clientIpResolver.resolve(request)), "Logo 绑定成功");
    }

    @ApiOperation("提交/重新提交球队认证")
    @PostMapping("/{teamId}/submit-certification")
    public R<TeamDetailResp> submitCertification(@PathVariable String teamId, HttpServletRequest request) {
        return R.ok(accountTeamService.submitCertification(teamId, clientIpResolver.resolve(request)), "已提交球队认证审核");
    }
}
