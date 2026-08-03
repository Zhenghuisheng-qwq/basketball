package com.zhs.web.controller.basket.v4;

import com.zhs.basket.constant.BasketEventV4Perms;
import com.zhs.basket.query.v4.BasketEventTeamQuery;
import com.zhs.basket.service.v4.IBasketV4EventTeamService;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/v4/event-team")
public class BasketV4EventTeamController extends BaseController {

    @Autowired
    private IBasketV4EventTeamService eventTeamService;

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.EVENT_TEAM_LIST + "')")
    @GetMapping("/list")
    public TableDataInfo list(BasketEventTeamQuery query) {
        startPage();
        return getDataTable(eventTeamService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.EVENT_TEAM_QUERY + "')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(eventTeamService.selectById(id));
    }
}
