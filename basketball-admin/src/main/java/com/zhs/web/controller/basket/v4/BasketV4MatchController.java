package com.zhs.web.controller.basket.v4;

import com.zhs.basket.constant.BasketEventV4Perms;
import com.zhs.basket.dto.v4.BasketV4MatchUpdateReq;
import com.zhs.basket.query.v4.BasketV4MatchQuery;
import com.zhs.basket.service.v4.IBasketV4MatchService;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/v4/match")
public class BasketV4MatchController extends BaseController {

    @Autowired
    private IBasketV4MatchService matchService;

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.MATCH_LIST + "')")
    @GetMapping("/list")
    public TableDataInfo list(BasketV4MatchQuery query) {
        startPage();
        return getDataTable(matchService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.MATCH_QUERY + "')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(matchService.selectById(id));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.MATCH_EDIT + "')")
    @PutMapping("/{id}")
    public AjaxResult update(@PathVariable Long id, @RequestBody BasketV4MatchUpdateReq req) {
        matchService.updateDraft(id, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.MATCH_REMOVE + "')")
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        matchService.deleteDraft(id);
        return success();
    }
}
