package com.zhs.web.controller.basket;

import com.zhs.basket.req.RoundCreateReq;
import com.zhs.basket.req.RoundUpdateReq;
import com.zhs.basket.service.IBasketTournamentRoundService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class BasketTournamentRoundController extends BaseController {

    @Autowired
    private IBasketTournamentRoundService roundService;

    @PreAuthorize("@ss.hasPermi('basket:round:list')")
    @GetMapping("/basket/stage/{stageId}/round/list")
    public AjaxResult listByStage(@PathVariable Long stageId) {
        return success(roundService.selectByStageId(stageId));
    }

    @PreAuthorize("@ss.hasPermi('basket:round:query')")
    @GetMapping("/basket/round/{roundId}")
    public AjaxResult getInfo(@PathVariable Long roundId) {
        return success(roundService.selectById(roundId));
    }

    @PreAuthorize("@ss.hasPermi('basket:round:add')")
    @Log(title = "轮次管理", businessType = BusinessType.INSERT)
    @PostMapping("/basket/stage/{stageId}/round")
    public AjaxResult add(@PathVariable Long stageId, @RequestBody RoundCreateReq req) {
        return success(roundService.create(stageId, req));
    }

    @PreAuthorize("@ss.hasPermi('basket:round:edit')")
    @Log(title = "轮次管理", businessType = BusinessType.UPDATE)
    @PutMapping("/basket/round/{roundId}")
    public AjaxResult edit(@PathVariable Long roundId, @RequestBody RoundUpdateReq req) {
        roundService.update(roundId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:round:status')")
    @Log(title = "轮次管理", businessType = BusinessType.UPDATE)
    @PostMapping("/basket/round/{roundId}/activate")
    public AjaxResult activate(@PathVariable Long roundId) {
        roundService.activate(roundId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:round:status')")
    @Log(title = "轮次管理", businessType = BusinessType.UPDATE)
    @PostMapping("/basket/round/{roundId}/finish")
    public AjaxResult finish(@PathVariable Long roundId) {
        roundService.finish(roundId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:round:status')")
    @Log(title = "轮次管理", businessType = BusinessType.UPDATE)
    @PostMapping("/basket/round/{roundId}/cancel")
    public AjaxResult cancel(@PathVariable Long roundId) {
        roundService.cancel(roundId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:round:remove')")
    @Log(title = "轮次管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/basket/round/{roundId}")
    public AjaxResult remove(@PathVariable Long roundId) {
        roundService.delete(roundId);
        return success();
    }
}
