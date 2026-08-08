package com.zhs.web.controller.basket.v4;

import com.zhs.basket.constant.BasketEventV4Perms;
import com.zhs.basket.dto.v4.BasketManualAdvanceReq;
import com.zhs.basket.dto.v4.BasketMatchForfeitReq;
import com.zhs.basket.dto.v4.BasketSeriesRevokeResolutionReq;
import com.zhs.basket.dto.v4.BasketPairingAdjustReq;
import com.zhs.basket.dto.v4.BasketSeriesForfeitReq;
import com.zhs.basket.service.v4.IStageExecutionExceptionApplicationService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/basket/v4/admin/stage-exception")
public class BasketV4StageExceptionController extends BaseController {

    @Autowired
    private IStageExecutionExceptionApplicationService exceptionService;

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_EXCEPTION + "')")
    @Log(title = "V4 MatchForfeit", businessType = BusinessType.UPDATE)
    @PostMapping("/match-forfeit")
    public AjaxResult forfeitMatch(@RequestBody BasketMatchForfeitReq req) {
        exceptionService.forfeitMatch(req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_EXCEPTION + "')")
    @Log(title = "V4 SeriesForfeit", businessType = BusinessType.UPDATE)
    @PostMapping("/series-forfeit")
    public AjaxResult forfeitSeries(@RequestBody BasketSeriesForfeitReq req) {
        exceptionService.forfeitSeries(req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_EXCEPTION + "')")
    @Log(title = "V4 ManualAdvance", businessType = BusinessType.UPDATE)
    @PostMapping("/manual-advance")
    public AjaxResult manualAdvance(@RequestBody BasketManualAdvanceReq req) {
        exceptionService.manualAdvance(req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_EXCEPTION + "')")
    @Log(title = "V4 PairingAdjust", businessType = BusinessType.UPDATE)
    @PostMapping("/pairing-adjust")
    public AjaxResult adjustPairing(@RequestBody BasketPairingAdjustReq req) {
        exceptionService.adjustPairing(req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.STAGE_EXCEPTION + "')")
    @Log(title = "V4 RevokeSeriesResolution", businessType = BusinessType.UPDATE)
    @PostMapping("/series-revoke-resolution")
    public AjaxResult revokeSeriesResolution(@RequestBody BasketSeriesRevokeResolutionReq req) {
        exceptionService.revokeSeriesResolution(req);
        return success();
    }
}
