package com.zhs.web.controller.basket;

import com.zhs.basket.query.SeasonRosterAdminQuery;
import com.zhs.basket.service.IBasketSeasonRosterAdminService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/seasonRoster")
public class BasketSeasonRosterAdminController extends BaseController {

    @Autowired
    private IBasketSeasonRosterAdminService seasonRosterAdminService;

    @PreAuthorize("@ss.hasPermi('basket:seasonRoster:list')")
    @GetMapping("/list")
    public TableDataInfo list(SeasonRosterAdminQuery query) { startPage(); return getDataTable(seasonRosterAdminService.selectList(query)); }
    @PreAuthorize("@ss.hasPermi('basket:seasonRoster:query')")
    @GetMapping("/change-history")
    public AjaxResult changeHistory(@RequestParam Long playerId, @RequestParam Long seasonId) { return success(seasonRosterAdminService.selectChangeHistory(playerId, seasonId)); }
}
