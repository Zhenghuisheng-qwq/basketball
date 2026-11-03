package com.zhs.controller.v4.publicapi;

import com.zhs.common.NoLoginRequest;
import com.zhs.response.v4.publicapi.V4PublicLeaderboardResp;
import com.zhs.service.impl.v4.BasketV4PublicLeaderboardService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@NoLoginRequest
@RestController
@RequestMapping("/basket/v4/public/leaderboard")
@Api(value = "V4 公开技术统计榜")
public class BasketV4PublicLeaderboardController {

    @Resource
    private BasketV4PublicLeaderboardService leaderboardService;

    @ApiOperation("已发布技术统计榜")
    @GetMapping
    public R<V4PublicLeaderboardResp> query(@RequestParam Long divisionId,
                                             @RequestParam(required = false) String scope,
                                             @RequestParam(required = false) String tab,
                                             @RequestParam(required = false) String mode,
                                             @RequestParam(required = false) String sortBy,
                                             @RequestParam(required = false) Integer page,
                                             @RequestParam(required = false) String keyword) {
        return R.ok(leaderboardService.query(divisionId, scope, tab, mode, sortBy, page, 10, keyword));
    }

    @ApiOperation("数据中心默认赛区")
    @GetMapping("/featured")
    public R<Map<String, Long>> featured() {
        return R.ok(Map.of("divisionId", leaderboardService.featuredDivisionId() == null ? 0L : leaderboardService.featuredDivisionId()));
    }
}
