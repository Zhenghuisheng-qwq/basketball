package com.zhs.web.controller.basket.v4;

import com.zhs.basket.constant.BasketEventV4Perms;
import com.zhs.basket.dto.v4.BasketPersonalRegistrationRejectReq;
import com.zhs.basket.query.v4.BasketPersonalRegistrationQuery;
import com.zhs.basket.service.v4.IPersonalRegistrationApplicationService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/v4/admin/personal-registrations")
public class BasketV4PersonalRegistrationController extends BaseController {

    @Autowired
    private IPersonalRegistrationApplicationService personalRegistrationApplicationService;

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.PERSONAL_REG_LIST + "')")
    @GetMapping("/list")
    public TableDataInfo list(BasketPersonalRegistrationQuery query) {
        startPage();
        return getDataTable(personalRegistrationApplicationService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.PERSONAL_REG_LIST + "')")
    @GetMapping("/{personalRegistrationId}")
    public AjaxResult getInfo(@PathVariable Long personalRegistrationId) {
        return success(personalRegistrationApplicationService.selectDetail(personalRegistrationId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.PERSONAL_REG_AUDIT + "')")
    @Log(title = "V4 PersonalRegistration", businessType = BusinessType.UPDATE)
    @PostMapping("/{personalRegistrationId}/approve")
    public AjaxResult approve(@PathVariable Long personalRegistrationId) {
        personalRegistrationApplicationService.approve(personalRegistrationId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.PERSONAL_REG_AUDIT + "')")
    @Log(title = "V4 PersonalRegistration", businessType = BusinessType.UPDATE)
    @PostMapping("/{personalRegistrationId}/reject")
    public AjaxResult reject(@PathVariable Long personalRegistrationId,
                             @RequestBody BasketPersonalRegistrationRejectReq req) {
        personalRegistrationApplicationService.reject(personalRegistrationId, req);
        return success();
    }
}
