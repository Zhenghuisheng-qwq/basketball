package com.zhs.support.v4;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zhs.config.StatsVisionProperties;
import com.zhs.exception.ServiceException;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Locale;
import java.util.Set;

/**
 * 把 2K 结算截图读成双方技术统计，再用这场已经写入赛事的球队名和名单做对应。
 * 模型给出的球员 ID 不直接采信，避免把人配到另一支队。
 */
@Component
public class StatsScreenshotRecognizer {

    private static final int MAX_BYTES = 8 * 1024 * 1024;
    private static final int MATCH_FLOOR = 90;

    private final StatsVisionProperties properties;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(15))
            .build();

    public StatsScreenshotRecognizer(StatsVisionProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public StatsScreenshotResult recognize(byte[] image, String mime, SideRoster home, SideRoster away) {
        if (!properties.isEnabled()) {
            throw new ServiceException("截图识别未开启");
        }
        String apiKey = properties.getApiKey() == null ? "" : properties.getApiKey().trim();
        if (apiKey.isEmpty()) {
            throw new ServiceException("截图识别还没配置密钥。请设置环境变量 CNPA_STATS_VISION_API_KEY");
        }
        if (image == null || image.length < 32) {
            throw new ServiceException("请上传比赛结算截图");
        }
        if (image.length > MAX_BYTES) {
            throw new ServiceException("截图不能超过 8MB");
        }
        String contentType = normalizeMime(mime, image);
        JsonNode vision = callVision(image, contentType, home, away);
        return align(vision, home, away);
    }

    private JsonNode callVision(byte[] image, String mime, SideRoster home, SideRoster away) {
        String base = properties.getBaseUrl() == null ? "" : properties.getBaseUrl().trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.isEmpty()) {
            throw new ServiceException("截图识别未配置接口地址");
        }
        String url = base.endsWith("/chat/completions") ? base : base + "/chat/completions";
        String model = properties.getModel() == null || properties.getModel().isBlank()
                ? "qwen-vl-plus" : properties.getModel().trim();
        int timeout = Math.max(20, properties.getTimeoutSeconds());

        ObjectNode body = objectMapper.createObjectNode();
        body.put("model", model);
        body.put("temperature", 0);
        int maxTokens = properties.getMaxTokens() > 0 ? properties.getMaxTokens() : 4096;
        body.put("max_tokens", maxTokens);
        body.put("enable_thinking", false);
        ArrayNode messages = body.putArray("messages");
        ObjectNode user = messages.addObject();
        user.put("role", "user");
        ArrayNode content = user.putArray("content");
        content.addObject().put("type", "text").put("text", prompt(home, away));
        content.addObject()
                .put("type", "image_url")
                .putObject("image_url")
                .put("url", "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(image));

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(timeout))
                .header("Authorization", "Bearer " + properties.getApiKey().trim())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ServiceException("截图识别被中断，请再试一次");
        } catch (Exception ex) {
            throw new ServiceException("截图识别服务连不上，请稍后再试");
        }
        if (response.statusCode() < 200 || response.statusCode() >= 300) {
            throw new ServiceException("截图识别失败（" + response.statusCode() + "）。" + brief(response.body()));
        }
        try {
            JsonNode root = objectMapper.readTree(response.body());
            String text = messageText(root.path("choices").path(0).path("message").path("content"));
            String json = extractJson(text);
            if (json.isEmpty()) {
                throw new ServiceException("没有从截图里读出比赛数据，请换一张完整的结算图");
            }
            return objectMapper.readTree(json);
        } catch (ServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new ServiceException("没有从截图里读出比赛数据，请换一张完整的结算图");
        }
    }

    private StatsScreenshotResult align(JsonNode vision, SideRoster home, SideRoster away) {
        List<RawPlayer> imageLeft = readPlayers(vision.path("homePlayers"));
        List<RawPlayer> imageRight = readPlayers(vision.path("awayPlayers"));
        for (RawPlayer row : imageLeft) {
            row.imageLeft = true;
        }
        List<int[]> periods = readPeriods(vision.path("periods"));
        if (imageLeft.isEmpty() && imageRight.isEmpty()) {
            throw new ServiceException("没有从截图里读到球员数据，请换一张能看清游戏 ID 和技术统计的结算图");
        }

        List<PlayerRef> homeRoster = home.players() == null ? List.of() : home.players();
        List<PlayerRef> awayRoster = away.players() == null ? List.of() : away.players();
        List<PlayerRef> combined = new ArrayList<>(homeRoster.size() + awayRoster.size());
        combined.addAll(homeRoster);
        combined.addAll(awayRoster);
        List<RawPlayer> raws = new ArrayList<>(imageLeft.size() + imageRight.size());
        raws.addAll(imageLeft);
        raws.addAll(imageRight);
        List<Assignment> assigned = matchSide(raws, combined);
        dropDuplicateNames(assigned);

        Set<Long> homeIds = idsOf(homeRoster);
        Set<Long> awayIds = idsOf(awayRoster);
        int leftHome = 0;
        int leftAway = 0;
        int rightHome = 0;
        int rightAway = 0;
        for (Assignment row : assigned) {
            if (row.player == null) {
                continue;
            }
            boolean onHome = homeIds.contains(row.player.eventPlayerId());
            if (row.raw.imageLeft) {
                if (onHome) {
                    leftHome++;
                } else if (awayIds.contains(row.player.eventPlayerId())) {
                    leftAway++;
                }
            } else if (onHome) {
                rightHome++;
            } else if (awayIds.contains(row.player.eventPlayerId())) {
                rightAway++;
            }
        }
        boolean imageLeftIsAway = leftAway > leftHome
                || (leftHome + leftAway == 0 && rightHome > rightAway);
        if (imageLeftIsAway) {
            for (int[] period : periods) {
                int leftScore = period[1];
                period[1] = period[2];
                period[2] = leftScore;
            }
        }

        List<Assignment> officialHome = new ArrayList<>();
        List<Assignment> officialAway = new ArrayList<>();
        for (Assignment row : assigned) {
            if (row.player != null && homeIds.contains(row.player.eventPlayerId())) {
                officialHome.add(row);
            } else if (row.player != null && awayIds.contains(row.player.eventPlayerId())) {
                officialAway.add(row);
            } else if (row.raw.imageLeft != imageLeftIsAway) {
                officialHome.add(row);
            } else {
                officialAway.add(row);
            }
        }

        List<String> warnings = new ArrayList<>();
        List<StatsScreenshotResult.PlayerStat> homePlayers = toStats(officialHome);
        List<StatsScreenshotResult.PlayerStat> awayPlayers = toStats(officialAway);
        int unmatched = (int) homePlayers.stream().filter(row -> !row.matched()).count()
                + (int) awayPlayers.stream().filter(row -> !row.matched()).count();
        if (unmatched > 0) {
            warnings.add(unmatched + " 个游戏 ID 没对上名单，请在下拉框里改");
        }
        List<StatsScreenshotResult.PeriodScore> periodScores = new ArrayList<>();
        int homePeriods = 0;
        int awayPeriods = 0;
        for (int[] period : periods) {
            periodScores.add(new StatsScreenshotResult.PeriodScore(period[0], period[1], period[2]));
            homePeriods += period[1];
            awayPeriods += period[2];
        }
        int homePoints = sumPoints(homePlayers);
        int awayPoints = sumPoints(awayPlayers);
        if (!periodScores.isEmpty() && (homePeriods != homePoints || awayPeriods != awayPoints)) {
            warnings.add("球员得分合计和节比分不一致，请对照截图改数字");
        }
        return new StatsScreenshotResult(
                homePoints,
                awayPoints,
                periodScores,
                homePlayers,
                awayPlayers,
                unmatched,
                warnings);
    }

    private static List<StatsScreenshotResult.PlayerStat> toStats(List<Assignment> rows) {
        List<StatsScreenshotResult.PlayerStat> result = new ArrayList<>();
        for (Assignment row : rows) {
            RawPlayer raw = row.raw;
            boolean matched = row.player != null;
            result.add(new StatsScreenshotResult.PlayerStat(
                    matched ? row.player.eventPlayerId() : null,
                    matched || raw.name == null ? "" : raw.name.trim(),
                    matched,
                    raw.points, raw.rebound, raw.assist, raw.steal, raw.block, raw.turnover, raw.foul,
                    raw.fieldGoalMade, raw.fieldGoalAttempt, raw.threePointMade, raw.threePointAttempt,
                    raw.freeThrowMade, raw.freeThrowAttempt));
        }
        return result;
    }

    private static Set<Long> idsOf(List<PlayerRef> players) {
        Set<Long> ids = new HashSet<>();
        for (PlayerRef player : players) {
            if (player != null && player.eventPlayerId() != null) {
                ids.add(player.eventPlayerId());
            }
        }
        return ids;
    }

    private static int sumPoints(List<StatsScreenshotResult.PlayerStat> rows) {
        int sum = 0;
        for (StatsScreenshotResult.PlayerStat row : rows) {
            sum += row.points();
        }
        return sum;
    }

    private static List<Assignment> matchSide(List<RawPlayer> raws, List<PlayerRef> roster) {
        boolean[] rawUsed = new boolean[raws.size()];
        boolean[] rosterUsed = new boolean[roster.size()];
        Long[] chosen = new Long[raws.size()];
        List<Candidate> candidates = new ArrayList<>();
        for (int i = 0; i < raws.size(); i++) {
            for (int j = 0; j < roster.size(); j++) {
                int score = nameScore(raws.get(i).name, roster.get(j));
                if (score >= MATCH_FLOOR) {
                    candidates.add(new Candidate(i, j, score));
                }
            }
        }
        candidates.sort(Comparator.comparingInt((Candidate item) -> item.score).reversed());
        for (Candidate candidate : candidates) {
            if (rawUsed[candidate.rawIndex] || rosterUsed[candidate.rosterIndex]) {
                continue;
            }
            rawUsed[candidate.rawIndex] = true;
            rosterUsed[candidate.rosterIndex] = true;
            chosen[candidate.rawIndex] = roster.get(candidate.rosterIndex).eventPlayerId();
        }
        List<Assignment> result = new ArrayList<>();
        for (int i = 0; i < raws.size(); i++) {
            PlayerRef player = null;
            if (chosen[i] != null) {
                for (PlayerRef item : roster) {
                    if (chosen[i].equals(item.eventPlayerId())) {
                        player = item;
                        break;
                    }
                }
            }
            result.add(new Assignment(raws.get(i), player));
        }
        return result;
    }

    static int nameScore(String recognized, PlayerRef player) {
        List<String> recognizedKeys = gameIdKeys(recognized);
        List<String> gameKeys = gameIdKeys(player == null ? null : player.gameId());
        if (recognizedKeys.isEmpty() || gameKeys.isEmpty()) {
            return 0;
        }
        int best = 0;
        for (String key : recognizedKeys) {
            for (String gameId : gameKeys) {
                if (key.equals(gameId)) {
                    return 100;
                }
                best = Math.max(best, exactOrContains(key, gameId, 100, 90));
                int similar = similarity(key, gameId);
                if (similar >= 92) {
                    best = Math.max(best, similar);
                }
            }
        }
        return best;
    }

    private static List<String> gameIdKeys(String recognized) {
        List<String> keys = new ArrayList<>();
        if (recognized == null || recognized.isBlank()) {
            return keys;
        }
        String trimmed = recognized.trim();
        addKey(keys, normalize(trimmed));
        int cut = delimiter(trimmed);
        if (cut > 0) {
            addKey(keys, normalize(trimmed.substring(0, cut)));
        }
        for (String part : trimmed.split("[\\s/|,，、]+")) {
            addKey(keys, normalize(part));
        }
        return keys;
    }

    private static void addKey(List<String> keys, String key) {
        if (key != null && key.length() >= 2 && !keys.contains(key)) {
            keys.add(key);
        }
    }

    private static int delimiter(String value) {
        for (int i = 0; i < value.length(); i++) {
            char ch = value.charAt(i);
            if (ch == '(' || ch == '（' || ch == '[' || ch == '【') {
                return i;
            }
        }
        return -1;
    }

    private static int exactOrContains(String raw, String candidate, int exact, int contains) {
        if (candidate.isEmpty()) {
            return 0;
        }
        if (raw.equals(candidate)) {
            return exact;
        }
        int shorter = Math.min(raw.length(), candidate.length());
        int longer = Math.max(raw.length(), candidate.length());
        if (shorter >= 4 && (raw.contains(candidate) || candidate.contains(raw)) && shorter * 10 >= longer * 6) {
            return contains;
        }
        return 0;
    }

    static String normalize(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder out = new StringBuilder(value.length());
        for (int i = 0; i < value.length(); ) {
            int cp = value.codePointAt(i);
            i += Character.charCount(cp);
            if (Character.isLetterOrDigit(cp)) {
                out.appendCodePoint(Character.toLowerCase(cp));
            }
        }
        return out.toString();
    }

    static int similarity(String left, String right) {
        if (left.isEmpty() || right.isEmpty()) {
            return 0;
        }
        int max = Math.max(left.length(), right.length());
        if (max > 64) {
            left = left.substring(0, Math.min(64, left.length()));
            right = right.substring(0, Math.min(64, right.length()));
            max = Math.max(left.length(), right.length());
        }
        int dist = levenshtein(left, right);
        return (int) Math.round((1.0 - (double) dist / max) * 100);
    }

    private static int levenshtein(String left, String right) {
        int[] prev = new int[right.length() + 1];
        int[] curr = new int[right.length() + 1];
        for (int j = 0; j <= right.length(); j++) {
            prev[j] = j;
        }
        for (int i = 1; i <= left.length(); i++) {
            curr[0] = i;
            for (int j = 1; j <= right.length(); j++) {
                int cost = left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1;
                curr[j] = Math.min(Math.min(curr[j - 1] + 1, prev[j] + 1), prev[j - 1] + cost);
            }
            int[] swap = prev;
            prev = curr;
            curr = swap;
        }
        return prev[right.length()];
    }

    private static void dropDuplicateNames(List<Assignment> assigned) {
        Map<String, List<Assignment>> byName = new LinkedHashMap<>();
        for (Assignment row : assigned) {
            String key = normalize(row.raw.name);
            if (key.length() < 2) {
                continue;
            }
            byName.computeIfAbsent(key, ignored -> new ArrayList<>()).add(row);
        }
        for (List<Assignment> group : byName.values()) {
            if (group.size() < 2) {
                continue;
            }
            for (Assignment row : group) {
                row.player = null;
            }
        }
    }

    private String prompt(SideRoster home, SideRoster away) {
        return """
                你是篮球游戏结算截图识别器。图片一般是 NBA 2K 的 GAME STATS。
                只输出一个 JSON 对象，不要 markdown，不要解释。
                图上的球员名字就是游戏 ID。图上有几名有数据的球员就输出几名，不要漏人，也不要编造。
                截图上的队名不要输出，也不要用来判断左右。
                homePlayers 和 periods.homeScore 必须是画面左边或上方那一队；awayPlayers 和 periods.awayScore 必须是右边或下方那一队。不要对调，也不要按游戏 ID 把球员换边。
                下面的游戏 ID 只用来辨认模糊文字。图上的字如果就是其中某一个，按名单写法输出。对不上任何一个时，按图上的字符原样输出，禁止改成名单里的另一个人。
                points 抄 PTS 列，不要用命中数反推。rebound、assist、steal、block、turnover、foul 分别抄 REB、AST、STL、BLK、TO、PF。
                FG、3PT、FT 如果图上是「命中/出手」，拆成 made 和 attempt。看不清的格子填 0。FGM 是总投篮命中，含三分。
                节比分能看到 Q1-Q4 或加时就写入 periods，periodNo 从 1 开始；看不到就给空数组。
                写完核对：同一队所有球员的 points 相加，必须等于该队各节比分相加。不相等就重看 PTS 列，不要改节比分来凑。

                本场游戏 ID：
                %s

                JSON 格式：
                {"periods":[{"periodNo":1,"homeScore":0,"awayScore":0}],"homePlayers":[{"recognizedName":"","points":0,"rebound":0,"assist":0,"steal":0,"block":0,"turnover":0,"foul":0,"fieldGoalMade":0,"fieldGoalAttempt":0,"threePointMade":0,"threePointAttempt":0,"freeThrowMade":0,"freeThrowAttempt":0}],"awayPlayers":[]}
                """.formatted(rosterLines(home, away));
    }

    private static String rosterLines(SideRoster home, SideRoster away) {
        StringBuilder out = new StringBuilder();
        appendGameIds(out, home);
        appendGameIds(out, away);
        if (out.isEmpty()) {
            return "（这场还没有写入游戏 ID）";
        }
        return out.toString().trim();
    }

    private static void appendGameIds(StringBuilder out, SideRoster side) {
        if (side == null || side.players() == null) {
            return;
        }
        int count = 0;
        for (PlayerRef player : side.players()) {
            if (count >= 20) {
                break;
            }
            String gameId = player == null || player.gameId() == null ? "" : player.gameId().replace('\n', ' ').trim();
            if (gameId.isBlank()) {
                continue;
            }
            count++;
            out.append("- ").append(gameId).append('\n');
        }
    }

    private List<RawPlayer> readPlayers(JsonNode node) {
        List<RawPlayer> rows = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return rows;
        }
        for (JsonNode item : node) {
            if (rows.size() >= 15 || item == null || !item.isObject()) {
                continue;
            }
            RawPlayer row = new RawPlayer();
            row.name = firstText(item, "recognizedName", "name", "playerName", "gameId");
            row.points = num(item, "points", "pts");
            row.rebound = num(item, "rebound", "rebounds", "reb");
            row.assist = num(item, "assist", "assists", "ast");
            row.steal = num(item, "steal", "steals", "stl");
            row.block = num(item, "block", "blocks", "blk");
            row.turnover = num(item, "turnover", "turnovers", "to");
            row.foul = num(item, "foul", "fouls", "pf");
            row.fieldGoalMade = num(item, "fieldGoalMade", "fgm");
            row.fieldGoalAttempt = num(item, "fieldGoalAttempt", "fga");
            row.threePointMade = num(item, "threePointMade", "tpm", "threePm");
            row.threePointAttempt = num(item, "threePointAttempt", "tpa", "threePa");
            row.freeThrowMade = num(item, "freeThrowMade", "ftm");
            row.freeThrowAttempt = num(item, "freeThrowAttempt", "fta");
            applySlash(item, row, "fg", "fieldGoal", "fieldGoal");
            applySlash(item, row, "threePoint", "3pt", "three");
            applySlash(item, row, "ft", "freeThrow", "free");
            applyMadeSlash(item, row);
            if (row.fieldGoalMade == 0 && row.fieldGoalAttempt == 0 && row.points == 0
                    && row.rebound == 0 && row.assist == 0 && (row.name == null || row.name.isBlank())) {
                continue;
            }
            rows.add(row);
        }
        return rows;
    }

    private static void applySlash(JsonNode item, RawPlayer row, String primary, String alias, String kind) {
        String text = firstText(item, primary, alias);
        writeSlash(row, kind, text);
    }

    private static void applyMadeSlash(JsonNode item, RawPlayer row) {
        writeSlash(row, "fieldGoal", firstText(item, "fieldGoalMade", "fgm"));
        writeSlash(row, "three", firstText(item, "threePointMade", "tpm"));
        writeSlash(row, "free", firstText(item, "freeThrowMade", "ftm"));
    }

    private static void writeSlash(RawPlayer row, String kind, String text) {
        if (text == null || !text.contains("/")) {
            return;
        }
        String[] parts = text.split("/");
        int made = parseInt(parts.length > 0 ? parts[0] : "");
        int attempt = parseInt(parts.length > 1 ? parts[1] : "");
        if ("three".equals(kind)) {
            row.threePointMade = made;
            row.threePointAttempt = attempt;
            return;
        }
        if ("free".equals(kind)) {
            row.freeThrowMade = made;
            row.freeThrowAttempt = attempt;
            return;
        }
        row.fieldGoalMade = made;
        row.fieldGoalAttempt = attempt;
    }

    private List<int[]> readPeriods(JsonNode node) {
        List<int[]> rows = new ArrayList<>();
        if (node == null || !node.isArray()) {
            return rows;
        }
        for (JsonNode item : node) {
            int periodNo = num(item, "periodNo", "quarter");
            if (periodNo <= 0 || periodNo > 10) {
                continue;
            }
            rows.add(new int[] {periodNo, num(item, "homeScore", "home"), num(item, "awayScore", "away")});
        }
        rows.sort(Comparator.comparingInt(item -> item[0]));
        return rows;
    }

    private static int num(JsonNode node, String... fields) {
        if (node == null) {
            return 0;
        }
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value == null || value.isNull()) {
                continue;
            }
            if (value.isNumber()) {
                return Math.max(0, value.asInt());
            }
            int parsed = parseInt(value.asText(""));
            if (parsed > 0 || value.asText("").trim().startsWith("0")) {
                return parsed;
            }
        }
        return 0;
    }

    private static int parseInt(String raw) {
        if (raw == null) {
            return 0;
        }
        String digits = raw.trim();
        int slash = digits.indexOf('/');
        if (slash >= 0) {
            digits = digits.substring(0, slash);
        }
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < digits.length(); i++) {
            char ch = digits.charAt(i);
            if (ch >= '0' && ch <= '9') {
                out.append(ch);
            } else if (out.length() > 0) {
                break;
            }
        }
        if (out.isEmpty()) {
            return 0;
        }
        try {
            return Math.max(0, Integer.parseInt(out.toString()));
        } catch (NumberFormatException ex) {
            return 0;
        }
    }

    private static String firstText(JsonNode node, String... fields) {
        if (node == null) {
            return "";
        }
        for (String field : fields) {
            JsonNode value = node.get(field);
            if (value != null && value.isTextual() && !value.asText().isBlank()) {
                return value.asText().trim();
            }
        }
        return "";
    }

    private static String messageText(JsonNode content) {
        if (content == null || content.isMissingNode() || content.isNull()) {
            return "";
        }
        if (content.isTextual()) {
            return content.asText("");
        }
        if (content.isArray()) {
            StringBuilder out = new StringBuilder();
            for (JsonNode part : content) {
                if (part.hasNonNull("text")) {
                    out.append(part.get("text").asText());
                } else if (part.isTextual()) {
                    out.append(part.asText());
                }
            }
            return out.toString();
        }
        return content.toString();
    }

    private static String brief(String body) {
        if (body == null || body.isBlank()) {
            return "请确认模型名称和密钥";
        }
        String text = body.replace('\n', ' ').replace('\r', ' ').trim();
        if (text.length() > 180) {
            text = text.substring(0, 180);
        }
        return text;
    }

    private static String extractJson(String text) {
        if (text == null) {
            return "";
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start < 0 || end <= start) {
            return "";
        }
        return text.substring(start, end + 1);
    }

    private static String normalizeMime(String mime, byte[] image) {
        String value = mime == null ? "" : mime.toLowerCase(Locale.ROOT);
        if (value.startsWith("image/jpeg") || value.startsWith("image/png") || value.startsWith("image/webp") || value.startsWith("image/gif")) {
            return value.startsWith("image/jpg") ? "image/jpeg" : value.split(";")[0];
        }
        if (image.length >= 3 && (image[0] & 0xFF) == 0xFF && (image[1] & 0xFF) == 0xD8) {
            return "image/jpeg";
        }
        if (image.length >= 4 && (image[0] & 0xFF) == 0x89 && image[1] == 'P' && image[2] == 'N' && image[3] == 'G') {
            return "image/png";
        }
        if (image.length >= 6 && image[0] == 'G' && image[1] == 'I' && image[2] == 'F') {
            return "image/gif";
        }
        if (image.length >= 12 && image[0] == 'R' && image[1] == 'I' && image[2] == 'F' && image[3] == 'F') {
            return "image/webp";
        }
        throw new ServiceException("请上传 jpg、png 或 webp 截图");
    }

    public record SideRoster(String teamName, List<PlayerRef> players) {
    }

    public record PlayerRef(Long eventPlayerId, String playerName, String gameId, String jerseyNo) {
    }

    private static final class RawPlayer {
        private boolean imageLeft;
        private String name;
        private int points;
        private int rebound;
        private int assist;
        private int steal;
        private int block;
        private int turnover;
        private int foul;
        private int fieldGoalMade;
        private int fieldGoalAttempt;
        private int threePointMade;
        private int threePointAttempt;
        private int freeThrowMade;
        private int freeThrowAttempt;
    }

    private static final class Assignment {
        private final RawPlayer raw;
        private PlayerRef player;

        private Assignment(RawPlayer raw, PlayerRef player) {
            this.raw = raw;
            this.player = player;
        }
    }

    private record Candidate(int rawIndex, int rosterIndex, int score) {
    }
}
