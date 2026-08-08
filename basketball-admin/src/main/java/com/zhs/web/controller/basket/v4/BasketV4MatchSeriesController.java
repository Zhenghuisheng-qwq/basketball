package com.zhs.web.controller.basket.v4;

import com.zhs.basket.constant.BasketEventV4Perms;
import com.zhs.basket.service.v4.IBasketMatchSeriesDisplayService;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/v4/match-series")
public class BasketV4MatchSeriesController extends BaseController {

    @Autowired
    private IBasketMatchSeriesDisplayService matchSeriesDisplayService;

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.MATCH_QUERY + "')")
    @GetMapping("/{seriesId}")
    public AjaxResult getInfo(@PathVariable Long seriesId) {
        return success(matchSeriesDisplayService.getDetail(seriesId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.MATCH_LIST + "')")
    @GetMapping("/stage/{stageId}")
    public AjaxResult listByStage(@PathVariable Long stageId) {
        return success(matchSeriesDisplayService.listByStageId(stageId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.MATCH_QUERY + "')")
    @GetMapping("/context/match/{matchId}")
    public AjaxResult getContextByMatch(@PathVariable Long matchId) {
        return success(matchSeriesDisplayService.getContextByMatchId(matchId));
    }
}
