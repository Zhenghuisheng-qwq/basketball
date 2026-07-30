package com.zhs.controller;

import com.zhs.request.matchdata.PlayerMatchDisputeSubmitReq;
import com.zhs.response.matchdata.PlayerMatchDisputeAvailableResp;
import com.zhs.response.matchdata.PlayerMatchDisputeDetailResp;
import com.zhs.response.matchdata.PlayerMatchDisputeHistoryItem;
import com.zhs.response.matchdata.PlayerMatchDisputeStatusResp;
import com.zhs.response.matchdata.PlayerMatchDisputeSubmitResultResp;
import com.zhs.service.IMPlayerMatchDisputeService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Api(value = "球员比赛数据争议")
@Slf4j
public class BasketPlayerMatchDisputeController {

    @Resource
    private IMPlayerMatchDisputeService playerMatchDisputeService;

    @ApiOperation("查询是否可发起争议")
    @GetMapping("/basket/player/match-dispute/{matchId}/available")
    public R<PlayerMatchDisputeAvailableResp> getAvailable(@PathVariable Long matchId) {
        return R.ok(playerMatchDisputeService.getAvailable(matchId));
    }

    @ApiOperation("获取争议详情")
    @GetMapping("/basket/player/match-dispute/{matchId}/detail")
    public R<PlayerMatchDisputeDetailResp> getDetail(@PathVariable Long matchId) {
        return R.ok(playerMatchDisputeService.getDetail(matchId));
    }

    @ApiOperation("查看争议状态")
    @GetMapping("/basket/player/match-dispute/{matchId}/status")
    public R<PlayerMatchDisputeStatusResp> getStatus(@PathVariable Long matchId) {
        return R.ok(playerMatchDisputeService.getStatus(matchId));
    }

    @ApiOperation("争议历史")
    @GetMapping("/basket/player/match-dispute/{matchId}/history")
    public R<List<PlayerMatchDisputeHistoryItem>> getHistory(@PathVariable Long matchId) {
        return R.ok(playerMatchDisputeService.getHistory(matchId));
    }

    @ApiOperation("提交数据争议")
    @PostMapping("/basket/player/match-dispute/{matchId}/submit")
    public R<PlayerMatchDisputeSubmitResultResp> submit(@PathVariable Long matchId,
                                                        @RequestBody PlayerMatchDisputeSubmitReq req) {
        return R.ok(playerMatchDisputeService.submit(matchId, req), "争议已提交，正在等待处理");
    }
}
