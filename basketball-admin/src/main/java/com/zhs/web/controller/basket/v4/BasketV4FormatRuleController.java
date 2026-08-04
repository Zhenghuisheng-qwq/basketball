package com.zhs.web.controller.basket.v4;

import com.zhs.basket.constant.BasketEventV4Perms;
import com.zhs.basket.dto.v4.BasketDivisionStageOrderReq;
import com.zhs.basket.dto.v4.BasketDivisionStageSaveReq;
import com.zhs.basket.dto.v4.BasketDivisionTieBreakRuleSaveReq;
import com.zhs.basket.service.v4.IBasketV4FormatSnapshotService;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Division DRAFT 规则编辑。
 */
@RestController
@RequestMapping("/basket/v4/divisions")
public class BasketV4FormatRuleController extends BaseController {

    @Autowired
    private IBasketV4FormatSnapshotService snapshotService;

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.FORMAT_RULE_VIEW + "') or @ss.hasPermi('" + BasketEventV4Perms.DIVISION_QUERY + "')")
    @GetMapping("/{divisionId}/format-snapshot")
    public AjaxResult getSnapshot(@PathVariable Long divisionId) {
        return success(snapshotService.getDivisionSnapshot(divisionId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.FORMAT_RULE_EDIT + "') or @ss.hasPermi('" + BasketEventV4Perms.DIVISION_EDIT + "')")
    @PostMapping("/{divisionId}/format-stages")
    public AjaxResult addStage(@PathVariable Long divisionId, @RequestBody BasketDivisionStageSaveReq req) {
        return success(snapshotService.addStage(divisionId, req));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.FORMAT_RULE_EDIT + "') or @ss.hasPermi('" + BasketEventV4Perms.DIVISION_EDIT + "')")
    @PutMapping("/{divisionId}/format-stages/{stageId}")
    public AjaxResult updateStage(@PathVariable Long divisionId,
                                  @PathVariable Long stageId,
                                  @RequestBody BasketDivisionStageSaveReq req) {
        snapshotService.updateStage(divisionId, stageId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.FORMAT_RULE_EDIT + "') or @ss.hasPermi('" + BasketEventV4Perms.DIVISION_EDIT + "')")
    @DeleteMapping("/{divisionId}/format-stages/{stageId}")
    public AjaxResult deleteStage(@PathVariable Long divisionId, @PathVariable Long stageId) {
        snapshotService.deleteStage(divisionId, stageId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.FORMAT_RULE_EDIT + "') or @ss.hasPermi('" + BasketEventV4Perms.DIVISION_EDIT + "')")
    @PutMapping("/{divisionId}/format-stages/order")
    public AjaxResult reorder(@PathVariable Long divisionId, @RequestBody BasketDivisionStageOrderReq req) {
        snapshotService.reorderStages(divisionId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.FORMAT_RULE_EDIT + "') or @ss.hasPermi('" + BasketEventV4Perms.DIVISION_EDIT + "')")
    @PutMapping("/{divisionId}/tie-break-rules")
    public AjaxResult saveTieBreakRules(@PathVariable Long divisionId,
                                        @RequestBody BasketDivisionTieBreakRuleSaveReq req) {
        snapshotService.saveTieBreakRules(divisionId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.FORMAT_RULE_EDIT + "') or @ss.hasPermi('" + BasketEventV4Perms.DIVISION_EDIT + "')")
    @PostMapping("/{divisionId}/format-snapshot/rebuild")
    public AjaxResult rebuild(@PathVariable Long divisionId, @RequestParam Long templateId) {
        snapshotService.rebuildDraftSnapshot(divisionId, templateId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.FORMAT_RULE_LOCK + "') or @ss.hasPermi('" + BasketEventV4Perms.DIVISION_OPEN + "')")
    @PostMapping("/{divisionId}/format-snapshot/lock")
    public AjaxResult lock(@PathVariable Long divisionId) {
        snapshotService.lockSnapshot(divisionId, SecurityUtils.getUserId());
        return success();
    }
}
