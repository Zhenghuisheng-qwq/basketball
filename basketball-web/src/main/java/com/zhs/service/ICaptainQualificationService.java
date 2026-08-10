package com.zhs.service;

import com.zhs.response.team.CaptainQualificationDetailResp;

import java.util.List;

public interface ICaptainQualificationService {

    CaptainQualificationDetailResp getCurrent();

    CaptainQualificationDetailResp getCurrent(Long seasonId);

    CaptainQualificationDetailResp getCurrent(Long seasonId, Long teamId);

    CaptainQualificationDetailResp getCurrentByDivision(Long divisionId, Long teamId);

    List<CaptainQualificationDetailResp> listActive();
}
