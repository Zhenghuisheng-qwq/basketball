package com.zhs.web.controller.basket;

import com.zhs.basket.query.TournamentQuery;
import com.zhs.basket.req.TournamentCreateReq;
import com.zhs.basket.req.TournamentUpdateReq;
import com.zhs.basket.service.IBasketTournamentService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/tournament")
public class BasketTournamentController extends BaseController {

    @Autowired
    private IBasketTournamentService tournamentService;

    @PreAuthorize("@ss.hasPermi('basket:tournament:list')")
    @GetMapping("/list")
    public TableDataInfo list(TournamentQuery query) {
        startPage();
        return getDataTable(tournamentService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('basket:tournament:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(tournamentService.selectById(id));
    }

    @PreAuthorize("@ss.hasPermi('basket:tournament:add')")
    @Log(title = "赛事管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody TournamentCreateReq req) {
        return success(tournamentService.create(req));
    }

    @PreAuthorize("@ss.hasPermi('basket:tournament:edit')")
    @Log(title = "赛事管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @RequestBody TournamentUpdateReq req) {
        tournamentService.update(id, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:tournament:publish')")
    @Log(title = "赛事管理", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/publish")
    public AjaxResult publish(@PathVariable Long id) {
        tournamentService.publish(id);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:tournament:publish')")
    @Log(title = "赛事管理", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/unpublish")
    public AjaxResult unpublish(@PathVariable Long id) {
        tournamentService.unpublish(id);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:tournament:close')")
    @Log(title = "赛事管理", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/close")
    public AjaxResult close(@PathVariable Long id) {
        tournamentService.close(id);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:tournament:remove')")
    @Log(title = "赛事管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        tournamentService.delete(id);
        return success();
    }
}
