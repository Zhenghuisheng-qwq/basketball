package com.zhs.controller.v4;

import com.zhs.request.v4.V4SeriesSubmissionDeclareReq;
import com.zhs.request.v4.V4SeriesSubmissionIdReq;
import com.zhs.request.v4.V4SeriesSubmissionResubmitReq;
import com.zhs.request.v4.V4SeriesSubmissionSaveGameReq;
import com.zhs.service.v4.IBasketV4PlayerSeriesSubmissionService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "V4 系列赛数据提交")
@RestController
@RequestMapping("/basket/v4/player/series-submission")
public class BasketV4PlayerSeriesSubmissionController {

    @Resource
    private IBasketV4PlayerSeriesSubmissionService seriesSubmissionService;

    @ApiOperation("系列赛提交工作台")
    @GetMapping("/{matchSeriesId}/workspace")
    public R workspace(@PathVariable Long matchSeriesId) {
        return R.ok(seriesSubmissionService.getWorkspace(matchSeriesId));
    }

    @ApiOperation("确认系列赛最终比分")
    @PostMapping("/declare-score")
    public R declareScore(@RequestBody V4SeriesSubmissionDeclareReq req) {
        return R.ok(seriesSubmissionService.declareScore(req));
    }

    @ApiOperation("保存本场 / 保存并完成本场")
    @PostMapping("/save-game")
    public R saveGame(@RequestBody V4SeriesSubmissionSaveGameReq req) {
        return R.ok(seriesSubmissionService.saveGame(req));
    }

    @ApiOperation("提交整个系列赛审核")
    @PostMapping("/submit")
    public R submit(@RequestBody V4SeriesSubmissionIdReq req) {
        seriesSubmissionService.submitSeries(req);
        return R.ok();
    }

    @ApiOperation("系列赛驳回后重提")
    @PostMapping("/resubmit")
    public R resubmit(@RequestBody V4SeriesSubmissionResubmitReq req) {
        return R.ok(seriesSubmissionService.resubmitSeries(req));
    }
}
