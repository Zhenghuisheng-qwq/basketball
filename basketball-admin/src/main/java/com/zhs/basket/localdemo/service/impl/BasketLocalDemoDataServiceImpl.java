package com.zhs.basket.localdemo.service.impl;

import com.zhs.basket.localdemo.config.BasketLocalDemoDataProperties;
import com.zhs.basket.localdemo.resp.BasketLocalDemoAvatarSyncResp;
import com.zhs.basket.localdemo.resp.BasketLocalDemoResetResp;
import com.zhs.basket.localdemo.resp.BasketLocalDemoSeedResp;
import com.zhs.basket.localdemo.resp.BasketLocalDemoStatusResp;
import com.zhs.basket.localdemo.service.IBasketLocalDemoDataService;
import com.zhs.basket.localdemo.support.LocalDemoResetSupport;
import com.zhs.basket.localdemo.support.LocalDemoSeedSupport;
import com.zhs.basket.localdemo.support.LocalDemoStatusHelper;
import com.zhs.common.exception.ServiceException;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Profile("local")
@ConditionalOnProperty(prefix = "cnpa.local-demo", name = "enabled", havingValue = "true")
public class BasketLocalDemoDataServiceImpl implements IBasketLocalDemoDataService {

    private final Environment environment;
    private final BasketLocalDemoDataProperties properties;
    private final LocalDemoStatusHelper statusHelper;
    private final LocalDemoSeedSupport seedSupport;
    private final LocalDemoResetSupport resetSupport;

    public BasketLocalDemoDataServiceImpl(Environment environment,
                                          BasketLocalDemoDataProperties properties,
                                          LocalDemoStatusHelper statusHelper,
                                          LocalDemoSeedSupport seedSupport,
                                          LocalDemoResetSupport resetSupport) {
        this.environment = environment;
        this.properties = properties;
        this.statusHelper = statusHelper;
        this.seedSupport = seedSupport;
        this.resetSupport = resetSupport;
    }

