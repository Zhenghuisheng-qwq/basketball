package com.zhs.basket.domain.v4;

import com.zhs.basket.enums.v4.GameModeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 阵容人数限制值对象，由 GameModeEnum 统一解析。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class RosterLimit {
    private int minCount;
    private int maxCount;

    public static RosterLimit resolve(GameModeEnum gameMode) {
        if (gameMode == null) {
            throw new IllegalArgumentException("gameMode 不能为空");
        }
        return gameMode.toRosterLimit();
    }

    public static RosterLimit resolve(String gameModeCode) {
        return resolve(GameModeEnum.requireCode(gameModeCode));
    }

    public void validateCounts(Integer min, Integer max) {
        if (min == null || min != minCount) {
            throw new IllegalArgumentException("lineupMinCount 必须为 " + minCount);
        }
        if (max == null || max != maxCount) {
            throw new IllegalArgumentException("lineupMaxCount 必须为 " + maxCount);
        }
    }

    public void validateLineupSize(int lineupSize) {
        if (lineupSize < minCount) {
            throw new IllegalArgumentException("阵容人数不能少于 " + minCount + " 人");
        }
        if (lineupSize > maxCount) {
            throw new IllegalArgumentException("阵容人数不能多于 " + maxCount + " 人");
        }
    }
}
