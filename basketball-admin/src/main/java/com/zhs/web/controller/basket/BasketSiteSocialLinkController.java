package com.zhs.web.controller.basket;

import com.zhs.basket.constant.BasketContentPerms;
import com.zhs.basket.req.SiteSocialLinkEditReq;
import com.zhs.basket.service.IBasketSiteSocialLinkService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/site/social-links")
public class BasketSiteSocialLinkController extends BaseController {

    @Autowired
    private IBasketSiteSocialLinkService socialLinkService;

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.SOCIAL_LINK_LIST + "')")
    @GetMapping
    public AjaxResult list() {
        return success(socialLinkService.selectList());
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.SOCIAL_LINK_EDIT + "')")
    @Log(title = "社群与媒体", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @RequestBody SiteSocialLinkEditReq req) {
        socialLinkService.edit(id, req);
        return success();
    }
}
