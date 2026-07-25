package com.zhs.web.controller.basket;

import com.zhs.basket.query.PlayerPlatformChangeQuery;
import com.zhs.basket.req.PlayerPlatformChangeApproveReq;
import com.zhs.basket.req.PlayerPlatformChangeRejectReq;
import com.zhs.basket.service.IBasketPlayerPlatformChangeService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/playerPlatformChange")
public class BasketPlayerPlatformChangeController extends BaseController {

    @Autowired
    private IBasketPlayerPlatformChangeService platformChangeService;

    @PreAuthorize("@ss.hasPermi('basket:playerPlatformChange:list')")
    @GetMapping("/list")
    public TableDataInfo list(PlayerPlatformChangeQuery query) {
        startPage();
        return getDataTable(platformChangeService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('basket:playerPlatformChange:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(platformChangeService.selectDetail(id));
    }

    @PreAuthorize("@ss.hasPermi('basket:playerPlatformChange:audit')")
    @Log(title = "平台游戏ID变更审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/approve")
    public AjaxResult approve(@PathVariable Long id, @RequestBody PlayerPlatformChangeApproveReq req) {
        platformChangeService.approve(id, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:playerPlatformChange:audit')")
    @Log(title = "平台游戏ID变更审核", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/reject")
    public AjaxResult reject(@PathVariable Long id, @RequestBody PlayerPlatformChangeRejectReq req) {
        platformChangeService.reject(id, req);
        return success();
    }
}
