package com.zhs.web.controller.basket;

import com.zhs.basket.query.TeamMembershipChangeQuery;
import com.zhs.basket.req.TeamMembershipChangeRejectReq;
import com.zhs.basket.service.IBasketTeamMembershipChangeService;
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
@RequestMapping("/basket/teamMembershipChange")
public class BasketTeamMembershipChangeController extends BaseController {

    @Autowired
    private IBasketTeamMembershipChangeService membershipChangeService;

    @PreAuthorize("@ss.hasPermi('basket:teamMembershipChange:list')")
    @GetMapping("/list")
    public TableDataInfo list(TeamMembershipChangeQuery query) {
        startPage();
        return getDataTable(membershipChangeService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('basket:teamMembershipChange:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(membershipChangeService.selectDetail(id));
    }

    @PreAuthorize("@ss.hasPermi('basket:teamMembershipChange:audit')")
    @Log(title = "球队成员变更审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/approve")
    public AjaxResult approve(@PathVariable Long id, @RequestBody(required = false) java.util.Map<String, String> body) {
        String remark = body == null ? null : body.get("auditRemark");
        membershipChangeService.approve(id, remark);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:teamMembershipChange:audit')")
    @Log(title = "球队成员变更审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/reject")
    public AjaxResult reject(@PathVariable Long id, @RequestBody TeamMembershipChangeRejectReq req) {
        membershipChangeService.reject(id, req);
        return success();
    }
}
