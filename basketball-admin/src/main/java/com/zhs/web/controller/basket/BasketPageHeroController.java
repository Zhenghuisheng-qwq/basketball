package com.zhs.web.controller.basket;

import com.zhs.basket.constant.BasketContentPerms;
import com.zhs.basket.constant.BasketEventV4Perms;
import com.zhs.basket.req.PageHeroSaveReq;
import com.zhs.basket.service.IBasketPageHeroService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/basket/page-hero")
public class BasketPageHeroController extends BaseController {

    @Autowired
    private IBasketPageHeroService pageHeroService;

    @PreAuthorize("@ss.hasAnyPermi('" + BasketContentPerms.PAGE_HERO_LIST + "," + BasketContentPerms.PAGE_HERO_QUERY + "')")
    @GetMapping("/generics")
    public AjaxResult generics() {
        return success(pageHeroService.listGeneric());
    }

    @PreAuthorize("@ss.hasAnyPermi('" + BasketContentPerms.PAGE_HERO_QUERY + "," + BasketEventV4Perms.SEASON_QUERY + "')")
    @GetMapping
    public AjaxResult get(@RequestParam String pageKey, @RequestParam(required = false) Long seasonId) {
        return success(pageHeroService.get(pageKey, seasonId));
    }

    @PreAuthorize("@ss.hasAnyPermi('" + BasketContentPerms.PAGE_HERO_EDIT + "," + BasketEventV4Perms.SEASON_EDIT + "')")
    @Log(title = "页面头图", businessType = BusinessType.UPDATE)
    @PostMapping("/save")
    public AjaxResult save(@RequestBody PageHeroSaveReq req) {
        return success(pageHeroService.save(req));
    }

    @PreAuthorize("@ss.hasAnyPermi('" + BasketContentPerms.PAGE_HERO_PUBLISH + "," + BasketEventV4Perms.SEASON_PUBLISH + "')")
    @Log(title = "页面头图", businessType = BusinessType.UPDATE)
    @PostMapping("/publish")
    public AjaxResult publish(@RequestBody PageHeroSaveReq req) {
        return success(pageHeroService.publish(req));
    }
}
