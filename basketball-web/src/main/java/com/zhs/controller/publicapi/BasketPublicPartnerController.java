package com.zhs.controller.publicapi;

import com.zhs.common.NoLoginRequest;
import com.zhs.response.publicapi.PublicPartnerResp;
import com.zhs.service.IBasketPublicPartnerService;
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
@RequestMapping("/basket/public/partner")
@Api(value = "官网公开合作伙伴")
public class BasketPublicPartnerController {

    @Resource
    private IBasketPublicPartnerService partnerService;

    @ApiOperation("合作伙伴列表")
    @GetMapping("/list")
    public R<List<PublicPartnerResp>> listPartners() {
        return R.ok(partnerService.listPartners());
    }
}
