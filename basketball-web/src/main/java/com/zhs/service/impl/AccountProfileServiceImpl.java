package com.zhs.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.common.UserContext;
import com.zhs.dao.BasketFileAssetDao;
import com.zhs.dao.BasketMemberAccountDao;
import com.zhs.dao.BasketMemberProfileChangeDao;
import com.zhs.dao.BasketMemberProfileDao;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.enums.FileUsageTypeEnum;
import com.zhs.enums.MemberProfileChangeAuditStatusEnum;
import com.zhs.enums.VerifiedFlagEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketFileAsset;
import com.zhs.model.BasketMemberAccount;
import com.zhs.model.BasketMemberProfile;
import com.zhs.model.BasketMemberProfileChange;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.request.account.AccountAvatarBindReq;
import com.zhs.request.account.AccountProfileChangeSubmitReq;
import com.zhs.request.account.AccountProfileUpdateReq;
import com.zhs.response.account.AccountProfileChangeResp;
import com.zhs.response.account.AccountProfileResp;
import com.zhs.service.IAccountFileService;
import com.zhs.service.IAccountProfileService;
import com.zhs.support.MemberAvatarSupport;
import com.zhs.util.DataMaskUtil;
import jakarta.annotation.Resource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

@Service
public class AccountProfileServiceImpl implements IAccountProfileService {

    private static final Integer NOT_DELETED = 0;
    private static final int ACTIVE = 1;
    private static final DateTimeFormatter BIRTHDAY_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    @Resource
    private BasketMemberAccountDao memberAccountDao;
    @Resource
    private BasketMemberProfileDao memberProfileDao;
    @Resource
    private BasketMemberProfileChangeDao memberProfileChangeDao;
    @Resource
    private BasketPlayerProfileDao playerProfileDao;
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

        if (StringUtils.hasText(req.getNickname())) {
            String nickname = req.getNickname().trim();
            if (!Objects.equals(nickname, profile.getNickname())) {
                throw new ServiceException("昵称修改需提交审核，请使用资料变更申请");
            }
        }

        validateNonPublicProfileUpdate(req);

        if (req.getGender() != null) {
            profile.setGender(req.getGender());
        }
        if (req.getBirthday() != null) {
            profile.setBirthDate(parseBirthday(req.getBirthday()));
        }
        if (req.getProvinceCode() != null) {
            profile.setProvinceCode(trimToNull(req.getProvinceCode()));
        }
        if (req.getProvinceName() != null) {
            profile.setProvinceName(trimToNull(req.getProvinceName()));
        }
        if (req.getCityCode() != null) {
            profile.setCityCode(trimToNull(req.getCityCode()));
        }
        if (req.getCityName() != null) {
            profile.setCityName(trimToNull(req.getCityName()));
        }
        if (req.getBio() != null) {
            profile.setBio(trimToNull(req.getBio()));
        }

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
        requireAccount(memberId);
        BasketFileAsset asset = accountFileService.requireOwnedFile(fileId, memberId);
        if (!FileUsageTypeEnum.ACCOUNT_AVATAR.getCode().equalsIgnoreCase(asset.getUsageType())) {
            throw new ServiceException("文件用途不匹配");
        }
        // 仅返回预览 URL，正式头像需审核通过后生效
        return resolveAvatarUrl(asset);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountProfileChangeResp submitPublicProfileChange(AccountProfileChangeSubmitReq req) {
        if (req == null) {
            throw new ServiceException("请求参数不能为空");
        }
        Long memberId = UserContext.getRequiredUserId();
        BasketMemberAccount account = requireAccount(memberId);
        BasketMemberProfile profile = loadOrInitProfile(memberId, account.getEmail());

        BasketMemberProfileChange pending = memberProfileChangeDao.selectActiveByMemberId(memberId);
        if (pending != null && MemberProfileChangeAuditStatusEnum.isPending(pending.getAuditStatus())) {
            throw new ServiceException("你已有一条资料变更申请正在审核，请等待审核完成后再次修改");
        }

        String newNickname = StringUtils.hasText(req.getNickname())
                ? req.getNickname().trim()
                : profile.getNickname();
        validateNickname(newNickname);

        Long newAvatarFileId = profile.getAvatarFileId();
        if (StringUtils.hasText(req.getAvatarFileId())) {
            newAvatarFileId = parseFileId(req.getAvatarFileId());
            BasketFileAsset asset = accountFileService.requireOwnedFile(newAvatarFileId, memberId);
            if (!FileUsageTypeEnum.ACCOUNT_AVATAR.getCode().equalsIgnoreCase(asset.getUsageType())) {
                throw new ServiceException("文件用途不匹配");
            }
        }

        boolean nicknameChanged = !Objects.equals(normalize(profile.getNickname()), normalize(newNickname));
        boolean avatarChanged = !Objects.equals(profile.getAvatarFileId(), newAvatarFileId);
        if (!nicknameChanged && !avatarChanged) {
            throw new ServiceException("资料没有发生变化");
        }

        Date now = new Date();
        BasketMemberProfileChange change = new BasketMemberProfileChange();
        change.setChangeNo("MPC" + IdUtil.getSnowflakeNextIdStr());
        change.setMemberId(memberId);
        change.setPlayerId(resolvePlayerId(memberId));
        change.setOldNickname(profile.getNickname());
        change.setNewNickname(newNickname);
        change.setOldAvatarFileId(profile.getAvatarFileId());
        change.setNewAvatarFileId(newAvatarFileId);
        change.setOldAvatarUrl(resolveAvatarUrl(profile.getAvatarFileId()));
        change.setNewAvatarUrl(resolveAvatarUrl(newAvatarFileId));
        change.setChangeSummary(buildSummary(nicknameChanged, avatarChanged));
        change.setAuditStatus(MemberProfileChangeAuditStatusEnum.PENDING.getCode());
        change.setActiveFlag(ACTIVE);
        change.setCreateTime(now);
        change.setUpdateTime(now);
        change.setIsDelete(NOT_DELETED);
        try {
            memberProfileChangeDao.insert(change);
        } catch (DuplicateKeyException ex) {
            throw new ServiceException("你已有一条资料变更申请正在审核，请等待审核完成后再次修改");
        }
        return toChangeResp(change);
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

        BasketMemberProfileChange active = memberProfileChangeDao.selectActiveByMemberId(account.getId());
        if (active != null && MemberProfileChangeAuditStatusEnum.isPending(active.getAuditStatus())) {
            resp.setHasPendingChange(true);
            resp.setPendingChange(toChangeResp(active));
        } else {
            resp.setHasPendingChange(false);
            BasketMemberProfileChange latest = memberProfileChangeDao.selectLatestByMemberId(account.getId());
            if (latest != null && Objects.equals(latest.getAuditStatus(), MemberProfileChangeAuditStatusEnum.REJECTED.getCode())) {
                resp.setLatestRejectedChange(toChangeResp(latest));
            }
        }
        return resp;
    }

