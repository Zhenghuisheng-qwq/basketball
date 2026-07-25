package com.zhs.service.impl;

import cn.hutool.core.util.IdUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhs.common.UserContext;
import com.zhs.dao.BasketDisciplineRecordDao;
import com.zhs.dao.BasketPlayerPlatformAccountDao;
import com.zhs.dao.BasketPlayerProfileDao;
import com.zhs.dao.BasketTournamentDao;
import com.zhs.dao.BasketTournamentOpenRegistrationDao;
import com.zhs.dao.BasketTournamentSeasonDao;
import com.zhs.enums.DisciplineStatusEnum;
import com.zhs.enums.OpenRegistrationStatusEnum;
import com.zhs.enums.PlatformAccountStatusEnum;
import com.zhs.enums.PlayerCertStatusEnum;
import com.zhs.enums.PlayerStatusEnum;
import com.zhs.enums.TournamentSeasonStatusEnum;
import com.zhs.enums.TournamentStatusEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketDisciplineRecord;
import com.zhs.model.BasketPlayerPlatformAccount;
import com.zhs.model.BasketPlayerProfile;
import com.zhs.model.BasketTournament;
import com.zhs.model.BasketTournamentOpenRegistration;
import com.zhs.model.BasketTournamentSeason;
import com.zhs.request.participation.OpenRegistrationDraftReq;
import com.zhs.request.participation.OpenRegistrationWithdrawReq;
import com.zhs.response.participation.OpenAvailableSeasonItemResp;
import com.zhs.response.participation.OpenRegistrationDetailResp;
import com.zhs.response.participation.OpenRegistrationItemResp;
import com.zhs.service.IBusinessMessageService;
import com.zhs.service.IOpenRegistrationBusinessService;
import jakarta.annotation.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class OpenRegistrationBusinessServiceImpl implements IOpenRegistrationBusinessService {

    private static final Integer NOT_DELETED = 0;
    private static final int ENABLED = 1;
    private static final String BIZ_TYPE_OPEN_REGISTRATION = "OPEN_REGISTRATION";
    private static final List<String> STANDARD_PLATFORMS = List.of("PC", "PS", "XBOX");

    @Resource
    private BasketTournamentSeasonDao seasonDao;
    @Resource
    private BasketTournamentDao tournamentDao;
    @Resource
    private BasketTournamentOpenRegistrationDao openRegistrationDao;
    @Resource
    private BasketPlayerProfileDao playerProfileDao;
    @Resource
    private BasketPlayerPlatformAccountDao platformAccountDao;
    @Resource
    private BasketDisciplineRecordDao disciplineRecordDao;
    @Resource
    private IBusinessMessageService businessMessageService;

    @Override
    public IPage<OpenAvailableSeasonItemResp> pageAvailableOpenSeasons(Integer currentPage, Integer pageSize) {
        Long memberId = UserContext.getRequiredUserId();
        requireApprovedPlayer(memberId);
        Date now = new Date();

        List<BasketTournamentSeason> seasons = seasonDao.selectList(new LambdaQueryWrapper<BasketTournamentSeason>()
                .eq(BasketTournamentSeason::getIsDelete, NOT_DELETED)
                .eq(BasketTournamentSeason::getSeasonStatus, TournamentSeasonStatusEnum.REGISTRATION_OPEN.getCode())
                .eq(BasketTournamentSeason::getAllowOpenRegistration, ENABLED)
                .le(BasketTournamentSeason::getRegistrationStartTime, now)
                .ge(BasketTournamentSeason::getRegistrationEndTime, now)
                .orderByDesc(BasketTournamentSeason::getRegistrationStartTime));

        Map<Long, BasketTournament> tournamentMap = loadTournamentMap(seasons);
        BasketPlayerProfile profile = loadPlayerByMember(memberId);
        Map<Long, BasketTournamentOpenRegistration> existingMap = loadExistingOpenRegistrationMap(profile.getId(), seasons);

        List<OpenAvailableSeasonItemResp> items = new ArrayList<>();
        for (BasketTournamentSeason season : seasons) {
            BasketTournament tournament = tournamentMap.get(season.getTournamentId());
            if (tournament == null || !Objects.equals(TournamentStatusEnum.ACTIVE.getCode(), tournament.getTournamentStatus())) {
                continue;
            }
            OpenAvailableSeasonItemResp resp = new OpenAvailableSeasonItemResp();
            resp.setSeasonId(toStringId(season.getId()));
            resp.setSeasonName(season.getSeasonName());
            resp.setTournamentId(toStringId(season.getTournamentId()));
            resp.setTournamentName(tournament.getTournamentName());
            resp.setRegistrationStartTime(season.getRegistrationStartTime());
            resp.setRegistrationEndTime(season.getRegistrationEndTime());
            BasketTournamentOpenRegistration existing = existingMap.get(season.getId());
            resp.setHasRegistration(existing != null);
            resp.setRegistrationId(existing == null ? null : toStringId(existing.getId()));
            items.add(resp);
        }
        return toPage(items, currentPage, pageSize);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OpenRegistrationDetailResp createDraft(OpenRegistrationDraftReq req) {
        if (req == null || !StringUtils.hasText(req.getSeasonId())) {
            throw new ServiceException("赛季主键不能为空");
        }
        Long memberId = UserContext.getRequiredUserId();
        BasketPlayerProfile profile = requireApprovedPlayer(memberId);
        assertPlayerNotRestricted(profile);
        Long seasonPk = parseLongId(req.getSeasonId(), "赛季主键");
        BasketTournamentSeason season = requireOpenRegistrationSeason(seasonPk);
        assertNoDuplicateOpenRegistration(seasonPk, profile.getId());

        String platform = requirePlatform(req.getPlatform());
        BasketPlayerPlatformAccount platformAccount = requireActivePlatformAccount(profile.getId(), platform);

        BasketTournamentOpenRegistration registration = new BasketTournamentOpenRegistration();
        registration.setRegistrationNo("OR" + IdUtil.getSnowflakeNextIdStr());
        registration.setSeasonId(seasonPk);
        registration.setMemberId(memberId);
        registration.setPlayerId(profile.getId());
        registration.setPlatform(platform);
        registration.setDisplayGameId(platformAccount.getDisplayGameId());
        registration.setNormalizedGameId(platformAccount.getNormalizedGameId());
        registration.setPositionCode(trimToNull(req.getPositionCode()));
        registration.setRegistrationStatus(OpenRegistrationStatusEnum.DRAFT.getCode());
        registration.setRemark(trimToNull(req.getRemark()));
        registration.setIsDelete(NOT_DELETED);
        openRegistrationDao.insert(registration);
        return buildDetailResp(registration, season, profile);
    }

    @Override
    public OpenRegistrationDetailResp getDetail(String registrationId) {
        Long registrationPk = parseLongId(registrationId, "报名主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketTournamentOpenRegistration registration = requireOwnedRegistration(memberId, registrationPk);
        BasketTournamentSeason season = seasonDao.selectById(registration.getSeasonId());
        BasketPlayerProfile profile = playerProfileDao.selectById(registration.getPlayerId());
        return buildDetailResp(registration, season, profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OpenRegistrationDetailResp submit(String registrationId) {
        Long registrationPk = parseLongId(registrationId, "报名主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketTournamentOpenRegistration registration = requireOwnedRegistration(memberId, registrationPk);
        OpenRegistrationStatusEnum status = OpenRegistrationStatusEnum.fromCode(registration.getRegistrationStatus());
        if (!status.canSubmit()) {
            throw new ServiceException("当前状态不可提交");
        }

        BasketPlayerProfile profile = requireApprovedPlayer(memberId);
        assertPlayerNotRestricted(profile);
        BasketTournamentSeason season = requireOpenRegistrationSeason(registration.getSeasonId());
        requireActivePlatformAccount(profile.getId(), registration.getPlatform());
        assertNoActiveDiscipline(profile, registration.getSeasonId());

        Date now = new Date();
        int updated = openRegistrationDao.update(null, new LambdaUpdateWrapper<BasketTournamentOpenRegistration>()
                .eq(BasketTournamentOpenRegistration::getId, registrationPk)
                .eq(BasketTournamentOpenRegistration::getMemberId, memberId)
                .eq(BasketTournamentOpenRegistration::getIsDelete, NOT_DELETED)
                .in(BasketTournamentOpenRegistration::getRegistrationStatus,
                        OpenRegistrationStatusEnum.DRAFT.getCode(),
                        OpenRegistrationStatusEnum.REJECTED.getCode())
                .set(BasketTournamentOpenRegistration::getRegistrationStatus, OpenRegistrationStatusEnum.SUBMITTED.getCode())
                .set(BasketTournamentOpenRegistration::getSubmittedTime, now));
        if (updated == 0) {
            throw new ServiceException("提交失败，报名状态已变更");
        }
        registration = openRegistrationDao.selectById(registrationPk);

        businessMessageService.sendSiteMessage(memberId, "Open 报名已提交",
                "您的 Open 个人报名已提交，请等待审核", BIZ_TYPE_OPEN_REGISTRATION, registration.getId(),
                "OPEN_REGISTRATION", String.valueOf(registration.getId()));

        return buildDetailResp(registration, season, profile);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OpenRegistrationDetailResp withdraw(String registrationId, OpenRegistrationWithdrawReq req) {
        Long registrationPk = parseLongId(registrationId, "报名主键");
        Long memberId = UserContext.getRequiredUserId();
        BasketTournamentOpenRegistration registration = requireOwnedRegistration(memberId, registrationPk);
        if (!OpenRegistrationStatusEnum.fromCode(registration.getRegistrationStatus()).canWithdraw()) {
            throw new ServiceException("当前状态不可撤回");
        }

        BasketTournamentSeason season = seasonDao.selectById(registration.getSeasonId());
        Date now = new Date();
        if (season != null && season.getRegistrationEndTime() != null && season.getRegistrationEndTime().before(now)) {
            throw new ServiceException("已过报名截止时间，无法撤回");
        }

        int updated = openRegistrationDao.update(null, new LambdaUpdateWrapper<BasketTournamentOpenRegistration>()
                .eq(BasketTournamentOpenRegistration::getId, registrationPk)
                .eq(BasketTournamentOpenRegistration::getMemberId, memberId)
                .eq(BasketTournamentOpenRegistration::getRegistrationStatus, OpenRegistrationStatusEnum.SUBMITTED.getCode())
                .eq(BasketTournamentOpenRegistration::getIsDelete, NOT_DELETED)
                .set(BasketTournamentOpenRegistration::getRegistrationStatus, OpenRegistrationStatusEnum.WITHDRAWN.getCode())
                .set(BasketTournamentOpenRegistration::getWithdrawalTime, now)
                .set(BasketTournamentOpenRegistration::getWithdrawalReason, req == null ? null : trimToNull(req.getWithdrawalReason())));
        if (updated == 0) {
            throw new ServiceException("撤回失败，报名状态已变更");
        }
        registration = openRegistrationDao.selectById(registrationPk);
        BasketPlayerProfile profile = playerProfileDao.selectById(registration.getPlayerId());
        return buildDetailResp(registration, season, profile);
    }

    @Override
    public IPage<OpenRegistrationItemResp> pageMyRegistrations(Integer currentPage, Integer pageSize, Integer status) {
        Long memberId = UserContext.getRequiredUserId();
        LambdaQueryWrapper<BasketTournamentOpenRegistration> wrapper = new LambdaQueryWrapper<BasketTournamentOpenRegistration>()
                .eq(BasketTournamentOpenRegistration::getMemberId, memberId)
                .eq(BasketTournamentOpenRegistration::getIsDelete, NOT_DELETED)
                .orderByDesc(BasketTournamentOpenRegistration::getCreateTime);
        if (status != null) {
            wrapper.eq(BasketTournamentOpenRegistration::getRegistrationStatus, status);
        }

        Page<BasketTournamentOpenRegistration> page = new Page<>(normalizePage(currentPage), normalizeSize(pageSize));
        IPage<BasketTournamentOpenRegistration> regPage = openRegistrationDao.selectPage(page, wrapper);
        Map<Long, BasketTournamentSeason> seasonMap = loadSeasonMap(regPage.getRecords());
        Map<Long, BasketTournament> tournamentMap = loadTournamentMap(seasonMap.values());

        Page<OpenRegistrationItemResp> respPage = new Page<>(regPage.getCurrent(), regPage.getSize(), regPage.getTotal());
        List<OpenRegistrationItemResp> items = new ArrayList<>();
        for (BasketTournamentOpenRegistration registration : regPage.getRecords()) {
            items.add(toItemResp(registration, seasonMap.get(registration.getSeasonId()),
                    tournamentMap.get(seasonMap.get(registration.getSeasonId()) == null
                            ? null : seasonMap.get(registration.getSeasonId()).getTournamentId())));
        }
        respPage.setRecords(items);
        return respPage;
    }

    /**
     * 校验同一球员在同一赛季不存在 Open 报名记录（uk_open_season_player）。
     */
    void assertNoDuplicateOpenRegistration(Long seasonId, Long playerId) {
        Long count = openRegistrationDao.selectCount(new LambdaQueryWrapper<BasketTournamentOpenRegistration>()
                .eq(BasketTournamentOpenRegistration::getSeasonId, seasonId)
                .eq(BasketTournamentOpenRegistration::getPlayerId, playerId)
                .eq(BasketTournamentOpenRegistration::getIsDelete, NOT_DELETED));
        if (count != null && count > 0) {
            throw new ServiceException("该赛季已存在 Open 报名记录");
        }
    }

    private BasketTournamentSeason requireOpenRegistrationSeason(Long seasonId) {
        BasketTournamentSeason season = seasonDao.selectById(seasonId);
        if (season == null || !NOT_DELETED.equals(season.getIsDelete())) {
            throw new ServiceException("赛季不存在");
        }
        Date now = new Date();
        if (!Objects.equals(TournamentSeasonStatusEnum.REGISTRATION_OPEN.getCode(), season.getSeasonStatus())) {
            throw new ServiceException("赛季未开放报名");
        }
        if (!Integer.valueOf(ENABLED).equals(season.getAllowOpenRegistration())) {
            throw new ServiceException("该赛季未开放 Open 报名");
        }
        if (season.getRegistrationStartTime() != null && season.getRegistrationStartTime().after(now)) {
            throw new ServiceException("报名尚未开始");
        }
        if (season.getRegistrationEndTime() != null && season.getRegistrationEndTime().before(now)) {
            throw new ServiceException("报名已截止");
        }
        return season;
    }

    private BasketPlayerProfile requireApprovedPlayer(Long memberId) {
        BasketPlayerProfile profile = loadPlayerByMember(memberId);
        if (!Integer.valueOf(PlayerCertStatusEnum.APPROVED.getCode()).equals(profile.getCertificationStatus())) {
            throw new ServiceException("球员认证尚未通过，无法报名");
        }
        return profile;
    }

    private BasketPlayerProfile loadPlayerByMember(Long memberId) {
        BasketPlayerProfile profile = playerProfileDao.selectOne(new LambdaQueryWrapper<BasketPlayerProfile>()
                .eq(BasketPlayerProfile::getMemberId, memberId)
                .eq(BasketPlayerProfile::getIsDelete, NOT_DELETED)
                .last("limit 1"));
        if (profile == null) {
            throw new ServiceException("尚未完成球员认证");
        }
        return profile;
    }

    private void assertPlayerNotRestricted(BasketPlayerProfile profile) {
        if (PlayerStatusEnum.fromCode(profile.getPlayerStatus()).isRestricted()) {
            throw new ServiceException("您的参赛资格受限，无法报名");
        }
    }

    private BasketPlayerPlatformAccount requireActivePlatformAccount(Long playerId, String platform) {
        BasketPlayerPlatformAccount account = platformAccountDao.selectOne(new LambdaQueryWrapper<BasketPlayerPlatformAccount>()
                .eq(BasketPlayerPlatformAccount::getPlayerId, playerId)
                .eq(BasketPlayerPlatformAccount::getPlatform, platform)
                .eq(BasketPlayerPlatformAccount::getIsDelete, NOT_DELETED)
                .eq(BasketPlayerPlatformAccount::getAccountStatus, PlatformAccountStatusEnum.ACTIVE.getCode())
                .last("limit 1"));
        if (account == null) {
            throw new ServiceException("所选平台游戏 ID 未生效，请先完成平台绑定");
        }
        return account;
    }

    private void assertNoActiveDiscipline(BasketPlayerProfile profile, Long seasonId) {
        Date now = new Date();
        List<BasketDisciplineRecord> records = disciplineRecordDao.selectList(new LambdaQueryWrapper<BasketDisciplineRecord>()
                .eq(BasketDisciplineRecord::getSeasonId, seasonId)
                .eq(BasketDisciplineRecord::getDisciplineStatus, DisciplineStatusEnum.ACTIVE.getCode())
                .eq(BasketDisciplineRecord::getIsDelete, NOT_DELETED)
                .and(w -> w.eq(BasketDisciplineRecord::getMemberId, profile.getMemberId())
                        .or().eq(BasketDisciplineRecord::getPlayerId, profile.getId())));
        for (BasketDisciplineRecord record : records) {
            if (isDisciplineEffective(record, now)) {
                throw new ServiceException("当前处于处罚期，无法提交报名");
            }
        }
    }

    private boolean isDisciplineEffective(BasketDisciplineRecord record, Date now) {
        if (record.getEffectiveStartTime() != null && record.getEffectiveStartTime().after(now)) {
            return false;
        }
        if (record.getEffectiveEndTime() != null && record.getEffectiveEndTime().before(now)) {
            return false;
        }
        return true;
    }

    private BasketTournamentOpenRegistration requireOwnedRegistration(Long memberId, Long registrationPk) {
        BasketTournamentOpenRegistration registration = openRegistrationDao.selectById(registrationPk);
        if (registration == null || !NOT_DELETED.equals(registration.getIsDelete())) {
            throw new ServiceException("报名不存在");
        }
        if (!memberId.equals(registration.getMemberId())) {
            throw new ServiceException("无权查看该报名", 403);
        }
        return registration;
    }

    private OpenRegistrationDetailResp buildDetailResp(BasketTournamentOpenRegistration registration,
                                                       BasketTournamentSeason season,
                                                       BasketPlayerProfile profile) {
        OpenRegistrationStatusEnum status = OpenRegistrationStatusEnum.fromCode(registration.getRegistrationStatus());
        OpenRegistrationDetailResp resp = new OpenRegistrationDetailResp();
        resp.setRegistrationId(toStringId(registration.getId()));
        resp.setRegistrationNo(registration.getRegistrationNo());
        resp.setSeasonId(toStringId(registration.getSeasonId()));
        resp.setSeasonName(season == null ? null : season.getSeasonName());
        resp.setPlayerId(toStringId(registration.getPlayerId()));
        resp.setPlayerNo(profile == null ? null : profile.getPlayerNo());
        resp.setPlayerNickname(profile == null ? null : profile.getGameNickname());
        resp.setPlatform(registration.getPlatform());
        resp.setDisplayGameId(registration.getDisplayGameId());
        resp.setPositionCode(registration.getPositionCode());
        resp.setRegistrationStatus(registration.getRegistrationStatus());
        resp.setRegistrationStatusText(status.getDescription());
        resp.setRemark(registration.getRemark());
        resp.setCanEdit(status.canEdit());
        resp.setCanSubmit(status.canSubmit());
        resp.setCanWithdraw(status.canWithdraw());
        resp.setSubmittedTime(registration.getSubmittedTime());
        resp.setRegistrationEndTime(season == null ? null : season.getRegistrationEndTime());
        if (season != null) {
            BasketTournament tournament = tournamentDao.selectById(season.getTournamentId());
            resp.setTournamentName(tournament == null ? null : tournament.getTournamentName());
        }
        return resp;
    }

    private OpenRegistrationItemResp toItemResp(BasketTournamentOpenRegistration registration,
                                              BasketTournamentSeason season,
                                              BasketTournament tournament) {
        OpenRegistrationStatusEnum status = OpenRegistrationStatusEnum.fromCode(registration.getRegistrationStatus());
        OpenRegistrationItemResp resp = new OpenRegistrationItemResp();
        resp.setRegistrationId(toStringId(registration.getId()));
        resp.setRegistrationNo(registration.getRegistrationNo());
        resp.setSeasonId(toStringId(registration.getSeasonId()));
        resp.setSeasonName(season == null ? null : season.getSeasonName());
        resp.setTournamentName(tournament == null ? null : tournament.getTournamentName());
        resp.setPlatform(registration.getPlatform());
        resp.setDisplayGameId(registration.getDisplayGameId());
        resp.setRegistrationStatus(registration.getRegistrationStatus());
        resp.setRegistrationStatusText(status.getDescription());
        resp.setCanEdit(status.canEdit());
        resp.setCanSubmit(status.canSubmit());
        resp.setCanWithdraw(status.canWithdraw());
        resp.setSubmittedTime(registration.getSubmittedTime());
        resp.setCreateTime(registration.getCreateTime());
        return resp;
    }

    private Map<Long, BasketTournamentOpenRegistration> loadExistingOpenRegistrationMap(Long playerId,
                                                                                        List<BasketTournamentSeason> seasons) {
        if (CollectionUtils.isEmpty(seasons)) {
            return Collections.emptyMap();
        }
        Set<Long> seasonIds = seasons.stream().map(BasketTournamentSeason::getId).collect(Collectors.toSet());
        Map<Long, BasketTournamentOpenRegistration> map = new HashMap<>();
        for (BasketTournamentOpenRegistration registration : openRegistrationDao.selectList(
                new LambdaQueryWrapper<BasketTournamentOpenRegistration>()
                        .eq(BasketTournamentOpenRegistration::getPlayerId, playerId)
                        .in(BasketTournamentOpenRegistration::getSeasonId, seasonIds)
                        .eq(BasketTournamentOpenRegistration::getIsDelete, NOT_DELETED))) {
            map.putIfAbsent(registration.getSeasonId(), registration);
        }
        return map;
    }

    private Map<Long, BasketTournamentSeason> loadSeasonMap(List<BasketTournamentOpenRegistration> registrations) {
        Set<Long> seasonIds = registrations.stream().map(BasketTournamentOpenRegistration::getSeasonId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (seasonIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, BasketTournamentSeason> map = new HashMap<>();
        for (BasketTournamentSeason season : seasonDao.selectBatchIds(seasonIds)) {
            if (season != null && NOT_DELETED.equals(season.getIsDelete())) {
                map.put(season.getId(), season);
            }
        }
        return map;
    }

    private Map<Long, BasketTournament> loadTournamentMap(java.util.Collection<BasketTournamentSeason> seasons) {
        Set<Long> tournamentIds = seasons.stream().map(BasketTournamentSeason::getTournamentId)
                .filter(Objects::nonNull).collect(Collectors.toSet());
        if (tournamentIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, BasketTournament> map = new HashMap<>();
        for (BasketTournament tournament : tournamentDao.selectBatchIds(tournamentIds)) {
            if (tournament != null && NOT_DELETED.equals(tournament.getIsDelete())) {
                map.put(tournament.getId(), tournament);
            }
        }
        return map;
    }

    private String requirePlatform(String platform) {
        if (!StringUtils.hasText(platform)) {
            throw new ServiceException("请选择游戏平台");
        }
        String normalized = platform.trim().toUpperCase();
        if ("PS5".equals(normalized) || "PSN".equals(normalized)) {
            normalized = "PS";
        }
        if (!STANDARD_PLATFORMS.contains(normalized)) {
            throw new ServiceException("不支持的游戏平台");
        }
        return normalized;
    }

    private <T> IPage<T> toPage(List<T> items, Integer currentPage, Integer pageSize) {
        long page = normalizePage(currentPage);
        long size = normalizeSize(pageSize);
        long total = items.size();
        int from = (int) Math.min((page - 1) * size, total);
        int to = (int) Math.min(from + size, total);
        List<T> records = from >= to ? Collections.emptyList() : items.subList(from, to);
        Page<T> respPage = new Page<>(page, size, total);
        respPage.setRecords(records);
        return respPage;
    }

    private long normalizePage(Integer currentPage) {
        return currentPage == null || currentPage < 1 ? 1L : currentPage;
    }

    private long normalizeSize(Integer pageSize) {
        if (pageSize == null || pageSize < 1) {
            return 10L;
        }
        return Math.min(pageSize, 100L);
    }

    private Long parseLongId(String id, String fieldName) {
        if (!StringUtils.hasText(id)) {
            throw new ServiceException(fieldName + "不能为空");
        }
        try {
            return Long.parseLong(id.trim());
        } catch (NumberFormatException ex) {
            throw new ServiceException(fieldName + "格式不正确");
        }
    }

    private String toStringId(Long id) {
        return id == null ? null : String.valueOf(id);
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
