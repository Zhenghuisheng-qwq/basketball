package com.zhs.web.controller.basket.v4;

import com.zhs.basket.constant.BasketEventV4Perms;
import com.zhs.basket.service.v4.IDivisionStageExecutionApplicationService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/v4/admin/stages")
public class BasketV4StageExecutionController extends BaseController {

    @Autowired
    private IDivisionStageExecutionApplicationService stageExecutionApplicationService;

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_LIST + "')")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam Long divisionId) {
        return success(stageExecutionApplicationService.listStages(divisionId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_GENERATE + "')")
    @Log(title = "V4 StageExecution", businessType = BusinessType.UPDATE)
    @PostMapping("/{stageId}/initialize-teams")
    public AjaxResult initializeTeams(@PathVariable Long stageId) {
        stageExecutionApplicationService.initializeStageTeams(stageId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_GENERATE + "')")
    @GetMapping("/{stageId}/schedule-preview")
    public AjaxResult previewSchedule(@PathVariable Long stageId) {
        return success(stageExecutionApplicationService.previewSchedule(stageId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_GENERATE + "')")
    @Log(title = "V4 StageExecution", businessType = BusinessType.INSERT)
    @PostMapping("/{stageId}/generate-schedule")
    public AjaxResult generateSchedule(@PathVariable Long stageId) {
        return success(stageExecutionApplicationService.generateSchedule(stageId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_PUBLISH + "')")
    @Log(title = "V4 StageExecution", businessType = BusinessType.UPDATE)
    @PostMapping("/{stageId}/publish-schedule")
    public AjaxResult publishSchedule(@PathVariable Long stageId) {
        stageExecutionApplicationService.publishSchedule(stageId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_GENERATE + "')")
    @Log(title = "V4 StageExecution", businessType = BusinessType.DELETE)
    @PostMapping("/{stageId}/cancel-draft-schedule")
    public AjaxResult cancelDraftSchedule(@PathVariable Long stageId) {
        stageExecutionApplicationService.cancelDraftSchedule(stageId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_LIST + "')")
    @PostMapping("/{stageId}/check-completion")
    public AjaxResult checkStageCompletion(@PathVariable Long stageId) {
        stageExecutionApplicationService.checkStageCompletion(stageId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_QUALIFICATION_PREVIEW + "')")
    @GetMapping("/{stageId}/qualification-preview")
    public AjaxResult previewQualification(@PathVariable Long stageId) {
        return success(stageExecutionApplicationService.previewQualification(stageId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_QUALIFICATION_CONFIRM + "')")
    @Log(title = "V4 StageQualification", businessType = BusinessType.UPDATE)
    @PostMapping("/{stageId}/confirm-qualification")
    public AjaxResult confirmQualification(@PathVariable Long stageId) {
        stageExecutionApplicationService.confirmQualification(stageId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_QUALIFICATION_CONFIRM + "')")
    @Log(title = "V4 StageQualification", businessType = BusinessType.UPDATE)
    @PostMapping("/{stageId}/cancel-qualification-confirm")
    public AjaxResult cancelQualificationConfirm(@PathVariable Long stageId) {
        stageExecutionApplicationService.cancelQualificationConfirm(stageId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STANDING_VIEW + "')")
    @GetMapping("/{stageId}/standings")
    public AjaxResult listStandings(@PathVariable Long stageId) {
        return success(stageExecutionApplicationService.listStandings(stageId));
    }
}
