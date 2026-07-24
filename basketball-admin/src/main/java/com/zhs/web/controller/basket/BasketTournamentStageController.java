package com.zhs.web.controller.basket;

import com.zhs.basket.query.StageQuery;
import com.zhs.basket.req.StageCreateReq;
import com.zhs.basket.req.StageUpdateReq;
import com.zhs.basket.service.IBasketTournamentStageService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/stage")
public class BasketTournamentStageController extends BaseController {

    @Autowired
    private IBasketTournamentStageService stageService;

    @PreAuthorize("@ss.hasPermi('basket:stage:list')")
    @GetMapping("/list")
    public TableDataInfo list(StageQuery query) {
        startPage();
        return getDataTable(stageService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('basket:stage:query')")
    @GetMapping("/{stageId}")
    public AjaxResult getInfo(@PathVariable Long stageId) {
        return success(stageService.selectById(stageId));
    }

    @PreAuthorize("@ss.hasPermi('basket:stage:add')")
    @Log(title = "阶段管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody StageCreateReq req) {
        return success(stageService.create(req));
    }

    @PreAuthorize("@ss.hasPermi('basket:stage:edit')")
    @Log(title = "阶段管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{stageId}")
    public AjaxResult edit(@PathVariable Long stageId, @RequestBody StageUpdateReq req) {
        stageService.update(stageId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:stage:status')")
    @Log(title = "阶段管理", businessType = BusinessType.UPDATE)
    @PostMapping("/{stageId}/activate")
    public AjaxResult activate(@PathVariable Long stageId) {
        stageService.activate(stageId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:stage:status')")
    @Log(title = "阶段管理", businessType = BusinessType.UPDATE)
    @PostMapping("/{stageId}/finish")
    public AjaxResult finish(@PathVariable Long stageId) {
        stageService.finish(stageId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:stage:status')")
    @Log(title = "阶段管理", businessType = BusinessType.UPDATE)
    @PostMapping("/{stageId}/cancel")
    public AjaxResult cancel(@PathVariable Long stageId) {
        stageService.cancel(stageId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:stage:remove')")
    @Log(title = "阶段管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{stageId}")
    public AjaxResult remove(@PathVariable Long stageId) {
        stageService.delete(stageId);
        return success();
    }
}
