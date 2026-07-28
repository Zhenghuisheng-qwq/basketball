package com.zhs.web.controller.basket;

import com.zhs.basket.query.CaptainQualificationQuery;
import com.zhs.basket.req.CaptainQualificationBindTeamReq;
import com.zhs.basket.req.CaptainQualificationGrantReq;
import com.zhs.basket.req.CaptainQualificationRevokeReq;
import com.zhs.basket.req.CaptainQualificationUpdateReq;
import com.zhs.basket.service.IBasketCaptainQualificationService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.core.page.TableDataInfo;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/captainQualification")
public class BasketCaptainQualificationController extends BaseController {

    @Autowired
    private IBasketCaptainQualificationService captainQualificationService;

    @PreAuthorize("@ss.hasPermi('basket:captainQualification:list')")
    @GetMapping("/grant-season-options")
    public AjaxResult grantSeasonOptions() {
        return success(captainQualificationService.selectGrantSeasonOptions());
    }

    @PreAuthorize("@ss.hasPermi('basket:captainQualification:list')")
    @GetMapping("/eligible-list")
    public AjaxResult eligibleList() {
        return success(captainQualificationService.selectEligibleList());
    }

    @PreAuthorize("@ss.hasPermi('basket:captainQualification:list')")
    @GetMapping("/list")
    public TableDataInfo list(CaptainQualificationQuery query) {
        startPage();
        return getDataTable(captainQualificationService.selectList(query));
    }

    @PreAuthorize("@ss.hasPermi('basket:captainQualification:query')")
    @GetMapping("/{qualificationId}")
    public AjaxResult getInfo(@PathVariable Long qualificationId) {
        return success(captainQualificationService.selectDetail(qualificationId));
    }

    @PreAuthorize("@ss.hasPermi('basket:captainQualification:grant')")
    @Log(title = "队长授权", businessType = BusinessType.INSERT)
    @PostMapping("/grant")
    public AjaxResult grant(@RequestBody CaptainQualificationGrantReq req) {
        captainQualificationService.grant(req);
        return success("授予队长权限成功");
    }

    @PreAuthorize("@ss.hasPermi('basket:captainQualification:edit')")
    @Log(title = "队长授权", businessType = BusinessType.UPDATE)
    @PutMapping("/{qualificationId}")
    public AjaxResult update(@PathVariable Long qualificationId, @RequestBody CaptainQualificationUpdateReq req) {
        captainQualificationService.update(qualificationId, req);
        return success("更新成功");
    }

    @PreAuthorize("@ss.hasPermi('basket:captainQualification:revoke')")
    @Log(title = "队长授权", businessType = BusinessType.UPDATE)
    @PostMapping("/{qualificationId}/revoke")
    public AjaxResult revoke(@PathVariable Long qualificationId, @RequestBody CaptainQualificationRevokeReq req) {
        captainQualificationService.revoke(qualificationId, req);
        return success("收回队长权限成功");
    }

    @PreAuthorize("@ss.hasPermi('basket:captainQualification:edit')")
    @Log(title = "队长授权", businessType = BusinessType.UPDATE)
    @PostMapping("/{qualificationId}/bind-team")
    public AjaxResult bindTeam(@PathVariable Long qualificationId, @RequestBody CaptainQualificationBindTeamReq req) {
        captainQualificationService.bindTeam(qualificationId, req);
        return success("绑定球队成功");
    }
}
