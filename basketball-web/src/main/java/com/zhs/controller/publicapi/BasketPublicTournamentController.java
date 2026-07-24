package com.zhs.controller.publicapi;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.common.NoLoginRequest;
import com.zhs.response.publicapi.PublicSeasonItemVo;
import com.zhs.response.publicapi.PublicSeasonOverviewVo;
import com.zhs.response.publicapi.PublicTournamentDetailVo;
import com.zhs.response.publicapi.PublicTournamentItemVo;
import com.zhs.service.IBasketPublicTournamentService;
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
@RequestMapping("/basket/public")
@Api(value = "官网公开赛事")
public class BasketPublicTournamentController {

    @Resource
    private IBasketPublicTournamentService publicTournamentService;

    @ApiOperation("公开赛事分页")
    @GetMapping("/tournament/page")
    public R<IPage<PublicTournamentItemVo>> pageTournaments(
            @RequestParam(defaultValue = "1") Integer currentPage,
            @RequestParam(defaultValue = "10") Integer pageSize,
            @RequestParam(required = false) Integer tournamentStatus,
            @RequestParam(required = false) String keyword) {
        return R.ok(publicTournamentService.pageTournaments(currentPage, pageSize, tournamentStatus, keyword));
    }

    @ApiOperation("公开赛事详情")
    @GetMapping("/tournament/{tournamentId}")
    public R<PublicTournamentDetailVo> getTournament(@PathVariable Long tournamentId) {
        return R.ok(publicTournamentService.getTournament(tournamentId));
    }

    @ApiOperation("公开赛事下赛季列表")
    @GetMapping("/tournament/{tournamentId}/season")
    public R<List<PublicSeasonItemVo>> listSeasons(@PathVariable Long tournamentId) {
        return R.ok(publicTournamentService.listSeasons(tournamentId));
    }

    @ApiOperation("公开赛季概览")
    @GetMapping("/season/{seasonId}/overview")
    public R<PublicSeasonOverviewVo> getSeasonOverview(@PathVariable Long seasonId) {
        return R.ok(publicTournamentService.getSeasonOverview(seasonId));
    }
}
