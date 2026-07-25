package com.zhs.web.controller.basket;

import com.zhs.basket.req.MatchPlayerStatSaveReq;
import com.zhs.basket.req.MatchReopenReq;
import com.zhs.basket.req.MatchTeamStatSaveReq;
import com.zhs.basket.service.IBasketMatchStatService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/match")
public class BasketMatchStatController extends BaseController {

    @Autowired
    private IBasketMatchStatService statService;

    @PreAuthorize("@ss.hasPermi('basket:matchStat:query')")
    @GetMapping("/{matchId}/team-stat")
    public AjaxResult teamStat(@PathVariable Long matchId) {
        return success(statService.selectTeamStats(matchId));
    }

    @PreAuthorize("@ss.hasPermi('basket:matchStat:edit')")
    @Log(title = "比赛技术统计", businessType = BusinessType.UPDATE)
    @PutMapping("/{matchId}/team-stat")
    public AjaxResult saveTeamStat(@PathVariable Long matchId, @RequestBody MatchTeamStatSaveReq req) {
        statService.saveTeamStats(matchId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:matchStat:submit')")
    @Log(title = "比赛技术统计", businessType = BusinessType.UPDATE)
    @PostMapping("/{matchId}/team-stat/submit")
    public AjaxResult submitTeamStat(@PathVariable Long matchId) {
        statService.submitTeamStats(matchId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:matchStat:query')")
    @GetMapping("/{matchId}/player-stat")
    public AjaxResult playerStat(@PathVariable Long matchId) {
        return success(statService.selectPlayerStats(matchId));
    }

    @PreAuthorize("@ss.hasPermi('basket:matchStat:edit')")
    @Log(title = "比赛技术统计", businessType = BusinessType.UPDATE)
    @PutMapping("/{matchId}/player-stat")
    public AjaxResult savePlayerStat(@PathVariable Long matchId, @RequestBody MatchPlayerStatSaveReq req) {
        statService.savePlayerStats(matchId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:matchStat:submit')")
    @Log(title = "比赛技术统计", businessType = BusinessType.UPDATE)
    @PostMapping("/{matchId}/player-stat/submit")
    public AjaxResult submitPlayerStat(@PathVariable Long matchId) {
        statService.submitPlayerStats(matchId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:matchStat:confirm')")
    @Log(title = "比赛技术统计", businessType = BusinessType.UPDATE)
    @PostMapping("/{matchId}/result/confirm")
    public AjaxResult confirmResult(@PathVariable Long matchId) {
        statService.confirmResult(matchId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:matchStat:confirm')")
    @Log(title = "比赛技术统计", businessType = BusinessType.UPDATE)
    @PostMapping("/{matchId}/result/reject")
    public AjaxResult rejectResult(@PathVariable Long matchId, @RequestBody MatchReopenReq req) {
        statService.rejectResult(matchId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:matchStat:reopen')")
    @Log(title = "比赛技术统计", businessType = BusinessType.UPDATE)
    @PostMapping("/{matchId}/result/reopen")
    public AjaxResult reopenResult(@PathVariable Long matchId, @RequestBody MatchReopenReq req) {
        statService.reopenResult(matchId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:matchStat:audit')")
    @GetMapping("/{matchId}/audit-log")
    public AjaxResult auditLog(@PathVariable Long matchId) {
        return success(statService.selectAuditLog(matchId));
    }
}
