package com.zhs.web.controller.basket;

import com.zhs.basket.query.DisciplineQuery;
import com.zhs.basket.req.DisciplineCreateReq;
import com.zhs.basket.req.DisciplineRevokeReq;
import com.zhs.basket.service.IBasketDisciplineService;
import com.zhs.basket.service.IPenaltyExecutionService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/discipline")
public class BasketDisciplineController extends BaseController {

    @Autowired
    private IBasketDisciplineService disciplineService;
    @Autowired
    private IPenaltyExecutionService penaltyExecutionService;

    @PreAuthorize("@ss.hasPermi('basket:discipline:list')")
    @GetMapping("/list")
    public TableDataInfo list(DisciplineQuery query) {
        startPage();
        return getDataTable(disciplineService.list(query));
    }

    @PreAuthorize("@ss.hasPermi('basket:discipline:list')")
    @GetMapping("/selector/seasons")
    public TableDataInfo seasonOptions(@RequestParam(required = false) String keyword) {
        startPage();
        return getDataTable(disciplineService.selectSeasonOptions(keyword));
    }

    @PreAuthorize("@ss.hasPermi('basket:discipline:list')")
    @GetMapping("/selector/players")
    public TableDataInfo playerOptions(@RequestParam Long seasonId,
                                       @RequestParam(required = false) String keyword) {
        startPage();
        return getDataTable(disciplineService.selectPlayerOptions(seasonId, keyword));
    }

    @PreAuthorize("@ss.hasPermi('basket:discipline:list')")
    @GetMapping("/selector/teams")
    public TableDataInfo teamOptions(@RequestParam Long seasonId,
                                     @RequestParam String targetType,
                                     @RequestParam(required = false) String keyword) {
        startPage();
        return getDataTable(disciplineService.selectTeamOptions(seasonId, targetType, keyword));
    }

    @PreAuthorize("@ss.hasPermi('basket:discipline:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) {
        return success(disciplineService.detail(id));
    }

    @PreAuthorize("@ss.hasPermi('basket:discipline:add')")
    @Log(title = "球队与球员处罚", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult create(@RequestBody DisciplineCreateReq req) {
        return success(disciplineService.create(req));
    }

    @PreAuthorize("@ss.hasPermi('basket:discipline:activate')")
    @Log(title = "球队与球员处罚", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/activate")
    public AjaxResult activate(@PathVariable Long id) {
        disciplineService.activate(id);
        if (disciplineService.needsEventExecution(id)) {
            penaltyExecutionService.execute(id);
        }
        return success(disciplineService.detail(id));
    }

    @PreAuthorize("@ss.hasPermi('basket:discipline:activate')")
    @Log(title = "球队与球员处罚", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/retry-execution")
    public AjaxResult retryExecution(@PathVariable Long id) {
        return success(penaltyExecutionService.execute(id));
    }

    @PreAuthorize("@ss.hasPermi('basket:discipline:revoke')")
    @Log(title = "球队与球员处罚", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/revoke")
    public AjaxResult revoke(@PathVariable Long id, @RequestBody DisciplineRevokeReq req) {
        disciplineService.revoke(id, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:discipline:consume')")
    @Log(title = "球队与球员处罚", businessType = BusinessType.UPDATE)
    @PostMapping("/consumeSuspension")
    public AjaxResult consumeSuspension(@RequestParam Long matchId, @RequestParam Long memberId) {
        disciplineService.consumeMatchSuspension(matchId, memberId);
        return success();
    }
}
