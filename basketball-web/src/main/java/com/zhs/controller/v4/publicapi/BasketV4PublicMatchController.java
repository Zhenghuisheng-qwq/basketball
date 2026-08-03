package com.zhs.controller.v4.publicapi;

import com.zhs.common.NoLoginRequest;
import com.zhs.response.v4.publicapi.V4PublicMatchDetailResp;
import com.zhs.response.v4.publicapi.V4PublicMatchItemResp;
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
@RequestMapping("/basket/v4/public/match")
@Api(value = "V4 公开赛程")
public class BasketV4PublicMatchController {

    @Resource
    private IBasketV4PublicService v4PublicService;

    @ApiOperation("Match 列表")
    @GetMapping("/list")
    public R<List<V4PublicMatchItemResp>> listMatches(
            @RequestParam Long divisionId,
            @RequestParam(required = false) Integer roundNo,
            @RequestParam(required = false) String matchStatus,
            @RequestParam(required = false) Boolean upcomingOnly,
            @RequestParam(required = false) Boolean finishedOnly,
            @RequestParam(required = false) Long eventTeamId) {
        return R.ok(v4PublicService.listMatches(divisionId, roundNo, matchStatus,
                upcomingOnly, finishedOnly, eventTeamId));
    }

    @ApiOperation("Match 详情")
    @GetMapping("/{matchId}")
    public R<V4PublicMatchDetailResp> getMatchDetail(@PathVariable Long matchId) {
        return R.ok(v4PublicService.getMatchDetail(matchId));
    }
}
