package com.zhs.web.controller.basket.v4;

import com.zhs.basket.constant.BasketEventV4Perms;
import com.zhs.basket.service.v4.IBasketV4FormatSnapshotService;
import com.zhs.basket.service.v4.IBasketV4FormatTemplateService;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/v4/format-template")
public class BasketV4FormatTemplateController extends BaseController {

    @Autowired
    private IBasketV4FormatTemplateService templateService;
    @Autowired
    private IBasketV4FormatSnapshotService snapshotService;

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.DIVISION_LIST + "')")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam Long seriesId) {
        return success(templateService.listEnabledTemplates(seriesId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.DIVISION_QUERY + "')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) {
        return success(templateService.getTemplateDetail(id));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.DIVISION_QUERY + "')")
    @GetMapping("/division/{divisionId}/snapshot")
    public AjaxResult divisionSnapshot(@PathVariable Long divisionId) {
        return success(snapshotService.getDivisionSnapshot(divisionId));
    }
}
