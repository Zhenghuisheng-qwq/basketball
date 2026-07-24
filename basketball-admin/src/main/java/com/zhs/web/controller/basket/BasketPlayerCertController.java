package com.zhs.web.controller.basket;

import com.zhs.basket.query.PlayerCertQuery;
import com.zhs.basket.req.PlayerCertApproveReq;
import com.zhs.basket.req.PlayerCertRejectReq;
import com.zhs.basket.service.IBasketPlayerCertService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/playerCert")
public class BasketPlayerCertController extends BaseController {

    @Autowired
    private IBasketPlayerCertService playerCertService;

    @PreAuthorize("@ss.hasPermi('basket:playerCert:list')")
    @GetMapping("/list")
    public TableDataInfo list(PlayerCertQuery query) {
        startPage();
        return getDataTable(playerCertService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('basket:playerCert:query')")
    @GetMapping("/{applicationId}")
    public AjaxResult getInfo(@PathVariable Long applicationId) {
        return success(playerCertService.selectDetail(applicationId));
    }

    @PreAuthorize("@ss.hasPermi('basket:playerCert:query')")
    @GetMapping("/{applicationId}/audit-record/list")
    public AjaxResult auditRecords(@PathVariable Long applicationId) {
        return success(playerCertService.selectAuditRecords(applicationId));
    }

    @PreAuthorize("@ss.hasPermi('basket:playerCert:audit')")
    @Log(title = "球员认证审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{applicationId}/start-review")
    public AjaxResult startReview(@PathVariable Long applicationId) {
        playerCertService.startReview(applicationId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:playerCert:audit')")
    @Log(title = "球员认证审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{applicationId}/approve")
    public AjaxResult approve(@PathVariable Long applicationId, @RequestBody PlayerCertApproveReq req) {
        playerCertService.approve(applicationId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:playerCert:audit')")
    @Log(title = "球员认证审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{applicationId}/reject")
    public AjaxResult reject(@PathVariable Long applicationId, @RequestBody PlayerCertRejectReq req) {
        playerCertService.reject(applicationId, req);
        return success();
    }
}
