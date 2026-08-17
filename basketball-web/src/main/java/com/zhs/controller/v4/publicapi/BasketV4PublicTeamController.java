package com.zhs.controller.v4.publicapi;

import com.zhs.common.NoLoginRequest;
import com.zhs.response.v4.publicapi.V4PublicEventTeamDetailResp;
import com.zhs.response.v4.publicapi.V4PublicEventTeamItemResp;
import com.zhs.response.v4.publicapi.V4PublicTeamArchiveResp;
import com.zhs.service.v4.IBasketV4PublicService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@NoLoginRequest
@RestController
@RequestMapping("/basket/v4/public/team")
@Api(value = "V4 公开参赛队伍")
public class BasketV4PublicTeamController {

    @Resource
    private IBasketV4PublicService v4PublicService;

    @ApiOperation("参赛队伍列表")
    @GetMapping("/list")
    public R<List<V4PublicEventTeamItemResp>> listTeams(@RequestParam Long divisionId) {
        return R.ok(v4PublicService.listTeams(divisionId));
    }

    @ApiOperation("参赛队伍详情")
    @GetMapping("/{eventTeamId}")
    public R<V4PublicEventTeamDetailResp> getTeamDetail(@PathVariable Long eventTeamId) {
        return R.ok(v4PublicService.getTeamDetail(eventTeamId));
    }

    @ApiOperation("公开球队档案")
    @GetMapping("/{eventTeamId}/archive")
    public R<V4PublicTeamArchiveResp> getTeamArchive(@PathVariable Long eventTeamId) {
        return R.ok(v4PublicService.getTeamArchive(eventTeamId));
    }
}