    private AccountProfileChangeResp toChangeResp(BasketMemberProfileChange change) {
        AccountProfileChangeResp resp = new AccountProfileChangeResp();
        resp.setChangeId(String.valueOf(change.getId()));
        resp.setChangeNo(change.getChangeNo());
        resp.setOldNickname(change.getOldNickname());
        resp.setNewNickname(change.getNewNickname());
        resp.setOldAvatarUrl(change.getOldAvatarUrl());
        resp.setNewAvatarUrl(change.getNewAvatarUrl());
        resp.setChangeSummary(change.getChangeSummary());
        resp.setAuditStatus(change.getAuditStatus());
        MemberProfileChangeAuditStatusEnum status = MemberProfileChangeAuditStatusEnum.fromCode(change.getAuditStatus());
        resp.setAuditStatusName(status == null ? null : status.getLabel());
        resp.setAuditRemark(change.getAuditRemark());
        resp.setSubmitTime(change.getCreateTime());
        resp.setAuditTime(change.getAuditTime());
        return resp;
    }

    private Long resolvePlayerId(Long memberId) {
        BasketPlayerProfile player = playerProfileDao.selectOne(new LambdaQueryWrapper<BasketPlayerProfile>()
                .eq(BasketPlayerProfile::getMemberId, memberId)
                .eq(BasketPlayerProfile::getIsDelete, NOT_DELETED)
                .last("limit 1"));
        return player == null ? null : player.getId();
    }

    private String buildSummary(boolean nicknameChanged, boolean avatarChanged) {
        List<String> parts = new ArrayList<>();
        if (nicknameChanged) {
            parts.add("昵称");
        }
        if (avatarChanged) {
            parts.add("头像");
        }
        return String.join("、", parts);
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

    private void validateNickname(String nickname) {
        if (!StringUtils.hasText(nickname)) {
            throw new ServiceException("昵称不能为空");
        }
        String value = nickname.trim();
        if (value.length() < 2 || value.length() > 20) {
            throw new ServiceException("昵称长度需在 2-20 个字符之间");
        }
        if (!value.matches("^[\\u4e00-\\u9fa5a-zA-Z0-9_]+$")) {
            throw new ServiceException("昵称仅支持中文、英文、数字和下划线");
        }
    }

    private void validateNonPublicProfileUpdate(AccountProfileUpdateReq req) {
        if (req.getGender() != null && (req.getGender() < 0 || req.getGender() > 2)) {
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

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }
}
