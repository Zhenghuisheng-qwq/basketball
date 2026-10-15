package com.zhs.service.v4;

import com.zhs.request.v4.V4RegistrationIdReq;
import com.zhs.request.v4.V4RegistrationSaveReq;
import com.zhs.request.v4.V4RosterChangeSubmitReq;
import com.zhs.response.v4.V4AvailableDivisionResp;
import com.zhs.response.v4.V4MyRegistrationResp;
import com.zhs.response.v4.V4RegistrationDetailResp;
import com.zhs.response.v4.V4RegistrationDivisionContextResp;

import java.util.List;

public interface IBasketV4PlayerRegistrationService {
    List<V4AvailableDivisionResp> listAvailableDivisions();

    /** 按 divisionId 加载报名上下文（存在与否 / 资格），不依赖 available 列表过滤。 */
    V4RegistrationDivisionContextResp getDivisionContext(Long divisionId);

    Long saveRegistration(V4RegistrationSaveReq req);

    void submitRegistration(V4RegistrationIdReq req);

    void withdrawRegistration(V4RegistrationIdReq req);

    Long submitRosterChange(V4RosterChangeSubmitReq req);

    List<V4MyRegistrationResp> listMyRegistrations();

    V4RegistrationDetailResp getRegistrationDetail(Long registrationId);
}
