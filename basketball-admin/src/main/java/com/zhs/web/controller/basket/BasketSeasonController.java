package com.zhs.web.controller.basket;

import com.zhs.basket.req.SeasonCreateReq;
import com.zhs.basket.req.SeasonUpdateReq;
import com.zhs.basket.service.IBasketSeasonService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class BasketSeasonController extends BaseController {

    @Autowired
    private IBasketSeasonService seasonService;

    @PreAuthorize("@ss.hasPermi('basket:season:list')")
    @GetMapping("/basket/tournament/{tournamentId}/season/list")
    public AjaxResult listByTournament(@PathVariable Long tournamentId) {
        return success(seasonService.selectByTournamentId(tournamentId));
    }

    @PreAuthorize("@ss.hasPermi('basket:season:query')")
    @GetMapping("/basket/season/{seasonId}")
    public AjaxResult getInfo(@PathVariable Long seasonId) {
        return success(seasonService.selectById(seasonId));
    }

    @PreAuthorize("@ss.hasPermi('basket:season:add')")
    @Log(title = "赛季管理", businessType = BusinessType.INSERT)
    @PostMapping("/basket/tournament/{tournamentId}/season")
    public AjaxResult add(@PathVariable Long tournamentId, @RequestBody SeasonCreateReq req) {
        return success(seasonService.create(tournamentId, req));
    }

    @PreAuthorize("@ss.hasPermi('basket:season:edit')")
    @Log(title = "赛季管理", businessType = BusinessType.UPDATE)
    @PutMapping("/basket/season/{seasonId}")
    public AjaxResult edit(@PathVariable Long seasonId, @RequestBody SeasonUpdateReq req) {
        seasonService.update(seasonId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:season:publish')")
    @Log(title = "赛季管理", businessType = BusinessType.UPDATE)
    @PostMapping("/basket/season/{seasonId}/publish")
    public AjaxResult publish(@PathVariable Long seasonId) {
        seasonService.publish(seasonId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:season:openRegistration')")
    @Log(title = "赛季管理", businessType = BusinessType.UPDATE)
    @PostMapping("/basket/season/{seasonId}/open-registration")
    public AjaxResult openRegistration(@PathVariable Long seasonId) {
        seasonService.openRegistration(seasonId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:season:closeRegistration')")
    @Log(title = "赛季管理", businessType = BusinessType.UPDATE)
    @PostMapping("/basket/season/{seasonId}/close-registration")
    public AjaxResult closeRegistration(@PathVariable Long seasonId) {
        seasonService.closeRegistration(seasonId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:season:start')")
    @Log(title = "赛季管理", businessType = BusinessType.UPDATE)
    @PostMapping("/basket/season/{seasonId}/start")
    public AjaxResult start(@PathVariable Long seasonId) {
        seasonService.start(seasonId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:season:finish')")
    @Log(title = "赛季管理", businessType = BusinessType.UPDATE)
    @PostMapping("/basket/season/{seasonId}/finish")
    public AjaxResult finish(@PathVariable Long seasonId) {
        seasonService.finish(seasonId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:season:remove')")
    @Log(title = "赛季管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/basket/season/{seasonId}")
    public AjaxResult remove(@PathVariable Long seasonId) {
        seasonService.delete(seasonId);
        return success();
    }
}
