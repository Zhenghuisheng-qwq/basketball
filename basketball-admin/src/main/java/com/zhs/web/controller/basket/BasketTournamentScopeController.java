package com.zhs.web.controller.basket;

import com.zhs.basket.req.TournamentScopeGrantReq;
import com.zhs.basket.service.IBasketTournamentScopeService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/tournamentScope")
public class BasketTournamentScopeController extends BaseController {

    @Autowired
    private IBasketTournamentScopeService tournamentScopeService;

    @PreAuthorize("@ss.hasPermi('basket:tournamentScope:list')")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(required = false) Long tournamentId,
                           @RequestParam(required = false) Long adminUserId) {
        return success(tournamentScopeService.list(tournamentId, adminUserId));
    }

    @PreAuthorize("@ss.hasPermi('basket:tournamentScope:grant')")
    @Log(title = "赛事管理员授权", businessType = BusinessType.INSERT)
    @PostMapping("/grant")
    public AjaxResult grant(@RequestBody TournamentScopeGrantReq req) {
        return success(tournamentScopeService.grant(req));
    }

    @PreAuthorize("@ss.hasPermi('basket:tournamentScope:revoke')")
    @Log(title = "赛事管理员授权", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/revoke")
    public AjaxResult revoke(@PathVariable Long id) {
        tournamentScopeService.revoke(id);
        return success();
    }
}
