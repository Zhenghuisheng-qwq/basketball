package com.zhs.controller;

import com.zhs.request.team.TeamProfileChangeCancelReq;
import com.zhs.request.team.TeamProfileChangeSubmitReq;
import com.zhs.response.team.TeamProfileChangeDetailResp;
import com.zhs.response.team.TeamProfileChangeItemResp;
import com.zhs.response.team.TeamProfileChangeSubmitResp;
import com.zhs.response.team.TeamProfileEditResp;
import com.zhs.service.ITeamProfileChangeService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/basket/team")
@Api(value = "球队资料变更")
@Slf4j
public class TeamProfileChangeController {

    @Resource
    private ITeamProfileChangeService teamProfileChangeService;

    @ApiOperation("查询球队资料编辑页数据")
    @GetMapping("/{teamId}/profile/edit")
    public R<TeamProfileEditResp> getProfileEdit(@PathVariable String teamId) {
        return R.ok(teamProfileChangeService.getProfileEditData(teamId));
    }

    @ApiOperation("提交或更新球队资料变更申请")
    @PostMapping("/{teamId}/profile/change/submit")
    public R<TeamProfileChangeSubmitResp> submitChange(@PathVariable String teamId,
                                                       @RequestBody TeamProfileChangeSubmitReq req) {
        return R.ok(teamProfileChangeService.submitChange(teamId, req), "变更申请已提交，等待审核");
    }

    @ApiOperation("取消球队资料变更申请")
    @PostMapping("/{teamId}/profile/change/{changeId}/cancel")
    public R<Void> cancelChange(@PathVariable String teamId,
                                @PathVariable String changeId,
                                @RequestBody(required = false) TeamProfileChangeCancelReq req) {
        teamProfileChangeService.cancelChange(teamId, changeId, req);
        return R.ok(null, "变更申请已取消");
    }

    @ApiOperation("查询球队资料变更历史")
    @GetMapping("/{teamId}/profile/change/history")
    public R<Map<String, Object>> pageHistory(@PathVariable String teamId,
                                               @RequestParam(required = false) Integer pageNum,
                                               @RequestParam(required = false) Integer pageSize,
                                               @RequestParam(required = false) Integer auditStatus,
                                               @RequestParam(required = false) String keyword) {
        List<TeamProfileChangeItemResp> records = teamProfileChangeService.pageHistory(
                teamId, pageNum, pageSize, auditStatus, keyword);
        long total = teamProfileChangeService.countHistory(teamId, auditStatus, keyword);
        long current = pageNum == null || pageNum < 1 ? 1L : pageNum;
        long size = pageSize == null || pageSize < 1 ? 5L : Math.min(pageSize, 100L);
        long pages = size <= 0 ? 0L : (total + size - 1) / size;
        Map<String, Object> page = new HashMap<>(8);
        page.put("records", records);
        page.put("total", total);
        page.put("pageNum", current);
        page.put("pageSize", size);
        page.put("current", current);
        page.put("size", size);
        page.put("pages", pages);
        return R.ok(page);
    }

    @ApiOperation("查询变更详情")
    @GetMapping("/{teamId}/profile/change/{changeId}")
    public R<TeamProfileChangeDetailResp> getChangeDetail(@PathVariable String teamId,
                                                          @PathVariable String changeId) {
        return R.ok(teamProfileChangeService.getChangeDetail(teamId, changeId));
    }
}
