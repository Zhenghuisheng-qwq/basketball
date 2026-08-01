package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.common.UserContext;
import com.zhs.dao.BasketFileAssetDao;
import com.zhs.dao.BasketMemberAccountDao;
import com.zhs.dao.BasketMemberProfileDao;
import com.zhs.enums.FileUsageTypeEnum;
import com.zhs.enums.VerifiedFlagEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketFileAsset;
import com.zhs.model.BasketMemberAccount;
import com.zhs.model.BasketMemberProfile;
import com.zhs.request.account.AccountAvatarBindReq;
import com.zhs.request.account.AccountProfileUpdateReq;
import com.zhs.response.account.AccountProfileResp;
import com.zhs.service.IAccountFileService;
import com.zhs.service.IAccountProfileService;
import com.zhs.support.MemberAvatarSupport;
import com.zhs.util.DataMaskUtil;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Date;

@Service
public class AccountProfileServiceImpl implements IAccountProfileService {

    private static final Integer NOT_DELETED = 0;
    private static final DateTimeFormatter BIRTHDAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Resource
    private BasketMemberAccountDao memberAccountDao;
    @Resource
    private BasketMemberProfileDao memberProfileDao;
    @Resource
    private BasketFileAssetDao fileAssetDao;
    @Resource
    private IAccountFileService accountFileService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountProfileResp getProfile() {
        Long memberId = UserContext.getRequiredUserId();
        BasketMemberAccount account = requireAccount(memberId);
        BasketMemberProfile profile = loadOrInitProfile(memberId, account.getEmail());
        return buildProfileResp(account, profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountProfileResp updateProfile(AccountProfileUpdateReq req) {
        if (req == null) {
            throw new ServiceException("请求参数不能为空");
        }

        Long memberId = UserContext.getRequiredUserId();
        BasketMemberAccount account = requireAccount(memberId);
        BasketMemberProfile profile = loadOrInitProfile(memberId, account.getEmail());

        validateProfileUpdate(req);

        profile.setNickname(req.getNickname().trim());
        profile.setGender(req.getGender());
        profile.setBirthDate(parseBirthday(req.getBirthday()));
        profile.setProvinceCode(trimToNull(req.getProvinceCode()));
        profile.setProvinceName(trimToNull(req.getProvinceName()));
        profile.setCityCode(trimToNull(req.getCityCode()));
        profile.setCityName(trimToNull(req.getCityName()));
        profile.setBio(trimToNull(req.getBio()));

        memberProfileDao.updateById(profile);
        return buildProfileResp(account, profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String bindAvatar(AccountAvatarBindReq req) {
        if (req == null || !StringUtils.hasText(req.getFileId())) {
            throw new ServiceException("文件主键不能为空");
        }

        Long fileId = parseFileId(req.getFileId());
        Long memberId = UserContext.getRequiredUserId();
        BasketMemberAccount account = requireAccount(memberId);
        BasketMemberProfile profile = loadOrInitProfile(memberId, account.getEmail());

        BasketFileAsset asset = accountFileService.requireOwnedFile(fileId, memberId);
        if (!FileUsageTypeEnum.ACCOUNT_AVATAR.getCode().equalsIgnoreCase(asset.getUsageType())) {
            throw new ServiceException("文件用途不匹配");
        }

        profile.setAvatarFileId(fileId);
        memberProfileDao.updateById(profile);
        return resolveAvatarUrl(asset);
    }

    private BasketMemberAccount requireAccount(Long memberId) {
        BasketMemberAccount account = memberAccountDao.selectById(memberId);
        if (account == null || !NOT_DELETED.equals(account.getIsDelete())) {
            throw new ServiceException("账号不存在");
        }
        return account;
    }

    private BasketMemberProfile loadOrInitProfile(Long memberId, String email) {
        BasketMemberProfile profile = memberProfileDao.selectOne(new LambdaQueryWrapper<BasketMemberProfile>()
                .eq(BasketMemberProfile::getMemberId, memberId)
                .eq(BasketMemberProfile::getIsDelete, NOT_DELETED)
                .last("limit 1"));
        if (profile != null) {
            return profile;
        }

        BasketMemberProfile created = new BasketMemberProfile();
        created.setMemberId(memberId);
        created.setNickname(deriveNicknameFromEmail(email));
        created.setGender(0);
        created.setProfileStatus(1);
        created.setIsDelete(NOT_DELETED);
        memberProfileDao.insert(created);
        return created;
    }

    private AccountProfileResp buildProfileResp(BasketMemberAccount account, BasketMemberProfile profile) {
        AccountProfileResp resp = new AccountProfileResp();
        resp.setAccountId(String.valueOf(account.getId()));
        resp.setAccountNo(account.getAccountNo());
        resp.setNickname(profile.getNickname());
        resp.setAvatarUrl(resolveAvatarUrl(profile.getAvatarFileId()));
        resp.setEmailMasked(DataMaskUtil.maskEmail(account.getEmail()));
        resp.setEmailVerified(VerifiedFlagEnum.isYes(account.getEmailVerified()));
        resp.setGender(profile.getGender());
        resp.setBirthday(formatBirthday(profile.getBirthDate()));
        resp.setProvinceCode(profile.getProvinceCode());
        resp.setProvinceName(profile.getProvinceName());
        resp.setCityCode(profile.getCityCode());
        resp.setCityName(profile.getCityName());
        resp.setBio(profile.getBio());
        resp.setRegisterTime(account.getCreateTime());
        resp.setLastLoginTime(account.getLastLoginTime());
        return resp;
    }

    private String resolveAvatarUrl(Long avatarFileId) {
        if (avatarFileId == null) {
            return MemberAvatarSupport.resolve(null);
        }
        BasketFileAsset asset = fileAssetDao.selectById(avatarFileId);
        if (asset == null || !NOT_DELETED.equals(asset.getIsDelete())) {
            return MemberAvatarSupport.resolve(null);
        }
        return resolveAvatarUrl(asset);
    }

    private String resolveAvatarUrl(BasketFileAsset asset) {
        return MemberAvatarSupport.resolve(asset == null ? null : asset.getAccessUrl());
    }

    private void validateProfileUpdate(AccountProfileUpdateReq req) {
        if (!StringUtils.hasText(req.getNickname())) {
            throw new ServiceException("昵称不能为空");
        }
        String nickname = req.getNickname().trim();
        if (nickname.length() < 2 || nickname.length() > 20) {
            throw new ServiceException("昵称长度需在 2-20 个字符之间");
        }

        if (req.getGender() == null) {
            throw new ServiceException("性别不能为空");
        }
        if (req.getGender() < 0 || req.getGender() > 2) {
            throw new ServiceException("性别取值无效");
        }

        if (StringUtils.hasText(req.getBirthday())) {
            LocalDate birthday = parseBirthdayLocalDate(req.getBirthday());
            if (birthday.isAfter(LocalDate.now(ZoneId.of("Asia/Shanghai")))) {
                throw new ServiceException("出生日期不能晚于今天");
            }
        }

        if (StringUtils.hasText(req.getBio()) && req.getBio().length() > 500) {
            throw new ServiceException("个人简介最多 500 字");
        }
    }

    private Date parseBirthday(String birthday) {
        if (!StringUtils.hasText(birthday)) {
            return null;
        }
        LocalDate localDate = parseBirthdayLocalDate(birthday);
        return Date.from(localDate.atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant());
    }

    private LocalDate parseBirthdayLocalDate(String birthday) {
        try {
            return LocalDate.parse(birthday.trim(), BIRTHDAY_FORMAT);
        } catch (DateTimeParseException ex) {
            throw new ServiceException("出生日期格式无效，请使用 yyyy-MM-dd");
        }
    }

    private String formatBirthday(Date birthDate) {
        if (birthDate == null) {
            return null;
        }
        return birthDate.toInstant().atZone(ZoneId.of("Asia/Shanghai")).toLocalDate().format(BIRTHDAY_FORMAT);
    }

    private Long parseFileId(String fileId) {
        try {
            return Long.parseLong(fileId.trim());
        } catch (NumberFormatException ex) {
            throw new ServiceException("文件主键格式无效");
        }
    }

    private String deriveNicknameFromEmail(String email) {
        if (!StringUtils.hasText(email)) {
            return "CNPA用户";
        }
        String local = email.split("@")[0];
        return local.length() > 20 ? local.substring(0, 20) : local;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
