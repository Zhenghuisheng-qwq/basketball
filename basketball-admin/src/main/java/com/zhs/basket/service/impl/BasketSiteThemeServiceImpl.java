package com.zhs.basket.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhs.basket.constant.BasketContentPerms;
import com.zhs.basket.domain.BasketSiteTheme;
import com.zhs.basket.domain.BasketSiteThemeBinding;
import com.zhs.basket.domain.v4.BasketSeason;
import com.zhs.basket.mapper.BasketSiteThemeMapper;
import com.zhs.basket.mapper.v4.BasketSeasonMapper;
import com.zhs.basket.req.SiteThemePublishReq;
import com.zhs.basket.req.SiteThemeSaveReq;
import com.zhs.basket.req.ThemeColorReq;
import com.zhs.basket.resp.SiteThemeBindingResp;
import com.zhs.basket.resp.SiteThemeImpactResp;
import com.zhs.basket.resp.SiteThemeResp;
import com.zhs.basket.service.IBasketSiteThemeService;
import com.zhs.basket.support.BasketPublicCacheCleaner;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.SecurityUtils;
import com.zhs.common.utils.StringUtils;
import com.zhs.framework.web.service.PermissionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;

@Service
public class BasketSiteThemeServiceImpl implements IBasketSiteThemeService {

    private static final Set<String> SCOPES = Set.of("HOME", "SEASON", "BASE", "GLOBAL");
    private static final Set<String> DISPLAYS = Set.of("TOP_CENTER", "SCROLL", "FIXED");
    private static final Pattern COLOR = Pattern.compile("^#[0-9a-fA-F]{6}$");
    private static final int DRAFT = 0;
    private static final int PUBLISHED = 1;
    private static final int OFFLINE = 2;
    private static final ObjectMapper JSON = new ObjectMapper();

    @Autowired private BasketSiteThemeMapper themeMapper;
    @Autowired private BasketSeasonMapper seasonMapper;
    @Autowired private BasketPublicCacheCleaner publicCacheCleaner;
    @Autowired private PermissionService permissionService;

    @Override
    public List<SiteThemeResp> list() {
        assertAny(BasketContentPerms.SITE_THEME_LIST, BasketContentPerms.SITE_THEME_QUERY);
        Map<Long, List<SiteThemeBindingResp>> grouped = groupBindings(themeMapper.selectLiveBindings());
        List<SiteThemeResp> result = new ArrayList<>();
        for (BasketSiteTheme theme : themeMapper.selectAll()) {
            SiteThemeResp resp = toResp(theme, false);
            resp.setBindings(grouped.getOrDefault(theme.getId(), List.of()));
            result.add(resp);
        }
        return result;
    }

