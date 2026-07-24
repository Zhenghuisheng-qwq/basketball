package com.zhs.controller.publicapi;

import com.zhs.common.NoLoginRequest;
import com.zhs.response.publicapi.PublicHomeVo;
import com.zhs.response.publicapi.PublicMatchItemVo;
import com.zhs.response.publicapi.PublicPlayerRankItemVo;
import com.zhs.response.publicapi.PublicSeasonSummaryVo;
import com.zhs.response.publicapi.PublicTeamRankItemVo;
import com.zhs.service.IBasketPublicHomeService;
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
@RequestMapping("/basket/public/home")
@Api(value = "官网公开首页")
public class BasketPublicHomeController {

    @Resource
    private IBasketPublicHomeService publicHomeService;

    @ApiOperation("首页聚合")
    @GetMapping
    public R<PublicHomeVo> getHome() {
        return R.ok(publicHomeService.getHome());
    }

    @ApiOperation("近期赛果")
    @GetMapping("/recent-match")
    public R<List<PublicMatchItemVo>> listRecentMatches(
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        return R.ok(publicHomeService.listRecentMatches(limit));
    }

    @ApiOperation("球队排行（当前公开赛季 WIN_RATE，非热度）")
    @GetMapping("/team-ranking")
    public R<List<PublicTeamRankItemVo>> listTeamRanking(
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        return R.ok(publicHomeService.listTeamRanking(limit));
    }

    @ApiOperation("球员排行")
    @GetMapping("/player-ranking")
    public R<List<PublicPlayerRankItemVo>> listPlayerRanking(
            @RequestParam(required = false, defaultValue = "10") Integer limit) {
        return R.ok(publicHomeService.listPlayerRanking(limit));
    }

    @ApiOperation("统计摘要")
    @GetMapping("/stat-summary")
    public R<PublicSeasonSummaryVo> getStatSummary() {
        return R.ok(publicHomeService.getStatSummary());
    }
}
