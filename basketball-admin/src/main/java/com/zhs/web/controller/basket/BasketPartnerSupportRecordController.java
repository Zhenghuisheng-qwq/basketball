package com.zhs.web.controller.basket;

import com.zhs.basket.constant.BasketContentPerms;
import com.zhs.basket.req.PartnerSupportAddReq;
import com.zhs.basket.req.PartnerSupportEditReq;
import com.zhs.basket.req.PartnerSupportQueryReq;
import com.zhs.basket.service.IBasketPartnerSupportRecordService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/partner-support")
public class BasketPartnerSupportRecordController extends BaseController {

    @Autowired
    private IBasketPartnerSupportRecordService supportRecordService;

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.PARTNER_SUPPORT_LIST + "')")
    @GetMapping("/list")
    public TableDataInfo list(PartnerSupportQueryReq query) {
        startPage();
        return getDataTable(supportRecordService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.PARTNER_SUPPORT_QUERY + "')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(supportRecordService.selectById(id));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.PARTNER_SUPPORT_ADD + "')")
    @Log(title = "合作伙伴支持记录", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody PartnerSupportAddReq req) {
        return success(supportRecordService.add(req));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.PARTNER_SUPPORT_EDIT + "')")
    @Log(title = "合作伙伴支持记录", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @RequestBody PartnerSupportEditReq req) {
        supportRecordService.edit(id, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.PARTNER_SUPPORT_STATUS + "')")
    @Log(title = "合作伙伴支持记录", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/status")
    public AjaxResult updateStatus(@PathVariable Long id, @RequestParam("publishStatus") Integer publishStatus) {
        if (publishStatus != null && publishStatus == 1) {
            supportRecordService.enable(id);
        } else {
            supportRecordService.disable(id);
        }
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.PARTNER_SUPPORT_REMOVE + "')")
    @Log(title = "合作伙伴支持记录", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids) {
        for (Long id : ids) {
            supportRecordService.remove(id);
        }
        return success();
    }
}
