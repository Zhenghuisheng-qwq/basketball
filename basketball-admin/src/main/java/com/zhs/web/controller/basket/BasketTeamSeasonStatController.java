package com.zhs.web.controller.basket;

import com.zhs.basket.constant.BasketSettlementPerms;
import com.zhs.basket.query.TeamSeasonStatQuery;
import com.zhs.basket.service.IBasketDataSettlementService;
import com.zhs.basket.service.IBasketTeamSeasonStatService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 球队赛季统计
 */
@RestController
@RequestMapping("/basket/team-season-stat")
public class BasketTeamSeasonStatController extends BaseController {

    @Autowired
    private IBasketTeamSeasonStatService teamSeasonStatService;

    @Autowired
    private IBasketDataSettlementService settlementService;

    @PreAuthorize("@ss.hasPermi('" + BasketSettlementPerms.TEAM_SEASON_STAT_LIST + "')")
    @GetMapping("/list")
    public TableDataInfo list(TeamSeasonStatQuery query) {
        startPage();
        return getDataTable(teamSeasonStatService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketSettlementPerms.TEAM_SEASON_STAT_REBUILD + "')")
    @Log(title = "球队赛季统计", businessType = BusinessType.UPDATE)
    @PostMapping("/rebuild/season/{seasonId}")
    public AjaxResult rebuildSeason(@PathVariable Long seasonId) {
        return success(settlementService.rebuildSeason(seasonId));
    }
}
