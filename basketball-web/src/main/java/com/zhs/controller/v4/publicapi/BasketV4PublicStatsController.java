package com.zhs.controller.v4.publicapi;

import com.zhs.common.NoLoginRequest;
import com.zhs.response.v4.publicapi.*;
import com.zhs.service.v4.IBasketV4PublicService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@NoLoginRequest
@RestController
@RequestMapping("/basket/v4/public/stats")
@Api(value = "V4 公开统计榜单")
public class BasketV4PublicStatsController {

    @Resource
    private IBasketV4PublicService v4PublicService;

    @ApiOperation("积分榜")
    @GetMapping("/standing")
    public R<List<V4PublicStandingItemResp>> listStanding(@RequestParam Long divisionId) {
        return R.ok(v4PublicService.listStanding(divisionId));
    }

    @ApiOperation("球队统计")
    @GetMapping("/team-stat")
    public R<List<V4PublicTeamStatItemResp>> listTeamStat(@RequestParam Long divisionId) {
        return R.ok(v4PublicService.listTeamStat(divisionId));
    }

    @ApiOperation("球员统计")
    @GetMapping("/player-stat")
    public R<List<V4PublicPlayerStatItemResp>> listPlayerStat(@RequestParam Long divisionId) {
        return R.ok(v4PublicService.listPlayerStat(divisionId));
    }

    @ApiOperation("排行榜")
    @GetMapping("/rank")
    public R<List<V4PublicRankItemResp>> listRank(
            @RequestParam Long divisionId,
            @RequestParam(required = false) String rankType) {
        return R.ok(v4PublicService.listRank(divisionId, rankType));
    }
}
