package com.zhs.web.controller.basket;

import com.zhs.basket.query.MatchQuery;
import com.zhs.basket.req.*;
import com.zhs.basket.service.IBasketMatchScheduleService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/match")
public class BasketMatchScheduleController extends BaseController {

    @Autowired
    private IBasketMatchScheduleService scheduleService;

    @PreAuthorize("@ss.hasPermi('basket:match:list')")
    @GetMapping("/list")
    public TableDataInfo list(MatchQuery query) {
        startPage();
        return getDataTable(scheduleService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('basket:match:query')")
    @GetMapping("/{matchId}")
    public AjaxResult getInfo(@PathVariable Long matchId) {
        return success(scheduleService.selectById(matchId));
    }

    @PreAuthorize("@ss.hasPermi('basket:match:query')")
    @GetMapping("/venues")
    public AjaxResult venues() {
        return success(scheduleService.selectVenues());
    }

    @PreAuthorize("@ss.hasPermi('basket:match:add')")
    @Log(title = "比赛管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody MatchCreateReq req) {
        return success(scheduleService.create(req));
    }

    @PreAuthorize("@ss.hasPermi('basket:match:edit')")
    @Log(title = "比赛管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{matchId}")
    public AjaxResult edit(@PathVariable Long matchId, @RequestBody MatchUpdateReq req) {
        scheduleService.update(matchId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:match:schedule')")
    @Log(title = "比赛管理", businessType = BusinessType.UPDATE)
    @PostMapping("/{matchId}/schedule")
    public AjaxResult schedule(@PathVariable Long matchId, @RequestBody MatchScheduleReq req) {
        scheduleService.schedule(matchId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:match:postpone')")
    @Log(title = "比赛管理", businessType = BusinessType.UPDATE)
    @PostMapping("/{matchId}/postpone")
    public AjaxResult postpone(@PathVariable Long matchId, @RequestBody MatchPostponeReq req) {
        scheduleService.postpone(matchId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:match:cancel')")
    @Log(title = "比赛管理", businessType = BusinessType.UPDATE)
    @PostMapping("/{matchId}/cancel")
    public AjaxResult cancel(@PathVariable Long matchId, @RequestBody MatchCancelReq req) {
        scheduleService.cancel(matchId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:match:start')")
    @Log(title = "比赛管理", businessType = BusinessType.UPDATE)
    @PostMapping("/{matchId}/start")
    public AjaxResult start(@PathVariable Long matchId) {
        scheduleService.start(matchId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:match:remove')")
    @Log(title = "比赛管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{matchId}")
    public AjaxResult remove(@PathVariable Long matchId) {
        scheduleService.delete(matchId);
        return success();
    }
}
