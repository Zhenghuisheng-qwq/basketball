package com.zhs.web.controller.basket;

import com.zhs.basket.constant.BasketContentPerms;
import com.zhs.basket.req.HonorRecordAddReq;
import com.zhs.basket.req.HonorRecordEditReq;
import com.zhs.basket.req.HonorRecordQueryReq;
import com.zhs.basket.req.HonorRecordRevokeReq;
import com.zhs.basket.req.PlayerHonorCandidateQueryReq;
import com.zhs.basket.service.IBasketHonorRecordService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/player-honor")
public class BasketPlayerHonorController extends BaseController {

    private static final String RECIPIENT_PLAYER = "PLAYER";
    private static final String SOURCE_PLAYER_HONOR = "PLAYER_HONOR";

    @Autowired
    private IBasketHonorRecordService honorRecordService;

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.PLAYER_HONOR_LIST + "')")
    @GetMapping("/list")
    public TableDataInfo list(HonorRecordQueryReq query) {
        startPage();
        return getDataTable(honorRecordService.selectList(query, RECIPIENT_PLAYER));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.PLAYER_HONOR_QUERY + "')")
    @GetMapping("/candidate")
    public AjaxResult candidate(PlayerHonorCandidateQueryReq query) {
        return success(honorRecordService.listPlayerCandidates(query));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.PLAYER_HONOR_QUERY + "')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id) {
        return success(honorRecordService.selectById(id, RECIPIENT_PLAYER));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.PLAYER_HONOR_ADD + "')")
    @Log(title = "球员荣誉", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody HonorRecordAddReq req) {
        return success(honorRecordService.add(req, RECIPIENT_PLAYER, SOURCE_PLAYER_HONOR));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.PLAYER_HONOR_EDIT + "')")
    @Log(title = "球员荣誉", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @RequestBody HonorRecordEditReq req) {
        honorRecordService.edit(id, req, RECIPIENT_PLAYER);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.PLAYER_HONOR_PUBLISH + "')")
    @Log(title = "球员荣誉", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/publish")
    public AjaxResult publish(@PathVariable Long id) {
        honorRecordService.publish(id, RECIPIENT_PLAYER);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.PLAYER_HONOR_REVOKE + "')")
    @Log(title = "球员荣誉", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/revoke")
    public AjaxResult revoke(@PathVariable Long id, @RequestBody HonorRecordRevokeReq req) {
        honorRecordService.revoke(id, req, RECIPIENT_PLAYER);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketContentPerms.PLAYER_HONOR_REMOVE + "')")
    @Log(title = "球员荣誉", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) {
        honorRecordService.remove(id, RECIPIENT_PLAYER);
        return success();
    }
}
