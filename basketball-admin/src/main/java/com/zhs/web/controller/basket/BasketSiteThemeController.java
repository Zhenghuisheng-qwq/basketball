package com.zhs.web.controller.basket;

import com.zhs.basket.constant.BasketContentPerms;
import com.zhs.basket.req.SiteThemePublishReq;
import com.zhs.basket.req.SiteThemeSaveReq;
import com.zhs.basket.service.IBasketSiteThemeService;
import com.zhs.basket.service.v4.IBasketV4AdminOptionsService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/basket/site-theme")
public class BasketSiteThemeController extends BaseController {

    @Autowired
    private IBasketSiteThemeService themeService;

    @Autowired
    private IBasketV4AdminOptionsService optionsService;

    @PreAuthorize("@ss.hasAnyPermi('" + BasketContentPerms.SITE_THEME_LIST + "," + BasketContentPerms.SITE_THEME_QUERY + "')")
    @GetMapping("/list")
    public AjaxResult list() {
        return success(themeService.list());
    }

    @PreAuthorize("@ss.hasAnyPermi('" + BasketContentPerms.SITE_THEME_LIST + "," + BasketContentPerms.SITE_THEME_QUERY + "," + BasketContentPerms.SITE_THEME_PUBLISH + "')")
    @GetMapping("/season-options")
    public AjaxResult seasonOptions() {
        return success(optionsService.selectSeasonOptions());
    }

    @PreAuthorize("@ss.hasAnyPermi('" + BasketContentPerms.SITE_THEME_QUERY + "," + BasketContentPerms.SITE_THEME_PUBLISH + "')")
    @GetMapping("/impact")
    public AjaxResult impact(@RequestParam Long themeId,
                             @RequestParam String scopeType,
                             @RequestParam(required = false) Long seasonId) {
        return success(themeService.impact(themeId, scopeType, seasonId));
    }

    @PreAuthorize("@ss.hasAnyPermi('" + BasketContentPerms.SITE_THEME_QUERY + "," + BasketContentPerms.SITE_THEME_LIST + "')")
    @GetMapping("/{id}")
    public AjaxResult get(@PathVariable Long id) {
        return success(themeService.get(id));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.SITE_THEME_EDIT + "')")
    @Log(title = "站点主题", businessType = BusinessType.UPDATE)
    @PostMapping("/save")
    public AjaxResult save(@RequestBody SiteThemeSaveReq req) {
        return success(themeService.save(req));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.SITE_THEME_PUBLISH + "')")
    @Log(title = "站点主题", businessType = BusinessType.UPDATE)
    @PostMapping("/publish")
    public AjaxResult publish(@RequestBody SiteThemePublishReq req) {
        return success(themeService.publish(req));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.SITE_THEME_EDIT + "')")
    @Log(title = "站点主题", businessType = BusinessType.INSERT)
    @PostMapping("/{id}/copy")
    public AjaxResult copy(@PathVariable Long id) {
        return success(themeService.copy(id));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.SITE_THEME_PUBLISH + "')")
    @Log(title = "站点主题", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/offline")
    public AjaxResult offline(@PathVariable Long id, @RequestParam Integer versionNo) {
        themeService.offline(id, versionNo);
        return success("主题已停用，正在使用它的页面回到基础主题");
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.SITE_THEME_PUBLISH + "')")
    @Log(title = "站点主题", businessType = BusinessType.DELETE)
    @PostMapping("/unbind")
    public AjaxResult unbind(@RequestParam String scopeType, @RequestParam(required = false) Long seasonId) {
        themeService.unbind(scopeType, seasonId);
        return success("已恢复默认，主题本身还在");
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.SITE_THEME_EDIT + "')")
    @Log(title = "站点主题", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        themeService.remove(id);
        return success();
    }
}
