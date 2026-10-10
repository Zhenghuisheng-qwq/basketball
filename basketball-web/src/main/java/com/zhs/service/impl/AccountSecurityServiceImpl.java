package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.common.UserContext;
import com.zhs.dao.BasketAccountSecurityLogDao;
import com.zhs.dao.BasketLoginLogDao;
import com.zhs.dao.BasketMemberAccountDao;
import com.zhs.enums.MemberAccountStatusEnum;
import com.zhs.exception.BusinessReason;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketAccountSecurityLog;
import com.zhs.model.BasketLoginLog;
import com.zhs.model.BasketMemberAccount;
import com.zhs.request.account.AccountChangeEmailReq;
import com.zhs.request.account.AccountChangePasswordReq;
import com.zhs.response.account.AccountChangeEmailResp;
import com.zhs.response.account.AccountChangePasswordResp;
import com.zhs.response.account.LoginLogItemResp;
import com.zhs.response.account.SecurityLogItemResp;
import com.zhs.service.IAccountSecurityService;
import com.zhs.service.IBasketMemberAuthService;
import com.zhs.support.MemberSessionSupport;
import com.zhs.support.PageSizeLimiter;
import com.zhs.util.DataMaskUtil;
import jakarta.annotation.Resource;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountSecurityServiceImpl implements IAccountSecurityService {

    private static final Integer NOT_DELETED = 0;
    private static final String EVENT_PASSWORD_CHANGE = "PASSWORD_CHANGE";
    private static final String OPERATOR_MEMBER = "MEMBER";

    @Resource
    private BasketLoginLogDao loginLogDao;
    @Resource
    private BasketAccountSecurityLogDao accountSecurityLogDao;
    @Resource
    private BasketMemberAccountDao memberAccountDao;
    @Resource
    private PasswordEncoder passwordEncoder;
    @Resource
    private MemberSessionSupport memberSessionSupport;
    @Resource
    private IBasketMemberAuthService basketMemberAuthService;

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

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountChangePasswordResp changePassword(AccountChangePasswordReq req, String clientIp, String terminalType, String currentToken) {
        if (req == null) {
            throw BusinessReason.INVALID_REQUEST.exception();
        }
        if (!StringUtils.hasText(req.getOldPassword())) {
            throw BusinessReason.CURRENT_PASSWORD_REQUIRED.exception();
        }
        validatePasswordPair(req.getNewPassword(), req.getConfirmPassword());

        Long memberId = UserContext.getRequiredUserId();
        BasketMemberAccount account = memberAccountDao.selectById(memberId);
        if (account == null || !NOT_DELETED.equals(account.getIsDelete())) {
            throw BusinessReason.ACCOUNT_NOT_FOUND.exception();
        }
        if (!MemberAccountStatusEnum.isNormal(account.getAccountStatus())) {
            throw BusinessReason.ACCOUNT_DISABLED.exception();
        }
        if (!StringUtils.hasText(account.getPasswordHash())
                || !passwordEncoder.matches(req.getOldPassword(), account.getPasswordHash())) {
            throw BusinessReason.PASSWORD_INCORRECT.exception();
        }
        if (passwordEncoder.matches(req.getNewPassword(), account.getPasswordHash())) {
            throw BusinessReason.PASSWORD_UNCHANGED.exception();
        }

        Date now = new Date();
        account.setPasswordHash(passwordEncoder.encode(req.getNewPassword()));
        account.setPasswordUpdateTime(now);
        memberAccountDao.updateById(account);

        BasketAccountSecurityLog logEntity = new BasketAccountSecurityLog();
        logEntity.setMemberId(memberId);
        logEntity.setEventType(EVENT_PASSWORD_CHANGE);
        logEntity.setEventTitle("修改登录密码");
        logEntity.setOperatorType(OPERATOR_MEMBER);
        logEntity.setOperatorId(memberId);
        logEntity.setClientIp(clientIp);
        logEntity.setTerminalType(terminalType);
        logEntity.setEventTime(now);
        logEntity.setIsDelete(NOT_DELETED);
        accountSecurityLogDao.insert(logEntity);

        long authVersion = memberSessionSupport.resolveAuthVersion(account);
        memberSessionSupport.invalidateAllSessions(memberId, currentToken, authVersion);

        AccountChangePasswordResp resp = new AccountChangePasswordResp();
        resp.setNeedRelogin(true);
        return resp;
    }

    @Override
    public AccountChangeEmailResp changeEmail(AccountChangeEmailReq req, String clientIp, String currentToken) {
        return basketMemberAuthService.changeEmail(req, UserContext.getRequiredUserId(), clientIp, currentToken);
    }

    private void validatePasswordPair(String password, String confirmPassword) {
        if (!StringUtils.hasText(password)) {
            throw BusinessReason.PASSWORD_REQUIRED.exception();
        }
        if (password.length() < 8 || password.length() > 20) {
            throw BusinessReason.PASSWORD_LENGTH_INVALID.exception();
        }
        if (!password.matches(".*[A-Za-z].*") || !password.matches(".*\\d.*")) {
            throw BusinessReason.PASSWORD_COMPLEXITY.exception();
        }
        if (!password.equals(confirmPassword)) {
            throw BusinessReason.PASSWORD_MISMATCH.exception();
        }
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
        return PageSizeLimiter.clamp(pageSize);
    }
}
