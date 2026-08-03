package com.zhs.web.controller.basket.v4;

import com.zhs.basket.constant.BasketEventV4Perms;
import com.zhs.basket.query.v4.BasketV4StatsQuery;
import com.zhs.basket.service.v4.IBasketV4StatsQueryService;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/basket/v4/stats")
public class BasketV4StatsController extends BaseController {

    @Autowired
    private IBasketV4StatsQueryService statsQueryService;

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.V4_STATS_QUERY + "')")
    @GetMapping("/standing")
    public AjaxResult standing(BasketV4StatsQuery query) {
        return success(statsQueryService.listStanding(query));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.V4_STATS_QUERY + "')")
    @GetMapping("/team-stat")
    public AjaxResult teamStat(BasketV4StatsQuery query) {
        return success(statsQueryService.listTeamStat(query));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.V4_STATS_QUERY + "')")
    @GetMapping("/player-stat")
    public AjaxResult playerStat(BasketV4StatsQuery query) {
        return success(statsQueryService.listPlayerStat(query));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.V4_STATS_QUERY + "')")
    @GetMapping("/rank")
    public AjaxResult rank(BasketV4StatsQuery query) {
        return success(statsQueryService.listRank(query));
    }
}
