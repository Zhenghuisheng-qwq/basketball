package com.zhs.controller.publicapi;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.common.NoLoginRequest;
import com.zhs.response.publicapi.PublicHallOverviewResp;
import com.zhs.response.publicapi.PublicHonorDetailResp;
import com.zhs.response.publicapi.PublicHonorItemResp;
import com.zhs.service.IBasketPublicHallOfFameService;
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
@RequestMapping("/basket/public/hall-of-fame")
@Api(value = "官网公开名人堂")
public class BasketPublicHallOfFameController {

    @Resource
    private IBasketPublicHallOfFameService hallOfFameService;

    @ApiOperation("名人堂概览")
    @GetMapping("/overview")
    public R<PublicHallOverviewResp> getOverview() {
        return R.ok(hallOfFameService.getOverview());
    }

    @ApiOperation("赛事荣誉分页")
    @GetMapping("/tournament/page")
    public R<IPage<PublicHonorItemResp>> pageTournamentHonors(
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long tournamentId,
            @RequestParam(required = false) Long seasonId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder) {
        return R.ok(hallOfFameService.pageTournamentHonors(
                currentPage, pageSize, tournamentId, seasonId, keyword, sortField, sortOrder));
    }

    @ApiOperation("球员荣誉分页")
    @GetMapping("/player/page")
    public R<IPage<PublicHonorItemResp>> pagePlayerHonors(
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long tournamentId,
            @RequestParam(required = false) Long seasonId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder) {
        return R.ok(hallOfFameService.pagePlayerHonors(
                currentPage, pageSize, tournamentId, seasonId, keyword, sortField, sortOrder));
    }

    @ApiOperation("球队荣誉分页")
    @GetMapping("/team/page")
    public R<IPage<PublicHonorItemResp>> pageTeamHonors(
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Long tournamentId,
            @RequestParam(required = false) Long seasonId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String sortField,
            @RequestParam(required = false) String sortOrder) {
        return R.ok(hallOfFameService.pageTeamHonors(
                currentPage, pageSize, tournamentId, seasonId, keyword, sortField, sortOrder));
    }

    @ApiOperation("荣誉详情（含冠军阵容）")
    @GetMapping("/{honorId}")
    public R<PublicHonorDetailResp> getHonorDetail(@PathVariable Long honorId) {
        return R.ok(hallOfFameService.getHonorDetail(honorId));
    }
}
