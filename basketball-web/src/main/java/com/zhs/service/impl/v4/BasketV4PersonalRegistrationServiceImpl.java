package com.zhs.service.impl.v4;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.common.UserContext;
import com.zhs.dao.BasketPlayerPlatformAccountDao;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.dao.v4.BasketDivisionDao;
import com.zhs.dao.v4.BasketPersonalRegistrationAuditDao;
import com.zhs.dao.v4.BasketPersonalRegistrationDao;
import com.zhs.dao.v4.BasketRegistrationPlayerDao;
import com.zhs.dao.v4.BasketSeasonDao;
import com.zhs.enums.v4.DivisionStatusEnum;
import com.zhs.enums.v4.PersonalAuditActionEnum;
import com.zhs.enums.v4.PersonalRegistrationStatusEnum;
import com.zhs.enums.v4.RegistrationModeEnum;
import com.zhs.exception.ServiceException;
import com.zhs.mapper.v4.BasketV4PersonalRegistrationMapper;
import com.zhs.model.BasketPlayerPlatformAccount;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.v4.BasketDivision;
import com.zhs.model.v4.BasketPersonalRegistration;
import com.zhs.model.v4.BasketPersonalRegistrationAudit;
import com.zhs.model.v4.BasketRegistrationPlayer;
import com.zhs.model.v4.BasketSeason;
import com.zhs.request.v4.V4PersonalRegistrationIdReq;
import com.zhs.request.v4.V4PersonalRegistrationSaveReq;
import com.zhs.response.v4.V4MyPersonalRegistrationResp;
import com.zhs.response.v4.V4PersonalAvailableDivisionResp;
import com.zhs.service.v4.IBasketV4PersonalRegistrationService;
import com.zhs.support.GameTerminalSupport;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

@Service
public class BasketV4PersonalRegistrationServiceImpl implements IBasketV4PersonalRegistrationService {

    private static final Integer NOT_DELETED = 0;
    private static final int PLAYER_CERT_APPROVED = 3;
    private static final int PLATFORM_ACCOUNT_ACTIVE = 1;

    @Resource
    private BasketV4PersonalRegistrationMapper personalRegistrationMapper;
    @Resource
    private BasketPersonalRegistrationDao personalRegistrationDao;
    @Resource
    private BasketPersonalRegistrationAuditDao personalRegistrationAuditDao;
    @Resource
    private BasketDivisionDao divisionDao;
    @Resource
    private BasketPlayerProfileDao playerProfileDao;
    @Resource
    private BasketPlayerPlatformAccountDao platformAccountDao;
    @Resource
    private BasketRegistrationPlayerDao registrationPlayerDao;
    @Resource
    private BasketSeasonDao seasonDao;

