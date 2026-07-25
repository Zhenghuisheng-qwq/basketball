package com.zhs.web.controller.basket;

import com.zhs.basket.query.RosterOperationLogQuery;
import com.zhs.basket.req.RosterOperationForceRemoveReq;
import com.zhs.basket.req.RosterOperationUpdateRoleReq;
import com.zhs.basket.service.IBasketRosterOperationService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/rosterOperation")
public class BasketRosterOperationController extends BaseController {

    @Autowired
    private IBasketRosterOperationService rosterOperationService;

    @PreAuthorize("@ss.hasPermi('basket:rosterOperation:list')")
    @GetMapping("/log/list")
    public TableDataInfo logs(RosterOperationLogQuery query) { startPage(); return getDataTable(rosterOperationService.selectLogs(query)); }
    @PreAuthorize("@ss.hasPermi('basket:rosterOperation:edit')")
    @Log(title = "阵容特殊操作", businessType = BusinessType.UPDATE)
    @PostMapping("/force-remove")
    public AjaxResult forceRemove(@RequestBody RosterOperationForceRemoveReq req) { rosterOperationService.forceRemove(req); return success(); }
    @PreAuthorize("@ss.hasPermi('basket:rosterOperation:edit')")
    @Log(title = "阵容特殊操作", businessType = BusinessType.UPDATE)
    @PostMapping("/update-role")
    public AjaxResult updateRole(@RequestBody RosterOperationUpdateRoleReq req) { rosterOperationService.updateRole(req); return success(); }
}
