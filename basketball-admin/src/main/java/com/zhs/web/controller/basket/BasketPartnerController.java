package com.zhs.web.controller.basket;

import com.zhs.basket.constant.BasketContentPerms;
import com.zhs.basket.req.PartnerAddReq;
import com.zhs.basket.req.PartnerEditReq;
import com.zhs.basket.req.PartnerQueryReq;
import com.zhs.basket.service.IBasketPartnerService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/partner")
public class BasketPartnerController extends BaseController {

    @Autowired
    private IBasketPartnerService partnerService;

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.PARTNER_LIST + "')")
    @GetMapping("/list")
    public TableDataInfo list(PartnerQueryReq query) {
        startPage();
        return getDataTable(partnerService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.PARTNER_QUERY + "')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(partnerService.selectById(id));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.PARTNER_ADD + "')")
    @Log(title = "合作伙伴", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PartnerAddReq req) {
        return success(partnerService.add(req));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.PARTNER_EDIT + "')")
    @Log(title = "合作伙伴", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @RequestBody PartnerEditReq req) {
        partnerService.edit(id, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.PARTNER_PUBLISH + "')")
    @Log(title = "合作伙伴", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/publish")
    public AjaxResult publish(@PathVariable Long id) {
        partnerService.publish(id);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.PARTNER_OFFLINE + "')")
    @Log(title = "合作伙伴", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/offline")
    public AjaxResult offline(@PathVariable Long id) {
        partnerService.offline(id);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.PARTNER_REMOVE + "')")
    @Log(title = "合作伙伴", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        partnerService.remove(id);
        return success();
    }
}
