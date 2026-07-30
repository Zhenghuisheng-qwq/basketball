package com.zhs.web.controller.basket;

import com.zhs.basket.service.IBasketScheduleRuleService;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/schedule/rule")
public class BasketScheduleRuleController extends BaseController {

    @Autowired
    private IBasketScheduleRuleService ruleService;

    @PreAuthorize("@ss.hasPermi('basket:schedule:list')")
    @GetMapping("/list")
    public AjaxResult list() {
        return success(ruleService.listEnabled());
    }

    @PreAuthorize("@ss.hasPermi('basket:schedule:query')")
    @GetMapping("/{ruleId}")
    public AjaxResult detail(@PathVariable Long ruleId) {
        return success(ruleService.getDetail(ruleId));
    }
}
