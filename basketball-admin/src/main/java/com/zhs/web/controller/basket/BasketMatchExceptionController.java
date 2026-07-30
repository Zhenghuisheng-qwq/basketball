package com.zhs.web.controller.basket;

import com.zhs.basket.query.MatchExceptionQuery;
import com.zhs.basket.req.MatchExceptionDecideReq;
import com.zhs.basket.service.IBasketMatchExceptionService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/basket/matchException")
public class BasketMatchExceptionController extends BaseController {

    @Autowired
    private IBasketMatchExceptionService exceptionService;

    @PreAuthorize("@ss.hasPermi('basket:matchException:list')")
    @GetMapping({"/list", "/page"})
    public AjaxResult list(MatchExceptionQuery query) {
        return success(exceptionService.list(query));
    }

    @PreAuthorize("@ss.hasPermi('basket:matchException:query')")
    @GetMapping("/{applicationId}")
    public AjaxResult detail(@PathVariable Long applicationId) {
        return success(exceptionService.getDetail(applicationId));
    }

    @PreAuthorize("@ss.hasPermi('basket:matchException:reject')")
    @Log(title = "异常比赛申请", businessType = BusinessType.UPDATE)
    @PostMapping("/{applicationId}/reject")
    public AjaxResult reject(@PathVariable Long applicationId, @RequestBody Map<String, String> body) {
        exceptionService.reject(applicationId, body.get("rejectReason"), body.get("auditRemark"));
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:matchException:decide')")
    @Log(title = "异常比赛裁定", businessType = BusinessType.UPDATE)
    @PostMapping("/{applicationId}/decide")
    public AjaxResult decide(@PathVariable Long applicationId, @RequestBody MatchExceptionDecideReq req) {
        exceptionService.decide(applicationId, req);
        return success();
    }
}
