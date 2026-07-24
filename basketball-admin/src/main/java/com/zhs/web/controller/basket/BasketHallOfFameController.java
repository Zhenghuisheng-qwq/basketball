package com.zhs.web.controller.basket;

import com.zhs.basket.constant.BasketContentPerms;
import com.zhs.basket.enums.HonorDisplayStatusEnum;
import com.zhs.basket.req.HonorRecordAddReq;
import com.zhs.basket.req.HonorRecordEditReq;
import com.zhs.basket.req.HonorRecordQueryReq;
import com.zhs.basket.service.IBasketHonorRecordService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * 名人堂后台：本质管理 honor_record（可筛选 scope），无独立表。
 */
@RestController
@RequestMapping("/basket/hall-of-fame")
public class BasketHallOfFameController extends BaseController {

    private static final String SOURCE_HALL = "HALL_OF_FAME";

    @Autowired
    private IBasketHonorRecordService honorRecordService;

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.HALL_OF_FAME_LIST + "')")
    @GetMapping("/list")
    public TableDataInfo list(HonorRecordQueryReq query) {
        startPage();
        HonorRecordQueryReq q = query == null ? new HonorRecordQueryReq() : query;
        // 默认看已发布；允许调用方覆盖 displayStatus / honorScope
        if (q.getDisplayStatus() == null) {
            q.setDisplayStatus(HonorDisplayStatusEnum.PUBLISHED.getCode());
        }
        return getDataTable(honorRecordService.selectList(q, null));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.HALL_OF_FAME_QUERY + "')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(honorRecordService.selectById(id, null));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.HALL_OF_FAME_ADD + "')")
    @Log(title = "名人堂", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody HonorRecordAddReq req) {
        return success(honorRecordService.add(req, null, SOURCE_HALL));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.HALL_OF_FAME_EDIT + "')")
    @Log(title = "名人堂", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @RequestBody HonorRecordEditReq req) {
        honorRecordService.edit(id, req, null);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.HALL_OF_FAME_PUBLISH + "')")
    @Log(title = "名人堂", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/publish")
    public AjaxResult publish(@PathVariable Long id) {
        honorRecordService.publish(id, null);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.HALL_OF_FAME_OFFLINE + "')")
    @Log(title = "名人堂", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/offline")
    public AjaxResult offline(@PathVariable Long id) {
        honorRecordService.offline(id);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.HALL_OF_FAME_REMOVE + "')")
    @Log(title = "名人堂", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        honorRecordService.remove(id, null);
        return success();
    }
}
