package com.zhs.web.controller.basket;

import com.zhs.basket.query.TeamProfileChangeQuery;
import com.zhs.basket.req.TeamProfileChangeApproveReq;
import com.zhs.basket.req.TeamProfileChangeRejectReq;
import com.zhs.basket.service.IBasketTeamProfileChangeService;
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
@RequestMapping("/basket/teamProfileChange")
public class BasketTeamProfileChangeController extends BaseController {

    @Autowired
    private IBasketTeamProfileChangeService teamProfileChangeService;

    @PreAuthorize("@ss.hasPermi('basket:teamProfileChange:list')")
    @GetMapping("/list")
    public TableDataInfo list(TeamProfileChangeQuery query) {
        startPage();
        return getDataTable(teamProfileChangeService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('basket:teamProfileChange:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(teamProfileChangeService.selectDetail(id));
    }

    @PreAuthorize("@ss.hasPermi('basket:teamProfileChange:audit')")
    @Log(title = "球队资料变更审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/approve")
    public AjaxResult approve(@PathVariable Long id, @RequestBody(required = false) TeamProfileChangeApproveReq req) {
        teamProfileChangeService.approve(id, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:teamProfileChange:audit')")
    @Log(title = "球队资料变更审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/reject")
    public AjaxResult reject(@PathVariable Long id, @RequestBody TeamProfileChangeRejectReq req) {
        teamProfileChangeService.reject(id, req);
        return success();
    }
}
