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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/basket/team/captain/qualification")
@Api(value = "队长授权")
@Slf4j
public class CaptainQualificationController {

    @Resource
    private ICaptainQualificationService captainQualificationService;

    @ApiOperation("获取当前队长授权（可按 divisionId 精确查询）")
    @GetMapping("/current")
    public R<CaptainQualificationDetailResp> getCurrent(
            @RequestParam(required = false) Long divisionId,
            @RequestParam(required = false) Long seasonId,
            @RequestParam(required = false) Long teamId) {
        if (divisionId != null) {
            return R.ok(captainQualificationService.getCurrentByDivision(divisionId, teamId));
        }
        return R.ok(captainQualificationService.getCurrent(seasonId, teamId));
    }

    @ApiOperation("列出当前用户所有有效 Division 队长授权")
    @GetMapping("/list")
    public R<List<CaptainQualificationDetailResp>> listActive() {
        return R.ok(captainQualificationService.listActive());
    }
}
