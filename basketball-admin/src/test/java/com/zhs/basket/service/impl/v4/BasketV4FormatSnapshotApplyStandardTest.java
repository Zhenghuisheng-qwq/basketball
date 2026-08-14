package com.zhs.basket.service.impl.v4;

import com.zhs.basket.domain.v4.BasketDivision;
import com.zhs.basket.domain.v4.BasketDivisionFormatSnapshot;
import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketDivisionTieBreakRule;
import com.zhs.basket.domain.v4.BasketFormatTemplate;
import com.zhs.basket.domain.v4.BasketSeason;
import com.zhs.basket.domain.v4.BasketSeries;
import com.zhs.basket.dto.v4.StandardTemplateApplyReq;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;
import com.zhs.basket.enums.v4.DivisionStatusEnum;
import com.zhs.basket.enums.v4.SnapshotStatusEnum;
import com.zhs.basket.enums.v4.StageTypeEnum;
import com.zhs.basket.enums.v4.TemplateTypeEnum;
import com.zhs.basket.mapper.v4.BasketDivisionFormatSnapshotMapper;
import com.zhs.basket.mapper.v4.BasketDivisionMapper;
import com.zhs.basket.mapper.v4.BasketDivisionStageMapper;
import com.zhs.basket.mapper.v4.BasketDivisionTieBreakRuleMapper;
import com.zhs.basket.mapper.v4.BasketSeasonMapper;
import com.zhs.basket.mapper.v4.BasketSeriesMapper;
import com.zhs.common.exception.ServiceException;
import com.zhs.basket.support.v4.DivisionFormatSnapshotFactory;
import com.zhs.basket.support.v4.DivisionRuleMutableSpecification;
import com.zhs.basket.support.v4.template.ArenaStandardTemplateBuilder;
import com.zhs.basket.support.v4.template.CupStandardTemplateBuilder;
import com.zhs.basket.support.v4.template.LeagueStandardTemplateBuilder;
import com.zhs.basket.support.v4.template.OpenStandardTemplateBuilder;
import com.zhs.basket.support.v4.template.StandardFormatTemplateFactory;
import com.zhs.basket.support.v4.template.StandardTemplateCodes;
import com.zhs.basket.vo.v4.BasketDivisionFormatSnapshotVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BasketV4FormatSnapshotApplyStandardTest {

    @Mock private BasketDivisionFormatSnapshotMapper snapshotMapper;
    @Mock private BasketDivisionStageMapper divisionStageMapper;
    @Mock private BasketDivisionTieBreakRuleMapper divisionTieBreakRuleMapper;
    @Mock private BasketDivisionMapper divisionMapper;
    @Mock private DivisionRuleMutableSpecification ruleMutableSpecification;
    @Mock private BasketV4FormatTemplateServiceImpl templateService;
    @Mock private BasketSeasonMapper seasonMapper;
    @Mock private BasketSeriesMapper seriesMapper;

    @InjectMocks
    private BasketV4FormatSnapshotServiceImpl service;

    private final AtomicReference<BasketDivisionFormatSnapshot> current = new AtomicReference<>();
    private final List<BasketDivisionStage> storedStages = new ArrayList<>();
    private final AtomicLong stageId = new AtomicLong(1000);

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "snapshotFactory", new DivisionFormatSnapshotFactory());
        ReflectionTestUtils.setField(service, "standardTemplateFactory", new StandardFormatTemplateFactory(List.of(
                new LeagueStandardTemplateBuilder(),
                new ArenaStandardTemplateBuilder(),
                new CupStandardTemplateBuilder(),
                new OpenStandardTemplateBuilder()
        )));
        org.mockito.Mockito.doAnswer(inv -> {
            BasketDivisionFormatSnapshot existing = inv.getArgument(1);
            if (existing != null && SnapshotStatusEnum.LOCKED.getCode().equals(existing.getSnapshotStatus())) {
                throw new ServiceException("\u5df2\u9501\u5b9a\u89c4\u5219\u5feb\u7167\u4e0d\u5141\u8bb8\u5e94\u7528\u6807\u51c6\u6a21\u677f");
            }
            return null;
        }).when(ruleMutableSpecification).assertCanApplyStandardTemplate(any(), any());
        when(ruleMutableSpecification.isMutable(any(), any())).thenAnswer(inv -> {
            BasketDivisionFormatSnapshot existing = inv.getArgument(1);
            return existing != null && SnapshotStatusEnum.DRAFT.getCode().equals(existing.getSnapshotStatus());
        });
        when(snapshotMapper.selectActiveByDivisionId(173L)).thenAnswer(inv -> current.get());
        when(snapshotMapper.insert(any())).thenAnswer(inv -> {
            BasketDivisionFormatSnapshot snapshot = inv.getArgument(0);
            snapshot.setId(900L);
            current.set(snapshot);
            return 1;
        });
        when(divisionStageMapper.insert(any())).thenAnswer(inv -> {
            BasketDivisionStage stage = inv.getArgument(0);
            stage.setId(stageId.getAndIncrement());
            storedStages.add(stage);
            return 1;
        });
        when(divisionStageMapper.selectBySnapshotId(anyLong())).thenAnswer(inv -> new ArrayList<>(storedStages));
        List<BasketDivisionTieBreakRule> storedRules = new ArrayList<>();
        lenient().when(divisionTieBreakRuleMapper.insert(any())).thenAnswer(inv -> {
            BasketDivisionTieBreakRule rule = inv.getArgument(0);
            rule.setId(2000L + storedRules.size());
            storedRules.add(rule);
            return 1;
        });
        when(divisionTieBreakRuleMapper.selectBySnapshotId(anyLong())).thenAnswer(inv -> new ArrayList<>(storedRules));
        when(divisionMapper.updateById(any())).thenReturn(1);
    }

    @Test
    void applyArenaStandard_replacesCustomRoundRobinDraft() {
        current.set(draftCustom());
        when(snapshotMapper.selectMaxVersionByDivisionId(173L)).thenReturn(1);
        stubDivisionGraph();
        when(templateService.requireEnabledTemplateByCode(StandardTemplateCodes.ARENA_STANDARD_V1))
                .thenReturn(template(6L, StandardTemplateCodes.ARENA_STANDARD_V1));

        StandardTemplateApplyReq req = new StandardTemplateApplyReq();
        req.setTemplateCode(StandardTemplateCodes.ARENA_STANDARD_V1);
        req.setRegistrationMode("TEAM");
        BasketDivisionFormatSnapshotVo vo = service.applyStandardTemplate(173L, req);

        assertEquals(StandardTemplateCodes.ARENA_STANDARD_V1, vo.getSourceTemplateCode());
        assertEquals(TemplateTypeEnum.STANDARD.getCode(), vo.getSourceTemplateType());
        assertNull(vo.getRuleVersionNo());
        assertEquals(2, vo.getSnapshotVersion());
        assertEquals(1, storedStages.size());
        assertEquals(StandardTemplateCodes.STAGE_KNOCKOUT, storedStages.get(0).getStageCode());
        assertEquals(StageTypeEnum.KNOCKOUT.getCode(), storedStages.get(0).getStageType());
        assertEquals(CompetitionFormatEnum.SINGLE_ELIMINATION.getCode(), storedStages.get(0).getCompetitionFormat());
        assertFalse(storedStages.stream().anyMatch(s -> "REGULAR".equals(s.getStageCode())));
        verify(divisionStageMapper).softDeleteBySnapshotId(11L);
    }

    @Test
    void applyLeagueStandard_onArenaSeries_buildsRegularAndPlayoff() {
        current.set(draftCustom());
        when(snapshotMapper.selectMaxVersionByDivisionId(173L)).thenReturn(1);
        stubDivisionGraph();
        when(templateService.requireEnabledTemplateByCode(StandardTemplateCodes.LEAGUE_STANDARD_V1))
                .thenReturn(template(2L, StandardTemplateCodes.LEAGUE_STANDARD_V1));

        StandardTemplateApplyReq req = new StandardTemplateApplyReq();
        req.setTemplateCode(StandardTemplateCodes.LEAGUE_STANDARD_V1);
        req.setRegistrationMode("TEAM");
        service.applyStandardTemplate(173L, req);

        assertEquals(2, storedStages.size());
        assertEquals(StandardTemplateCodes.STAGE_REGULAR, storedStages.get(0).getStageCode());
        assertEquals(CompetitionFormatEnum.ROUND_ROBIN.getCode(), storedStages.get(0).getCompetitionFormat());
        assertEquals(StandardTemplateCodes.STAGE_PLAYOFF, storedStages.get(1).getStageCode());
        assertEquals(CompetitionFormatEnum.SINGLE_ELIMINATION.getCode(), storedStages.get(1).getCompetitionFormat());
    }

    @Test
    void applyCupStandard_replacesRoundRobinDraft() {
        current.set(draftCustom());
        when(snapshotMapper.selectMaxVersionByDivisionId(173L)).thenReturn(1);
        stubDivisionGraph();
        when(templateService.requireEnabledTemplateByCode(StandardTemplateCodes.CUP_STANDARD_V1))
                .thenReturn(template(7L, StandardTemplateCodes.CUP_STANDARD_V1));

        StandardTemplateApplyReq req = new StandardTemplateApplyReq();
        req.setTemplateCode(StandardTemplateCodes.CUP_STANDARD_V1);
        req.setRegistrationMode("TEAM");
        service.applyStandardTemplate(173L, req);

        assertEquals(2, storedStages.size());
        assertEquals(StandardTemplateCodes.STAGE_GROUP, storedStages.get(0).getStageCode());
        assertEquals(StandardTemplateCodes.STAGE_KNOCKOUT, storedStages.get(1).getStageCode());
        verify(divisionStageMapper).softDeleteBySnapshotId(11L);
    }

    @Test
    void applyArenaStandard_afterLockedCustom_rejected() {
        BasketDivisionFormatSnapshot locked = draftCustom();
        locked.setSnapshotStatus(SnapshotStatusEnum.LOCKED.getCode());
        locked.setSnapshotVersion(3);
        current.set(locked);
        stubDivisionGraph();
        when(templateService.requireEnabledTemplateByCode(StandardTemplateCodes.ARENA_STANDARD_V1))
                .thenReturn(template(6L, StandardTemplateCodes.ARENA_STANDARD_V1));

        StandardTemplateApplyReq req = new StandardTemplateApplyReq();
        req.setTemplateCode(StandardTemplateCodes.ARENA_STANDARD_V1);
        ServiceException ex = assertThrows(ServiceException.class, () -> service.applyStandardTemplate(173L, req));
        assertTrue(ex.getMessage().contains("\u5df2\u9501\u5b9a"));
        verify(snapshotMapper, never()).insert(any());
        verify(divisionStageMapper, never()).softDeleteBySnapshotId(11L);
    }

    @Test
    void getDivisionSnapshot_lockedDraftRevision3_productRuleIsV1() {
        BasketDivisionFormatSnapshot locked = draftCustom();
        locked.setId(265L);
        locked.setSnapshotStatus(SnapshotStatusEnum.LOCKED.getCode());
        locked.setSnapshotVersion(3);
        current.set(locked);
        when(divisionMapper.selectById(173L)).thenReturn(division());

        BasketDivisionFormatSnapshotVo vo = service.getDivisionSnapshot(173L);
        assertEquals(265L, vo.getRuleVersionId());
        assertEquals(3, vo.getSnapshotVersion());
        assertEquals(1, vo.getRuleVersionNo());
        assertTrue(Boolean.TRUE.equals(vo.getLocked()));
        assertFalse(Boolean.TRUE.equals(vo.getMutable()));
        assertFalse(Boolean.TRUE.equals(vo.getCanApplyStandard()));
    }

    private void stubDivisionGraph() {
        when(divisionMapper.selectById(173L)).thenReturn(division());
        when(seasonMapper.selectById(138L)).thenReturn(season());
        when(seriesMapper.selectById(2L)).thenReturn(series());
    }

    private static BasketDivision division() {
        BasketDivision division = new BasketDivision();
        division.setId(173L);
        division.setSeasonId(138L);
        division.setDivisionStatus(DivisionStatusEnum.DRAFT.getCode());
        division.setPlatformCode("PS_XBOX");
        division.setGameMode("THREE_V_THREE");
        division.setRegistrationMode("TEAM");
        division.setLineupMinCount(3);
        division.setLineupMaxCount(8);
        division.setFormatTemplateId(3L);
        return division;
    }

    private static BasketSeason season() {
        BasketSeason season = new BasketSeason();
        season.setId(138L);
        season.setSeriesId(2L);
        return season;
    }

    private static BasketSeries series() {
        BasketSeries series = new BasketSeries();
        series.setId(2L);
        series.setSeriesCode("ARENA");
        return series;
    }

    private static BasketDivisionFormatSnapshot draftCustom() {
        BasketDivisionFormatSnapshot snapshot = new BasketDivisionFormatSnapshot();
        snapshot.setId(11L);
        snapshot.setDivisionId(173L);
        snapshot.setSnapshotVersion(1);
        snapshot.setSnapshotStatus(SnapshotStatusEnum.DRAFT.getCode());
        snapshot.setSourceTemplateType(TemplateTypeEnum.CUSTOM.getCode());
        snapshot.setSourceTemplateCode("ROUND_ROBIN_BASIC_V1");
        snapshot.setSourceTemplateId(3L);
        snapshot.setIsCurrent(1);
        snapshot.setRegistrationMode("TEAM");
        return snapshot;
    }

    private static BasketFormatTemplate template(Long id, String code) {
        BasketFormatTemplate template = new BasketFormatTemplate();
        template.setId(id);
        template.setTemplateCode(code);
        template.setTemplateName(code);
        template.setTemplateType(TemplateTypeEnum.STANDARD.getCode());
        template.setVersionNo(1);
        template.setEnabled(1);
        return template;
    }
}
