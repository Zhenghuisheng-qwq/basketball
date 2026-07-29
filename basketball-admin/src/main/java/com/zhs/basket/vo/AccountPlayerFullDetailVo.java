package com.zhs.basket.vo;

import lombok.Data;
import java.util.List;

@Data
public class AccountPlayerFullDetailVo {
    private AccountPlayerAccountVo account;
    private AccountPlayerProfileVo profile;
    private List<AccountPlayerPlatformAccountVo> platformAccounts;
    private List<AccountPlayerCertHistoryVo> certHistory;
    private AccountPlayerCaptainQualVo captainQual;
    private List<AccountPlayerCaptainGrantVo> captainGrants;
    private List<AccountPlayerTeamSummaryVo> teamSummary;
    private List<AccountPlayerRegistrationVo> registrations;
    private List<AccountPlayerNotificationVo> notifications;
    private List<AccountPlayerOperationLogVo> operationLogs;
}
