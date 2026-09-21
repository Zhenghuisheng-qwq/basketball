package com.zhs.basket.support.v4;

import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

class FreshDbInitChainR9Test {

    @Test
    void matchSubmissionMigrationIncludesScoreColumns() throws Exception {
        Path cwd = Path.of(System.getProperty("user.dir")).toAbsolutePath().normalize();
        Path root = cwd;
        if (!Files.exists(root.resolve("docs/sql/cnpa-v13-r9-final-migration.sql"))) {
            root = cwd.getParent();
        }
        String migration = readFlexible(root.resolve("docs/sql/cnpa-v13-r9-final-migration.sql"));
        String patch = readFlexible(root.resolve("docs/sql/cnpa-v4-round5-match-data-patch.sql"));

        assertTrue(patch.contains("home_score"));
        assertTrue(patch.contains("away_score"));
        assertTrue(patch.contains("submit_remark"));
        assertTrue(migration.contains("home_score"));
        assertTrue(migration.contains("away_score"));
        assertTrue(migration.contains("submit_remark"));
    }

    private static String readFlexible(Path path) throws Exception {
        byte[] raw = Files.readAllBytes(path);
        if (raw.length >= 2 && raw[0] == (byte) 0xff && raw[1] == (byte) 0xfe) {
            return new String(raw, Charset.forName("UTF-16LE")).replace("\u0000", "");
        }
        return new String(raw, StandardCharsets.UTF_8).replace("\u0000", "");
    }
}