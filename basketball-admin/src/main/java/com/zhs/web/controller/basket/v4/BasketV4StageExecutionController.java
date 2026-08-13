package com.zhs.web.controller.basket.v4;

import com.zhs.basket.constant.BasketEventV4Perms;
import com.zhs.basket.dto.v4.BasketStageManualRankReq;
import com.zhs.basket.dto.v4.ScheduleAssignReq;
import com.zhs.basket.dto.v4.ScheduleBatchAssignReq;
import com.zhs.basket.dto.v4.ScheduleRecommendApplyReq;
import com.zhs.basket.dto.v4.StageExternalRankingReq;
import com.zhs.basket.dto.v4.StageManualSlotReq;
import com.zhs.basket.dto.v4.StagePairingSeedReq;
import com.zhs.basket.dto.v4.StageWithdrawReq;
import com.zhs.basket.service.v4.IDivisionStageExecutionApplicationService;
import com.zhs.basket.service.v4.IStagePairingExecutionApplicationService;
import com.zhs.basket.service.v4.IStagePublishApplicationService;
import com.zhs.basket.service.v4.IStageScheduleApplicationService;
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
    @Autowired
    private IStageScheduleApplicationService stageScheduleApplicationService;
    @Autowired
    private IStagePublishApplicationService stagePublishApplicationService;

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
        return success(stagePublishApplicationService.publish(stageId));
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

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_LIST + "')")
    @GetMapping("/{stageId}/schedule-units")
    public AjaxResult listScheduleUnits(@PathVariable Long stageId,
                                        @RequestParam(required = false) String scheduleStatus) {
        return success(stageScheduleApplicationService.listScheduleUnits(stageId, scheduleStatus));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_LIST + "')")
    @GetMapping("/{stageId}/schedule-summary")
    public AjaxResult scheduleSummary(@PathVariable Long stageId) {
        return success(stageScheduleApplicationService.summary(stageId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_GENERATE + "')")
    @Log(title = "V4 StageSchedule", businessType = BusinessType.UPDATE)
    @PostMapping("/{stageId}/schedule/assign")
    public AjaxResult assignSchedule(@PathVariable Long stageId, @RequestBody ScheduleAssignReq req) {
        stageScheduleApplicationService.assign(stageId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_GENERATE + "')")
    @Log(title = "V4 StageSchedule", businessType = BusinessType.UPDATE)
    @PostMapping("/{stageId}/schedule/batch-assign")
    public AjaxResult batchAssignSchedule(@PathVariable Long stageId, @RequestBody ScheduleBatchAssignReq req) {
        stageScheduleApplicationService.batchAssign(stageId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_GENERATE + "')")
    @Log(title = "V4 StageSchedule", businessType = BusinessType.UPDATE)
    @PostMapping("/{stageId}/schedule/clear")
    public AjaxResult clearSchedule(@PathVariable Long stageId, @RequestBody ScheduleAssignReq req) {
        stageScheduleApplicationService.clear(stageId, req.getUnitType(), req.getUnitId());
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_GENERATE + "')")
    @GetMapping("/{stageId}/schedule/recommend-preview")
    public AjaxResult previewRecommend(@PathVariable Long stageId,
                                       @RequestParam(defaultValue = "false") boolean overwriteScheduled) {
        return success(stageScheduleApplicationService.previewRecommend(stageId, overwriteScheduled));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_GENERATE + "')")
    @Log(title = "V4 StageSchedule", businessType = BusinessType.UPDATE)
    @PostMapping("/{stageId}/schedule/recommend-apply")
    public AjaxResult applyRecommend(@PathVariable Long stageId, @RequestBody(required = false) ScheduleRecommendApplyReq req) {
        stageScheduleApplicationService.applyRecommend(stageId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_PUBLISH + "')")
    @GetMapping("/{stageId}/publish-preview")
    public AjaxResult publishPreview(@PathVariable Long stageId) {
        return success(stagePublishApplicationService.preview(stageId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_PUBLISH + "')")
    @Log(title = "V4 StagePublish", businessType = BusinessType.UPDATE)
    @PostMapping("/{stageId}/withdraw-schedule")
    public AjaxResult withdrawSchedule(@PathVariable Long stageId, @RequestBody StageWithdrawReq req) {
        return success(stagePublishApplicationService.withdraw(stageId, req));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_PUBLISH + "')")
    @Log(title = "V4 StagePublish", businessType = BusinessType.UPDATE)
    @PostMapping("/{stageId}/republish-schedule")
    public AjaxResult republishSchedule(@PathVariable Long stageId) {
        return success(stagePublishApplicationService.republish(stageId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_LIST + "')")
    @GetMapping("/{stageId}/publish-records")
    public AjaxResult publishRecords(@PathVariable Long stageId) {
        return success(stagePublishApplicationService.listRecords(stageId));
    }
}
