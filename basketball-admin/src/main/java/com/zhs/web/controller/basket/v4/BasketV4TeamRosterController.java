package com.zhs.web.controller.basket.v4;

import com.zhs.basket.constant.BasketEventV4Perms;
import com.zhs.basket.dto.v4.TeamRosterAllocationReq;
import com.zhs.basket.dto.v4.TeamRosterCandidateOrderReq;
import com.zhs.basket.dto.v4.TeamRosterPromoteReq;
import com.zhs.basket.service.v4.ITeamRosterAllocationService;
import com.zhs.basket.service.v4.ITeamRosterCandidatePromotionService;
import com.zhs.basket.service.v4.ITeamRosterLockApplicationService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.enums.BusinessType;
import com.zhs.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/basket/v4/divisions/{divisionId}/roster/team")
public class BasketV4TeamRosterController extends BaseController {

    @Autowired
    private ITeamRosterLockApplicationService lockApplicationService;
    @Autowired
    private ITeamRosterAllocationService allocationService;
    @Autowired
    private ITeamRosterCandidatePromotionService promotionService;

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.REGISTRATION_AUDIT_LIST + "') or @ss.hasPermi('" + BasketEventV4Perms.REGISTRATION_AUDIT_QUERY + "')")
    @GetMapping
    public AjaxResult workspace(@PathVariable Long divisionId) {
        return success(lockApplicationService.getWorkspace(divisionId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.REGISTRATION_AUDIT_QUERY + "')")
    @GetMapping("/versions/{rosterVersionId}")
    public AjaxResult version(@PathVariable Long divisionId, @PathVariable Long rosterVersionId) {
        return success(lockApplicationService.getVersion(divisionId, rosterVersionId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.REGISTRATION_AUDIT_QUERY + "')")
    @GetMapping("/versions")
    public AjaxResult versions(@PathVariable Long divisionId) {
        return success(lockApplicationService.listVersions(divisionId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.REGISTRATION_AUDIT_APPROVE + "')")
    @Log(title = "V4 TeamRoster", businessType = BusinessType.UPDATE)
    @PostMapping("/allocation")
    public AjaxResult allocate(@PathVariable Long divisionId, @RequestBody TeamRosterAllocationReq req) {
        allocationService.assign(divisionId, req, SecurityUtils.getUserId());
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.REGISTRATION_AUDIT_APPROVE + "')")
    @Log(title = "V4 TeamRoster", businessType = BusinessType.UPDATE)
    @PostMapping("/auto-allocate")
    public AjaxResult autoAllocate(@PathVariable Long divisionId) {
        allocationService.autoAllocate(divisionId, SecurityUtils.getUserId());
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.REGISTRATION_AUDIT_APPROVE + "')")
    @Log(title = "V4 TeamRoster", businessType = BusinessType.UPDATE)
    @PostMapping("/candidate/order")
    public AjaxResult moveCandidate(@PathVariable Long divisionId, @RequestBody TeamRosterCandidateOrderReq req) {
        allocationService.moveCandidate(divisionId, req, SecurityUtils.getUserId());
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.DIVISION_EDIT + "')")
    @Log(title = "V4 TeamRoster", businessType = BusinessType.UPDATE)
    @PostMapping("/lock")
    public AjaxResult lock(@PathVariable Long divisionId) {
        return success(lockApplicationService.lock(divisionId, SecurityUtils.getUserId()));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.DIVISION_EDIT + "')")
    @Log(title = "V4 TeamRoster", businessType = BusinessType.UPDATE)
    @PostMapping("/promote-candidate")
    public AjaxResult promote(@PathVariable Long divisionId, @RequestBody TeamRosterPromoteReq req) {
        return success(promotionService.promote(divisionId, req, SecurityUtils.getUserId()));
    }
}
