package com.zhs.web.controller.basket.v4;

import com.zhs.basket.constant.BasketEventV4Perms;
import com.zhs.basket.dto.v4.BasketDivisionGroupTeamAssignReq;
import com.zhs.basket.dto.v4.BasketDivisionGroupUpdateReq;
import com.zhs.basket.dto.v4.BasketDivisionGroupingBatchCreateReq;
import com.zhs.basket.query.v4.BasketDivisionGroupingBatchQuery;
import com.zhs.basket.service.v4.IDivisionGroupingApplicationService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/v4/admin/division-grouping-batches")
public class BasketV4DivisionGroupingController extends BaseController {

    @Autowired
    private IDivisionGroupingApplicationService divisionGroupingApplicationService;

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.GROUPING_LIST + "')")
    @GetMapping("/list")
    public TableDataInfo list(BasketDivisionGroupingBatchQuery query) {
        startPage();
        return getDataTable(divisionGroupingApplicationService.selectBatchList(query));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.GROUPING_LIST + "')")
    @GetMapping("/{groupingBatchId}")
    public AjaxResult getInfo(@PathVariable Long groupingBatchId) {
        return success(divisionGroupingApplicationService.selectBatchDetail(groupingBatchId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.GROUPING_EDIT + "')")
    @Log(title = "V4 DivisionGroupingBatch", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult create(@RequestBody BasketDivisionGroupingBatchCreateReq req) {
        return success(divisionGroupingApplicationService.createDraftBatch(req));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.GROUPING_EDIT + "')")
    @Log(title = "V4 DivisionGroup", businessType = BusinessType.UPDATE)
    @PutMapping("/{groupingBatchId}/groups/{groupId}")
    public AjaxResult updateGroup(@PathVariable Long groupingBatchId,
                                  @PathVariable Long groupId,
                                  @RequestBody BasketDivisionGroupUpdateReq req) {
        divisionGroupingApplicationService.updateGroup(groupingBatchId, groupId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.GROUPING_EDIT + "')")
    @Log(title = "V4 DivisionGroupTeam", businessType = BusinessType.INSERT)
    @PostMapping("/{groupingBatchId}/groups/{groupId}/teams")
    public AjaxResult assignTeam(@PathVariable Long groupingBatchId,
                                 @PathVariable Long groupId,
                                 @RequestBody BasketDivisionGroupTeamAssignReq req) {
        return success(divisionGroupingApplicationService.assignEventTeam(groupingBatchId, groupId, req));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.GROUPING_EDIT + "')")
    @Log(title = "V4 DivisionGroupTeam", businessType = BusinessType.DELETE)
    @DeleteMapping("/{groupingBatchId}/assignments/{assignmentId}")
    public AjaxResult removeAssignment(@PathVariable Long groupingBatchId, @PathVariable Long assignmentId) {
        divisionGroupingApplicationService.removeAssignment(groupingBatchId, assignmentId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.GROUPING_LOCK + "')")
    @Log(title = "V4 DivisionGroupingBatch", businessType = BusinessType.UPDATE)
    @PostMapping("/{groupingBatchId}/lock")
    public AjaxResult lockBatch(@PathVariable Long groupingBatchId) {
        divisionGroupingApplicationService.lockBatch(groupingBatchId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.GROUPING_EDIT + "')")
    @Log(title = "V4 DivisionGroupingBatch", businessType = BusinessType.UPDATE)
    @PostMapping("/{groupingBatchId}/cancel")
    public AjaxResult cancelBatch(@PathVariable Long groupingBatchId) {
        divisionGroupingApplicationService.cancelBatch(groupingBatchId);
        return success();
    }
}
