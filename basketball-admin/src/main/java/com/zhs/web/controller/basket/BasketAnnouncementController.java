package com.zhs.web.controller.basket;

import com.zhs.basket.constant.BasketContentPerms;
import com.zhs.basket.req.AnnouncementAddReq;
import com.zhs.basket.req.AnnouncementEditReq;
import com.zhs.basket.req.AnnouncementQueryReq;
import com.zhs.basket.service.IBasketAnnouncementService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/announcement")
public class BasketAnnouncementController extends BaseController {

    @Autowired
    private IBasketAnnouncementService announcementService;

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.ANNOUNCEMENT_LIST + "')")
    @GetMapping("/list")
    public TableDataInfo list(AnnouncementQueryReq query) {
        startPage();
        return getDataTable(announcementService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.ANNOUNCEMENT_QUERY + "')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(announcementService.selectById(id));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.ANNOUNCEMENT_ADD + "')")
    @Log(title = "公告管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody AnnouncementAddReq req) {
        return success(announcementService.add(req));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.ANNOUNCEMENT_EDIT + "')")
    @Log(title = "公告管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @RequestBody AnnouncementEditReq req) {
        announcementService.edit(id, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.ANNOUNCEMENT_PUBLISH + "')")
    @Log(title = "公告管理", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/publish")
    public AjaxResult publish(@PathVariable Long id) {
        announcementService.publish(id);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.ANNOUNCEMENT_OFFLINE + "')")
    @Log(title = "公告管理", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/offline")
    public AjaxResult offline(@PathVariable Long id) {
        announcementService.offline(id);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.ANNOUNCEMENT_TOP + "')")
    @Log(title = "公告管理", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/top")
    public AjaxResult top(@PathVariable Long id) {
        announcementService.top(id);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.ANNOUNCEMENT_TOP + "')")
    @Log(title = "公告管理", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/untop")
    public AjaxResult untop(@PathVariable Long id) {
        announcementService.untop(id);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.ANNOUNCEMENT_REMOVE + "')")
    @Log(title = "公告管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        announcementService.remove(id);
        return success();
    }
}
