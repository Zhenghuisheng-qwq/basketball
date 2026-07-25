package com.zhs.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.request.participation.OpenRegistrationDraftReq;
import com.zhs.request.participation.OpenRegistrationWithdrawReq;
import com.zhs.response.participation.OpenAvailableSeasonItemResp;
import com.zhs.response.participation.OpenRegistrationDetailResp;
import com.zhs.response.participation.OpenRegistrationItemResp;

public interface IOpenRegistrationBusinessService {

    IPage<OpenAvailableSeasonItemResp> pageAvailableOpenSeasons(Integer currentPage, Integer pageSize);

    OpenRegistrationDetailResp createDraft(OpenRegistrationDraftReq req);

    OpenRegistrationDetailResp submit(String registrationId);

    OpenRegistrationDetailResp withdraw(String registrationId, OpenRegistrationWithdrawReq req);

    OpenRegistrationDetailResp getDetail(String registrationId);

    IPage<OpenRegistrationItemResp> pageMyRegistrations(Integer currentPage, Integer pageSize, Integer status);
}
