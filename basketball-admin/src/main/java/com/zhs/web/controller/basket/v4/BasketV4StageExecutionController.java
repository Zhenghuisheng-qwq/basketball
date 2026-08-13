package com.zhs.web.controller.basket.v4;

import com.zhs.basket.constant.BasketEventV4Perms;
import com.zhs.basket.dto.v4.BasketStageManualRankReq;
import com.zhs.basket.dto.v4.StageExternalRankingReq;
import com.zhs.basket.dto.v4.StageManualSlotReq;
import com.zhs.basket.dto.v4.StagePairingSeedReq;
import com.zhs.basket.service.v4.IDivisionStageExecutionApplicationService;
import com.zhs.basket.service.v4.IStagePairingExecutionApplicationService;
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
    @Autowired
    private IStagePairingExecutionApplicationService pairingExecutionApplicationService;

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_LIST + "')")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam Long divisionId) {
        return success(stageExecutionApplicationService.listStages(divisionId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_GENERATE + "')")
    @Log(title = "V4 StageExecution", businessType = BusinessType.UPDATE)
    @PostMapping("/{stageId}/initialize-teams")
    public AjaxResult initializeTeams(@PathVariable Long stageId) {
        return success(stageExecutionApplicationService.initializeStageTeams(stageId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_LIST + "')")
    @GetMapping("/{stageId}/teams")
    public AjaxResult listStageTeams(@PathVariable Long stageId) {
        return success(stageExecutionApplicationService.listStageTeams(stageId));
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

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STANDING_MANUAL_RANK + "')")
    @Log(title = "V4 ManualRank", businessType = BusinessType.UPDATE)
    @PostMapping("/{stageId}/manual-rank")
    public AjaxResult applyManualRank(@PathVariable Long stageId, @RequestBody BasketStageManualRankReq req) {
        req.setStageId(stageId);
        stageExecutionApplicationService.applyManualRank(req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_LIST + "')")
    @GetMapping("/{stageId}/pairing-execution")
    public AjaxResult getPairingExecution(@PathVariable Long stageId) {
        return success(pairingExecutionApplicationService.getCurrent(stageId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_LIST + "')")
    @GetMapping("/{stageId}/bracket-preview")
    public AjaxResult previewBracket(@PathVariable Long stageId) {
        return success(pairingExecutionApplicationService.previewBracket(stageId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_GENERATE + "')")
    @Log(title = "V4 PairingExecution", businessType = BusinessType.UPDATE)
    @PostMapping("/{stageId}/admin-seeds")
    public AjaxResult saveAdminSeeds(@PathVariable Long stageId, @RequestBody StagePairingSeedReq req) {
        return success(pairingExecutionApplicationService.saveAdminSeeds(stageId, req));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_GENERATE + "')")
    @Log(title = "V4 PairingExecution", businessType = BusinessType.UPDATE)
    @PostMapping("/{stageId}/external-ranking")
    public AjaxResult saveExternalRanking(@PathVariable Long stageId, @RequestBody StageExternalRankingReq req) {
        return success(pairingExecutionApplicationService.saveExternalRanking(stageId, req));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_GENERATE + "')")
    @Log(title = "V4 PairingExecution", businessType = BusinessType.INSERT)
    @PostMapping("/{stageId}/generate-draw")
    public AjaxResult generateDraw(@PathVariable Long stageId) {
        return success(pairingExecutionApplicationService.generateRandomDraw(stageId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_GENERATE + "')")
    @Log(title = "V4 PairingExecution", businessType = BusinessType.UPDATE)
    @PostMapping("/{stageId}/confirm-draw")
    public AjaxResult confirmDraw(@PathVariable Long stageId) {
        return success(pairingExecutionApplicationService.confirm(stageId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_GENERATE + "')")
    @Log(title = "V4 PairingExecution", businessType = BusinessType.UPDATE)
    @PostMapping("/{stageId}/manual-slots")
    public AjaxResult saveManualSlots(@PathVariable Long stageId, @RequestBody StageManualSlotReq req) {
        return success(pairingExecutionApplicationService.saveManualSlots(stageId, req));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_GENERATE + "')")
    @Log(title = "V4 PairingExecution", businessType = BusinessType.UPDATE)
    @PostMapping("/{stageId}/redraw")
    public AjaxResult redraw(@PathVariable Long stageId) {
        return success(pairingExecutionApplicationService.redraw(stageId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_GENERATE + "')")
    @Log(title = "V4 PairingExecution", businessType = BusinessType.INSERT)
    @PostMapping("/{stageId}/generate-group-cross")
    public AjaxResult generateGroupCross(@PathVariable Long stageId) {
        return success(pairingExecutionApplicationService.generateGroupCross(stageId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_GENERATE + "')")
    @Log(title = "V4 PairingExecution", businessType = BusinessType.UPDATE)
    @PostMapping("/{stageId}/resolve-placeholders")
    public AjaxResult resolvePlaceholders(@PathVariable Long stageId) {
        return success(pairingExecutionApplicationService.resolvePlaceholders(stageId));
    }
}