    @Override
    public List<V4PersonalAvailableDivisionResp> listAvailableDivisions() {
        Long memberId = UserContext.getRequiredMemberId();
        BasketPlayerProfile player = requirePlayerByMemberId(memberId);
        List<V4PersonalAvailableDivisionResp> rows = personalRegistrationMapper
                .selectAvailablePersonalDivisions(memberId, player.getId());
        Date now = new Date();
        for (V4PersonalAvailableDivisionResp row : rows) {
            enrichAvailability(row, player.getId(), now);
        }
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveRegistration(V4PersonalRegistrationSaveReq req) {
        if (req == null || req.getDivisionId() == null) {
            throw new ServiceException("divisionId 不能为空");
        }
        Long memberId = UserContext.getRequiredMemberId();
        BasketPlayerProfile player = requirePlayerByMemberId(memberId);
        BasketDivision division = requirePersonalDivision(req.getDivisionId());
        validateRegistrationWindow(division);
        validateCapacity(division);
        requireCertifiedPlayer(player.getId());
        BasketPlayerPlatformAccount account = requirePlatformIdentity(player.getId(), division.getPlatformCode());
        assertNoTeamRegistrationConflict(division.getId(), player.getId());

        BasketPersonalRegistration existing = findActiveRegistration(division.getId(), player.getId());
        if (existing != null && PersonalRegistrationStatusEnum.APPROVED.getCode().equals(existing.getRegistrationStatus())) {
            throw new ServiceException("个人报名已审核通过，不可再编辑");
        }
        if (existing != null && PersonalRegistrationStatusEnum.ASSIGNED.getCode().equals(existing.getRegistrationStatus())) {
            throw new ServiceException("个人报名已分配临时队伍，不可再编辑");
        }
        if (existing != null && PersonalRegistrationStatusEnum.CONVERTED.getCode().equals(existing.getRegistrationStatus())) {
            throw new ServiceException("个人报名已完成转换，不可再编辑");
        }

        Date now = new Date();
        if (existing == null) {
            BasketPersonalRegistration registration = new BasketPersonalRegistration();
            registration.setDivisionId(division.getId());
            registration.setSeasonId(division.getSeasonId());
            registration.setSeriesId(requireSeriesId(division.getSeasonId()));
            registration.setUserId(memberId);
            registration.setPlayerId(player.getId());
            registration.setPlayerPlatformId(account.getId());
            registration.setActualPlatformCode(normalizePlatformCode(account.getPlatform()));
            registration.setGameAccountId(account.getDisplayGameId());
            registration.setPreferredPosition(req.getPreferredPosition());
            registration.setRegistrationStatus(PersonalRegistrationStatusEnum.DRAFT.getCode());
            registration.setCreateBy(memberId);
            registration.setUpdateBy(memberId);
            registration.setCreateTime(now);
            registration.setUpdateTime(now);
            registration.setIsDelete(NOT_DELETED);
            personalRegistrationDao.insert(registration);
            return registration.getId();
        }

        if (!PersonalRegistrationStatusEnum.DRAFT.getCode().equals(existing.getRegistrationStatus())
                && !PersonalRegistrationStatusEnum.REJECTED.getCode().equals(existing.getRegistrationStatus())
                && !PersonalRegistrationStatusEnum.WITHDRAWN.getCode().equals(existing.getRegistrationStatus())) {
            throw new ServiceException("当前状态不可保存草稿");
        }
        existing.setPreferredPosition(req.getPreferredPosition());
        existing.setPlayerPlatformId(account.getId());
        existing.setActualPlatformCode(normalizePlatformCode(account.getPlatform()));
        existing.setGameAccountId(account.getDisplayGameId());
        existing.setRegistrationStatus(PersonalRegistrationStatusEnum.DRAFT.getCode());
        existing.setRejectReason(null);
        existing.setUpdateBy(memberId);
        existing.setUpdateTime(now);
        personalRegistrationDao.updateById(existing);
        return existing.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void submitRegistration(V4PersonalRegistrationIdReq req) {
        BasketPersonalRegistration registration = requireOwnRegistration(req.getPersonalRegistrationId());
        if (!PersonalRegistrationStatusEnum.DRAFT.getCode().equals(registration.getRegistrationStatus())
                && !PersonalRegistrationStatusEnum.REJECTED.getCode().equals(registration.getRegistrationStatus())
                && !PersonalRegistrationStatusEnum.WITHDRAWN.getCode().equals(registration.getRegistrationStatus())) {
            throw new ServiceException("当前状态不可提交");
        }
        BasketDivision division = requirePersonalDivision(registration.getDivisionId());
        validateRegistrationWindow(division);
        validateCapacity(division);
        requireCertifiedPlayer(registration.getPlayerId());
        requirePlatformIdentity(registration.getPlayerId(), division.getPlatformCode());
        assertNoTeamRegistrationConflict(division.getId(), registration.getPlayerId());
        assertNoDuplicatePersonal(division.getId(), registration.getPlayerId(), registration.getId());

        Date now = new Date();
        Long memberId = UserContext.getRequiredMemberId();
        String beforeStatus = registration.getRegistrationStatus();
        registration.setRegistrationStatus(PersonalRegistrationStatusEnum.PENDING_REVIEW.getCode());
        registration.setSubmitTime(now);
        registration.setActiveUniqueKey(buildActiveUniqueKey(division.getId(), registration.getPlayerId()));
        registration.setUpdateBy(memberId);
        registration.setUpdateTime(now);
        personalRegistrationDao.updateById(registration);
        writeAudit(registration, PersonalAuditActionEnum.SUBMIT, beforeStatus,
                PersonalRegistrationStatusEnum.PENDING_REVIEW.getCode(), null, memberId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void withdrawRegistration(V4PersonalRegistrationIdReq req) {
        BasketPersonalRegistration registration = requireOwnRegistration(req.getPersonalRegistrationId());
        if (!PersonalRegistrationStatusEnum.PENDING_REVIEW.getCode().equals(registration.getRegistrationStatus())) {
            throw new ServiceException("仅待审核状态可撤回");
        }
        Date now = new Date();
        Long memberId = UserContext.getRequiredMemberId();
        String beforeStatus = registration.getRegistrationStatus();
        registration.setRegistrationStatus(PersonalRegistrationStatusEnum.WITHDRAWN.getCode());
        registration.setActiveUniqueKey(null);
        registration.setUpdateBy(memberId);
        registration.setUpdateTime(now);
        personalRegistrationDao.updateById(registration);
        writeAudit(registration, PersonalAuditActionEnum.WITHDRAW, beforeStatus,
                PersonalRegistrationStatusEnum.WITHDRAWN.getCode(), null, memberId);
    }

    @Override
    public List<V4MyPersonalRegistrationResp> listMyRegistrations() {
        return personalRegistrationMapper.selectMyPersonalRegistrations(UserContext.getRequiredMemberId());
    }

    private void enrichAvailability(V4PersonalAvailableDivisionResp row, Long playerId, Date now) {
        row.setCanRegister(false);
        if (row.getRegistrationStartTime() != null && row.getRegistrationStartTime().after(now)) {
            row.setCannotRegisterReason("报名尚未开始");
            return;
        }
        if (row.getRegistrationEndTime() != null && row.getRegistrationEndTime().before(now)) {
            row.setCannotRegisterReason("报名已结束");
            return;
        }
        if (row.getRemainingPersonCount() != null && row.getRemainingPersonCount() <= 0) {
            row.setCannotRegisterReason("名额已满");
            return;
        }
        try {
            requireCertifiedPlayer(playerId);
            requirePlatformIdentity(playerId, row.getPlatformCode());
        } catch (ServiceException ex) {
            row.setCannotRegisterReason(ex.getMessage());
            return;
        }
        if (PersonalRegistrationStatusEnum.PENDING_REVIEW.getCode().equals(row.getMyRegistrationStatus())) {
            row.setCannotRegisterReason("已有审核中的个人报名");
            return;
        }
        if (PersonalRegistrationStatusEnum.APPROVED.getCode().equals(row.getMyRegistrationStatus())
                || PersonalRegistrationStatusEnum.ASSIGNED.getCode().equals(row.getMyRegistrationStatus())
                || PersonalRegistrationStatusEnum.CONVERTED.getCode().equals(row.getMyRegistrationStatus())) {
            row.setCannotRegisterReason("已完成个人报名");
            return;
        }
        row.setCanRegister(true);
        row.setCannotRegisterReason(null);
    }

    private BasketPersonalRegistration findActiveRegistration(Long divisionId, Long playerId) {
        return personalRegistrationDao.selectOne(new LambdaQueryWrapper<BasketPersonalRegistration>()
                .eq(BasketPersonalRegistration::getDivisionId, divisionId)
                .eq(BasketPersonalRegistration::getPlayerId, playerId)
                .eq(BasketPersonalRegistration::getIsDelete, NOT_DELETED)
                .isNotNull(BasketPersonalRegistration::getActiveUniqueKey)
                .last("limit 1"));
    }

    private BasketPersonalRegistration requireOwnRegistration(Long personalRegistrationId) {
        if (personalRegistrationId == null) {
            throw new ServiceException("personalRegistrationId 不能为空");
        }
        Long memberId = UserContext.getRequiredMemberId();
        BasketPersonalRegistration registration = personalRegistrationDao.selectById(personalRegistrationId);
        if (registration == null || !NOT_DELETED.equals(registration.getIsDelete())) {
            throw new ServiceException("个人报名不存在");
        }
        if (!memberId.equals(registration.getUserId())) {
            throw new ServiceException("无权操作该个人报名");
        }
        return registration;
    }

    private BasketDivision requirePersonalDivision(Long divisionId) {
        BasketDivision division = divisionDao.selectById(divisionId);
        if (division == null || !NOT_DELETED.equals(division.getIsDelete())) {
            throw new ServiceException("Division 不存在");
        }
        if (!RegistrationModeEnum.PERSONAL.getCode().equals(division.getRegistrationMode())) {
            throw new ServiceException("该 Division 不支持个人报名");
        }
        if (!DivisionStatusEnum.REGISTRATION_OPEN.getCode().equals(division.getDivisionStatus())) {
            throw new ServiceException("该 Division 尚未开放报名");
        }
        return division;
    }

    private void validateRegistrationWindow(BasketDivision division) {
        Date now = new Date();
        if (division.getRegistrationStartTime() != null && division.getRegistrationStartTime().after(now)) {
            throw new ServiceException("报名尚未开始");
        }
        if (division.getRegistrationEndTime() != null && division.getRegistrationEndTime().before(now)) {
            throw new ServiceException("报名已结束");
        }
    }

    private void validateCapacity(BasketDivision division) {
        if (division.getMaxPersonCount() == null) {
            return;
        }
        Long count = personalRegistrationDao.selectCount(new LambdaQueryWrapper<BasketPersonalRegistration>()
                .eq(BasketPersonalRegistration::getDivisionId, division.getId())
                .eq(BasketPersonalRegistration::getIsDelete, NOT_DELETED)
                .isNotNull(BasketPersonalRegistration::getActiveUniqueKey)
                .notIn(BasketPersonalRegistration::getRegistrationStatus,
                        PersonalRegistrationStatusEnum.WITHDRAWN.getCode(),
                        PersonalRegistrationStatusEnum.REJECTED.getCode()));
        if (count != null && count >= division.getMaxPersonCount()) {
            throw new ServiceException("个人报名名额已满");
        }
    }

    private void assertNoDuplicatePersonal(Long divisionId, Long playerId, Long excludeId) {
        BasketPersonalRegistration active = findActiveRegistration(divisionId, playerId);
        if (active != null && !active.getId().equals(excludeId)) {
            throw new ServiceException("您已在该 Division 提交个人报名");
        }
    }

    private void assertNoTeamRegistrationConflict(Long divisionId, Long playerId) {
        Long count = registrationPlayerDao.selectCount(new LambdaQueryWrapper<BasketRegistrationPlayer>()
                .eq(BasketRegistrationPlayer::getDivisionId, divisionId)
                .eq(BasketRegistrationPlayer::getPlayerId, playerId)
                .eq(BasketRegistrationPlayer::getIsDelete, NOT_DELETED));
        if (count != null && count > 0) {
            throw new ServiceException("您已通过球队报名参与该 Division，不能重复个人报名");
        }
    }

    private Long requireSeriesId(Long seasonId) {
        BasketSeason season = seasonDao.selectById(seasonId);
        if (season == null || !NOT_DELETED.equals(season.getIsDelete())) {
            throw new ServiceException("Season 不存在");
        }
        return season.getSeriesId();
    }

    private BasketPlayerProfile requirePlayerByMemberId(Long memberId) {
        BasketPlayerProfile profile = playerProfileDao.selectOne(new LambdaQueryWrapper<BasketPlayerProfile>()
                .eq(BasketPlayerProfile::getMemberId, memberId)
                .eq(BasketPlayerProfile::getIsDelete, NOT_DELETED)
                .last("limit 1"));
        if (profile == null) {
            throw new ServiceException("当前账号未关联认证球员");
        }
        return profile;
    }

    private BasketPlayerProfile requireCertifiedPlayer(Long playerId) {
        BasketPlayerProfile profile = playerProfileDao.selectById(playerId);
        if (profile == null || !NOT_DELETED.equals(profile.getIsDelete())) {
            throw new ServiceException("球员不存在");
        }
        if (!Integer.valueOf(PLAYER_CERT_APPROVED).equals(profile.getCertificationStatus())) {
            throw new ServiceException("球员尚未完成认证");
        }
        return profile;
    }

    private BasketPlayerPlatformAccount requirePlatformIdentity(Long playerId, String divisionPlatformCode) {
        List<BasketPlayerPlatformAccount> accounts = platformAccountDao.selectList(new LambdaQueryWrapper<BasketPlayerPlatformAccount>()
                .eq(BasketPlayerPlatformAccount::getPlayerId, playerId)
                .eq(BasketPlayerPlatformAccount::getIsDelete, NOT_DELETED));
        for (BasketPlayerPlatformAccount account : accounts) {
            if (Integer.valueOf(PLATFORM_ACCOUNT_ACTIVE).equals(account.getAccountStatus())
                    && GameTerminalSupport.matchesDivisionPlatform(divisionPlatformCode, account.getPlatform())) {
                return account;
            }
        }
        throw new ServiceException("球员缺少对应平台身份：" + GameTerminalSupport.formatDisplayName(divisionPlatformCode));
    }

    private void writeAudit(BasketPersonalRegistration registration,
                            PersonalAuditActionEnum action,
                            String beforeStatus,
                            String afterStatus,
                            String remark,
                            Long operatorId) {
        Date now = new Date();
        BasketPersonalRegistrationAudit audit = new BasketPersonalRegistrationAudit();
        audit.setPersonalRegistrationId(registration.getId());
        audit.setDivisionId(registration.getDivisionId());
        audit.setBeforeStatus(beforeStatus);
        audit.setAfterStatus(afterStatus);
        audit.setAuditAction(action.getCode());
        audit.setAuditRemark(remark);
        audit.setOperatorId(operatorId);
        audit.setOperateTime(now);
        audit.setCreateTime(now);
        audit.setIsDelete(NOT_DELETED);
        personalRegistrationAuditDao.insert(audit);
    }

    private String buildActiveUniqueKey(Long divisionId, Long playerId) {
        return "DIVISION:" + divisionId + ":PLAYER:" + playerId;
    }

    private String normalizePlatformCode(String platformCode) {
        return GameTerminalSupport.normalize(platformCode);
    }
}
