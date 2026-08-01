package com.zhs.web.controller.basket;

import com.zhs.basket.query.ScheduleMatchQuery;
import com.zhs.basket.req.*;
import com.zhs.basket.service.IBasketScheduleGenerateService;
import com.zhs.basket.service.IBasketScheduleMatchAdminService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/schedule/manage")
public class BasketScheduleManageController extends BaseController {

    @Autowired private IBasketScheduleGenerateService generateService;
    @Autowired private IBasketScheduleMatchAdminService matchAdminService;

    @PreAuthorize("@ss.hasPermi('basket:schedule:query')")
    @GetMapping("/event/{eventId}/context")
    public AjaxResult eventContext(@PathVariable Long eventId) {
        return success(generateService.getEventContext(eventId));
    }

    @PreAuthorize("@ss.hasPermi('basket:schedule:query')")
    @PostMapping("/preview")
    public AjaxResult preview(@RequestBody ScheduleGenerateReq req) {
        return success(generateService.preview(req));
    }

    @PreAuthorize("@ss.hasPermi('basket:schedule:generate')")
    @Log(title = "赛程管理", businessType = BusinessType.INSERT)
    @PostMapping("/generate")
    public AjaxResult generate(@RequestBody ScheduleGenerateReq req) {
        return success(generateService.generateDraftSchedule(req));
    }

    @PreAuthorize("@ss.hasPermi('basket:schedule:publish')")
    @Log(title = "赛程管理", businessType = BusinessType.UPDATE)
    @PostMapping("/publish")
    public AjaxResult publish(@RequestBody SchedulePublishReq req) {
        generateService.publish(req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:schedule:publish')")
    @Log(title = "赛程管理", businessType = BusinessType.UPDATE)
    @PostMapping("/unpublish")
    public AjaxResult unpublish(@RequestBody SchedulePublishReq req) {
        generateService.unpublish(req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:schedule:remove')")
    @Log(title = "赛程管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/draft/{eventId}")
    public AjaxResult deleteDraft(@PathVariable Long eventId, @RequestParam(required = false) String reason) {
        generateService.deleteDraft(eventId, reason);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:schedule:list')")
    @GetMapping("/match/list")
    public TableDataInfo matchList(ScheduleMatchQuery query) {
        startPage();
        return getDataTable(matchAdminService.list(query));
    }

    @PreAuthorize("@ss.hasPermi('basket:schedule:query')")
    @GetMapping("/match/{matchId}")
    public AjaxResult matchDetail(@PathVariable Long matchId) {
        return success(matchAdminService.getDetail(matchId));
    }

    @PreAuthorize("@ss.hasPermi('basket:schedule:add')")
    @Log(title = "赛程管理", businessType = BusinessType.INSERT)
    @PostMapping("/match")
    public AjaxResult createMatch(@RequestBody ScheduleMatchCreateReq req) {
        return success(matchAdminService.createManual(req));
    }

    @PreAuthorize("@ss.hasPermi('basket:schedule:edit')")
    @Log(title = "赛程管理", businessType = BusinessType.UPDATE)
    @PutMapping("/match/{matchId}")
    public AjaxResult updateMatch(@PathVariable Long matchId, @RequestBody ScheduleMatchUpdateReq req) {
        matchAdminService.update(matchId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:schedule:edit')")
    @Log(title = "赛程管理", businessType = BusinessType.UPDATE)
    @PostMapping("/match/{matchId}/swap")
    public AjaxResult swapMatch(@PathVariable Long matchId, @RequestParam(required = false) String reason) {
        matchAdminService.swapHomeAway(matchId, reason);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:schedule:delete')")
    @Log(title = "赛程管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/match/{matchId}")
    public AjaxResult deleteMatch(@PathVariable Long matchId, @RequestParam(required = false) String reason) {
        matchAdminService.deleteDraftMatch(matchId, reason);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:schedule:score')")
    @Log(title = "赛程管理", businessType = BusinessType.UPDATE)
    @PostMapping("/match/{matchId}/score")
    public AjaxResult saveScore(@PathVariable Long matchId, @RequestBody ScheduleScoreSaveReq req) {
        matchAdminService.saveScore(matchId, req);
        return success();
    }
}
