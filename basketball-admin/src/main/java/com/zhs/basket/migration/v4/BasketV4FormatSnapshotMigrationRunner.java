package com.zhs.basket.migration.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionFormatSnapshot;
import com.zhs.basket.domain.v4.BasketFormatTemplate;
import com.zhs.basket.enums.v4.DivisionStatusEnum;
import com.zhs.basket.enums.v4.SnapshotStatusEnum;
import com.zhs.basket.mapper.v4.*;
import com.zhs.basket.service.v4.IBasketV4FormatSnapshotService;
import com.zhs.basket.support.v4.DivisionRuleMutableSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.List;

/**
 * 一次性兼容迁移：为缺少规则快照的历史 Division 创建 V4_LEGACY 快照。
 * 默认不开启。启用：cnpa.v4.format-snapshot-migration.enabled=true
 *
 * 状态规则：
 * 1) 已有报名/EventTeam/赛程/比赛 → IN_USE
 * 2) 已开放报名但无后续业务数据 → LOCKED
 * 3) Division=DRAFT 且无业务数据 → DRAFT
 */
@Component
@ConditionalOnProperty(name = "cnpa.v4.format-snapshot-migration.enabled", havingValue = "true")
public class BasketV4FormatSnapshotMigrationRunner implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(BasketV4FormatSnapshotMigrationRunner.class);

    @Autowired
    private BasketDivisionMapper divisionMapper;
    @Autowired
    private BasketFormatTemplateMapper templateMapper;
    @Autowired
    private BasketDivisionFormatSnapshotMapper snapshotMapper;
    @Autowired
    private IBasketV4FormatSnapshotService formatSnapshotService;
    @Autowired
    private DivisionRuleMutableSpecification ruleMutableSpecification;
    @Autowired
    private BasketRegistrationMapper registrationMapper;
    @Autowired
    private BasketEventTeamMapper eventTeamMapper;
    @Autowired
    private BasketScheduleBatchMapper scheduleBatchMapper;
    @Autowired
    private BasketV4MatchMapper matchMapper;

    @Override
    public void run(String... args) {
        BasketFormatTemplate legacy = templateMapper.selectByCodeVersion("V4_LEGACY_ROUND_ROBIN_V1", 1);
        if (legacy == null) {
            log.warn("[FormatSnapshotMigration] 模板 V4_LEGACY_ROUND_ROBIN_V1 不存在，跳过");
            return;
        }
        List<BasketDivision> divisions = divisionMapper.selectWithoutFormatSnapshot();
        log.info("[FormatSnapshotMigration] 待处理 Division 数量={}", divisions.size());
        for (BasketDivision division : divisions) {
            try {
                if (division.getFormatSnapshotId() != null
                        && snapshotMapper.selectById(division.getFormatSnapshotId()) != null) {
                    continue;
                }
                if (snapshotMapper.selectActiveByDivisionId(division.getId()) != null) {
                    continue;
                }
                Long snapshotId = formatSnapshotService.createDraftSnapshot(division.getId(), legacy.getId());
                BasketDivisionFormatSnapshot snapshot = snapshotMapper.selectById(snapshotId);
                if (snapshot == null) {
                    continue;
                }
                String targetStatus = resolveBackfillStatus(division);
                if (!SnapshotStatusEnum.DRAFT.getCode().equals(targetStatus)) {
                    snapshot.setSnapshotStatus(targetStatus);
                    snapshot.setLockedTime(new Date());
                    snapshotMapper.updateById(snapshot);
                }
                log.info("[FormatSnapshotMigration] Division id={} snapshotId={} status={}",
                        division.getId(), snapshotId, targetStatus);
            } catch (Exception ex) {
                log.error("[FormatSnapshotMigration] Division id={} 失败: {}", division.getId(), ex.getMessage());
            }
        }
    }

    private String resolveBackfillStatus(BasketDivision division) {
        Long divisionId = division.getId();
        boolean hasBusiness = registrationMapper.countActiveByDivisionId(divisionId) > 0
                || eventTeamMapper.countActiveByDivisionId(divisionId) > 0
                || scheduleBatchMapper.countByDivisionId(divisionId) > 0
                || matchMapper.countByDivisionId(divisionId) > 0
                || ruleMutableSpecification.hasPersonalRegistration(divisionId);
        if (hasBusiness) {
            return SnapshotStatusEnum.IN_USE.getCode();
        }
        if (!DivisionStatusEnum.DRAFT.getCode().equals(division.getDivisionStatus())) {
            return SnapshotStatusEnum.LOCKED.getCode();
        }
        return SnapshotStatusEnum.DRAFT.getCode();
    }
}
