package com.zhs.controller.v4.publicapi;

import com.zhs.common.NoLoginRequest;
import com.zhs.response.v4.publicapi.V4PublicSeasonDetailResp;
import com.zhs.response.v4.publicapi.V4PublicSeasonItemResp;
import com.zhs.service.v4.IBasketV4PublicService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@NoLoginRequest
@RestController
@RequestMapping("/basket/v4/public/season")
@Api(value = "V4 公开 Season")
public class BasketV4PublicSeasonController {

    @Resource
    private IBasketV4PublicService v4PublicService;

    @ApiOperation("Season 列表")
    @GetMapping("/list")
    public R<List<V4PublicSeasonItemResp>> listSeasons(
            @RequestParam(required = false) String seriesCode,
            @RequestParam(required = false) Integer seasonYear,
            @RequestParam(required = false) String seasonStatus) {
        return R.ok(v4PublicService.listSeasons(seriesCode, seasonYear, seasonStatus));
    }

    @ApiOperation("当前公开 Season：同一 Series 同时只开一届，按业务优先级取当前赛季")
    @GetMapping("/current")
    public R<V4PublicSeasonItemResp> getCurrentSeason(@RequestParam(required = false) String seriesCode) {
        return R.ok(v4PublicService.getCurrentSeason(seriesCode));
    }

    @ApiOperation("Season 详情")
    @GetMapping("/{seasonId}")
    public R<V4PublicSeasonDetailResp> getSeasonDetail(@PathVariable Long seasonId) {
        return R.ok(v4PublicService.getSeasonDetail(seasonId));
    }
}
