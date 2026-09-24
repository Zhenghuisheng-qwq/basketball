package com.zhs.config;

/**
 * C-end rate-limit groups. AUTH is an extra IP layer on top of existing email lock.
 */
public final class WebRateLimitClassifier {

    public static final String PUBLIC_NORMAL = "PUBLIC_NORMAL";
    public static final String PUBLIC_HIGH_COST = "PUBLIC_HIGH_COST";
    public static final String USER_WRITE = "USER_WRITE";
    public static final String AUTH = "AUTH";

    private WebRateLimitClassifier() {
    }

    public static String classify(String method, String requestUri) {
        String path = GeneratedCrudPaths.normalizePath(requestUri);
        if (path == null) {
            return null;
        }
        String verb = method == null ? "GET" : method.toUpperCase();

        if (isAuth(path)) {
            return AUTH;
        }
        if (isUserWrite(verb, path)) {
            return USER_WRITE;
        }
        if (isPublicHighCost(path)) {
            return PUBLIC_HIGH_COST;
        }
        if (isPublicNormal(path)) {
            return PUBLIC_NORMAL;
        }
        return null;
    }

    static boolean isAuth(String path) {
        return path.startsWith("/basket/member/auth/login/")
                || path.equals("/basket/member/auth/register")
                || path.equals("/basket/member/auth/password/reset")
                || path.equals("/basket/member/auth/email-code/send");
    }

    static boolean isUserWrite(String method, String path) {
        if (!("POST".equals(method) || "PUT".equals(method) || "DELETE".equals(method))) {
            return false;
        }
        if (path.equals("/basket/team/create")
                || path.equals("/basket/team/captain/grant/bind-team")
                || path.endsWith("/submit-certification")
                || path.endsWith("/logo")
                || path.endsWith("/leave")
                || path.contains("/profile/change/submit")
                || path.contains("/personal-formation/bind-team")
                || path.endsWith("/create-team")
                || path.endsWith("/confirm")) {
            return true;
        }
        if (path.startsWith("/basket/v4/player/registration/")
                || path.startsWith("/basket/v4/player/personal-registration/")
                || path.startsWith("/basket/v4/player/match-submission/")
                || path.startsWith("/basket/v4/player/series-submission/")) {
            return path.endsWith("/save")
                    || path.endsWith("/submit")
                    || path.endsWith("/resubmit")
                    || path.endsWith("/withdraw")
                    || path.endsWith("/declare-score")
                    || path.endsWith("/save-game");
        }
        if (path.startsWith("/basket/team/") && (path.contains("/member/") || path.contains("/invitation")
                || path.contains("/join-application"))) {
            return true;
        }
        return path.startsWith("/basket/team/") && path.matches("/basket/team/\\d+");
    }

    static boolean isPublicHighCost(String path) {
        return path.startsWith("/basket/v4/public/match")
                || path.startsWith("/basket/v4/public/match-series")
                || path.startsWith("/basket/v4/public/stats")
                || path.startsWith("/basket/v4/public/stage")
                || path.startsWith("/basket/public/data-center")
                || path.equals("/basket/v4/public/team/list")
                || path.endsWith("/schedule")
                || path.endsWith("/standings")
                || path.endsWith("/bracket")
                || path.contains("/match/page")
                || path.contains("/compare")
                || path.endsWith("/recent-match")
                || path.endsWith("/team-ranking")
                || path.endsWith("/player-ranking");
    }

    static boolean isPublicNormal(String path) {
        return path.startsWith("/basket/public/")
                || path.startsWith("/basket/v4/public/");
    }
}