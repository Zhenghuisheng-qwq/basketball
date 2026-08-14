package com.zhs.web.controller.basket.v4;

import com.zhs.basket.constant.BasketEventV4Perms;
import com.zhs.basket.dto.v4.BasketDivisionCreateReq;
import com.zhs.basket.dto.v4.BasketDivisionFinishReq;
import com.zhs.basket.dto.v4.BasketDivisionUpdateReq;
import com.zhs.basket.query.v4.BasketDivisionQuery;
import com.zhs.basket.service.v4.IBasketV4DivisionService;
import com.zhs.basket.service.v4.IDivisionOperationOverviewQueryService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/v4/division")
public class BasketV4DivisionController extends BaseController {

    @Autowired
    private IBasketV4DivisionService divisionService;
    @Autowired
    private IDivisionOperationOverviewQueryService overviewQueryService;

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.DIVISION_LIST + "')")
    @GetMapping("/list")
    public TableDataInfo list(BasketDivisionQuery query) {
        startPage();
        return getDataTable(divisionService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.DIVISION_LIST + "')")
    @GetMapping("/selector-list")
    public TableDataInfo selectorList(BasketDivisionQuery query,
                                      @RequestParam(required = false) String entry) {
        startPage();
        return getDataTable(overviewQueryService.listSelector(query, entry));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.DIVISION_QUERY + "')")
    @GetMapping("/{id}/workflow-overview")
    public AjaxResult workflowOverview(@PathVariable Long id) {
        return success(overviewQueryService.getOverview(id));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.DIVISION_QUERY + "')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(divisionService.selectById(id));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.DIVISION_ADD + "')")
    @Log(title = "V4 Division", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BasketDivisionCreateReq req) {
        return success(divisionService.create(req));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.DIVISION_EDIT + "')")
    @Log(title = "V4 Division", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @RequestBody BasketDivisionUpdateReq req) {
        divisionService.update(id, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.DIVISION_OPEN + "')")
    @Log(title = "V4 Division", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/open-registration")
    public AjaxResult openRegistration(@PathVariable Long id) {
        divisionService.openRegistration(id);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.DIVISION_CLOSE + "')")
    @Log(title = "V4 Division", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/close-registration")
    public AjaxResult closeRegistration(@PathVariable Long id) {
        divisionService.closeRegistration(id);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.DIVISION_FINISH + "')")
    @Log(title = "V4 Division 结束赛事", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/finish")
    public AjaxResult finish(@PathVariable Long id, @RequestBody(required = false) BasketDivisionFinishReq req) {
        divisionService.finish(id, req == null ? null : req.getReason());
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.DIVISION_REMOVE + "')")
    @Log(title = "V4 Division", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        divisionService.delete(id);
        return success();
    }
}