    @Override
    public BasketLocalDemoStatusResp getStatus() {
        BasketLocalDemoStatusResp resp = new BasketLocalDemoStatusResp();
        resp.setLocalProfileActive(environment.acceptsProfiles(Profiles.of("local")));
        resp.setFeatureEnabled(properties.isEnabled());
        resp.setMarker(properties.getMarker());
        statusHelper.fillCounts(resp);
        resp.setDataExists(resp.getAccountCount() > 0);
        List<String> missing = statusHelper.evaluateMissing(resp);
        resp.setMissingStages(missing);
        resp.setDataComplete(missing.isEmpty());
        resp.setCanSeed(properties.isEnabled() && (resp.getAccountCount() == 0 || resp.isDataComplete()));
        resp.setCanReset(properties.isEnabled() && resp.getAccountCount() > 0);
        if (resp.getAccountCount() > 0 && !resp.isDataComplete()) {
            resp.getWarnings().add("检测到不完整演示数据，请先 reset 后再 seed");
        }
        if (resp.getAvatarMissingCount() > 0) {
            resp.getWarnings().add("有 " + resp.getAvatarMissingCount()
                    + " 个演示账号缺少头像，可调用 POST /basket/local-demo-data/sync-avatars 补写");
        }
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BasketLocalDemoSeedResp seedLocalDemoData() {
        long started = System.currentTimeMillis();
        seedSupport.validatePasswordConfigured();

        BasketLocalDemoStatusResp status = getStatus();
        BasketLocalDemoSeedResp resp = new BasketLocalDemoSeedResp();
        resp.setMarker(properties.getMarker());
        resp.setStartedTime(started);

        if (status.isDataComplete()) {
            copyCounts(status, resp);
            BasketLocalDemoAvatarSyncResp avatarSync = seedSupport.syncDemoAvatars();
            applyAvatarSync(resp, avatarSync);
            resp.setAlreadyExists(true);
            resp.setSuccess(true);
            if (avatarSync.getUpdatedCount() > 0) {
                resp.setMessage("演示数据已存在且完整，已补写 " + avatarSync.getUpdatedCount() + " 个头像");
            } else {
                resp.setMessage("演示数据已存在且完整，未重复创建");
            }
            resp.getSkippedStages().add("ALL");
            resp.setFinishedTime(System.currentTimeMillis());
            resp.setDurationMillis(resp.getFinishedTime() - started);
            return resp;
        }
        if (status.getAccountCount() > 0) {
            resp.setSuccess(false);
            resp.setMessage("检测到不完整演示数据：" + String.join("；", status.getMissingStages()));
            resp.setValidationErrors(status.getMissingStages());
            resp.setFinishedTime(System.currentTimeMillis());
            resp.setDurationMillis(resp.getFinishedTime() - started);
            throw new ServiceException(resp.getMessage());
        }

        seedSupport.executeFullSeed();

        BasketLocalDemoStatusResp after = getStatus();
        List<String> validationErrors = statusHelper.evaluateMissing(after);
        if (!validationErrors.isEmpty()) {
            resp.setValidationErrors(validationErrors);
            throw new ServiceException("演示数据校验失败：" + String.join("；", validationErrors));
        }

        copyCounts(after, resp);
        resp.setAlreadyExists(false);
        resp.setSuccess(true);
        resp.setMessage("本地演示数据初始化成功");
        resp.getCompletedStages().add("FULL_SEED");
        resp.setFinishedTime(System.currentTimeMillis());
        resp.setDurationMillis(resp.getFinishedTime() - started);
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BasketLocalDemoAvatarSyncResp syncDemoAvatars() {
        return seedSupport.syncDemoAvatars();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public BasketLocalDemoResetResp resetLocalDemoData(String confirmMarker) {
        if (!properties.getMarker().equals(confirmMarker)) {
            throw new ServiceException("confirmMarker 不匹配，拒绝清理");
        }
        return resetSupport.reset(properties.getMarker());
    }

    private void copyCounts(BasketLocalDemoStatusResp from, BasketLocalDemoSeedResp to) {
        to.setAccountCount(from.getAccountCount());
        to.setPlayerCount(from.getPlayerCount());
        to.setCaptainCount(from.getCaptainCount());
        to.setTeamCount(from.getTeamCount());
        to.setTeamMemberCount(from.getTeamMemberCount());
        to.setTournamentCount(from.getTournamentCount());
        to.setSeasonCount(from.getSeasonCount());
        to.setRegistrationCount(from.getRegistrationCount());
        to.setRegistrationPlayerCount(from.getRegistrationPlayerCount());
        to.setStageCount(from.getStageCount());
        to.setRoundCount(from.getRoundCount());
        to.setGroupCount(from.getGroupCount());
        to.setMatchCount(from.getMatchCount());
        to.setConfirmedMatchCount(from.getConfirmedMatchCount());
        to.setScheduledMatchCount(from.getScheduledMatchCount());
        to.setLineupCount(from.getLineupCount());
        to.setPeriodScoreCount(from.getPeriodScoreCount());
        to.setTeamMatchStatCount(from.getTeamMatchStatCount());
        to.setPlayerMatchStatCount(from.getPlayerMatchStatCount());
        to.setStandingCount(from.getStandingCount());
        to.setTeamSeasonStatCount(from.getTeamSeasonStatCount());
        to.setPlayerSeasonStatCount(from.getPlayerSeasonStatCount());
        to.setAnnouncementCount(from.getAnnouncementCount());
        to.setRecommendationCount(from.getRecommendationCount());
    }

    private void applyAvatarSync(BasketLocalDemoSeedResp seedResp, BasketLocalDemoAvatarSyncResp avatarSync) {
        seedResp.setAvatarSyncUpdated(avatarSync.getUpdatedCount());
        seedResp.setAvatarSyncSkipped(avatarSync.getSkippedCount());
        seedResp.setAvatarMissingBefore(avatarSync.getMissingAvatarBefore());
        seedResp.setAvatarMissingAfter(avatarSync.getMissingAvatarAfter());
        if (!avatarSync.getWarnings().isEmpty()) {
            seedResp.getWarnings().addAll(avatarSync.getWarnings());
        }
    }
}

