package com.zhs.web.controller.basket.v4;

import com.zhs.basket.constant.BasketEventV4Perms;
import com.zhs.basket.service.v4.IBasketV4AdminOptionsService;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/basket/v4/admin/options")
public class BasketV4AdminOptionsController extends BaseController {

    @Autowired
    private IBasketV4AdminOptionsService optionsService;

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.SEASON_LIST + "')")
    @GetMapping("/seasons")
    public AjaxResult seasons() {
        return success(optionsService.selectSeasonOptions());
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.DIVISION_LIST + "')")
    @GetMapping("/divisions")
    public AjaxResult divisions(@RequestParam Long seasonId) {
        return success(optionsService.selectDivisionOptions(seasonId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.CAPTAIN_GRANT_LIST + "')")
    @GetMapping("/teams")
    public AjaxResult teams(@RequestParam Long divisionId) {
        return success(optionsService.selectGrantableTeamOptions(divisionId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.CAPTAIN_GRANT_LIST + "')")
    @GetMapping("/team-players")
    public AjaxResult teamPlayers(@RequestParam Long teamId, @RequestParam Long divisionId) {
        return success(optionsService.selectTeamPlayerOptions(teamId, divisionId));
    }
}
