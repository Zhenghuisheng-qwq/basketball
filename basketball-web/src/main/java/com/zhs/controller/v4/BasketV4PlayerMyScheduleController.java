package com.zhs.controller.v4;

import com.zhs.response.v4.V4MySchedulePageResp;
import com.zhs.service.v4.IBasketV4PlayerMyScheduleService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "V4 我的赛程")
@RestController
@RequestMapping("/basket/v4/player/my-schedule")
public class BasketV4PlayerMyScheduleController {

    @Resource
    private IBasketV4PlayerMyScheduleService myScheduleService;

    @ApiOperation("我的赛程（跨赛事聚合）")
    @GetMapping
    public R mySchedule(@RequestParam(required = false) Long seasonId,
                        @RequestParam(required = false) Long divisionId,
                        @RequestParam(required = false) String status,
                        @RequestParam(required = false) Boolean includeFinished,
                        @RequestParam(required = false) Boolean onlyPendingSubmit) {
        return R.ok(myScheduleService.listMySchedule(seasonId, divisionId, status, includeFinished, onlyPendingSubmit));
    }
}
