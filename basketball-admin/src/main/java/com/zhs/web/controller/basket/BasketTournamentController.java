package com.zhs.web.controller.basket;

import com.zhs.basket.query.TournamentQuery;
import com.zhs.basket.service.IBasketTournamentService;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
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
}