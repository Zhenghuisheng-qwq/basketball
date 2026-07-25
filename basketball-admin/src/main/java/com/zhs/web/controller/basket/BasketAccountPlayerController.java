package com.zhs.web.controller.basket;

import com.zhs.basket.query.AccountPlayerQuery;
import com.zhs.basket.service.IBasketAccountPlayerService;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/accountPlayer")
public class BasketAccountPlayerController extends BaseController {

    @Autowired
    private IBasketAccountPlayerService accountPlayerService;

    @PreAuthorize("@ss.hasPermi('basket:accountPlayer:list')")
    @GetMapping("/list")
    public TableDataInfo list(AccountPlayerQuery query) {
        startPage();
        return getDataTable(accountPlayerService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('basket:accountPlayer:query')")
    @GetMapping("/{memberId}")
    public AjaxResult getInfo(@PathVariable String memberId) {
        return success(accountPlayerService.selectDetail(Long.valueOf(memberId)));
    }
}
