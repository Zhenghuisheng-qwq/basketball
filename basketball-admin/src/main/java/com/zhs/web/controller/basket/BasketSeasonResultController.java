package com.zhs.web.controller.basket;

import com.zhs.basket.constant.BasketContentPerms;
import com.zhs.basket.req.SeasonResultConfirmReq;
import com.zhs.basket.req.SeasonResultRevokeReq;
import com.zhs.basket.service.IBasketSeasonResultService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/season-result")
public class BasketSeasonResultController extends BaseController {

    @Autowired
    private IBasketSeasonResultService seasonResultService;

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.SEASON_RESULT_LIST + "')")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(required = false) Long tournamentId) {
        return success(seasonResultService.list(tournamentId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.SEASON_RESULT_QUERY + "')")
    @GetMapping("/{seasonId}")
    public AjaxResult getInfo(@PathVariable Long seasonId) {
        return success(seasonResultService.getDetail(seasonId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.SEASON_RESULT_QUERY + "')")
    @GetMapping("/{seasonId}/candidate-team")
    public AjaxResult candidateTeam(@PathVariable Long seasonId) {
        return success(seasonResultService.listCandidateTeams(seasonId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.SEASON_RESULT_CALCULATE + "')")
    @GetMapping("/{seasonId}/end-check")
    public AjaxResult endCheck(@PathVariable Long seasonId) {
        return success(seasonResultService.endCheck(seasonId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.SEASON_RESULT_CONFIRM + "')")
    @Log(title = "赛季名次", businessType = BusinessType.UPDATE)
    @PostMapping("/{seasonId}/confirm")
    public AjaxResult confirm(@PathVariable Long seasonId, @RequestBody SeasonResultConfirmReq req) {
        seasonResultService.confirm(seasonId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.SEASON_RESULT_PUBLISH + "')")
    @Log(title = "赛季名次", businessType = BusinessType.UPDATE)
    @PostMapping("/{seasonId}/publish")
    public AjaxResult publish(@PathVariable Long seasonId) {
        seasonResultService.publish(seasonId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.SEASON_RESULT_REVOKE + "')")
    @Log(title = "赛季名次", businessType = BusinessType.UPDATE)
    @PostMapping("/{seasonId}/revoke")
    public AjaxResult revoke(@PathVariable Long seasonId, @RequestBody SeasonResultRevokeReq req) {
        seasonResultService.revoke(seasonId, req);
        return success();
    }
}
