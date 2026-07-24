package com.zhs.service.impl;

import com.zhs.common.UserContext;
import com.zhs.response.team.CaptainQualificationDetailResp;
import com.zhs.service.ICaptainPermissionService;
import com.zhs.service.ICaptainQualificationService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;

@Service
public class CaptainQualificationServiceImpl implements ICaptainQualificationService {

    @Resource
    private ICaptainPermissionService captainPermissionService;

    @Override
    public CaptainQualificationDetailResp getCurrent() {
        Long memberId = UserContext.getRequiredUserId();
        return captainPermissionService.getCurrentQualification(memberId);
    }
}
