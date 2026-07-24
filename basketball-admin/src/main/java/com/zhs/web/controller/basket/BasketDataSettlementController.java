package com.zhs.web.controller.basket;

import com.zhs.basket.constant.BasketSettlementPerms;
import com.zhs.basket.service.IBasketDataSettlementService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 数据结算编排（校验 / 摘要 / 重算）
 */
@RestController
@RequestMapping("/basket/data-settlement")
public class BasketDataSettlementController extends BaseController {

    @Autowired
    private IBasketDataSettlementService settlementService;

    @PreAuthorize("@ss.hasPermi('" + BasketSettlementPerms.DATA_SETTLEMENT_LIST + "')")
    @GetMapping("/season/list")
    public AjaxResult seasonList(@RequestParam(required = false) Long tournamentId) {
        return success(settlementService.listSeasonSummaries(tournamentId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketSettlementPerms.DATA_SETTLEMENT_QUERY + "')")
    @GetMapping("/season/{seasonId}/summary")
    public AjaxResult seasonSummary(@PathVariable Long seasonId) {
        return success(settlementService.getSeasonSummary(seasonId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketSettlementPerms.DATA_SETTLEMENT_QUERY + "')")
    @GetMapping("/season/{seasonId}/validation")
    public AjaxResult seasonValidation(@PathVariable Long seasonId) {
        return success(settlementService.validate(seasonId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketSettlementPerms.DATA_SETTLEMENT_REBUILD + "')")
    @Log(title = "数据结算", businessType = BusinessType.UPDATE)
    @PostMapping("/season/{seasonId}/rebuild")
    public AjaxResult rebuildSeason(@PathVariable Long seasonId) {
        return success(settlementService.rebuildSeason(seasonId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketSettlementPerms.DATA_SETTLEMENT_REBUILD + "')")
    @Log(title = "数据结算", businessType = BusinessType.UPDATE)
    @PostMapping("/stage/{stageId}/rebuild")
    public AjaxResult rebuildStage(@PathVariable Long stageId) {
        return success(settlementService.rebuildStage(stageId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketSettlementPerms.DATA_SETTLEMENT_REBUILD + "')")
    @Log(title = "数据结算", businessType = BusinessType.UPDATE)
    @PostMapping("/group/{groupId}/rebuild")
    public AjaxResult rebuildGroup(@PathVariable Long groupId) {
        return success(settlementService.rebuildGroup(groupId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketSettlementPerms.DATA_SETTLEMENT_REBUILD + "')")
    @Log(title = "数据结算", businessType = BusinessType.UPDATE)
    @PostMapping("/match/{matchId}/rebuild")
    public AjaxResult rebuildMatch(@PathVariable Long matchId) {
        return success(settlementService.rebuildMatch(matchId));
    }
}
