package com.zhs.web.controller.basket;

import com.zhs.basket.constant.BasketSettlementPerms;
import com.zhs.basket.query.StandingQuery;
import com.zhs.basket.service.IBasketDataSettlementService;
import com.zhs.basket.service.IBasketStandingService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 积分榜
 */
@RestController
@RequestMapping("/basket/standing")
public class BasketStandingController extends BaseController {

    @Autowired
    private IBasketStandingService standingService;

    @Autowired
    private IBasketDataSettlementService settlementService;

    @PreAuthorize("@ss.hasPermi('" + BasketSettlementPerms.STANDING_LIST + "')")
    @GetMapping("/list")
    public TableDataInfo list(StandingQuery query) {
        startPage();
        return getDataTable(standingService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketSettlementPerms.STANDING_REBUILD + "')")
    @Log(title = "积分榜", businessType = BusinessType.UPDATE)
    @PostMapping("/rebuild/season/{seasonId}")
    public AjaxResult rebuildSeason(@PathVariable Long seasonId) {
        return success(settlementService.rebuildSeason(seasonId));
    }
}
