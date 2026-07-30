package com.zhs.controller;

import com.zhs.request.matchdata.PlayerMatchExceptionSubmitReq;
import com.zhs.response.matchdata.PlayerMatchExceptionAvailableResp;
import com.zhs.response.matchdata.PlayerMatchExceptionHistoryItem;
import com.zhs.response.matchdata.PlayerMatchExceptionStatusResp;
import com.zhs.response.matchdata.PlayerMatchExceptionSubmitResultResp;
import com.zhs.service.IMPlayerMatchExceptionService;
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
@Api(value = "球员比赛异常申请")
@Slf4j
public class BasketPlayerMatchExceptionController {

    @Resource
    private IMPlayerMatchExceptionService playerMatchExceptionService;

    @ApiOperation("查询是否可提交异常申请")
    @GetMapping("/basket/player/match-exception/{matchId}/available")
    public R<PlayerMatchExceptionAvailableResp> getAvailable(@PathVariable Long matchId) {
        return R.ok(playerMatchExceptionService.getAvailable(matchId));
    }

    @ApiOperation("查看异常申请状态")
    @GetMapping("/basket/player/match-exception/{matchId}/status")
    public R<PlayerMatchExceptionStatusResp> getStatus(@PathVariable Long matchId) {
        return R.ok(playerMatchExceptionService.getStatus(matchId));
    }

    @ApiOperation("异常申请历史")
    @GetMapping("/basket/player/match-exception/{matchId}/history")
    public R<List<PlayerMatchExceptionHistoryItem>> getHistory(@PathVariable Long matchId) {
        return R.ok(playerMatchExceptionService.getHistory(matchId));
    }

    @ApiOperation("提交异常申请")
    @PostMapping("/basket/player/match-exception/{matchId}/submit")
    public R<PlayerMatchExceptionSubmitResultResp> submit(@PathVariable Long matchId,
                                                          @RequestBody PlayerMatchExceptionSubmitReq req) {
        return R.ok(playerMatchExceptionService.submit(matchId, req), "异常申请已提交，正在等待处理");
    }
}
