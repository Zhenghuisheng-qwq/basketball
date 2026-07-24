package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.common.UserContext;
import com.zhs.dao.BasketNotificationSettingDao;
import com.zhs.dao.BasketPrivacySettingDao;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketNotificationSetting;
import com.zhs.model.BasketPrivacySetting;
import com.zhs.request.account.NotificationSettingUpdateReq;
import com.zhs.request.account.PrivacySettingUpdateReq;
import com.zhs.response.account.NotificationSettingResp;
import com.zhs.response.account.PrivacySettingResp;
import com.zhs.service.IAccountSettingService;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountSettingServiceImpl implements IAccountSettingService {

    private static final Integer NOT_DELETED = 0;

    @Resource
    private BasketPrivacySettingDao privacySettingDao;
    @Resource
    private BasketNotificationSettingDao notificationSettingDao;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrivacySettingResp getPrivacy() {
        Long memberId = UserContext.getRequiredUserId();
        BasketPrivacySetting setting = loadOrInitPrivacy(memberId);
        return toPrivacyResp(setting);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PrivacySettingResp updatePrivacy(PrivacySettingUpdateReq req) {
        if (req == null) {
            throw new ServiceException("请求参数不能为空");
        }
        validateToggle(req.getProfileVisible(), "资料公开状态");
        validateToggle(req.getAllowTeamInvite(), "允许球队邀请");
        validateToggle(req.getShowTeamHistory(), "展示球队历史");
        validateToggle(req.getShowMatchData(), "展示比赛数据");
        validateToggle(req.getShowOnlineStatus(), "展示在线状态");

        Long memberId = UserContext.getRequiredUserId();
        BasketPrivacySetting setting = loadOrInitPrivacy(memberId);
        setting.setProfileVisible(req.getProfileVisible());
        setting.setAllowTeamInvite(req.getAllowTeamInvite());
        setting.setShowTeamHistory(req.getShowTeamHistory());
        setting.setShowMatchData(req.getShowMatchData());
        setting.setShowOnlineStatus(req.getShowOnlineStatus());
        privacySettingDao.updateById(setting);
        return toPrivacyResp(setting);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotificationSettingResp getNotification() {
        Long memberId = UserContext.getRequiredUserId();
        BasketNotificationSetting setting = loadOrInitNotification(memberId);
        return toNotificationResp(setting);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public NotificationSettingResp updateNotification(NotificationSettingUpdateReq req) {
        if (req == null) {
            throw new ServiceException("请求参数不能为空");
        }
        validateToggle(req.getSystemNotice(), "系统通知");
        validateToggle(req.getCertificationNotice(), "认证通知");
        validateToggle(req.getTournamentNotice(), "赛事通知");
        validateToggle(req.getTeamNotice(), "球队通知");
        validateToggle(req.getAnnouncementNotice(), "公告通知");
        validateToggle(req.getEmailNotice(), "邮件通知");
        validateToggle(req.getSmsNotice(), "短信通知");

        Long memberId = UserContext.getRequiredUserId();
        BasketNotificationSetting setting = loadOrInitNotification(memberId);
        setting.setSystemNotice(req.getSystemNotice());
        setting.setCertificationNotice(req.getCertificationNotice());
        setting.setTournamentNotice(req.getTournamentNotice());
        setting.setTeamNotice(req.getTeamNotice());
        setting.setAnnouncementNotice(req.getAnnouncementNotice());
        setting.setEmailNotice(req.getEmailNotice());
        setting.setSmsNotice(req.getSmsNotice());
        notificationSettingDao.updateById(setting);
        return toNotificationResp(setting);
    }

    private BasketPrivacySetting loadOrInitPrivacy(Long memberId) {
        BasketPrivacySetting setting = findPrivacy(memberId);
        if (setting != null) {
            return setting;
        }

        BasketPrivacySetting created = new BasketPrivacySetting();
        created.setMemberId(memberId);
        created.setProfileVisible(1);
        created.setAllowTeamInvite(1);
        created.setShowTeamHistory(1);
        created.setShowMatchData(1);
        created.setShowOnlineStatus(0);
        created.setIsDelete(NOT_DELETED);
        try {
            privacySettingDao.insert(created);
            return created;
        } catch (DuplicateKeyException ex) {
            BasketPrivacySetting existing = findPrivacy(memberId);
            if (existing != null) {
                return existing;
            }
            throw new ServiceException("隐私设置初始化失败，请稍后重试");
        }
    }

    private BasketNotificationSetting loadOrInitNotification(Long memberId) {
        BasketNotificationSetting setting = findNotification(memberId);
        if (setting != null) {
            return setting;
        }

        BasketNotificationSetting created = new BasketNotificationSetting();
        created.setMemberId(memberId);
        created.setSystemNotice(1);
        created.setCertificationNotice(1);
        created.setTournamentNotice(1);
        created.setTeamNotice(1);
        created.setAnnouncementNotice(1);
        created.setEmailNotice(1);
        created.setSmsNotice(0);
        created.setIsDelete(NOT_DELETED);
        try {
            notificationSettingDao.insert(created);
            return created;
        } catch (DuplicateKeyException ex) {
            BasketNotificationSetting existing = findNotification(memberId);
            if (existing != null) {
                return existing;
            }
            throw new ServiceException("通知设置初始化失败，请稍后重试");
        }
    }

    private BasketPrivacySetting findPrivacy(Long memberId) {
        return privacySettingDao.selectOne(new LambdaQueryWrapper<BasketPrivacySetting>()
                .eq(BasketPrivacySetting::getMemberId, memberId)
                .eq(BasketPrivacySetting::getIsDelete, NOT_DELETED)
                .last("limit 1"));
    }

    private BasketNotificationSetting findNotification(Long memberId) {
        return notificationSettingDao.selectOne(new LambdaQueryWrapper<BasketNotificationSetting>()
                .eq(BasketNotificationSetting::getMemberId, memberId)
                .eq(BasketNotificationSetting::getIsDelete, NOT_DELETED)
                .last("limit 1"));
    }

    private PrivacySettingResp toPrivacyResp(BasketPrivacySetting setting) {
        PrivacySettingResp resp = new PrivacySettingResp();
        resp.setProfileVisible(setting.getProfileVisible());
        resp.setAllowTeamInvite(setting.getAllowTeamInvite());
        resp.setShowTeamHistory(setting.getShowTeamHistory());
        resp.setShowMatchData(setting.getShowMatchData());
        resp.setShowOnlineStatus(setting.getShowOnlineStatus());
        return resp;
    }

    private NotificationSettingResp toNotificationResp(BasketNotificationSetting setting) {
        NotificationSettingResp resp = new NotificationSettingResp();
        resp.setSystemNotice(setting.getSystemNotice());
        resp.setCertificationNotice(setting.getCertificationNotice());
        resp.setTournamentNotice(setting.getTournamentNotice());
        resp.setTeamNotice(setting.getTeamNotice());
        resp.setAnnouncementNotice(setting.getAnnouncementNotice());
        resp.setEmailNotice(setting.getEmailNotice());
        resp.setSmsNotice(setting.getSmsNotice());
        return resp;
    }

    private void validateToggle(Integer value, String fieldName) {
        if (value == null) {
            throw new ServiceException(fieldName + "不能为空");
        }
        if (value != 0 && value != 1) {
            throw new ServiceException(fieldName + "取值无效，仅支持 0 或 1");
        }
    }
}
