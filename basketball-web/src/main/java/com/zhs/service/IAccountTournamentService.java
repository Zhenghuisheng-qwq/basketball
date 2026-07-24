package com.zhs.service;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.zhs.response.account.AccountTournamentItemResp;

import java.util.List;

/**
 * 账户赛事业务服务
 */
public interface IAccountTournamentService {

    IPage<AccountTournamentItemResp> pageTournaments(Integer currentPage, Integer pageSize,
                                                     Integer status, String keyword);

    AccountTournamentItemResp getTournamentDetail(Long registrationId);

    List<AccountTournamentItemResp> listRecentTournaments(int limit);
}
