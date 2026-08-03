package com.zhs.controller.v4.publicapi;

import com.zhs.common.NoLoginRequest;
import com.zhs.response.v4.publicapi.V4PublicDivisionDetailResp;
import com.zhs.service.v4.IBasketV4PublicService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@NoLoginRequest
@RestController
@RequestMapping("/basket/v4/public/division")
@Api(value = "V4 公开 Division")
public class BasketV4PublicDivisionController {

    @Resource
    private IBasketV4PublicService v4PublicService;

    @ApiOperation("Division 详情")
    @GetMapping("/{divisionId}")
    public R<V4PublicDivisionDetailResp> getDivisionDetail(@PathVariable Long divisionId) {
        return R.ok(v4PublicService.getDivisionDetail(divisionId));
    }
}