    @Override
    public SiteThemeResp get(Long id) {
        assertAny(BasketContentPerms.SITE_THEME_QUERY, BasketContentPerms.SITE_THEME_LIST, BasketContentPerms.SITE_THEME_EDIT);
        BasketSiteTheme theme = require(id);
        SiteThemeResp resp = toResp(theme, true);
        resp.setBindings(bindingsOf(id));
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public SiteThemeResp save(SiteThemeSaveReq req) {
        assertEdit();
        Normalized input = normalize(req);
        if (req.getId() == null) {
            BasketSiteTheme created = newRow(input, DRAFT);
            themeMapper.insert(created);
            return get(created.getId());
        }
        BasketSiteTheme existing = themeMapper.selectForUpdate(req.getId());
        if (existing == null) {
            throw new ServiceException("主题不存在");
        }
        assertVersion(existing, req.getVersionNo());
        BasketSiteTheme row = new BasketSiteTheme();
        row.setId(existing.getId());
        row.setVersionNo(existing.getVersionNo());
        row.setUpdateBy(operator());
        row.setDraftJson(input.draftJson);
        int changed;
        if (Objects.equals(existing.getThemeStatus(), PUBLISHED)) {
            changed = themeMapper.updateDraft(row);
        } else {
            fillLive(row, input);
            row.setColorsJson(input.colorsJson);
            changed = themeMapper.updateWorking(row);
        }
        if (changed != 1) {
            throw conflict();
        }
        return get(existing.getId());
    }

    @Override
    public SiteThemeImpactResp impact(Long themeId, String scopeType, Long seasonId) {
        assertAny(BasketContentPerms.SITE_THEME_QUERY, BasketContentPerms.SITE_THEME_PUBLISH);
        BasketSiteTheme theme = require(themeId);
        String scope = normalizeScope(scopeType);
        SiteThemeImpactResp resp = new SiteThemeImpactResp();
        if ("GLOBAL".equals(scope)) {
            resp.getAffected().add("首页");
            resp.getAffected().add("数据中心、名人堂、公告、合作伙伴，以及没有单独主题的赛事页");
            resp.setReplaceHint("全站会同时换掉首页和基础主题。某一届赛事如果单独发过主题，仍用那一届自己的。球队页和球员页不跟某一届赛事走。");
            return resp;
        }
        long scopeValue = normalizeScopeId(scope, seasonId);
        for (SiteThemeBindingResp binding : bindingsOf(theme.getId())) {
            resp.getAffected().add(binding.getScopeLabel());
        }
        BasketSiteThemeBinding current = themeMapper.selectBinding(scope, scopeValue);
        if (current != null && !Objects.equals(current.getThemeId(), theme.getId())) {
            resp.setReplaceHint(scopeLabel(current) + " 当前使用「" + displayName(current.getThemeName()) + "」，发布后会换成「" + theme.getThemeName() + "」");
        } else if (current == null) {
            resp.getAffected().add(scopeText(scope, scopeValue));
        }
        if (resp.getAffected().isEmpty()) {
            resp.getAffected().add(scopeText(scope, scopeValue));
        }
        return resp;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String publish(SiteThemePublishReq req) {
        assertPublish();
        if (req == null || req.getId() == null) {
            throw new ServiceException("请先保存主题");
        }
        Normalized input = normalize(req);
        String scope = normalizeScope(req.getScopeType());
        long scopeValue = normalizeScopeId(scope, req.getSeasonId());
        BasketSiteTheme existing = themeMapper.selectForUpdate(req.getId());
        if (existing == null) {
            throw new ServiceException("主题不存在");
        }
        assertVersion(existing, req.getVersionNo());
        int nextVersion = existing.getVersionNo() + 1;
        BasketSiteTheme row = new BasketSiteTheme();
        row.setId(existing.getId());
        row.setVersionNo(existing.getVersionNo());
        row.setUpdateBy(operator());
        fillLive(row, input);
        row.setColorsJson(input.colorsJson);
        if (themeMapper.applyPublish(row) != 1) {
            throw conflict();
        }
        if ("GLOBAL".equals(scope)) {
            upsertBinding("HOME", 0L, existing.getId(), nextVersion);
            upsertBinding("BASE", 0L, existing.getId(), nextVersion);
        } else {
            upsertBinding(scope, scopeValue, existing.getId(), nextVersion);
        }
        themeMapper.touchBindings(existing.getId(), nextVersion, operator());
        publicCacheCleaner.clearAfterCommit();
        return "GLOBAL".equals(scope) ? "主题已发布到全站" : "主题已发布";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long copy(Long id) {
        assertEdit();
        BasketSiteTheme source = require(id);
        Normalized input = fromStored(source, true);
        String name = source.getThemeName();
        input.themeName = name.length() > 60 ? name.substring(0, 60) + " 副本" : name + " 副本";
        input.colorsJson = writeColors(input.colors);
        input.draftJson = writeDraft(input);
        BasketSiteTheme created = newRow(input, DRAFT);
        themeMapper.insert(created);
        return created.getId();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void offline(Long id, Integer versionNo) {
        assertPublish();
        BasketSiteTheme existing = themeMapper.selectForUpdate(id);
        if (existing == null) {
            throw new ServiceException("主题不存在");
        }
        assertVersion(existing, versionNo);
        if (themeMapper.updateStatus(id, OFFLINE, existing.getVersionNo(), operator()) != 1) {
            throw conflict();
        }
        publicCacheCleaner.clearAfterCommit();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(Long id) {
        assertEdit();
        if (themeMapper.softDelete(id, operator()) != 1) {
            throw new ServiceException("主题不存在");
        }
        themeMapper.softDeleteBindingsByTheme(id, operator());
        publicCacheCleaner.clearAfterCommit();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void unbind(String scopeType, Long seasonId) {
        assertPublish();
        String scope = normalizeScope(scopeType);
        long scopeValue = normalizeScopeId(scope, seasonId);
        BasketSiteThemeBinding binding = themeMapper.selectBindingForUpdate(scope, scopeValue);
        if (binding == null) {
            throw new ServiceException("这个位置没有主题绑定");
        }
        if (themeMapper.softDeleteBinding(binding.getId(), operator()) != 1) {
            throw new ServiceException("解除绑定失败");
        }
        publicCacheCleaner.clearAfterCommit();
    }

    private void upsertBinding(String scope, long scopeValue, Long themeId, int version) {
        BasketSiteThemeBinding existing = themeMapper.selectBindingForUpdate(scope, scopeValue);
        String user = operator();
        if (existing == null) {
            BasketSiteThemeBinding created = new BasketSiteThemeBinding();
            created.setScopeType(scope);
            created.setScopeId(scopeValue);
            created.setThemeId(themeId);
            created.setThemeVersion(version);
            created.setActiveKey(scope + "#" + scopeValue);
            created.setUpdateBy(user);
            created.setCreateTime(new Date());
            created.setUpdateTime(created.getCreateTime());
            try {
                themeMapper.insertBinding(created);
            } catch (DuplicateKeyException ex) {
                throw new ServiceException("这个位置正在被其他人绑定，请刷新后再发布");
            }
            return;
        }
        existing.setThemeId(themeId);
        existing.setThemeVersion(version);
        existing.setUpdateBy(user);
        themeMapper.updateBinding(existing);
    }

    private SiteThemeResp toResp(BasketSiteTheme theme, boolean withDraft) {
        Normalized view = fromStored(theme, withDraft && Objects.equals(theme.getHasDraft(), 1));
        SiteThemeResp resp = new SiteThemeResp();
        resp.setId(theme.getId());
        resp.setThemeName(view.themeName);
        resp.setDescription(view.description);
        resp.setBgColor(view.bgColor);
        resp.setBgImageFileId(view.bgImageFileId);
        resp.setMobileBgImageFileId(view.mobileBgImageFileId);
        resp.setBgDisplay(view.bgDisplay);
        resp.setColors(view.colors);
        resp.setThemeStatus(theme.getThemeStatus());
        resp.setHasDraft(theme.getHasDraft());
        resp.setVersionNo(theme.getVersionNo());
        resp.setBgImageUrl(urlOf(view.bgImageFileId));
        resp.setMobileBgImageUrl(urlOf(view.mobileBgImageFileId));
        resp.setThemeStatusName(switch (theme.getThemeStatus() == null ? DRAFT : theme.getThemeStatus()) {
            case PUBLISHED -> "已发布";
            case OFFLINE -> "已停用";
            default -> "未发布";
        });
        if (Objects.equals(theme.getThemeStatus(), DRAFT)) {
            resp.setPendingChangeLabel("未发布");
        } else if (Objects.equals(theme.getHasDraft(), 1)) {
            resp.setPendingChangeLabel("有未发布修改");
        } else if (Objects.equals(theme.getThemeStatus(), OFFLINE)) {
            resp.setPendingChangeLabel("已停用");
        }
        return resp;
    }

    private String urlOf(Long fileId) {
        if (fileId == null) {
            return null;
        }
        return themeMapper.selectFileUrl(fileId);
    }

    private List<SiteThemeBindingResp> bindingsOf(Long themeId) {
        return groupBindings(themeMapper.selectLiveBindings()).getOrDefault(themeId, List.of());
    }

    private Map<Long, List<SiteThemeBindingResp>> groupBindings(List<BasketSiteThemeBinding> rows) {
        Map<Long, List<SiteThemeBindingResp>> grouped = new LinkedHashMap<>();
        for (BasketSiteThemeBinding row : rows) {
            SiteThemeBindingResp resp = new SiteThemeBindingResp();
            resp.setId(row.getId());
            resp.setScopeType(row.getScopeType());
            resp.setScopeId(row.getScopeId());
            resp.setThemeId(row.getThemeId());
            resp.setThemeName(row.getThemeName());
            resp.setSeasonName(row.getSeasonName());
            resp.setThemeVersion(row.getThemeVersion());
            resp.setScopeLabel(scopeLabel(row));
            grouped.computeIfAbsent(row.getThemeId(), key -> new ArrayList<>()).add(resp);
        }
        return grouped;
    }

    private String scopeLabel(BasketSiteThemeBinding row) {
        if ("HOME".equals(row.getScopeType())) {
            return "首页";
        }
        if ("BASE".equals(row.getScopeType())) {
            return "基础主题";
        }
        String season = StringUtils.isEmpty(row.getSeasonName()) ? String.valueOf(row.getScopeId()) : row.getSeasonName();
        return "赛季 " + season;
    }

    private String scopeText(String scope, long scopeId) {
        if ("HOME".equals(scope)) {
            return "首页";
        }
        if ("BASE".equals(scope)) {
            return "基础主题";
        }
        BasketSeason season = seasonMapper.selectById(scopeId);
        String name = season == null ? String.valueOf(scopeId)
                : (StringUtils.isEmpty(season.getDisplayTitle()) ? season.getSeasonName() : season.getDisplayTitle());
        return "赛季 " + name;
    }

    private BasketSiteTheme newRow(Normalized input, int status) {
        Date now = new Date();
        BasketSiteTheme row = new BasketSiteTheme();
        fillLive(row, input);
        row.setColorsJson(input.colorsJson);
        row.setDraftJson(input.draftJson);
        row.setHasDraft(1);
        row.setThemeStatus(status);
        row.setVersionNo(0);
        row.setUpdateBy(operator());
        row.setCreateTime(now);
        row.setUpdateTime(now);
        row.setIsDelete(0);
        return row;
    }

    private void fillLive(BasketSiteTheme row, Normalized input) {
        row.setThemeName(input.themeName);
        row.setDescription(input.description);
        row.setBgColor(input.bgColor);
        row.setBgImageFileId(input.bgImageFileId);
        row.setMobileBgImageFileId(input.mobileBgImageFileId);
        row.setBgDisplay(input.bgDisplay);
    }

    private Normalized normalize(SiteThemeSaveReq req) {
        if (req == null || StringUtils.isEmpty(req.getThemeName())) {
            throw new ServiceException("主题名称不能为空");
        }
        Normalized input = new Normalized();
        input.themeName = req.getThemeName().trim();
        if (input.themeName.length() > 64) {
            throw new ServiceException("主题名称不能超过64字");
        }
        input.description = blank(req.getDescription());
        if (input.description != null && input.description.length() > 500) {
            throw new ServiceException("说明不能超过500字");
        }
        input.bgColor = color(req.getBgColor(), "背景色");
        input.bgImageFileId = req.getBgImageFileId();
        input.mobileBgImageFileId = req.getMobileBgImageFileId();
        input.bgDisplay = display(req.getBgDisplay());
        input.colors = colors(req.getColors());
        input.colorsJson = writeColors(input.colors);
        input.draftJson = writeDraft(input);
        return input;
    }

    private Normalized fromStored(BasketSiteTheme theme, boolean preferDraft) {
        if (preferDraft && StringUtils.isNotEmpty(theme.getDraftJson())) {
            try {
                DraftPayload payload = JSON.readValue(theme.getDraftJson(), DraftPayload.class);
                Normalized input = new Normalized();
                input.themeName = StringUtils.isEmpty(payload.themeName) ? theme.getThemeName() : payload.themeName;
                input.description = payload.description;
                input.bgColor = payload.bgColor;
                input.bgImageFileId = payload.bgImageFileId;
                input.mobileBgImageFileId = payload.mobileBgImageFileId;
                input.bgDisplay = payload.bgDisplay;
                input.colors = payload.colors;
                return input;
            } catch (Exception ex) {
                throw new ServiceException("主题草稿无法读取");
            }
        }
        Normalized input = new Normalized();
        input.themeName = theme.getThemeName();
        input.description = theme.getDescription();
        input.bgColor = theme.getBgColor();
        input.bgImageFileId = theme.getBgImageFileId();
        input.mobileBgImageFileId = theme.getMobileBgImageFileId();
        input.bgDisplay = theme.getBgDisplay();
        input.colors = readColors(theme.getColorsJson());
        return input;
    }

    private ThemeColorReq colors(ThemeColorReq raw) {
        if (raw == null) {
            throw new ServiceException("语义色不能为空");
        }
        ThemeColorReq colors = new ThemeColorReq();
        colors.setPrimary(color(raw.getPrimary(), "主色"));
        colors.setSecondary(color(raw.getSecondary(), "辅助色"));
        colors.setAccent(color(raw.getAccent(), "强调色"));
        colors.setContent(color(raw.getContent(), "内容区"));
        colors.setCard(color(raw.getCard(), "卡片"));
        colors.setTableHead(color(raw.getTableHead(), "表头"));
        colors.setBorder(color(raw.getBorder(), "边框"));
        colors.setText(color(raw.getText(), "主文字"));
        colors.setTextMuted(color(raw.getTextMuted(), "辅助文字"));
        colors.setOnAccent(color(raw.getOnAccent(), "强调面上的文字"));
        return colors;
    }

    private ThemeColorReq readColors(String json) {
        if (StringUtils.isEmpty(json)) {
            return new ThemeColorReq();
        }
        try {
            return JSON.readValue(json, ThemeColorReq.class);
        } catch (Exception ex) {
            throw new ServiceException("主题颜色无法读取");
        }
    }

    private String writeColors(ThemeColorReq colors) {
        try {
            return JSON.writeValueAsString(colors);
        } catch (Exception ex) {
            throw new ServiceException("主题颜色无法保存");
        }
    }

    private String writeDraft(Normalized input) {
        DraftPayload payload = new DraftPayload();
        payload.themeName = input.themeName;
        payload.description = input.description;
        payload.bgColor = input.bgColor;
        payload.bgImageFileId = input.bgImageFileId;
        payload.mobileBgImageFileId = input.mobileBgImageFileId;
        payload.bgDisplay = input.bgDisplay;
        payload.colors = input.colors;
        try {
            return JSON.writeValueAsString(payload);
        } catch (Exception ex) {
            throw new ServiceException("主题草稿无法保存");
        }
    }

    private String color(String value, String label) {
        if (value == null || !COLOR.matcher(value.trim()).matches()) {
            throw new ServiceException(label + "请使用 #RRGGBB");
        }
        return value.trim().toUpperCase();
    }

    private String display(String value) {
        String display = StringUtils.isEmpty(value) ? "TOP_CENTER" : value.trim().toUpperCase();
        if (!DISPLAYS.contains(display)) {
            throw new ServiceException("背景展示方式不正确");
        }
        return display;
    }

    private String normalizeScope(String scopeType) {
        String scope = scopeType == null ? "" : scopeType.trim().toUpperCase();
        if (!SCOPES.contains(scope)) {
            throw new ServiceException("发布位置不正确");
        }
        return scope;
    }

    private long normalizeScopeId(String scope, Long seasonId) {
        if (!"SEASON".equals(scope)) {
            return 0L;
        }
        if (seasonId == null || seasonId <= 0 || seasonMapper.selectById(seasonId) == null) {
            throw new ServiceException("请选择有效赛季");
        }
        return seasonId;
    }

    private void assertVersion(BasketSiteTheme existing, Integer versionNo) {
        if (versionNo == null || !Objects.equals(existing.getVersionNo(), versionNo)) {
            throw conflict();
        }
    }

    private BasketSiteTheme require(Long id) {
        if (id == null) {
            throw new ServiceException("主题不存在");
        }
        BasketSiteTheme theme = themeMapper.selectById(id);
        if (theme == null) {
            throw new ServiceException("主题不存在");
        }
        return theme;
    }

    private void assertEdit() {
        if (!permissionService.hasPermi(BasketContentPerms.SITE_THEME_EDIT)) {
            throw new ServiceException("没有权限编辑站点主题");
        }
    }

    private void assertPublish() {
        if (!permissionService.hasPermi(BasketContentPerms.SITE_THEME_PUBLISH)) {
            throw new ServiceException("没有权限发布站点主题");
        }
    }

    private void assertAny(String... perms) {
        for (String perm : perms) {
            if (permissionService.hasPermi(perm)) {
                return;
            }
        }
        throw new ServiceException("没有权限查看站点主题");
    }

    private ServiceException conflict() {
        return new ServiceException("主题已被其他人修改，请刷新后再保存");
    }

    private String operator() {
        try {
            String name = SecurityUtils.getUsername();
            return name == null ? "" : name;
        } catch (Exception ex) {
            return "";
        }
    }

    private String blank(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String displayName(String name) {
        return StringUtils.isEmpty(name) ? "未命名主题" : name;
    }

    private static final class Normalized {
        private String themeName;
        private String description;
        private String bgColor;
        private Long bgImageFileId;
        private Long mobileBgImageFileId;
        private String bgDisplay;
        private ThemeColorReq colors;
        private String colorsJson;
        private String draftJson;
    }

    private static final class DraftPayload {
        public String themeName;
        public String description;
        public String bgColor;
        public Long bgImageFileId;
        public Long mobileBgImageFileId;
        public String bgDisplay;
        public ThemeColorReq colors;
    }
}
