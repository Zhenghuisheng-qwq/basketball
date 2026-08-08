package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.enums.v4.AdvancementTypeEnum;
import com.zhs.basket.enums.v4.CompetitionFormatEnum;
import com.zhs.basket.enums.v4.GroupingModeEnum;
import com.zhs.basket.enums.v4.OrganizationModeEnum;
import com.zhs.common.exception.ServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LinearStageChainValidatorTest {

    private LinearStageChainValidator validator;

    @BeforeEach
    void setUp() {
        validator = new LinearStageChainValidator();
    }

    @Test
    void pass_oneStage() {
        assertDoesNotThrow(() -> validator.validate(List.of(stage("S1", 1, null))));
    }

    @Test
    void pass_twoStages() {
        assertDoesNotThrow(() -> validator.validate(List.of(
                stage("S1", 1, "S2"),
                stage("S2", 2, null))));
    }

    @Test
    void pass_fourStages() {
        assertDoesNotThrow(() -> validator.validate(List.of(
                stage("S1", 1, "S2"),
                stage("S2", 2, "S3"),
                stage("S3", 3, "S4"),
                stage("S4", 4, null))));
    }

    @Test
    void pass_sixStages() {
        List<BasketDivisionStage> stages = new ArrayList<>();
        for (int i = 1; i <= 6; i++) {
            stages.add(stage("S" + i, i, i < 6 ? "S" + (i + 1) : null));
        }
        assertDoesNotThrow(() -> validator.validate(stages));
    }

    @Test
    void fail_stageOrderGap() {
        assertThrows(ServiceException.class, () -> validator.validate(List.of(
                stage("S1", 1, "S3"),
                stage("S3", 3, null))));
    }

    @Test
    void fail_duplicateStageOrder() {
        assertThrows(ServiceException.class, () -> validator.validate(List.of(
                stage("S1", 1, "S2"),
                stage("S2", 2, null),
                stage("S2B", 2, null))));
    }

    @Test
    void fail_skipNextLink() {
        assertThrows(ServiceException.class, () -> validator.validate(List.of(
                stage("S1", 1, "S3"),
                stage("S2", 2, "S3"),
                stage("S3", 3, null))));
    }

    @Test
    void fail_secondStageNoPredecessor() {
        assertThrows(ServiceException.class, () -> validator.validate(List.of(
                stage("S1", 1, null),
                stage("S2", 2, null))));
    }

    @Test
    void fail_backReference() {
        assertThrows(ServiceException.class, () -> validator.validate(List.of(
                stage("S1", 1, "S2"),
                stage("S2", 2, "S1"))));
    }

    @Test
    void fail_lastStageHasNext() {
        assertThrows(ServiceException.class, () -> validator.validate(List.of(
                stage("S1", 1, "S2"),
                stage("S2", 2, "S3"))));
    }

    @Test
    void fail_duplicateStageCode() {
        assertThrows(ServiceException.class, () -> validator.validate(List.of(
                stage("S1", 1, "S1"),
                stage("S1", 2, null))));
    }

    private BasketDivisionStage stage(String code, int order, String next) {
        BasketDivisionStage s = new BasketDivisionStage();
        s.setStageCode(code);
        s.setStageName(code);
        s.setStageOrder(order);
        s.setNextStageCode(next);
        s.setOrganizationMode(OrganizationModeEnum.UNGROUPED.getCode());
        s.setCompetitionFormat(CompetitionFormatEnum.ROUND_ROBIN.getCode());
        s.setGroupingMode(GroupingModeEnum.NONE.getCode());
        s.setAdvancementType(AdvancementTypeEnum.NONE.getCode());
        return s;
    }
}
