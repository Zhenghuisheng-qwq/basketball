package com.zhs.web.controller.basket;

import com.zhs.basket.constant.BasketSettlementPerms;
import com.zhs.basket.req.HonorGenerationConfirmReq;
import com.zhs.basket.service.IBasketHonorGenerationService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/honor-generation")
public class BasketHonorGenerationController extends BaseController {

    @Autowired
    private IBasketHonorGenerationService honorGenerationService;

    @PreAuthorize("@ss.hasPermi('" + BasketSettlementPerms.DATA_SETTLEMENT_QUERY + "')")
    @GetMapping("/preview/{seasonId}")
    public AjaxResult preview(@PathVariable Long seasonId) {
        return success(honorGenerationService.preview(seasonId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketSettlementPerms.DATA_SETTLEMENT_CONFIRM + "')")
    @Log(title = "荣誉自动生成", businessType = BusinessType.INSERT)
    @PostMapping("/confirm")
    public AjaxResult confirm(@RequestBody HonorGenerationConfirmReq req) {
        return success(honorGenerationService.confirm(req));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketSettlementPerms.DATA_SETTLEMENT_CONFIRM + "')")
    @Log(title = "荣誉自动生成", businessType = BusinessType.UPDATE)
    @PostMapping("/batch/{batchId}/revoke")
    public AjaxResult revoke(@PathVariable Long batchId) {
        honorGenerationService.revokeBatch(batchId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketSettlementPerms.DATA_SETTLEMENT_QUERY + "')")
    @GetMapping("/batch/list/{seasonId}")
    public AjaxResult listBatches(@PathVariable Long seasonId) {
        return success(honorGenerationService.listBatches(seasonId));
    }
}
