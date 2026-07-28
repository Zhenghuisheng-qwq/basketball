package com.zhs.service;

import com.zhs.response.team.CaptainQualificationDetailResp;

public interface ICaptainQualificationService {

    CaptainQualificationDetailResp getCurrent();

    CaptainQualificationDetailResp getCurrent(Long seasonId);
}
