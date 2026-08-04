package com.zhs.web.controller.basket.v4;

import com.zhs.basket.constant.BasketEventV4Perms;
import com.zhs.basket.query.v4.BasketScheduleBatchQuery;
import com.zhs.basket.service.v4.IBasketV4ScheduleBatchService;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/v4/schedule-batch")
public class BasketV4ScheduleBatchController extends BaseController {

    @Autowired
    private IBasketV4ScheduleBatchService scheduleBatchService;

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.SCHEDULE_BATCH_LIST + "')")
    @GetMapping("/list")
    public TableDataInfo list(BasketScheduleBatchQuery query) {
        startPage();
        return getDataTable(scheduleBatchService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.SCHEDULE_BATCH_QUERY + "')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(scheduleBatchService.selectById(id));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.SCHEDULE_BATCH_PUBLISH + "')")
    @PutMapping("/{id}/publish")
    public AjaxResult publish(@PathVariable Long id) {
        scheduleBatchService.publish(id);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.SCHEDULE_BATCH_CANCEL + "')")
    @PutMapping("/{id}/cancel")
    public AjaxResult cancel(@PathVariable Long id) {
        scheduleBatchService.cancel(id);
        return success();
    }
}
