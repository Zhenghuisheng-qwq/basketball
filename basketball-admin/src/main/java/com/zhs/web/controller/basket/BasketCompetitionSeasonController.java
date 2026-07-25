package com.zhs.web.controller.basket;

import com.zhs.basket.query.CompetitionSeasonQuery;
import com.zhs.basket.req.CompetitionSeasonCreateReq;
import com.zhs.basket.req.CompetitionSeasonUpdateReq;
import com.zhs.basket.service.IBasketCompetitionSeasonService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/competition-season")
public class BasketCompetitionSeasonController extends BaseController {

    @Autowired
    private IBasketCompetitionSeasonService competitionSeasonService;

    @PreAuthorize("@ss.hasPermi('basket:season:list')")
    @GetMapping("/list")
    public TableDataInfo list(CompetitionSeasonQuery query) {
        startPage();
        return getDataTable(competitionSeasonService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('basket:season:list') or @ss.hasPermi('basket:tournament:list') or @ss.hasPermi('basket:tournament:add')")
    @GetMapping("/options")
    public AjaxResult options() {
        CompetitionSeasonQuery query = new CompetitionSeasonQuery();
        return success(competitionSeasonService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('basket:season:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(competitionSeasonService.selectById(id));
    }

    @PreAuthorize("@ss.hasPermi('basket:season:add')")
    @Log(title = "赛季管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody CompetitionSeasonCreateReq req) {
        return success(competitionSeasonService.create(req));
    }

    @PreAuthorize("@ss.hasPermi('basket:season:edit')")
    @Log(title = "赛季管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @RequestBody CompetitionSeasonUpdateReq req) {
        competitionSeasonService.update(id, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:season:remove')")
    @Log(title = "赛季管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        competitionSeasonService.delete(id);
        return success();
    }
}
