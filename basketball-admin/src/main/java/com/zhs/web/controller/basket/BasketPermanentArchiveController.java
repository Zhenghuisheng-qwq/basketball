package com.zhs.web.controller.basket;

import com.zhs.basket.service.IBasketPermanentArchiveService;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/permanentArchive")
public class BasketPermanentArchiveController extends BaseController {

    @Autowired
    private IBasketPermanentArchiveService permanentArchiveService;

    @PreAuthorize("@ss.hasPermi('basket:permanentPlayer:list')")
    @GetMapping("/player/{playerId}")
    public AjaxResult playerArchive(@PathVariable Long playerId) {
        return success(permanentArchiveService.getPlayerArchive(playerId));
    }

    @PreAuthorize("@ss.hasPermi('basket:permanentTeam:list')")
    @GetMapping("/team/{teamId}")
    public AjaxResult teamArchive(@PathVariable Long teamId) {
        return success(permanentArchiveService.getTeamArchive(teamId));
    }
}
