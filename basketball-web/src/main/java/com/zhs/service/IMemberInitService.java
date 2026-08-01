package com.zhs.service;

import com.zhs.response.member.MemberInitAccountsResp;

public interface IMemberInitService {

    MemberInitAccountsResp initAccounts(String clientIp);
}
