package com.zhs.web.controller.basket.v4;

import com.zhs.basket.constant.BasketEventV4Perms;
import com.zhs.basket.dto.v4.BasketCaptainGrantAdjustReq;
import com.zhs.basket.dto.v4.BasketCaptainGrantCreateReq;
import com.zhs.basket.dto.v4.BasketCaptainGrantSyncReq;
import com.zhs.basket.query.v4.BasketCaptainGrantQuery;
import com.zhs.basket.service.v4.IBasketV4CaptainGrantService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/v4/captain-grant")
public class BasketV4CaptainGrantController extends BaseController {

    @Autowired
    private IBasketV4CaptainGrantService captainGrantService;

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.CAPTAIN_GRANT_LIST + "')")
    @GetMapping("/list")
    public TableDataInfo list(BasketCaptainGrantQuery query) {
        startPage();
        return getDataTable(captainGrantService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.CAPTAIN_GRANT_LIST + "')")
    @GetMapping("/resolve-valid-to")
    public AjaxResult resolveValidTo(@RequestParam Long divisionId) {
        return success(captainGrantService.resolveValidTo(divisionId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.CAPTAIN_GRANT_ADD + "')")
    @Log(title = "V4 CaptainGrant", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody BasketCaptainGrantCreateReq req) {
        return success(captainGrantService.create(req));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.CAPTAIN_GRANT_REVOKE + "')")
    @Log(title = "V4 CaptainGrant", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/revoke")
    public AjaxResult revoke(@PathVariable Long id) {
        captainGrantService.revoke(id);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.CAPTAIN_GRANT_EDIT + "')")
    @Log(title = "V4 CaptainGrant调整有效期", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/valid-to")
    public AjaxResult adjustValidTo(@PathVariable Long id, @RequestBody BasketCaptainGrantAdjustReq req) {
        captainGrantService.adjustValidTo(id, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.CAPTAIN_GRANT_EDIT + "')")
    @PostMapping("/sync-event-time/preview")
    public AjaxResult previewSync(@RequestBody BasketCaptainGrantSyncReq req) {
        return success(captainGrantService.previewSync(req));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.CAPTAIN_GRANT_EDIT + "')")
    @Log(title = "V4 CaptainGrant同步赛事时间", businessType = BusinessType.UPDATE)
    @PostMapping("/sync-event-time")
    public AjaxResult syncEventTime(@RequestBody BasketCaptainGrantSyncReq req) {
        return success(captainGrantService.syncEventTime(req));
    }
}
