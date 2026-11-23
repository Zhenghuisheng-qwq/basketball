package com.zhs.basket.support.v4;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 手动探测结算截图识别的密钥和模型。
 * 默认不跑，避免每次构建都打外网：
 * mvn -pl basketball-admin -Dtest=StatsVisionApiKeyTest -Dcnpa.stats.vision.live=true test
 */
class StatsVisionApiKeyTest {

    @Test
    @EnabledIfSystemProperty(named = "cnpa.stats.vision.live", matches = "true")
    void newKeyCanCallVisionModel() throws Exception {
        VisionConfig config = VisionConfig.fromApplicationYml();
        String url = config.baseUrl.endsWith("/chat/completions")
                ? config.baseUrl
                : config.baseUrl + "/chat/completions";
        String body = """
                {
                  "model": "%s",
                  "temperature": 0,
                  "max_tokens": 32,
                  "enable_thinking": false,
                  "messages": [{
                    "role": "user",
                    "content": [
                      {"type": "text", "text": "图里有什么？只回复 OK"},
                      {"type": "image_url", "image_url": {"url": "data:image/png;base64,%s"}}
                    ]
                  }]
                }
                """.formatted(config.model, samplePng());

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(120))
                .header("Authorization", "Bearer " + config.apiKey)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        HttpResponse<String> response = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(15))
                .build()
                .send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        String text = response.body() == null ? "" : response.body().replace('\n', ' ');
        if (text.length() > 500) {
            text = text.substring(0, 500);
        }
        System.out.println("stats-vision status=" + response.statusCode()
                + " model=" + config.model
                + " keyTail=" + tail(config.apiKey));
        System.out.println(text);

        assertTrue(response.statusCode() >= 200 && response.statusCode() < 300,
                "截图识别接口没有成功，HTTP " + response.statusCode() + " " + text);
    }

    private static String samplePng() throws Exception {
        BufferedImage image = new BufferedImage(32, 32, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = image.createGraphics();
        graphics.setColor(Color.WHITE);
        graphics.fillRect(0, 0, 32, 32);
        graphics.setColor(Color.BLACK);
        graphics.drawString("OK", 6, 20);
        graphics.dispose();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return Base64.getEncoder().encodeToString(out.toByteArray());
    }

    private static String tail(String apiKey) {
        if (apiKey.length() <= 6) {
            return "****";
        }
        return "…" + apiKey.substring(apiKey.length() - 6);
    }

    private record VisionConfig(String baseUrl, String apiKey, String model) {

        private static VisionConfig fromApplicationYml() throws Exception {
            Path yml = Path.of("src/main/resources/application.yml");
            if (!Files.isRegularFile(yml)) {
                yml = Path.of("basketball-admin/src/main/resources/application.yml");
            }
            String text = Files.readString(yml, StandardCharsets.UTF_8);
            String baseUrl = resolve(placeholder(text, "base-url"),
                    "https://ws-sauyaoq4apm7zyhc.cn-beijing.maas.aliyuncs.com/compatible-mode/v1");
            String apiKey = resolve(placeholder(text, "api-key"), "");
            String model = resolve(placeholder(text, "model"), "qwen3.7-plus");
            if (apiKey.isBlank()) {
                throw new IllegalStateException("application.yml 里没有 cnpa.stats-vision.api-key");
            }
            while (baseUrl.endsWith("/")) {
                baseUrl = baseUrl.substring(0, baseUrl.length() - 1);
            }
            return new VisionConfig(baseUrl, apiKey, model);
        }

        private static String placeholder(String yml, String key) {
            Matcher matcher = Pattern.compile("(?m)^\\s*" + Pattern.quote(key) + ":\\s*(.+)$").matcher(yml);
            if (!matcher.find()) {
                return "";
            }
            return matcher.group(1).trim();
        }

        private static String resolve(String raw, String fallback) {
            Matcher matcher = Pattern.compile("\\$\\{([^:}]+)(?::([^}]*))?}").matcher(raw);
            if (!matcher.find()) {
                return raw.isBlank() ? fallback : stripQuotes(raw);
            }
            String env = System.getenv(matcher.group(1));
            if (env != null && !env.isBlank()) {
                return env.trim();
            }
            String embedded = matcher.group(2);
            if (embedded == null || embedded.isBlank()) {
                return fallback;
            }
            return stripQuotes(embedded);
        }

        private static String stripQuotes(String value) {
            String text = value.trim();
            if ((text.startsWith("\"") && text.endsWith("\"")) || (text.startsWith("'") && text.endsWith("'"))) {
                return text.substring(1, text.length() - 1);
            }
            return text;
        }
    }
}
