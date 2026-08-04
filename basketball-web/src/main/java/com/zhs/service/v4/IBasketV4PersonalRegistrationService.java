package com.zhs.service.v4;

import com.zhs.request.v4.V4PersonalRegistrationIdReq;
import com.zhs.request.v4.V4PersonalRegistrationSaveReq;
import com.zhs.response.v4.V4MyPersonalRegistrationResp;
import com.zhs.response.v4.V4PersonalAvailableDivisionResp;

import java.util.List;

public interface IBasketV4PersonalRegistrationService {
    List<V4PersonalAvailableDivisionResp> listAvailableDivisions();

    Long saveRegistration(V4PersonalRegistrationSaveReq req);

    void submitRegistration(V4PersonalRegistrationIdReq req);

    void withdrawRegistration(V4PersonalRegistrationIdReq req);

    List<V4MyPersonalRegistrationResp> listMyRegistrations();
}
