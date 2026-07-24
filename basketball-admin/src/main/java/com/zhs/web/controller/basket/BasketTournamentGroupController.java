package com.zhs.web.controller.basket;

import com.zhs.basket.req.GroupCreateReq;
import com.zhs.basket.req.GroupTeamAssignReq;
import com.zhs.basket.req.GroupUpdateReq;
import com.zhs.basket.service.IBasketTournamentGroupService;
import com.zhs.common.annotation.Log;
import com.zhs.common.core.controller.BaseController;
import com.zhs.common.core.domain.AjaxResult;
import com.zhs.common.enums.BusinessType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
public class BasketTournamentGroupController extends BaseController {

    @Autowired
    private IBasketTournamentGroupService groupService;

    @PreAuthorize("@ss.hasPermi('basket:group:list')")
    @GetMapping("/basket/stage/{stageId}/group/list")
    public AjaxResult listByStage(@PathVariable Long stageId) {
        return success(groupService.selectByStageId(stageId));
    }

    @PreAuthorize("@ss.hasPermi('basket:group:query')")
    @GetMapping("/basket/group/{groupId}")
    public AjaxResult getInfo(@PathVariable Long groupId) {
        return success(groupService.selectById(groupId));
    }

    @PreAuthorize("@ss.hasPermi('basket:group:add')")
    @Log(title = "分组管理", businessType = BusinessType.INSERT)
    @PostMapping("/basket/stage/{stageId}/group")
    public AjaxResult add(@PathVariable Long stageId, @RequestBody GroupCreateReq req) {
        return success(groupService.create(stageId, req));
    }

    @PreAuthorize("@ss.hasPermi('basket:group:edit')")
    @Log(title = "分组管理", businessType = BusinessType.UPDATE)
    @PutMapping("/basket/group/{groupId}")
    public AjaxResult edit(@PathVariable Long groupId, @RequestBody GroupUpdateReq req) {
        groupService.update(groupId, req);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:group:remove')")
    @Log(title = "分组管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/basket/group/{groupId}")
    public AjaxResult remove(@PathVariable Long groupId) {
        groupService.delete(groupId);
        return success();
    }

    @PreAuthorize("@ss.hasPermi('basket:group:list')")
    @GetMapping("/basket/group/{groupId}/team/list")
    public AjaxResult listTeams(@PathVariable Long groupId) {
        return success(groupService.selectTeamsByGroupId(groupId));
    }

    @PreAuthorize("@ss.hasPermi('basket:group:assignTeam')")
    @GetMapping("/basket/group/{groupId}/available-team")
    public AjaxResult availableTeams(@PathVariable Long groupId) {
        return success(groupService.selectAvailableTeams(groupId));
    }

    @PreAuthorize("@ss.hasPermi('basket:group:assignTeam')")
    @Log(title = "分组管理", businessType = BusinessType.UPDATE)
    @PutMapping("/basket/group/{groupId}/team")
    public AjaxResult assignTeams(@PathVariable Long groupId, @RequestBody GroupTeamAssignReq req) {
        groupService.assignTeams(groupId, req);
        return success();
    }
}
