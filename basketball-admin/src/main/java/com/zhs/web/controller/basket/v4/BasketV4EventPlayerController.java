package com.zhs.web.controller.basket.v4;

import com.zhs.basket.constant.BasketEventV4Perms;
import com.zhs.basket.query.v4.BasketEventPlayerQuery;
import com.zhs.basket.service.v4.IBasketV4EventPlayerService;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.page.TableDataInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/v4/event-player")
public class BasketV4EventPlayerController extends BaseController {

    @Autowired
    private IBasketV4EventPlayerService eventPlayerService;

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.EVENT_PLAYER_LIST + "')")
    @GetMapping("/list")
    public TableDataInfo list(BasketEventPlayerQuery query) {
        startPage();
        return getDataTable(eventPlayerService.selectList(query));
    }
}
