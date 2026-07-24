package com.zhs.controller.publicapi;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.common.NoLoginRequest;
import com.zhs.response.publicapi.PublicMatchItemVo;
import com.zhs.response.publicapi.PublicTeamCompareVo;
import com.zhs.response.publicapi.PublicTeamDetailVo;
import com.zhs.response.publicapi.PublicTeamRankItemVo;
import com.zhs.response.publicapi.PublicTeamRosterItemVo;
import com.zhs.response.publicapi.PublicTeamSeasonHistoryVo;
import com.zhs.response.publicapi.PublicTeamSeasonStatVo;
import com.zhs.service.IBasketPublicTeamService;
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
@RequestMapping("/basket/public/team")
@Api(value = "官网公开球队")
public class BasketPublicTeamController {

    @Resource
    private IBasketPublicTeamService publicTeamService;

    @ApiOperation("球队详情")
    @GetMapping("/{teamId}")
    public R<PublicTeamDetailVo> getTeam(@PathVariable Long teamId) {
        return R.ok(publicTeamService.getTeam(teamId));
    }

    @ApiOperation("球队赛季历史")
    @GetMapping("/{teamId}/season-history")
    public R<List<PublicTeamSeasonHistoryVo>> listSeasonHistory(@PathVariable Long teamId) {
        return R.ok(publicTeamService.listSeasonHistory(teamId));
    }

    @ApiOperation("球队比赛分页（已确认赛果）")
    @GetMapping("/{teamId}/match/page")
    public R<IPage<PublicMatchItemVo>> pageMatches(
            @PathVariable Long teamId,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long seasonId) {
        return R.ok(publicTeamService.pageMatches(teamId, currentPage, pageSize, seasonId));
    }

    @ApiOperation("球队公开阵容")
    @GetMapping("/{teamId}/roster")
    public R<List<PublicTeamRosterItemVo>> listRoster(@PathVariable Long teamId) {
        return R.ok(publicTeamService.listRoster(teamId));
    }

    @ApiOperation("球队赛季统计")
    @GetMapping("/{teamId}/stat")
    public R<List<PublicTeamSeasonStatVo>> listStat(
            @PathVariable Long teamId,
            @RequestParam(required = false) Long seasonId) {
        return R.ok(publicTeamService.listStat(teamId, seasonId));
    }

    @ApiOperation("球队排行快照")
    @GetMapping("/{teamId}/ranking")
    public R<List<PublicTeamRankItemVo>> listRanking(
            @PathVariable Long teamId,
            @RequestParam(required = false) Long seasonId,
            @RequestParam(required = false) String rankType) {
        return R.ok(publicTeamService.listRanking(teamId, seasonId, rankType));
    }

    @ApiOperation("球队对比")
    @GetMapping("/compare")
    public R<PublicTeamCompareVo> compare(
            @RequestParam Long leftTeamId,
            @RequestParam Long rightTeamId,
            @RequestParam(required = false) Long seasonId) {
        return R.ok(publicTeamService.compare(leftTeamId, rightTeamId, seasonId));
    }
}
