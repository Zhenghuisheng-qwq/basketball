package com.zhs.service.impl;

import com.zhs.common.UserContext;
import com.zhs.response.team.CaptainQualificationDetailResp;
import com.zhs.service.ICaptainPermissionService;
import com.zhs.service.ICaptainQualificationService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CaptainQualificationServiceImpl implements ICaptainQualificationService {

    @Resource
    private ICaptainPermissionService captainPermissionService;

    @Override
    public CaptainQualificationDetailResp getCurrent() {
        return getCurrent(null);
    }

    @Override
    public CaptainQualificationDetailResp getCurrent(Long seasonId) {
        return getCurrent(seasonId, null);
    }

    @Override
    public CaptainQualificationDetailResp getCurrent(Long seasonId, Long teamId) {
        Long memberId = UserContext.getRequiredUserId();
        return captainPermissionService.getCurrentQualification(memberId, seasonId, teamId);
    }

    @Override
    public CaptainQualificationDetailResp getCurrentByDivision(Long divisionId, Long teamId) {
        Long memberId = UserContext.getRequiredUserId();
        return captainPermissionService.getCurrentQualificationByDivision(memberId, divisionId, teamId);
    }

    @Override
    public List<CaptainQualificationDetailResp> listActive() {
        Long memberId = UserContext.getRequiredUserId();
        return captainPermissionService.listActiveQualifications(memberId);
    }
}
