package com.zhs.basket.support.v4.stage;

import com.zhs.basket.domain.v4.BasketDivisionStage;
import com.zhs.common.exception.ServiceException;
import com.zhs.common.utils.StringUtils;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Stage 严格线性链校验：Stage1 → Stage2 → ... → StageN。
 */
@Component
public class LinearStageChainValidator {

    public void validate(List<BasketDivisionStage> stages) {
        if (stages == null || stages.isEmpty()) {
            throw new ServiceException("规则快照至少需要一个阶段");
        }

        List<BasketDivisionStage> ordered = stages.stream()
                .sorted(Comparator.comparing(BasketDivisionStage::getStageOrder))
                .toList();

        validateStageOrders(ordered);
        validateStageCodes(ordered);
        validateLinearNextLinks(ordered);
        validateSinglePredecessor(ordered);
        detectCycles(ordered);
    }

    private void validateStageOrders(List<BasketDivisionStage> ordered) {
        Set<Integer> orders = new HashSet<>();
        for (int i = 0; i < ordered.size(); i++) {
            BasketDivisionStage stage = ordered.get(i);
            Integer order = stage.getStageOrder();
            if (order == null || order <= 0) {
                throw new ServiceException("阶段顺序必须大于 0");
            }
            if (!orders.add(order)) {
                throw new ServiceException("阶段顺序不能重复：" + order);
            }
            if (!Integer.valueOf(i + 1).equals(order)) {
                throw new ServiceException("阶段顺序必须为连续正整数（从 1 开始）");
            }
        }
    }

    private void validateStageCodes(List<BasketDivisionStage> ordered) {
        Set<String> codes = new HashSet<>();
        for (BasketDivisionStage stage : ordered) {
            if (StringUtils.isEmpty(stage.getStageCode())) {
                throw new ServiceException("阶段编码不能为空");
            }
            if (!codes.add(stage.getStageCode())) {
                throw new ServiceException("阶段编码不能重复：" + stage.getStageCode());
            }
        }
    }

    private void validateLinearNextLinks(List<BasketDivisionStage> ordered) {
        for (int i = 0; i < ordered.size(); i++) {
            BasketDivisionStage stage = ordered.get(i);
            boolean isLast = i == ordered.size() - 1;
            String nextCode = stage.getNextStageCode();

            if (isLast) {
                if (nextCode != null && !nextCode.isEmpty()) {
                    throw new ServiceException("最后阶段「" + stage.getStageName() + "」不应再指向下一阶段");
                }
            } else {
                BasketDivisionStage expectedNext = ordered.get(i + 1);
                if (nextCode == null || nextCode.isEmpty()) {
                    throw new ServiceException("阶段「" + stage.getStageName() + "」需要指定下一比赛阶段");
                }
                if (!expectedNext.getStageCode().equals(nextCode)) {
                    throw new ServiceException("阶段「" + stage.getStageName()
                            + "」的下一阶段必须指向顺序相邻的 Stage（期望 " + expectedNext.getStageCode() + "）");
                }
            }
        }
    }

    private void validateSinglePredecessor(List<BasketDivisionStage> ordered) {
        if (ordered.size() <= 1) {
            return;
        }
        Map<String, String> predecessorCount = new HashMap<>();
        for (int i = 1; i < ordered.size(); i++) {
            String targetCode = ordered.get(i).getStageCode();
            predecessorCount.merge(targetCode, ordered.get(i - 1).getStageCode(), (a, b) -> a + "," + b);
        }
        for (Map.Entry<String, String> entry : predecessorCount.entrySet()) {
            if (entry.getValue().contains(",")) {
                throw new ServiceException("阶段「" + entry.getKey() + "」存在多个前驱，不允许分叉");
            }
        }
        Set<String> allCodes = new HashSet<>();
        for (BasketDivisionStage stage : ordered) {
            allCodes.add(stage.getStageCode());
        }
        for (BasketDivisionStage stage : ordered) {
            String next = stage.getNextStageCode();
            if (next != null && !next.isEmpty() && !allCodes.contains(next)) {
                throw new ServiceException("阶段「" + stage.getStageName() + "」指向不存在的下一阶段：" + next);
            }
        }
    }

    private void detectCycles(List<BasketDivisionStage> ordered) {
        Map<String, String> nextMap = new HashMap<>();
        for (BasketDivisionStage stage : ordered) {
            if (stage.getNextStageCode() != null && !stage.getNextStageCode().isEmpty()) {
                nextMap.put(stage.getStageCode(), stage.getNextStageCode());
            }
        }
        for (BasketDivisionStage stage : ordered) {
            Set<String> visited = new HashSet<>();
            String current = stage.getStageCode();
            while (nextMap.containsKey(current)) {
                if (!visited.add(current)) {
                    throw new ServiceException("阶段链路存在循环：" + current);
                }
                current = nextMap.get(current);
            }
        }
    }
}
