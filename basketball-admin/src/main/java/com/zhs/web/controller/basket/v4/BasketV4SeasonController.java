package com.zhs.web.controller.basket.v4;

import com.zhs.basket.constant.BasketEventV4Perms;
import com.zhs.basket.dto.v4.BasketSeasonCreateReq;
import com.zhs.basket.dto.v4.BasketSeasonUpdateReq;
import com.zhs.basket.query.v4.BasketSeasonQuery;
import com.zhs.basket.service.v4.IBasketV4SeasonService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/v4/season")
public class BasketV4SeasonController extends BaseController {

    @Autowired
    private IBasketV4SeasonService seasonService;

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.SEASON_LIST + "')")
    @GetMapping("/list")
    public TableDataInfo list(BasketSeasonQuery query) {
        startPage();
        return getDataTable(seasonService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.SEASON_QUERY + "')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(seasonService.selectById(id));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.SEASON_ADD + "')")
    @Log(title = "V4 Season", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BasketSeasonCreateReq req) {
        return success(seasonService.create(req));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.SEASON_EDIT + "')")
    @Log(title = "V4 Season", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @RequestBody BasketSeasonUpdateReq req) {
        seasonService.update(id, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.SEASON_PUBLISH + "')")
    @Log(title = "V4 Season", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/publish")
    public AjaxResult publish(@PathVariable Long id) {
        seasonService.publish(id);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.SEASON_OFFLINE + "')")
    @Log(title = "V4 Season", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/offline")
    public AjaxResult offline(@PathVariable Long id) {
        seasonService.offline(id);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.SEASON_REMOVE + "')")
    @Log(title = "V4 Season", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        seasonService.delete(id);
        return success();
    }
}
