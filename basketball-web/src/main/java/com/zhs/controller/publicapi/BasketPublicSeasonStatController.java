package com.zhs.controller.publicapi;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.common.NoLoginRequest;
import com.zhs.response.publicapi.PublicPlayerSeasonStatVo;
import com.zhs.response.publicapi.PublicSeasonSummaryVo;
import com.zhs.response.publicapi.PublicTeamSeasonStatVo;
import com.zhs.service.IBasketPublicSeasonStatService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@NoLoginRequest
@RestController
@RequestMapping("/basket/public")
@Api(value = "官网公开赛季统计")
public class BasketPublicSeasonStatController {

    @Resource
    private IBasketPublicSeasonStatService publicSeasonStatService;

    @ApiOperation("球队赛季统计分页（排序字段白名单）")
    @GetMapping("/season/{seasonId}/team-stat/page")
    public R<IPage<PublicTeamSeasonStatVo>> pageTeamStat(
            @PathVariable Long seasonId,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder) {
        return R.ok(publicSeasonStatService.pageTeamStat(seasonId, currentPage, pageSize, stageId, sortField, sortOrder));
    }

    @ApiOperation("球员赛季统计分页（排序字段白名单）")
    @GetMapping("/season/{seasonId}/player-stat/page")
    public R<IPage<PublicPlayerSeasonStatVo>> pagePlayerStat(
            @PathVariable Long seasonId,
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long stageId,
            @RequestParam(required = false) Long teamId,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false, defaultValue = "desc") String sortOrder) {
        return R.ok(publicSeasonStatService.pagePlayerStat(seasonId, currentPage, pageSize, stageId, teamId, sortField, sortOrder));
    }

    @ApiOperation("赛季汇总")
    @GetMapping("/season/{seasonId}/summary")
    public R<PublicSeasonSummaryVo> getSummary(@PathVariable Long seasonId) {
        return R.ok(publicSeasonStatService.getSummary(seasonId));
    }
}
