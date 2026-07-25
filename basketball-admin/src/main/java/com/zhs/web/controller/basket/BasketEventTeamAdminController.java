package com.zhs.web.controller.basket;

import com.zhs.basket.query.EventTeamAdminQuery;
import com.zhs.basket.req.EventTeamParticipationStatusReq;
import com.zhs.basket.service.IBasketEventTeamAdminService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/eventTeam")
public class BasketEventTeamAdminController extends BaseController {

    @Autowired
    private IBasketEventTeamAdminService eventTeamAdminService;

    @PreAuthorize("@ss.hasPermi('basket:eventTeam:list')")
    @GetMapping("/list")
    public TableDataInfo list(EventTeamAdminQuery query) { startPage(); return getDataTable(eventTeamAdminService.selectList(query)); }
    @PreAuthorize("@ss.hasPermi('basket:eventTeam:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) { return success(eventTeamAdminService.selectDetail(id)); }
    @PreAuthorize("@ss.hasPermi('basket:eventTeam:edit')")
    @Log(title = "赛事球队", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/lock-roster")
    public AjaxResult lockRoster(@PathVariable Long id) { eventTeamAdminService.lockRoster(id); return success(); }
    @PreAuthorize("@ss.hasPermi('basket:eventTeam:edit')")
    @Log(title = "赛事球队", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/unlock-roster")
    public AjaxResult unlockRoster(@PathVariable Long id) { eventTeamAdminService.unlockRoster(id); return success(); }
    @PreAuthorize("@ss.hasPermi('basket:eventTeam:edit')")
    @Log(title = "赛事球队", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/participation-status")
    public AjaxResult updateParticipationStatus(@PathVariable Long id, @RequestBody EventTeamParticipationStatusReq req) { eventTeamAdminService.updateParticipationStatus(id, req); return success(); }
}
