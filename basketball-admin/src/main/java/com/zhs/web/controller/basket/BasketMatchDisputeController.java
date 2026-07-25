package com.zhs.web.controller.basket;

import com.zhs.basket.query.MatchDisputeQuery;
import com.zhs.basket.req.MatchDisputeCreateReq;
import com.zhs.basket.req.MatchDisputeHandleReq;
import com.zhs.basket.service.IBasketMatchDisputeService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/matchDispute")
public class BasketMatchDisputeController extends BaseController {

    @Autowired
    private IBasketMatchDisputeService disputeService;

    @PreAuthorize("@ss.hasPermi('basket:matchDispute:list')")
    @GetMapping("/list")
    public AjaxResult list(MatchDisputeQuery query) {
        return success(disputeService.list(query));
    }

    @PreAuthorize("@ss.hasPermi('basket:matchDispute:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) {
        return success(disputeService.getById(id));
    }

    @PreAuthorize("@ss.hasPermi('basket:matchDispute:add')")
    @Log(title = "比赛数据争议", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult create(@RequestBody MatchDisputeCreateReq req) {
        return success(disputeService.create(req));
    }

    @PreAuthorize("@ss.hasPermi('basket:matchDispute:handle')")
    @Log(title = "比赛数据争议", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/reject")
    public AjaxResult reject(@PathVariable Long id, @RequestBody MatchDisputeHandleReq req) {
        disputeService.reject(id, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:matchDispute:handle')")
    @Log(title = "比赛数据争议", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/uphold")
    public AjaxResult uphold(@PathVariable Long id, @RequestBody MatchDisputeHandleReq req) {
        disputeService.uphold(id, req);
        return success();
    }
}
