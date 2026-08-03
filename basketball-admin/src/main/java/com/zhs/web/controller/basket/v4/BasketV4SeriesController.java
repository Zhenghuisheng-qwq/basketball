package com.zhs.web.controller.basket.v4;

import com.zhs.basket.constant.BasketEventV4Perms;
import com.zhs.basket.dto.v4.BasketSeriesEnabledReq;
import com.zhs.basket.service.v4.IBasketV4SeriesService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/v4/series")
public class BasketV4SeriesController extends BaseController {

    @Autowired
    private IBasketV4SeriesService seriesService;

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.SERIES_LIST + "')")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(required = false) Integer enabled) {
        return success(seriesService.selectList(enabled));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.SERIES_QUERY + "')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(seriesService.selectById(id));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.SERIES_EDIT + "')")
    @Log(title = "V4 Series", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/enabled")
    public AjaxResult updateEnabled(@PathVariable Long id, @RequestBody BasketSeriesEnabledReq req) {
        seriesService.updateEnabled(id, req.getEnabled());
        return success();
    }
}
