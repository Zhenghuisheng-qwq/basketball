package com.zhs.controller.publicapi;

import com.zhs.common.NoLoginRequest;
import com.zhs.response.publicapi.PublicPlayerRankItemVo;
import com.zhs.response.publicapi.PublicSeasonItemVo;
import com.zhs.response.publicapi.PublicSeasonSummaryVo;
import com.zhs.response.publicapi.PublicTeamRankItemVo;
import com.zhs.service.IBasketPublicDataCenterService;
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
@RequestMapping("/basket/public/data-center")
@Api(value = "官网公开数据中心")
public class BasketPublicDataCenterController {

    @Resource
    private IBasketPublicDataCenterService publicDataCenterService;

    @ApiOperation("公开赛季列表")
    @GetMapping("/seasons")
    public R<List<PublicSeasonItemVo>> listSeasons() {
        return R.ok(publicDataCenterService.listSeasons());
    }

    @ApiOperation("球队排行（最新 snapshot_date）")
    @GetMapping("/team-ranking")
    public R<List<PublicTeamRankItemVo>> listTeamRanking(
            @RequestParam(required = false) Long seasonId,
            @RequestParam(required = false) String rankType,
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        return R.ok(publicDataCenterService.listTeamRanking(seasonId, rankType, limit));
    }

    @ApiOperation("球员排行（最新 snapshot_date）")
    @GetMapping("/player-ranking")
    public R<List<PublicPlayerRankItemVo>> listPlayerRanking(
            @RequestParam(required = false) Long seasonId,
            @RequestParam(required = false) String rankType,
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        return R.ok(publicDataCenterService.listPlayerRanking(seasonId, rankType, limit));
    }

    @ApiOperation("赛季汇总")
    @GetMapping("/season-summary")
    public R<PublicSeasonSummaryVo> getSeasonSummary(@RequestParam(required = false) Long seasonId) {
        return R.ok(publicDataCenterService.getSeasonSummary(seasonId));
    }
}
