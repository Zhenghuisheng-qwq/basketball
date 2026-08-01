package com.zhs.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhs.config.MemberInitProperties;
import com.zhs.dao.BasketFileAssetDao;
import com.zhs.enums.FileUsageTypeEnum;
import com.zhs.exception.ServiceException;
import com.zhs.model.BasketFileAsset;
import com.zhs.model.BasketMemberAccount;
import com.zhs.response.member.MemberInitAccountItemResp;
import com.zhs.response.member.MemberInitAccountsResp;
import com.zhs.service.IMemberInitService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
public class MemberInitServiceImpl implements IMemberInitService {

    @Resource
    private MemberInitProperties memberInitProperties;
    @Resource
    private BasketFileAssetDao fileAssetDao;
    @Resource
    private MemberInitAccountCreator memberInitAccountCreator;
    @Resource
    private MemberInitAccountExecutor memberInitAccountExecutor;
    @Resource
    private ResourceLoader resourceLoader;

    @Override
    public MemberInitAccountsResp initAccounts(String clientIp) {
        if (!memberInitProperties.isEnabled()) {
            throw new ServiceException("初始化接口未启用", 403);
        }

        Path dataFile = resolveDataFile();
        long nextEmailNo = resolveNextEmailNo(dataFile);
        List<Long> avatarPool = loadAvatarPool();
        byte[] screenshotBytes = loadCertScreenshotBytes();

        List<MemberInitAccountItemResp> created = new ArrayList<>();
        List<String> linesToAppend = new ArrayList<>();

        for (int i = 0; i < memberInitProperties.getBatchSize(); i++) {
            long emailNo = nextEmailNo + i;
            String email = emailNo + "@" + memberInitProperties.getEmailDomain();
            if (memberInitAccountCreator.findByEmail(email) != null) {
                log.warn("跳过已存在账号: {}", email);
                continue;
            }

            Long avatarFileId = avatarPool.isEmpty()
                    ? null
                    : avatarPool.get(ThreadLocalRandom.current().nextInt(avatarPool.size()));
            MemberInitAccountItemResp item = memberInitAccountExecutor.createAccountAndApplyCert(
                    email, emailNo, avatarFileId, screenshotBytes, clientIp, i);
            created.add(item);
            linesToAppend.add(email + " " + memberInitProperties.getPassword());
        }

        appendDataFile(dataFile, linesToAppend);

        MemberInitAccountsResp resp = new MemberInitAccountsResp();
        resp.setCreatedCount(created.size());
        resp.setDataFilePath(dataFile.toAbsolutePath().toString());
        resp.setAccounts(created);
        return resp;
    }

    private List<Long> loadAvatarPool() {
        List<BasketFileAsset> assets = fileAssetDao.selectList(new LambdaQueryWrapper<BasketFileAsset>()
                .eq(BasketFileAsset::getUsageType, FileUsageTypeEnum.ACCOUNT_AVATAR.getCode())
                .eq(BasketFileAsset::getIsDelete, 0));
        if (assets == null || assets.isEmpty()) {
            return Collections.emptyList();
        }
        return assets.stream().map(BasketFileAsset::getId).toList();
    }

    private byte[] loadCertScreenshotBytes() {
        String location = memberInitProperties.getCertScreenshotResource();
        org.springframework.core.io.Resource resource = resourceLoader.getResource(location);
        if (!resource.exists()) {
            throw new ServiceException("球员认证截图资源不存在: " + location);
        }
        try {
            return StreamUtils.copyToByteArray(resource.getInputStream());
        } catch (IOException ex) {
            throw new ServiceException("读取球员认证截图失败");
        }
    }

    private Path resolveDataFile() {
        Path configured = Paths.get(memberInitProperties.getDataFilePath());
        if (configured.isAbsolute()) {
            return configured.normalize();
        }
        Path cwd = Paths.get(System.getProperty("user.dir")).normalize();
        for (int depth = 0; depth <= 4; depth++) {
            Path base = depth == 0 ? cwd : cwd.resolve("../".repeat(depth)).normalize();
            Path candidate = base.resolve(configured).normalize();
            if (Files.exists(candidate.getParent())) {
                return candidate;
            }
        }
        return cwd.resolve(configured).normalize();
    }

    private long resolveNextEmailNo(Path dataFile) {
        long max = memberInitProperties.getEmailStart() - 1;
        String domain = memberInitProperties.getEmailDomain();
        String suffix = "@" + domain;

        if (Files.exists(dataFile)) {
            try {
                for (String line : Files.readAllLines(dataFile, StandardCharsets.UTF_8)) {
                    if (!StringUtils.hasText(line) || line.trim().startsWith("#")) {
                        continue;
                    }
                    String[] parts = line.trim().split("\\s+");
                    if (parts.length < 2 || !parts[0].endsWith(suffix)) {
                        continue;
                    }
                    String localPart = parts[0].substring(0, parts[0].length() - suffix.length());
                    if (localPart.chars().allMatch(Character::isDigit)) {
                        max = Math.max(max, Long.parseLong(localPart));
                    }
                }
            } catch (IOException ex) {
                throw new ServiceException("读取测试数据文件失败: " + dataFile);
            }
        }

        return max + 1;
    }

    private void appendDataFile(Path dataFile, List<String> lines) {
        if (lines.isEmpty()) {
            return;
        }
        try {
            Path parent = dataFile.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            if (!Files.exists(dataFile)) {
                Files.createFile(dataFile);
            }
            String content = String.join(System.lineSeparator(), lines) + System.lineSeparator();
            Files.writeString(dataFile, content, StandardCharsets.UTF_8, StandardOpenOption.APPEND);
        } catch (IOException ex) {
            throw new ServiceException("写入测试数据文件失败: " + dataFile);
        }
    }
}
