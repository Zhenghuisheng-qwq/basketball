package com.zhs.basket.support.v4.roster;

import com.zhs.basket.enums.v4.GameModeEnum;
import com.zhs.common.exception.ServiceException;

public final class GameModeRosterRule {

    private final GameModeEnum gameMode;
    private final int minTeamSize;
    private final int maxTeamSize;
    private final int minimumValidPersonCount;

    private GameModeRosterRule(GameModeEnum gameMode) {
        this.gameMode = gameMode;
        this.minTeamSize = gameMode.getMinRosterCount();
        this.maxTeamSize = gameMode.getMaxRosterCount();
        this.minimumValidPersonCount = gameMode.getMinRosterCount() * 2;
    }

    public static GameModeRosterRule of(String gameModeCode) {
        try {
            return new GameModeRosterRule(GameModeEnum.requireCode(gameModeCode));
        } catch (IllegalArgumentException ex) {
            throw new ServiceException(ex.getMessage());
        }
    }

    public static GameModeRosterRule of(GameModeEnum gameMode) {
        if (gameMode == null) {
            throw new ServiceException("gameMode \u4e0d\u80fd\u4e3a\u7a7a");
        }
        return new GameModeRosterRule(gameMode);
    }

    public GameModeEnum getGameMode() {
        return gameMode;
    }

    public String getGameModeCode() {
        return gameMode.getCode();
    }

    public int getMinTeamSize() {
        return minTeamSize;
    }

    public int getMaxTeamSize() {
        return maxTeamSize;
    }

    public int getMinimumValidPersonCount() {
        return minimumValidPersonCount;
    }

    public void assertValidPersonCount(int validPersonCount) {
        if (validPersonCount < minimumValidPersonCount) {
            throw new ServiceException(gameMode.getDescription() + " \u81f3\u5c11\u9700\u8981 "
                    + minimumValidPersonCount + " \u540d\u6709\u6548\u4e2a\u4eba\u624d\u80fd\u5f62\u6210 2 \u652f\u5408\u6cd5\u7403\u961f");
        }
    }
}
