package com.zhs.controller.v4;

import com.zhs.request.v4.V4MatchSubmissionIdReq;
import com.zhs.request.v4.V4MatchSubmissionResubmitReq;
import com.zhs.request.v4.V4MatchSubmissionSaveReq;
import com.zhs.service.v4.IBasketV4PlayerMatchSubmissionService;
import com.zhs.service.v4.V4StatsScreenshotService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Api(tags = "V4 比赛数据提交")
@RestController
@RequestMapping("/basket/v4/player/match-submission")
public class BasketV4PlayerMatchSubmissionController {

    @Resource
    private IBasketV4PlayerMatchSubmissionService matchSubmissionService;
    @Resource
    private V4StatsScreenshotService statsScreenshotService;

    @ApiOperation("我的可提交比赛")
    @GetMapping("/my-matches")
    public R myMatches(@RequestParam(required = false) Long seasonId,
                       @RequestParam(required = false) Long divisionId,
                       @RequestParam(required = false) String matchStatus,
                       @RequestParam(required = false) String publishStatus,
                       @RequestParam(required = false) String submissionStatus,
                       @RequestParam(required = false) Boolean upcomingOnly) {
        return R.ok(matchSubmissionService.listMyMatches(seasonId, divisionId, matchStatus, publishStatus, submissionStatus, upcomingOnly));
    }

    @ApiOperation("识别结算截图并按赛事名单对应球员")
    @PostMapping("/{matchId}/recognize-screenshot")
    public R recognizeScreenshot(@PathVariable Long matchId,
                                 @RequestParam(required = false) Long submitEventTeamId,
                                 @RequestParam("file") MultipartFile file) {
        return R.ok(statsScreenshotService.recognize(matchId, submitEventTeamId, file));
    }

    @ApiOperation("提交详情/草稿")
    @GetMapping("/{matchId}/detail")
    public R detail(@PathVariable Long matchId, @RequestParam(required = false) Long submitEventTeamId) {
        return R.ok(matchSubmissionService.getDetail(matchId, submitEventTeamId));
    }

    @ApiOperation("本场可选择的本队 EventPlayer")
    @GetMapping("/{matchId}/eligible-players")
    public R eligiblePlayers(@PathVariable Long matchId, @RequestParam(required = false) Long submitEventTeamId) {
        return R.ok(matchSubmissionService.listEligiblePlayers(matchId, submitEventTeamId));
    }

    @ApiOperation("保存草稿")
    @PostMapping("/save")
    public R save(@RequestBody V4MatchSubmissionSaveReq req) {
        return R.ok(matchSubmissionService.saveDraft(req));
    }

    @ApiOperation("提交审核")
    @PostMapping("/submit")
    public R submit(@RequestBody V4MatchSubmissionIdReq req) {
        matchSubmissionService.submit(req);
        return R.ok();
    }

    @ApiOperation("驳回后重提")
    @PostMapping("/resubmit")
    public R resubmit(@RequestBody V4MatchSubmissionResubmitReq req) {
        return R.ok(matchSubmissionService.resubmit(req));
    }

    @ApiOperation("我的提交列表")
    @GetMapping("/my-list")
    public R myList() {
        return R.ok(matchSubmissionService.listMySubmissions());
    }
}
