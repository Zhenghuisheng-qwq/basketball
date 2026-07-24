package com.zhs.web.controller.basket;

import com.zhs.basket.req.MatchForfeitReq;
import com.zhs.basket.req.MatchPeriodScoreSaveReq;
import com.zhs.basket.req.MatchReopenReq;
import com.zhs.basket.service.IBasketMatchScoreService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/match")
public class BasketMatchScoreController extends BaseController {

    @Autowired
    private IBasketMatchScoreService scoreService;

    @PreAuthorize("@ss.hasPermi('basket:score:query')")
    @GetMapping("/{matchId}/score")
    public AjaxResult score(@PathVariable Long matchId) {
        return success(scoreService.selectScore(matchId));
    }

    @PreAuthorize("@ss.hasPermi('basket:score:edit')")
    @Log(title = "比赛比分", businessType = BusinessType.UPDATE)
    @PutMapping("/{matchId}/period-score")
    public AjaxResult savePeriodScore(@PathVariable Long matchId, @RequestBody MatchPeriodScoreSaveReq req) {
        scoreService.savePeriodScores(matchId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:score:submit')")
    @Log(title = "比赛比分", businessType = BusinessType.UPDATE)
    @PostMapping("/{matchId}/score/submit")
    public AjaxResult submitScore(@PathVariable Long matchId) {
        scoreService.submitScore(matchId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:score:confirm')")
    @Log(title = "比赛比分", businessType = BusinessType.UPDATE)
    @PostMapping("/{matchId}/score/confirm")
    public AjaxResult confirmScore(@PathVariable Long matchId) {
        scoreService.confirmScore(matchId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:score:reopen')")
    @Log(title = "比赛比分", businessType = BusinessType.UPDATE)
    @PostMapping("/{matchId}/score/reopen")
    public AjaxResult reopenScore(@PathVariable Long matchId, @RequestBody MatchReopenReq req) {
        scoreService.reopenScore(matchId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:score:edit')")
    @Log(title = "比赛比分", businessType = BusinessType.UPDATE)
    @PostMapping("/{matchId}/forfeit")
    public AjaxResult forfeit(@PathVariable Long matchId, @RequestBody MatchForfeitReq req) {
        scoreService.forfeit(matchId, req);
        return success();
    }
}
