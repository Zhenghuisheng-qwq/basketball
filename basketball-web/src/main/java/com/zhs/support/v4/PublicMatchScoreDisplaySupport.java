package com.zhs.support.v4;

public final class PublicMatchScoreDisplaySupport {

    private PublicMatchScoreDisplaySupport() {
    }

    public static String childMatchScore(Integer teamScore, Integer opponentScore, String resultLabel) {
        if (resultLabel != null && !resultLabel.isBlank()) {
            return resultLabel.trim();
        }
        if (teamScore == null || opponentScore == null) {
            return null;
        }
        return teamScore + ":" + opponentScore;
    }
}