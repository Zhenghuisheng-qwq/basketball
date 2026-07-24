package com.zhs.controller.publicapi;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.common.NoLoginRequest;
import com.zhs.response.publicapi.PublicMatchItemVo;
import com.zhs.response.publicapi.PublicPlayerDetailVo;
import com.zhs.response.publicapi.PublicPlayerRankItemVo;
import com.zhs.response.publicapi.PublicPlayerSeasonStatVo;
import com.zhs.service.IBasketPublicPlayerService;
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
@RequestMapping("/basket/public/player")
@Api(value = "官网公开球员")
public class BasketPublicPlayerController {

    @Resource
    private IBasketPublicPlayerService publicPlayerService;

    @ApiOperation("球员详情（尊重隐私）")
    @GetMapping("/{playerId}")
    public R<PublicPlayerDetailVo> getPlayer(@PathVariable Long playerId) {
        return R.ok(publicPlayerService.getPlayer(playerId));
    }

    @ApiOperation("球员赛季统计")
    @GetMapping("/{playerId}/season-stat")
    public R<List<PublicPlayerSeasonStatVo>> listSeasonStat(
            @PathVariable Long playerId,
            @RequestParam(required = false) Long seasonId) {
        return R.ok(publicPlayerService.listSeasonStat(playerId, seasonId));
    }

    @ApiOperation("球员比赛分页")
    @GetMapping("/{playerId}/match/page")
    public R<IPage<PublicMatchItemVo>> pageMatches(
            @PathVariable Long playerId,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long seasonId) {
        return R.ok(publicPlayerService.pageMatches(playerId, currentPage, pageSize, seasonId));
    }

    @ApiOperation("球员排行快照")
    @GetMapping("/{playerId}/ranking")
    public R<List<PublicPlayerRankItemVo>> listRanking(
            @PathVariable Long playerId,
            @RequestParam(required = false) Long seasonId,
            @RequestParam(required = false) String rankType) {
        return R.ok(publicPlayerService.listRanking(playerId, seasonId, rankType));
    }
}
