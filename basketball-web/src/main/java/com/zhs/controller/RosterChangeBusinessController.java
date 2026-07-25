package com.zhs.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.request.participation.RosterChangeCaptainTransferReq;
import com.zhs.request.participation.RosterChangeFreeAgentAddReq;
import com.zhs.request.participation.RosterChangeTransferReq;
import com.zhs.response.participation.RosterChangeRequestItemResp;
import com.zhs.response.participation.RosterChangeSubmitResp;
import com.zhs.service.IRosterChangeBusinessService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(value = "赛事阵容变更")
@Slf4j
public class RosterChangeBusinessController {

    @Resource
    private IRosterChangeBusinessService rosterChangeBusinessService;

    @ApiOperation("提交转会申请")
    @PostMapping("/basket/player/roster-change/transfer")
    public R<RosterChangeSubmitResp> submitTransfer(@RequestBody RosterChangeTransferReq req) {
        return R.ok(rosterChangeBusinessService.submitTransfer(req), "转会申请已提交");
    }

    @ApiOperation("提交自由球员补员申请")
    @PostMapping("/basket/player/roster-change/free-agent-add")
    public R<RosterChangeSubmitResp> submitFreeAgentAdd(@RequestBody RosterChangeFreeAgentAddReq req) {
        return R.ok(rosterChangeBusinessService.submitFreeAgentAdd(req), "补员申请已提交");
    }

    @ApiOperation("提交队长移交申请")
    @PostMapping("/basket/player/roster-change/captain-transfer")
    public R<RosterChangeSubmitResp> submitCaptainTransfer(@RequestBody RosterChangeCaptainTransferReq req) {
        return R.ok(rosterChangeBusinessService.submitCaptainTransfer(req), "队长移交申请已提交");
    }

    @ApiOperation("撤回阵容变更申请")
    @PostMapping("/basket/player/roster-change/{requestId}/withdraw")
    public R<Void> withdrawRequest(@PathVariable String requestId) {
        rosterChangeBusinessService.withdrawRequest(requestId);
        return R.ok(null, "申请撤回成功");
    }

    @ApiOperation("我的阵容变更申请分页")
    @GetMapping("/basket/player/roster-change/page")
    public R<IPage<RosterChangeRequestItemResp>> pageMyRequests(@RequestParam(required = false) Integer currentPage,
                                                                @RequestParam(required = false) Integer pageSize,
                                                                @RequestParam(required = false) Integer status) {
        return R.ok(rosterChangeBusinessService.pageMyRequests(currentPage, pageSize, status));
    }
}
