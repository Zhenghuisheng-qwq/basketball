package com.zhs.controller.v4.publicapi;

import com.zhs.common.NoLoginRequest;
import com.zhs.response.v4.publicapi.V4PublicMatchSeriesDetailResp;
import com.zhs.service.v4.IBasketV4PublicService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@NoLoginRequest
@RestController
@RequestMapping("/basket/v4/public/match-series")
@Api(value = "V4 公开系列赛")
public class BasketV4PublicMatchSeriesController {

    @Resource
    private IBasketV4PublicService v4PublicService;

    @ApiOperation("系列赛详情")
    @GetMapping("/{seriesId}")
    public R<V4PublicMatchSeriesDetailResp> getSeriesDetail(@PathVariable Long seriesId) {
        return R.ok(v4PublicService.getMatchSeriesDetail(seriesId));
    }

    @ApiOperation("阶段系列赛列表")
    @GetMapping("/stage/{stageId}")
    public R<List<V4PublicMatchSeriesDetailResp>> listStageSeries(@PathVariable Long stageId) {
        return R.ok(v4PublicService.listStageSeries(stageId));
    }
}
