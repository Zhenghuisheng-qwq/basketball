package com.zhs.basket.service.impl;

import com.zhs.basket.constant.BasketContentPerms;
import com.zhs.basket.constant.BasketEventV4Perms;
import com.zhs.basket.domain.BasketPageHero;
import com.zhs.basket.domain.v4.BasketSeason;
import com.zhs.basket.mapper.BasketPageHeroMapper;
import com.zhs.basket.mapper.v4.BasketSeasonMapper;
import com.zhs.basket.req.PageHeroSaveReq;
import com.zhs.basket.resp.PageHeroResp;
import com.zhs.basket.service.IBasketPageHeroService;
import com.zhs.basket.support.BasketPublicCacheCleaner;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import com.zhs.framework.web.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
public class BasketPageHeroServiceImpl implements IBasketPageHeroService {

    static final String EVENT_SEASON = "EVENT_SEASON";
    private static final Set<String> GENERIC_KEYS = Set.of(
            "DATA", "HALL", "ANNOUNCEMENT", "PARTNER",
            "EVENT_LEAGUE", "EVENT_ARENA", "EVENT_CUP", "EVENT_OPEN");
    private static final Set<String> MODES = Set.of("FULL_IMAGE", "BACKGROUND_TEXT");
    private static final int DRAFT = 0;
    private static final int PUBLISHED = 1;
    private static final int OFFLINE = 2;

    @Autowired private BasketPageHeroMapper pageHeroMapper;
    @Autowired private BasketSeasonMapper seasonMapper;
    @Autowired private BasketPublicCacheCleaner publicCacheCleaner;
    @Autowired private PermissionService permissionService;

    @Override
    public PageHeroResp get(String pageKey, Long seasonId) {
        String key = normalizeKey(pageKey);
        long season = normalizeSeason(key, seasonId);
        assertRead(key);
        PageHeroResp detail = pageHeroMapper.selectDetail(key, season);
        if (detail == null) {
            return empty(key, season);
        }
        fillLabels(detail);
        detail.setExists(true);
        return detail;
    }

