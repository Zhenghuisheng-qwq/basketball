package com.zhs.web.controller.basket.v4;

import com.zhs.basket.constant.BasketEventV4Perms;
import com.zhs.basket.dto.v4.PersonalFormationMoveReq;
import com.zhs.basket.dto.v4.PersonalFormationRenameReq;
import com.zhs.basket.dto.v4.PersonalFormationSwapReq;
import com.zhs.basket.service.v4.IPersonalFormationCommandService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.enums.BusinessType;
import com.zhs.common.utils.SecurityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/basket/v4/admin/personal-formation")
public class BasketV4PersonalFormationController extends BaseController {

    @Autowired
    private IPersonalFormationCommandService personalFormationCommandService;

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.PERSONAL_FORMATION_LIST + "')")
    @GetMapping("/{divisionId}/workspace")
    public AjaxResult workspace(@PathVariable Long divisionId) {
        return success(personalFormationCommandService.getWorkspace(divisionId));
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.PERSONAL_FORMATION_EDIT + "')")
    @Log(title = "V4 PersonalFormation", businessType = BusinessType.UPDATE)
    @PostMapping("/{divisionId}/generate")
    public AjaxResult generate(@PathVariable Long divisionId) {
        personalFormationCommandService.generate(divisionId, SecurityUtils.getUserId());
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.PERSONAL_FORMATION_EDIT + "')")
    @Log(title = "V4 PersonalFormation", businessType = BusinessType.UPDATE)
    @PostMapping("/{divisionId}/regenerate")
    public AjaxResult regenerate(@PathVariable Long divisionId) {
        personalFormationCommandService.generate(divisionId, SecurityUtils.getUserId());
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.PERSONAL_FORMATION_EDIT + "')")
    @Log(title = "V4 PersonalFormation", businessType = BusinessType.UPDATE)
    @PutMapping("/{divisionId}/teams/{drawTeamId}/name")
    public AjaxResult rename(@PathVariable Long divisionId, @PathVariable Long drawTeamId,
                             @RequestBody PersonalFormationRenameReq req) {
        personalFormationCommandService.renameTeam(divisionId, drawTeamId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.PERSONAL_FORMATION_EDIT + "')")
    @Log(title = "V4 PersonalFormation", businessType = BusinessType.UPDATE)
    @PostMapping("/{divisionId}/move")
    public AjaxResult move(@PathVariable Long divisionId, @RequestBody PersonalFormationMoveReq req) {
        personalFormationCommandService.moveMember(divisionId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.PERSONAL_FORMATION_EDIT + "')")
    @Log(title = "V4 PersonalFormation", businessType = BusinessType.UPDATE)
    @PostMapping("/{divisionId}/swap")
    public AjaxResult swap(@PathVariable Long divisionId, @RequestBody PersonalFormationSwapReq req) {
        personalFormationCommandService.swapMembers(divisionId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('" + BasketEventV4Perms.PERSONAL_FORMATION_CONFIRM + "')")
    @Log(title = "V4 PersonalFormation", businessType = BusinessType.UPDATE)
    @PostMapping("/{divisionId}/lock")
    public AjaxResult lock(@PathVariable Long divisionId) {
        return success(personalFormationCommandService.lock(divisionId, SecurityUtils.getUserId()));
    }
}
