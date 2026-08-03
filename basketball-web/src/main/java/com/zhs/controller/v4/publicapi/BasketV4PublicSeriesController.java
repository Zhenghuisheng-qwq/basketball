package com.zhs.controller.v4.publicapi;

import com.zhs.common.NoLoginRequest;
import com.zhs.response.v4.publicapi.V4PublicSeriesItemResp;
import com.zhs.service.v4.IBasketV4PublicService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@NoLoginRequest
@RestController
@RequestMapping("/basket/v4/public/series")
@Api(value = "V4 公开 Series")
public class BasketV4PublicSeriesController {

    @Resource
    private IBasketV4PublicService v4PublicService;

    @ApiOperation("Series 列表")
    @GetMapping("/list")
    public R<List<V4PublicSeriesItemResp>> listSeries() {
        return R.ok(v4PublicService.listSeries());
    }
}
