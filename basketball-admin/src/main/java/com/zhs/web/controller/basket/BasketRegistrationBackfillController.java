package com.zhs.web.controller.basket;

import com.zhs.basket.service.IBasketRegistrationBackfillService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.enums.BusinessType;
import com.zhs.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/basket/registrationBackfill")
public class BasketRegistrationBackfillController extends BaseController {

    @Autowired
    private IBasketRegistrationBackfillService registrationBackfillService;

    @PreAuthorize("@ss.hasPermi('basket:registrationAudit:audit')")
    @Log(title = "报名快照补偿", businessType = BusinessType.UPDATE)
    @PostMapping("/backfill-approved")
    public AjaxResult backfillApproved() {
        return success(registrationBackfillService.backfillApprovedRegistrations(SecurityUtils.getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('basket:registrationAudit:audit')")
    @Log(title = "报名快照补偿", businessType = BusinessType.UPDATE)
    @PostMapping("/{registrationId}/backfill")
    public AjaxResult backfillOne(@PathVariable Long registrationId) {
        return success(registrationBackfillService.backfillRegistration(registrationId, SecurityUtils.getUserId()));
    }
}
