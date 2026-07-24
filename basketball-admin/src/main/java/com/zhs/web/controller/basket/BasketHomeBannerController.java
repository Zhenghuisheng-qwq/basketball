package com.zhs.web.controller.basket;

import com.zhs.basket.constant.BasketContentPerms;
import com.zhs.basket.req.HomeBannerAddReq;
import com.zhs.basket.req.HomeBannerEditReq;
import com.zhs.basket.req.HomeBannerQueryReq;
import com.zhs.basket.service.IBasketHomeBannerService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping({"/basket/home-banner", "/basket/home-content"})
public class BasketHomeBannerController extends BaseController {

    @Autowired
    private IBasketHomeBannerService homeBannerService;

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.HOME_CONTENT_LIST + "')")
    @GetMapping("/list")
    public TableDataInfo list(HomeBannerQueryReq query) {
        startPage();
        return getDataTable(homeBannerService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.HOME_CONTENT_QUERY + "')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(homeBannerService.selectById(id));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.HOME_CONTENT_ADD + "')")
    @Log(title = "首页横幅", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody HomeBannerAddReq req) {
        return success(homeBannerService.add(req));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.HOME_CONTENT_EDIT + "')")
    @Log(title = "首页横幅", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @RequestBody HomeBannerEditReq req) {
        homeBannerService.edit(id, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.HOME_CONTENT_PUBLISH + "')")
    @Log(title = "首页横幅", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/publish")
    public AjaxResult publish(@PathVariable Long id) {
        homeBannerService.publish(id);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.HOME_CONTENT_OFFLINE + "')")
    @Log(title = "首页横幅", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/offline")
    public AjaxResult offline(@PathVariable Long id) {
        homeBannerService.offline(id);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.HOME_CONTENT_REMOVE + "')")
    @Log(title = "首页横幅", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        homeBannerService.remove(id);
        return success();
    }
}
