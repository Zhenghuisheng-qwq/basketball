package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.common.UserContext;
import com.zhs.dao.BasketAccountSecurityLogDao;
import com.zhs.dao.BasketLoginLogDao;
import com.zhs.model.BasketAccountSecurityLog;
import com.zhs.model.BasketLoginLog;
import com.zhs.response.account.LoginLogItemResp;
import com.zhs.response.account.SecurityLogItemResp;
import com.zhs.service.IAccountSecurityService;
import com.zhs.util.DataMaskUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountSecurityServiceImpl implements IAccountSecurityService {

    private static final Integer NOT_DELETED = 0;

    @Resource
    private BasketLoginLogDao loginLogDao;
    @Resource
    private BasketAccountSecurityLogDao accountSecurityLogDao;

    @Override
    public IPage<LoginLogItemResp> pageLoginLogs(Integer currentPage, Integer pageSize) {
        Long memberId = UserContext.getRequiredUserId();
        Page<BasketLoginLog> page = new Page<>(normalizePage(currentPage), normalizeSize(pageSize));
        IPage<BasketLoginLog> logPage = loginLogDao.selectPage(page, new LambdaQueryWrapper<BasketLoginLog>()
                .eq(BasketLoginLog::getMemberId, memberId)
                .eq(BasketLoginLog::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketLoginLog::getLoginTime)
                .orderByDesc(BasketLoginLog::getId));

        Page<LoginLogItemResp> respPage = new Page<>(logPage.getCurrent(), logPage.getSize(), logPage.getTotal());
        respPage.setRecords(toLoginLogItems(logPage.getRecords()));
        return respPage;
    }

    @Override
    public IPage<SecurityLogItemResp> pageOperationLogs(Integer currentPage, Integer pageSize) {
        Long memberId = UserContext.getRequiredUserId();
        Page<BasketAccountSecurityLog> page = new Page<>(normalizePage(currentPage), normalizeSize(pageSize));
        IPage<BasketAccountSecurityLog> logPage = accountSecurityLogDao.selectPage(page,
                new LambdaQueryWrapper<BasketAccountSecurityLog>()
                        .eq(BasketAccountSecurityLog::getMemberId, memberId)
                        .eq(BasketAccountSecurityLog::getIsDelete, NOT_DELETED)
                        .orderByDesc(BasketAccountSecurityLog::getEventTime)
                        .orderByDesc(BasketAccountSecurityLog::getId));

        Page<SecurityLogItemResp> respPage = new Page<>(logPage.getCurrent(), logPage.getSize(), logPage.getTotal());
        respPage.setRecords(toSecurityLogItems(logPage.getRecords()));
        return respPage;
    }

    private List<LoginLogItemResp> toLoginLogItems(List<BasketLoginLog> logs) {
        if (CollectionUtils.isEmpty(logs)) {
            return Collections.emptyList();
        }
        return logs.stream().map(this::toLoginLogItem).collect(Collectors.toList());
    }

    private LoginLogItemResp toLoginLogItem(BasketLoginLog log) {
        LoginLogItemResp resp = new LoginLogItemResp();
        resp.setLogId(String.valueOf(log.getId()));
        resp.setLoginType(log.getLoginType());
        resp.setLoginResult(log.getLoginResult());
        resp.setFailureReason(log.getFailureReason());
        resp.setClientIpMasked(DataMaskUtil.maskIp(log.getClientIp()));
        resp.setClientRegion(log.getClientRegion());
        resp.setTerminalType(log.getTerminalType());
        resp.setUserAgentMasked(DataMaskUtil.maskUa(log.getUserAgent()));
        resp.setLoginTime(log.getLoginTime());
        return resp;
    }

    private List<SecurityLogItemResp> toSecurityLogItems(List<BasketAccountSecurityLog> logs) {
        if (CollectionUtils.isEmpty(logs)) {
            return Collections.emptyList();
        }
        return logs.stream().map(this::toSecurityLogItem).collect(Collectors.toList());
    }

    private SecurityLogItemResp toSecurityLogItem(BasketAccountSecurityLog log) {
        SecurityLogItemResp resp = new SecurityLogItemResp();
        resp.setLogId(String.valueOf(log.getId()));
        resp.setEventType(log.getEventType());
        resp.setEventTitle(log.getEventTitle());
        resp.setOldValueMasked(log.getOldValueMasked());
        resp.setNewValueMasked(log.getNewValueMasked());
        resp.setOperatorType(log.getOperatorType());
        resp.setClientIpMasked(DataMaskUtil.maskIp(log.getClientIp()));
        resp.setTerminalType(log.getTerminalType());
        resp.setEventTime(log.getEventTime());
        resp.setRemark(log.getRemark());
        return resp;
    }

    private long normalizePage(Integer currentPage) {
        return currentPage == null || currentPage < 1 ? 1L : currentPage;
    }

    private long normalizeSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10L : pageSize;
    }
}
