package com.zhs.web.controller.basket;

import com.zhs.basket.constant.BasketSettlementPerms;
import com.zhs.basket.query.RankingQuery;
import com.zhs.basket.service.IBasketDataSettlementService;
import com.zhs.basket.service.IBasketRankingService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 球队 / 球员排行榜
 */
@RestController
@RequestMapping("/basket/ranking")
public class BasketRankingController extends BaseController {

    @Autowired
    private IBasketRankingService rankingService;

    @Autowired
    private IBasketDataSettlementService settlementService;

    @PreAuthorize("@ss.hasPermi('" + BasketSettlementPerms.RANKING_LIST + "')")
    @GetMapping("/team/list")
    public TableDataInfo teamList(RankingQuery query) {
        startPage();
        return getDataTable(rankingService.selectTeamLatest(query));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketSettlementPerms.RANKING_LIST + "')")
    @GetMapping("/player/list")
    public TableDataInfo playerList(RankingQuery query) {
        startPage();
        return getDataTable(rankingService.selectPlayerLatest(query));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketSettlementPerms.RANKING_REBUILD + "')")
    @Log(title = "排行榜", businessType = BusinessType.UPDATE)
    @PostMapping("/rebuild/season/{seasonId}")
    public AjaxResult rebuildSeason(@PathVariable Long seasonId) {
        return success(settlementService.rebuildSeason(seasonId));
    }
}
