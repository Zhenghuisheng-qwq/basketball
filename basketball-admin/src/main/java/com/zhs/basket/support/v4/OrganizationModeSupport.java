package com.zhs.basket.support.v4;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.basket.domain.v4.BasketFormatTemplateStage;
import com.zhs.basket.enums.v4.GroupingModeEnum;
import com.zhs.basket.enums.v4.OrganizationModeEnum;

/**
 * organization_mode 解析与兼容（R1-1）。
 * <p>
 * 读取旧数据时可 fallback；新建/修改保存路径必须显式写入 organizationMode，不依赖 fallback。
 */
public final class OrganizationModeSupport {

    private OrganizationModeSupport() {
    }

    public static String resolveOrganizationMode(BasketDivisionStage stage) {
        if (stage == null) {
            return OrganizationModeEnum.UNGROUPED.getCode();
        }
        if (stage.getOrganizationMode() != null && !stage.getOrganizationMode().isEmpty()) {
            return stage.getOrganizationMode();
        }
        return resolveFromGroupingMode(stage.getGroupingMode());
    }

    public static String resolveOrganizationMode(BasketFormatTemplateStage stage) {
        if (stage == null) {
            return OrganizationModeEnum.UNGROUPED.getCode();
        }
        if (stage.getOrganizationMode() != null && !stage.getOrganizationMode().isEmpty()) {
            return stage.getOrganizationMode();
        }
        return resolveFromGroupingMode(stage.getGroupingMode());
    }

    /**
     * 由 grouping_mode 推导 organization_mode（迁移回填与只读兼容）。
     */
    public static String resolveFromGroupingMode(String groupingMode) {
        if (groupingMode == null || groupingMode.isEmpty()) {
            return OrganizationModeEnum.UNGROUPED.getCode();
        }
        if (GroupingModeEnum.NONE.getCode().equals(groupingMode)) {
            return OrganizationModeEnum.UNGROUPED.getCode();
        }
        return OrganizationModeEnum.GROUPED.getCode();
    }

    public static boolean isGrouped(BasketDivisionStage stage) {
        return OrganizationModeEnum.GROUPED.getCode().equals(resolveOrganizationMode(stage));
    }

    /**
     * 仅用于历史数据迁移/内部回填，新业务保存禁止调用。
     *
     * @deprecated 新建/修改 Stage 必须显式传入 organizationMode，见 {@link StageFormatSpecification#requireExplicitOrganizationMode}
     */
    @Deprecated
    public static String resolveForPersist(String organizationMode, String groupingMode) {
        if (organizationMode != null && !organizationMode.isEmpty()) {
            OrganizationModeEnum.fromCode(organizationMode);
            return organizationMode;
        }
        return resolveFromGroupingMode(groupingMode);
    }
}
