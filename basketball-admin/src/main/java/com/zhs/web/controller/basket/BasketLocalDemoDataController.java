package com.zhs.web.controller.basket;

import com.zhs.basket.localdemo.constant.BasketLocalDemoPerms;
import com.zhs.basket.localdemo.service.IBasketLocalDemoDataService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.enums.BusinessType;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/basket/local-demo-data")
@Profile("local")
@ConditionalOnProperty(prefix = "cnpa.local-demo", name = "enabled", havingValue = "true")
public class BasketLocalDemoDataController extends BaseController {

    private final IBasketLocalDemoDataService localDemoDataService;

    public BasketLocalDemoDataController(IBasketLocalDemoDataService localDemoDataService) {
        this.localDemoDataService = localDemoDataService;
    }

    @PreAuthorize("@ss.hasPermi('" + BasketLocalDemoPerms.QUERY + "')")
    @GetMapping("/status")
    public AjaxResult status() {
        return success(localDemoDataService.getStatus());
    }

    @PreAuthorize("@ss.hasPermi('" + BasketLocalDemoPerms.SEED + "')")
    @Log(title = "本地演示数据", businessType = BusinessType.INSERT)
    @PostMapping("/seed")
    public AjaxResult seed() {
        return success(localDemoDataService.seedLocalDemoData());
    }

    @PreAuthorize("@ss.hasPermi('" + BasketLocalDemoPerms.RESET + "')")
    @Log(title = "本地演示数据", businessType = BusinessType.DELETE)
    @DeleteMapping("/reset")
    public AjaxResult reset(@RequestParam("confirmMarker") String confirmMarker) {
        return success(localDemoDataService.resetLocalDemoData(confirmMarker));
    }
}
