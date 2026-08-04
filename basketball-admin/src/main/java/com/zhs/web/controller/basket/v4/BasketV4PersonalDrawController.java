package com.zhs.web.controller.basket.v4;

import com.zhs.basket.constant.BasketEventV4Perms;
import com.zhs.basket.dto.v4.*;
import com.zhs.basket.query.v4.BasketPersonalDrawBatchQuery;
import com.zhs.basket.service.v4.IPersonalTeamFormationApplicationService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/v4/admin/personal-draw-batches")
public class BasketV4PersonalDrawController extends BaseController {

    @Autowired
    private IPersonalTeamFormationApplicationService personalTeamFormationApplicationService;

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.PERSONAL_FORMATION_LIST + "')")
    @GetMapping("/list")
    public TableDataInfo list(BasketPersonalDrawBatchQuery query) {
        startPage();
        return getDataTable(personalTeamFormationApplicationService.selectBatchList(query));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.PERSONAL_FORMATION_LIST + "')")
    @GetMapping("/{drawBatchId}")
    public AjaxResult getInfo(@PathVariable Long drawBatchId) {
        return success(personalTeamFormationApplicationService.selectBatchDetail(drawBatchId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.PERSONAL_FORMATION_EDIT + "')")
    @Log(title = "V4 PersonalDrawBatch", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult create(@RequestBody BasketPersonalDrawBatchCreateReq req) {
        return success(personalTeamFormationApplicationService.createDraftBatch(req));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.PERSONAL_FORMATION_EDIT + "')")
    @Log(title = "V4 PersonalDrawTeam", businessType = BusinessType.INSERT)
    @PostMapping("/{drawBatchId}/teams")
    public AjaxResult createTeam(@PathVariable Long drawBatchId, @RequestBody BasketPersonalDrawTeamCreateReq req) {
        return success(personalTeamFormationApplicationService.createTeam(drawBatchId, req));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.PERSONAL_FORMATION_EDIT + "')")
    @Log(title = "V4 PersonalDrawAssignment", businessType = BusinessType.INSERT)
    @PostMapping("/{drawBatchId}/teams/{drawTeamId}/assignments")
    public AjaxResult addAssignment(@PathVariable Long drawBatchId,
                                    @PathVariable Long drawTeamId,
                                    @RequestBody BasketPersonalDrawAssignmentCreateReq req) {
        return success(personalTeamFormationApplicationService.addAssignment(drawBatchId, drawTeamId, req));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.PERSONAL_FORMATION_EDIT + "')")
    @Log(title = "V4 PersonalDrawAssignment", businessType = BusinessType.DELETE)
    @DeleteMapping("/{drawBatchId}/assignments/{assignmentId}")
    public AjaxResult removeAssignment(@PathVariable Long drawBatchId, @PathVariable Long assignmentId) {
        personalTeamFormationApplicationService.removeAssignment(drawBatchId, assignmentId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.PERSONAL_FORMATION_EDIT + "')")
    @Log(title = "V4 PersonalDrawBatch", businessType = BusinessType.UPDATE)
    @PostMapping("/{drawBatchId}/confirm")
    public AjaxResult confirmBatch(@PathVariable Long drawBatchId,
                                   @RequestBody(required = false) BasketPersonalDrawBatchConfirmReq req) {
        personalTeamFormationApplicationService.confirmBatch(drawBatchId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.PERSONAL_FORMATION_EDIT + "')")
    @Log(title = "V4 PersonalDrawBatch", businessType = BusinessType.UPDATE)
    @PostMapping("/{drawBatchId}/cancel")
    public AjaxResult cancelBatch(@PathVariable Long drawBatchId) {
        personalTeamFormationApplicationService.cancelBatch(drawBatchId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.PERSONAL_FORMATION_EDIT + "')")
    @Log(title = "V4 PersonalFormation", businessType = BusinessType.UPDATE)
    @PostMapping("/teams/{drawTeamId}/bind-team")
    public AjaxResult bindTeam(@PathVariable Long drawTeamId, @RequestBody BasketPersonalFormationBindTeamReq req) {
        personalTeamFormationApplicationService.bindLongTermTeam(drawTeamId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.PERSONAL_FORMATION_CONFIRM + "')")
    @Log(title = "V4 PersonalFormation", businessType = BusinessType.UPDATE)
    @PostMapping("/teams/{drawTeamId}/confirm")
    public AjaxResult confirmFormation(@PathVariable Long drawTeamId) {
        return success(personalTeamFormationApplicationService.confirmFormation(drawTeamId));
    }
}
