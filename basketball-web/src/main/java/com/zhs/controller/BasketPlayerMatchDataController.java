package com.zhs.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.request.matchdata.PlayerMatchDataResubmitReq;
import com.zhs.request.matchdata.PlayerMatchDataSubmitReq;
import com.zhs.response.matchdata.PlayerMatchDataStatusResp;
import com.zhs.response.matchdata.PlayerMatchDataSubmissionDetailResp;
import com.zhs.response.matchdata.PlayerMatchDataSubmitResultResp;
import com.zhs.response.matchdata.PlayerMatchTaskResp;
import com.zhs.service.IMPlayerMatchDataService;
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
@Api(value = "球员比赛数据提交")
@Slf4j
public class BasketPlayerMatchDataController {

    @Resource
    private IMPlayerMatchDataService playerMatchDataService;

    @ApiOperation("我的比赛分页")
    @GetMapping({"/basket/player/match-data/page", "/basket/player/match-data/my-matches"})
    public R<IPage<PlayerMatchTaskResp>> pageMyMatches(@RequestParam(required = false) Long seasonId,
                                                       @RequestParam(required = false) Long stageId,
                                                       @RequestParam(required = false) Integer dataStatus,
                                                       @RequestParam(required = false) Integer matchStatus,
                                                       @RequestParam(required = false) Integer pageNum,
                                                       @RequestParam(required = false) Integer pageSize) {
        return R.ok(playerMatchDataService.pageMyMatches(seasonId, stageId, dataStatus, matchStatus, pageNum, pageSize));
    }

    @ApiOperation("获取比赛数据提交详情")
    @GetMapping("/basket/player/match-data/{matchId}/submit-detail")
    public R<PlayerMatchDataSubmissionDetailResp> getSubmitDetail(@PathVariable Long matchId) {
        return R.ok(playerMatchDataService.getSubmitDetail(matchId));
    }

    @ApiOperation("提交比赛数据")
    @PostMapping("/basket/player/match-data/{matchId}/submit")
    public R<PlayerMatchDataSubmitResultResp> submit(@PathVariable Long matchId,
                                                     @RequestBody PlayerMatchDataSubmitReq req) {
        return R.ok(playerMatchDataService.submit(matchId, req), "比赛数据已提交，正在等待审核");
    }

    @ApiOperation("查看比赛数据审核状态")
    @GetMapping("/basket/player/match-data/{matchId}/status")
    public R<PlayerMatchDataStatusResp> getStatus(@PathVariable Long matchId) {
        return R.ok(playerMatchDataService.getStatus(matchId));
    }

    @ApiOperation("获取驳回后重新提交详情")
    @GetMapping("/basket/player/match-data/{matchId}/resubmit-detail")
    public R<PlayerMatchDataSubmissionDetailResp> getResubmitDetail(@PathVariable Long matchId) {
        return R.ok(playerMatchDataService.getResubmitDetail(matchId));
    }

    @ApiOperation("重新提交比赛数据")
    @PostMapping("/basket/player/match-data/{matchId}/resubmit")
    public R<PlayerMatchDataSubmitResultResp> resubmit(@PathVariable Long matchId,
                                                       @RequestBody PlayerMatchDataResubmitReq req) {
        return R.ok(playerMatchDataService.resubmit(matchId, req), "比赛数据已重新提交，正在等待审核");
    }
}
