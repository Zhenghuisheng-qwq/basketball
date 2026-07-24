package com.zhs.web.controller.basket;

import com.zhs.basket.constant.BasketSettlementPerms;
import com.zhs.basket.query.PlayerSeasonStatQuery;
import com.zhs.basket.service.IBasketDataSettlementService;
import com.zhs.basket.service.IBasketPlayerSeasonStatService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 球员赛季统计
 */
@RestController
@RequestMapping("/basket/player-season-stat")
public class BasketPlayerSeasonStatController extends BaseController {

    @Autowired
    private IBasketPlayerSeasonStatService playerSeasonStatService;

    @Autowired
    private IBasketDataSettlementService settlementService;

    @PreAuthorize("@ss.hasPermi('" + BasketSettlementPerms.PLAYER_SEASON_STAT_LIST + "')")
    @GetMapping("/list")
    public TableDataInfo list(PlayerSeasonStatQuery query) {
        startPage();
        return getDataTable(playerSeasonStatService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketSettlementPerms.PLAYER_SEASON_STAT_REBUILD + "')")
    @Log(title = "球员赛季统计", businessType = BusinessType.UPDATE)
    @PostMapping("/rebuild/season/{seasonId}")
    public AjaxResult rebuildSeason(@PathVariable Long seasonId) {
        return success(settlementService.rebuildSeason(seasonId));
    }
}
