package com.zhs.web.controller.basket;

import com.zhs.basket.query.MemberProfileChangeQuery;
import com.zhs.basket.req.MemberProfileChangeApproveReq;
import com.zhs.basket.req.MemberProfileChangeRejectReq;
import com.zhs.basket.service.IBasketMemberProfileChangeService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/basket/memberProfileChange")
public class BasketMemberProfileChangeController extends BaseController {

    @Autowired
    private IBasketMemberProfileChangeService memberProfileChangeService;

    @PreAuthorize("@ss.hasPermi('basket:memberProfileChange:list')")
    @GetMapping("/list")
    public TableDataInfo list(MemberProfileChangeQuery query) {
        startPage();
        return getDataTable(memberProfileChangeService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('basket:memberProfileChange:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(memberProfileChangeService.selectDetail(id));
    }

    @PreAuthorize("@ss.hasPermi('basket:memberProfileChange:audit')")
    @Log(title = "用户资料变更审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/approve")
    public AjaxResult approve(@PathVariable Long id, @RequestBody(required = false) MemberProfileChangeApproveReq req) {
        memberProfileChangeService.approve(id, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:memberProfileChange:audit')")
    @Log(title = "用户资料变更审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/reject")
    public AjaxResult reject(@PathVariable Long id, @RequestBody MemberProfileChangeRejectReq req) {
        memberProfileChangeService.reject(id, req);
        return success();
    }
}
