package com.zhs.web.controller.basket;

import com.zhs.basket.req.MatchLineupSaveReq;
import com.zhs.basket.service.IBasketMatchLineupService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/match")
public class BasketMatchLineupController extends BaseController {

    @Autowired
    private IBasketMatchLineupService lineupService;

    @PreAuthorize("@ss.hasPermi('basket:lineup:list')")
    @GetMapping("/{matchId}/lineup")
    public AjaxResult lineup(@PathVariable Long matchId, @RequestParam Long teamId) {
        return success(lineupService.selectLineup(matchId, teamId));
    }

    @PreAuthorize("@ss.hasPermi('basket:lineup:query')")
    @GetMapping("/{matchId}/available-player/{teamId}")
    public AjaxResult availablePlayers(@PathVariable Long matchId, @PathVariable Long teamId) {
        return success(lineupService.selectAvailablePlayers(matchId, teamId));
    }

    @PreAuthorize("@ss.hasPermi('basket:lineup:edit')")
    @Log(title = "出场名单", businessType = BusinessType.UPDATE)
    @PutMapping("/{matchId}/lineup/{teamId}")
    public AjaxResult saveLineup(@PathVariable Long matchId, @PathVariable Long teamId,
                                 @RequestBody MatchLineupSaveReq req) {
        lineupService.saveLineup(matchId, teamId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:lineup:confirm')")
    @Log(title = "出场名单", businessType = BusinessType.UPDATE)
    @PostMapping("/{matchId}/lineup/{teamId}/confirm")
    public AjaxResult confirmLineup(@PathVariable Long matchId, @PathVariable Long teamId) {
        lineupService.confirmLineup(matchId, teamId);
        return success();
    }
}
