package com.zhs.web.controller.basket;

import com.zhs.basket.req.TournamentRuleCreateReq;
import com.zhs.basket.req.TournamentRuleUpdateReq;
import com.zhs.basket.service.IBasketTournamentRuleService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class BasketTournamentRuleController extends BaseController {

    @Autowired
    private IBasketTournamentRuleService tournamentRuleService;

    @PreAuthorize("@ss.hasPermi('basket:tournamentRule:list')")
    @GetMapping("/basket/season/{seasonId}/rule/list")
    public AjaxResult list(@PathVariable Long seasonId) {
        return success(tournamentRuleService.selectBySeasonId(seasonId));
    }

    @PreAuthorize("@ss.hasPermi('basket:tournamentRule:add')")
    @Log(title = "赛事规则", businessType = BusinessType.INSERT)
    @PostMapping("/basket/season/{seasonId}/rule")
    public AjaxResult add(@PathVariable Long seasonId, @RequestBody TournamentRuleCreateReq req) {
        return success(tournamentRuleService.create(seasonId, req));
    }

    @PreAuthorize("@ss.hasPermi('basket:tournamentRule:edit')")
    @Log(title = "赛事规则", businessType = BusinessType.UPDATE)
    @PutMapping("/basket/tournamentRule/{ruleId}")
    public AjaxResult edit(@PathVariable Long ruleId, @RequestBody TournamentRuleUpdateReq req) {
        tournamentRuleService.update(ruleId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:tournamentRule:remove')")
    @Log(title = "赛事规则", businessType = BusinessType.DELETE)
    @DeleteMapping("/basket/tournamentRule/{ruleId}")
    public AjaxResult remove(@PathVariable Long ruleId) {
        tournamentRuleService.delete(ruleId);
        return success();
    }
}
