package com.zhs.web.controller.basket;

import com.zhs.basket.service.IBasketPermanentArchiveService;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/permanentArchive")
public class BasketPermanentArchiveController extends BaseController {

    @Autowired
    private IBasketPermanentArchiveService permanentArchiveService;

    @PreAuthorize("@ss.hasPermi('basket:permanentPlayer:list')")
    @GetMapping("/player-options")
    public TableDataInfo playerOptions(@RequestParam(required = false) String keyword) {
        startPage();
        return getDataTable(permanentArchiveService.selectPlayerOptions(keyword));
    }

    @PreAuthorize("@ss.hasPermi('basket:permanentTeam:list')")
    @GetMapping("/team-options")
    public TableDataInfo teamOptions(@RequestParam(required = false) String keyword) {
        startPage();
        return getDataTable(permanentArchiveService.selectTeamOptions(keyword));
    }

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