    @Override
    public List<PageHeroResp> listGeneric() {
        assertAny(BasketContentPerms.PAGE_HERO_LIST, BasketContentPerms.PAGE_HERO_QUERY);
        List<PageHeroResp> list = pageHeroMapper.selectGenericList();
        list.forEach(item -> {
            fillLabels(item);
            item.setExists(true);
        });
        return list;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PageHeroResp save(PageHeroSaveReq req) {
        publish(req);
        Normalized input = normalize(req);
        return requireDetail(input.pageKey, input.seasonId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String publish(PageHeroSaveReq req) {
        Normalized input = normalize(req);
        assertShelf(input.pageKey);
        BasketPageHero existing = pageHeroMapper.selectForUpdate(input.pageKey, input.seasonId);
        boolean enable = input.draftEnabled == 1;
        if (enable && input.imageFileId == null) {
            throw new ServiceException("请先上传电脑端图片");
        }
        try {
            if (existing == null) {
                BasketPageHero created = newRow(input, enable ? PUBLISHED : OFFLINE);
                created.setHasDraft(0);
                created.setDraftMode(null);
                created.setDraftImageFileId(null);
                created.setDraftMobileImageFileId(null);
                created.setDraftFocusX(null);
                created.setDraftFocusY(null);
                created.setDraftMaskOpacity(null);
                created.setDraftAltText(null);
                created.setDraftEnabled(null);
                pageHeroMapper.insert(created);
            } else {
                BasketPageHero next = liveOf(existing, input, enable ? PUBLISHED : OFFLINE);
                if (pageHeroMapper.applyPublish(next) != 1) {
                    throw conflict();
                }
            }
        } catch (DuplicateKeyException ex) {
            throw new ServiceException("该页面头图已存在，请刷新后再发布");
        }
        publicCacheCleaner.clearAfterCommit();
        return enable ? "已上架" : "已下架，官网回到原来的头图";
    }

    private PageHeroResp requireDetail(String pageKey, long seasonId) {
        PageHeroResp detail = pageHeroMapper.selectDetail(pageKey, seasonId);
        if (detail == null) {
            throw new ServiceException("头图保存失败");
        }
        fillLabels(detail);
        detail.setExists(true);
        return detail;
    }

    private BasketPageHero newRow(Normalized input, int status) {
        Date now = new Date();
        BasketPageHero row = new BasketPageHero();
        row.setPageKey(input.pageKey);
        row.setSeasonId(input.seasonId);
        row.setMode(input.mode);
        row.setImageFileId(input.imageFileId);
        row.setMobileImageFileId(input.mobileImageFileId);
        row.setFocusX(input.focusX);
        row.setFocusY(input.focusY);
        row.setMaskOpacity(input.maskOpacity);
        row.setAltText(input.altText);
        row.setHeroStatus(status);
        row.setHasDraft(1);
        row.setDraftMode(input.mode);
        row.setDraftImageFileId(input.imageFileId);
        row.setDraftMobileImageFileId(input.mobileImageFileId);
        row.setDraftFocusX(input.focusX);
        row.setDraftFocusY(input.focusY);
        row.setDraftMaskOpacity(input.maskOpacity);
        row.setDraftAltText(input.altText);
        row.setDraftEnabled(input.draftEnabled);
        row.setVersionNo(0);
        row.setActiveKey(input.pageKey + "#" + input.seasonId);
        row.setUpdateBy(operator());
        row.setCreateTime(now);
        row.setUpdateTime(now);
        row.setIsDelete(0);
        return row;
    }

    private BasketPageHero draftOf(BasketPageHero existing, Normalized input) {
        BasketPageHero row = baseUpdate(existing, input);
        row.setDraftMode(input.mode);
        row.setDraftImageFileId(input.imageFileId);
        row.setDraftMobileImageFileId(input.mobileImageFileId);
        row.setDraftFocusX(input.focusX);
        row.setDraftFocusY(input.focusY);
        row.setDraftMaskOpacity(input.maskOpacity);
        row.setDraftAltText(input.altText);
        row.setDraftEnabled(input.draftEnabled);
        return row;
    }

    private BasketPageHero workingOf(BasketPageHero existing, Normalized input) {
        BasketPageHero row = draftOf(existing, input);
        row.setMode(input.mode);
        row.setImageFileId(input.imageFileId);
        row.setMobileImageFileId(input.mobileImageFileId);
        row.setFocusX(input.focusX);
        row.setFocusY(input.focusY);
        row.setMaskOpacity(input.maskOpacity);
        row.setAltText(input.altText);
        return row;
    }

    private BasketPageHero liveOf(BasketPageHero existing, Normalized input, int status) {
        BasketPageHero row = baseUpdate(existing, input);
        row.setMode(input.mode);
        row.setImageFileId(input.imageFileId);
        row.setMobileImageFileId(input.mobileImageFileId);
        row.setFocusX(input.focusX);
        row.setFocusY(input.focusY);
        row.setMaskOpacity(input.maskOpacity);
        row.setAltText(input.altText);
        row.setHeroStatus(status);
        return row;
    }

    private BasketPageHero baseUpdate(BasketPageHero existing, Normalized input) {
        if (input.versionNo == null || !Objects.equals(existing.getVersionNo(), input.versionNo)) {
            throw conflict();
        }
        BasketPageHero row = new BasketPageHero();
        row.setId(existing.getId());
        row.setVersionNo(existing.getVersionNo());
        row.setUpdateBy(operator());
        return row;
    }

    private Normalized normalize(PageHeroSaveReq req) {
        if (req == null) {
            throw new ServiceException("头图内容不能为空");
        }
        Normalized input = new Normalized();
        input.pageKey = normalizeKey(req.getPageKey());
        input.seasonId = normalizeSeason(input.pageKey, req.getSeasonId());
        input.mode = normalizeMode(req.getMode());
        input.imageFileId = req.getImageFileId();
        input.mobileImageFileId = req.getMobileImageFileId();
        input.altText = normalizeAlt(req.getAltText());
        input.focusX = clampPercent(req.getFocusX(), 50);
        input.focusY = clampPercent(req.getFocusY(), 50);
        input.maskOpacity = clampPercent(req.getMaskOpacity(), 40);
        input.draftEnabled = Objects.equals(req.getDraftEnabled(), 0) ? 0 : 1;
        input.versionNo = req.getVersionNo() == null ? 0 : req.getVersionNo();
        return input;
    }

    private String normalizeKey(String pageKey) {
        String key = pageKey == null ? "" : pageKey.trim().toUpperCase();
        if (!EVENT_SEASON.equals(key) && !GENERIC_KEYS.contains(key)) {
            throw new ServiceException("页面标识不正确");
        }
        return key;
    }

    private long normalizeSeason(String pageKey, Long seasonId) {
        long season = seasonId == null ? 0L : seasonId;
        if (EVENT_SEASON.equals(pageKey)) {
            if (season <= 0) {
                throw new ServiceException("赛事头图必须绑定赛季");
            }
            BasketSeason found = seasonMapper.selectById(season);
            if (found == null) {
                throw new ServiceException("赛季不存在");
            }
            return season;
        }
        if (season != 0) {
            throw new ServiceException("通用页头图不绑定赛季");
        }
        return 0L;
    }

    private String normalizeMode(String mode) {
        String value = StringUtils.isEmpty(mode) ? "BACKGROUND_TEXT" : mode.trim().toUpperCase();
        if (!MODES.contains(value)) {
            throw new ServiceException("头图模式不正确");
        }
        return value;
    }

    private String normalizeAlt(String altText) {
        if (altText == null || altText.isBlank()) {
            return null;
        }
        String trimmed = altText.trim();
        if (trimmed.length() > 256) {
            throw new ServiceException("替代文字不能超过256字");
        }
        return trimmed;
    }

    private int clampPercent(Integer value, int fallback) {
        if (value == null) {
            return fallback;
        }
        return Math.max(0, Math.min(100, value));
    }

    private void assertRead(String pageKey) {
        if (permissionService.hasPermi(BasketContentPerms.PAGE_HERO_QUERY)
                || permissionService.hasPermi(BasketContentPerms.PAGE_HERO_LIST)) {
            return;
        }
        if (EVENT_SEASON.equals(pageKey) && permissionService.hasPermi(BasketEventV4Perms.SEASON_QUERY)) {
            return;
        }
        throw new ServiceException("没有权限查看页面头图");
    }

    private void assertEdit(String pageKey) {
        if (permissionService.hasPermi(BasketContentPerms.PAGE_HERO_EDIT)) {
            return;
        }
        if (EVENT_SEASON.equals(pageKey) && permissionService.hasPermi(BasketEventV4Perms.SEASON_EDIT)) {
            return;
        }
        throw new ServiceException("没有权限编辑页面头图");
    }

    private void assertShelf(String pageKey) {
        if (permissionService.hasPermi(BasketContentPerms.PAGE_HERO_EDIT)
                || permissionService.hasPermi(BasketContentPerms.PAGE_HERO_PUBLISH)) {
            return;
        }
        if (EVENT_SEASON.equals(pageKey) && (permissionService.hasPermi(BasketEventV4Perms.SEASON_EDIT)
                || permissionService.hasPermi(BasketEventV4Perms.SEASON_PUBLISH))) {
            return;
        }
        throw new ServiceException("没有权限上架页面头图");
    }

    private void assertPublish(String pageKey) {
        if (permissionService.hasPermi(BasketContentPerms.PAGE_HERO_PUBLISH)) {
            return;
        }
        if (EVENT_SEASON.equals(pageKey) && permissionService.hasPermi(BasketEventV4Perms.SEASON_PUBLISH)) {
            return;
        }
        throw new ServiceException("没有权限发布页面头图");
    }

    private void assertAny(String... perms) {
        for (String perm : perms) {
            if (permissionService.hasPermi(perm)) {
                return;
            }
        }
        throw new ServiceException("没有权限查看页面头图");
    }

    private PageHeroResp empty(String pageKey, long seasonId) {
        PageHeroResp resp = new PageHeroResp();
        resp.setPageKey(pageKey);
        resp.setSeasonId(seasonId);
        resp.setMode("BACKGROUND_TEXT");
        resp.setFocusX(50);
        resp.setFocusY(50);
        resp.setMaskOpacity(40);
        resp.setDraftEnabled(1);
        resp.setHasDraft(0);
        resp.setVersionNo(0);
        resp.setExists(false);
        resp.setPendingChangeLabel("未配置");
        if (seasonId > 0) {
            BasketSeason season = seasonMapper.selectById(seasonId);
            if (season != null) {
                resp.setSeasonName(StringUtils.isEmpty(season.getDisplayTitle())
                        ? season.getSeasonName() : season.getDisplayTitle());
            }
        }
        return resp;
    }

    private void fillLabels(PageHeroResp resp) {
        if (resp.getHeroStatus() == null) {
            resp.setPendingChangeLabel("未配置");
            return;
        }
        resp.setHeroStatusName(switch (resp.getHeroStatus()) {
            case PUBLISHED -> "已发布";
            case OFFLINE -> "已停用";
            default -> "未发布";
        });
        if (Objects.equals(resp.getHeroStatus(), DRAFT)) {
            resp.setPendingChangeLabel("未发布");
            return;
        }
        if (Objects.equals(resp.getHasDraft(), 1) && Objects.equals(resp.getDraftEnabled(), 0)) {
            resp.setPendingChangeLabel("发布后停用");
            return;
        }
        if (Objects.equals(resp.getHasDraft(), 1)) {
            resp.setPendingChangeLabel("有未发布修改");
            return;
        }
        resp.setPendingChangeLabel(Objects.equals(resp.getHeroStatus(), OFFLINE) ? "已停用" : null);
    }

    private ServiceException conflict() {
        return new ServiceException("头图已被其他人修改，请刷新后再保存");
    }

    private String operator() {
        try {
            String name = SecurityUtils.getUsername();
            return name == null ? "" : name;
        } catch (Exception ex) {
            return "";
        }
    }

    private static final class Normalized {
        private String pageKey;
        private long seasonId;
        private String mode;
        private Long imageFileId;
        private Long mobileImageFileId;
        private String altText;
        private int focusX;
        private int focusY;
        private int maskOpacity;
        private int draftEnabled;
        private Integer versionNo;
    }
}
