from pathlib import Path

ROOT = Path(r"d:\code\dz\basketball\scaffolding-v2\basketball-web-ui\src\views\events\match-detail")


def ensure_utf8(path: Path) -> None:
    raw = path.read_bytes()
    if len(raw) > 3 and raw[1] == 0:
        text = raw.decode("utf-16")
        tmp = path.with_suffix(path.suffix + ".tmp")
        tmp.write_bytes(text.encode("utf-8"))
        path.write_bytes(tmp.read_bytes())
        tmp.unlink()


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise SystemExit(f"missing {label}")
    return text.replace(old, new, 1)


def patch_scoreboard() -> None:
    path = ROOT / "MatchScoreboard.vue"
    text = path.read_text(encoding="utf-8")
    text = replace_once(
        text,
        "  metaText?: string\n  caption?: string\n}>()",
        "  metaText?: string\n  caption?: string\n  advanceLabel?: string\n}>()",
        "scoreboard props",
    )
    text = replace_once(
        text,
        "        {{ leftTeam.name }}\n      </strong>\n    </TeamEntryLink>",
        "        {{ leftTeam.name }}\n      </strong>\n      <span\n        v-if=\"advanceLabel && isWinner(leftTeam, winnerEventTeamId)\"\n        class=\"match-scoreboard__advance\"\n      >{{ advanceLabel }}</span>\n    </TeamEntryLink>",
        "left badge",
    )
    text = replace_once(
        text,
        "        {{ rightTeam.name }}\n      </strong>\n    </TeamEntryLink>",
        "        {{ rightTeam.name }}\n      </strong>\n      <span\n        v-if=\"advanceLabel && isWinner(rightTeam, winnerEventTeamId)\"\n        class=\"match-scoreboard__advance\"\n      >{{ advanceLabel }}</span>\n    </TeamEntryLink>",
        "right badge",
    )
    text = replace_once(
        text,
        ".match-scoreboard__name.is-loser,",
        """.match-scoreboard__advance {
  display: inline-flex;
  align-items: center;
  min-height: 20px;
  padding: 0 8px;
  border: 1px solid var(--cnpa-red, #e51422);
  color: var(--cnpa-red, #e51422);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.08em;
  line-height: 1;
}

.match-scoreboard__name.is-loser,""",
        "advance css",
    )
    path.write_text(text, encoding="utf-8", newline="\n")


def patch_tabs() -> None:
    path = ROOT / "SeriesGameTabs.vue"
    text = path.read_text(encoding="utf-8")
    text = replace_once(
        text,
        "defineProps<{\n  tabs: SeriesTab[]\n  modelValue: string\n}>()",
        "defineProps<{\n  tabs: SeriesTab[]\n  modelValue: string\n  primaryKey?: string\n}>()",
        "tabs props",
    )
    text = replace_once(
        text,
        """      class=\"series-game-tabs__btn\"
      :class=\"{ 'is-active': modelValue === tab.key }\"""",
        """      class=\"series-game-tabs__btn\"
      :class=\"{
        'is-active': modelValue === tab.key,
        'is-primary': tab.key === (primaryKey || SERIES_TOTAL_TAB),
      }\"""",
        "tabs class",
    )
    text = replace_once(
        text,
        "  min-height: 36px;\n  padding: 0 16px;",
        "  min-height: 34px;\n  padding: 0 12px;",
        "tabs padding",
    )
    text = replace_once(
        text,
        "  &.is-active {\n    background: var(--cnpa-red, #e51422);\n    color: #fff;\n  }\n}",
        "  &.is-primary {\n    min-width: 108px;\n    padding: 0 16px;\n  }\n\n  &.is-active {\n    background: var(--cnpa-red, #e51422);\n    color: #fff;\n  }\n}",
        "tabs primary css",
    )
    path.write_text(text, encoding="utf-8", newline="\n")


def patch_box() -> None:
    path = ROOT / "BoxScorePanel.vue"
    text = path.read_text(encoding="utf-8")
    text = replace_once(
        text,
        """const props = defineProps<{
  teams: Team[]
  selectedTeamId: string
  players: V4PublicMatchPlayerStatItem[]
  teamStats?: V4PublicMatchTeamStatItem[]
}>()""",
        """const props = withDefaults(defineProps<{
  teams: Team[]
  selectedTeamId: string
  players: V4PublicMatchPlayerStatItem[]
  teamStats?: V4PublicMatchTeamStatItem[]
  title?: string
  subtitle?: string
  hint?: string
}>(), {
  title: '\u6bd4\u8d5b\u6280\u672f\u7edf\u8ba1',
  subtitle: 'BOX SCORE',
  hint: '',
})""",
        "box props",
    )
    text = replace_once(
        text,
        "        <h2>\u6bd4\u8d5b\u6280\u672f\u7edf\u8ba1</h2>\n        <span>BOX SCORE</span>",
        "        <h2>{{ title }}</h2>\n        <span>{{ subtitle }}</span>\n        <small v-if=\"hint\">{{ hint }}</small>",
        "box title",
    )
    text = replace_once(
        text,
        """  span {
    display: block;
    margin-top: 4px;
    color: var(--cnpa-red, #e51422);
    font-size: 12px;
    font-weight: 800;
    letter-spacing: 1.4px;
  }
}""",
        """  span {
    display: block;
    margin-top: 4px;
    color: var(--cnpa-red, #e51422);
    font-size: 12px;
    font-weight: 800;
    letter-spacing: 1.4px;
  }

  small {
    display: block;
    margin-top: 6px;
    color: #888;
    font-size: 12px;
    font-weight: 600;
    letter-spacing: 0;
  }
}""",
        "box hint css",
    )
    path.write_text(text, encoding="utf-8", newline="\n")


if __name__ == "__main__":
    ensure_utf8(ROOT / "SeriesProgress.vue")
    patch_scoreboard()
    patch_tabs()
    patch_box()
    print("patched ok")
