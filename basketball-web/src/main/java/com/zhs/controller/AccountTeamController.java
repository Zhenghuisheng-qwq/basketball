package com.zhs.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.request.team.TeamCreateReq;
import com.zhs.request.team.TeamLogoBindReq;
import com.zhs.request.team.TeamUpdateReq;
import com.zhs.response.team.TeamDetailResp;
import com.zhs.response.team.TeamSummaryResp;
import com.zhs.service.IAccountTeamService;
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

    @ApiOperation("我的球队列表")
    @GetMapping("/mine")
    public R<IPage<TeamSummaryResp>> pageMine(@RequestParam(required = false) Integer currentPage,
                                              @RequestParam(required = false) Integer pageSize) {
        return R.ok(accountTeamService.pageMine(currentPage, pageSize));
    }

    @ApiOperation("可加入球队分页")
    @GetMapping("/joinable/page")
    public R<IPage<TeamSummaryResp>> pageJoinable(@RequestParam(required = false) Integer currentPage,
                                                  @RequestParam(required = false) Integer pageSize,
                                                  @RequestParam(required = false) String keyword) {
        return R.ok(accountTeamService.pageJoinable(currentPage, pageSize, keyword));
    }

    @ApiOperation("创建球队")
    @PostMapping("/create")
    public R<TeamDetailResp> create(@RequestBody TeamCreateReq req, HttpServletRequest request) {
        return R.ok(accountTeamService.create(req, resolveClientIp(request)), "球队创建成功");
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
        return R.ok(accountTeamService.update(teamId, req, resolveClientIp(request)), "球队更新成功");
    }

    @ApiOperation("绑定球队 Logo")
    @PostMapping("/{teamId}/logo")
    public R<TeamDetailResp> bindLogo(@PathVariable String teamId,
                                      @RequestBody TeamLogoBindReq req,
                                      HttpServletRequest request) {
        return R.ok(accountTeamService.bindLogo(teamId, req, resolveClientIp(request)), "Logo 绑定成功");
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
