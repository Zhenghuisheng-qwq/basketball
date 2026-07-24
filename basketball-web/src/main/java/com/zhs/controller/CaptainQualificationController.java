package com.zhs.controller;

import com.zhs.response.team.CaptainQualificationDetailResp;
import com.zhs.service.ICaptainQualificationService;
import com.zhs.util.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/basket/team/captain/qualification")
@Api(value = "队长资格")
@Slf4j
public class CaptainQualificationController {

    @Resource
    private ICaptainQualificationService captainQualificationService;

    @ApiOperation("获取当前队长资格")
    @GetMapping("/current")
    public R<CaptainQualificationDetailResp> getCurrent() {
        return R.ok(captainQualificationService.getCurrent());
    }
}
