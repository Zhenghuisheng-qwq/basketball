from pathlib import Path

PAGE = Path(r"d:\code\dz\basketball\scaffolding-v2\basketball-web-ui\src\views\events\v4\V4MatchDetailPage.vue")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    if old not in text:
        raise SystemExit(f"missing {label}\n---\n{old[:180]}")
    return text.replace(old, new, 1)


def main() -> None:
    text = PAGE.read_text(encoding="utf-8")
    text = replace_once(
        text,
        "import { getV4PublicMatchDetail, getV4PublicMatchSeries } from '@/api/v4Public'",
        "import { getV4PublicDivisionSchedule, getV4PublicMatchDetail, getV4PublicMatchSeries } from '@/api/v4Public'",
        "import api",
    )
    text = replace_once(
        text,
        "import MatchScoreboard from '../match-detail/MatchScoreboard.vue'\nimport SeriesGameTabs from '../match-detail/SeriesGameTabs.vue'",
        "import MatchScoreboard from '../match-detail/MatchScoreboard.vue'\nimport SeriesProgress from '../match-detail/SeriesProgress.vue'\nimport SeriesGameTabs from '../match-detail/SeriesGameTabs.vue'",
        "import progress",
    )
    text = replace_once(
        text,
        "const selectedTeamId = ref('')\nconst activeTab = ref(SERIES_TOTAL_TAB)\n\nconst visibleGames = computed(() => publicSeriesGames(series.value))",
        "const selectedTeamId = ref('')\nconst activeTab = ref(SERIES_TOTAL_TAB)\nconst seriesRoundName = ref('')\n\nconst visibleGames = computed(() => publicSeriesGames(series.value))\nconst progressGames = computed(() =>\n  [...(series.value?.games || [])].sort((a, b) => (a.seriesGameNo || 0) - (b.seriesGameNo || 0)),\n)",
        "progress games",
    )
    text = replace_once(
        text,
        "    { key: SERIES_TOTAL_TAB, label: '\u7cfb\u5217\u8d5b\u603b\u8ba1' },",
        "    { key: SERIES_TOTAL_TAB, label: '\u7cfb\u5217\u8d5b\u7edf\u8ba1' },",
        "tab label",
    )
    text = replace_once(
        text,
        """const leftTeam = computed(() => {
  if (match.value) return teamFromMatchSide(match.value, 'away')
  const s = series.value
  return toDetailTeam(s?.awayEventTeamId, s?.awayTeamName, s?.awayTeamLogoUrl, s?.awayTeamShortName)
})

const rightTeam = computed(() => {
  if (match.value && !seriesMode.value) return teamFromMatchSide(match.value, 'home')
  if (match.value && activeGame.value) return teamFromMatchSide(match.value, 'home')
  const s = series.value
  return toDetailTeam(s?.homeEventTeamId, s?.homeTeamName, s?.homeTeamLogoUrl, s?.homeTeamShortName)
})

const score = computed(() => {
  if (seriesMode.value && activeTab.value === SERIES_TOTAL_TAB) {
    return seriesWinScore(series.value)
  }
  return matchBasketballScore(match.value)
})

const winnerEventTeamId = computed(() => {
  if (seriesMode.value && activeTab.value === SERIES_TOTAL_TAB) {
    return series.value?.winnerEventTeamId || ''
  }
  return match.value?.winnerEventTeamId || ''
})

const statusText = computed(() => {
  if (seriesMode.value && activeTab.value === SERIES_TOTAL_TAB) {
    return v4SeriesHeroStatusLabel(series.value?.seriesStatus)
  }
  if (seriesMode.value && activeGame.value) {
    return ''
  }
  return match.value?.matchStatusName || v4MatchStatusLabel(match.value?.matchStatus)
})

const caption = computed(() => {
  if (seriesMode.value && activeGame.value) {
    return gameCaption(activeGame.value)
  }
  return ''
})

const metaText = computed(() => {
  if (seriesMode.value) {
    const s = series.value
    const stage = v4PublicRoundLabel(s?.stageName, s?.roundNo) || s?.stageName || ''
    return [stage, v4BestOfLabel(s?.bestOf)].filter(Boolean).join(' \u00b7 ')
  }""",
        """const leftTeam = computed(() => {
  if (seriesMode.value) {
    const s = series.value
    return toDetailTeam(s?.awayEventTeamId, s?.awayTeamName, s?.awayTeamLogoUrl, s?.awayTeamShortName)
  }
  if (match.value) return teamFromMatchSide(match.value, 'away')
  return toDetailTeam()
})

const rightTeam = computed(() => {
  if (seriesMode.value) {
    const s = series.value
    return toDetailTeam(s?.homeEventTeamId, s?.homeTeamName, s?.homeTeamLogoUrl, s?.homeTeamShortName)
  }
  if (match.value) return teamFromMatchSide(match.value, 'home')
  return toDetailTeam()
})

const score = computed(() => {
  if (seriesMode.value) {
    return seriesWinScore(series.value)
  }
  return matchBasketballScore(match.value)
})

const winnerEventTeamId = computed(() => {
  if (seriesMode.value) {
    return series.value?.winnerEventTeamId || ''
  }
  return match.value?.winnerEventTeamId || ''
})

const statusText = computed(() => {
  if (seriesMode.value) {
    return v4SeriesHeroStatusLabel(series.value?.seriesStatus)
  }
  return match.value?.matchStatusName || v4MatchStatusLabel(match.value?.matchStatus)
})

const caption = computed(() => {
  if (seriesMode.value) return ''
  return ''
})

const advanceLabel = computed(() => {
  if (!seriesMode.value) return ''
  const status = String(series.value?.seriesStatus || '').toUpperCase()
  if (status !== 'COMPLETED' || !series.value?.winnerEventTeamId) return ''
  return '\u664b\u7ea7'
})

const metaText = computed(() => {
  if (seriesMode.value) {
    const s = series.value
    const round = v4PublicRoundLabel(seriesRoundName.value || s?.stageName, s?.roundNo)
    return [round, v4BestOfLabel(s?.bestOf)].filter(Boolean).join(' \u00b7 ')
  }""",
        "hero computeds",
    )
    text = replace_once(
        text,
        "const showBoxScore = computed(() => {\n  if (seriesMode.value && activeTab.value === SERIES_TOTAL_TAB) {\n    return Boolean(boxPlayers.value.length || boxTeamStats.value.length)\n  }\n  return true\n})\n",
        """const showBoxScore = computed(() => {
  if (seriesMode.value && activeTab.value === SERIES_TOTAL_TAB) {
    return Boolean(boxPlayers.value.length || boxTeamStats.value.length)
  }
  return true
})

const boxTitle = computed(() => {
  if (seriesMode.value && activeTab.value === SERIES_TOTAL_TAB) return '\u7cfb\u5217\u8d5b\u7d2f\u8ba1\u7edf\u8ba1'
  return '\u6bd4\u8d5b\u6280\u672f\u7edf\u8ba1'
})

const boxSubtitle = computed(() => {
  if (seriesMode.value && activeTab.value === SERIES_TOTAL_TAB) return 'SERIES TOTALS'
  return 'BOX SCORE'
})

const boxHint = computed(() => {
  if (seriesMode.value && activeTab.value === SERIES_TOTAL_TAB) {
    const n = visibleGames.value.length || progressGames.value.filter((game) => !game.skipped).length
    return n ? n + ' \u573a\u7cfb\u5217\u8d5b\u7d2f\u8ba1\u6570\u636e' : ''
  }
  if (seriesMode.value && activeGame.value) {
    const no = activeGame.value.seriesGameNo
    const when = formatMatchWhen(activeGame.value.scheduledTime)
    return ['G' + no, when].filter(Boolean).join(' \u00b7 ')
  }
  return ''
})
""",
        "box copy",
    )
    text = replace_once(
        text,
        """      applyTabFromRoute()
      if (activeTab.value !== SERIES_TOTAL_TAB) {
        await loadMatchById(activeTab.value)
      } else {
        match.value = null
      }""",
        """      seriesRoundName.value = ''
      applyTabFromRoute()
      if (series.value.divisionId) {
        try {
          const schedule = await getV4PublicDivisionSchedule(String(series.value.divisionId))
          const row = (schedule.data || []).find((item) =>
            item.unitType === 'MATCH_SERIES' && String(item.id) === String(series.value?.id),
          )
          seriesRoundName.value = String(row?.roundName || '').trim()
        } catch {
          seriesRoundName.value = ''
        }
      }
      if (activeTab.value !== SERIES_TOTAL_TAB) {
        await loadMatchById(activeTab.value)
      } else {
        match.value = null
      }""",
        "load round name",
    )
    text = replace_once(
        text,
        """          <MatchScoreboard
            :left-team=\"leftTeam\"
            :right-team=\"rightTeam\"
            :score=\"score\"
            :winner-event-team-id=\"winnerEventTeamId\"
            :status-text=\"statusText\"
            :meta-text=\"metaText\"
            :caption=\"caption\"
          />""",
        """          <MatchScoreboard
            :left-team=\"leftTeam\"
            :right-team=\"rightTeam\"
            :score=\"score\"
            :winner-event-team-id=\"winnerEventTeamId\"
            :status-text=\"statusText\"
            :meta-text=\"metaText\"
            :caption=\"caption\"
            :advance-label=\"advanceLabel\"
          />
          <SeriesProgress
            v-if=\"seriesMode && progressGames.length\"
            :games=\"progressGames\"
            :left-team=\"leftTeam\"
            :right-team=\"rightTeam\"
            :active-match-id=\"activeTab === SERIES_TOTAL_TAB ? '' : activeTab\"
            @select=\"onTabChange\"
          />""",
        "template hero",
    )
    text = replace_once(
        text,
          """          <SeriesGameTabs
            v-if=\"showSeriesTabs\"
            :tabs=\"seriesTabs\"
            :model-value=\"activeTab\"
            @update:model-value=\"onTabChange\"
          />""",
        """          <SeriesGameTabs
            v-if=\"showSeriesTabs\"
            :tabs=\"seriesTabs\"
            :model-value=\"activeTab\"
            :primary-key=\"SERIES_TOTAL_TAB\"
            @update:model-value=\"onTabChange\"
          />""",
        "tabs primary",
    )
    text = replace_once(
        text,
        """            <BoxScorePanel
              v-if=\"showBoxScore\"
              :teams=\"boxTeams\"
              :selected-team-id=\"selectedTeamId\"
              :players=\"boxPlayers\"
              :team-stats=\"boxTeamStats\"
              @update:selected-team-id=\"selectedTeamId = $event\"
              @go-player=\"goPlayer\"
            />""",
        """            <BoxScorePanel
              v-if=\"showBoxScore\"
              :teams=\"boxTeams\"
              :selected-team-id=\"selectedTeamId\"
              :players=\"boxPlayers\"
              :team-stats=\"boxTeamStats\"
              :title=\"boxTitle\"
              :subtitle=\"boxSubtitle\"
              :hint=\"boxHint\"
              @update:selected-team-id=\"selectedTeamId = $event\"
              @go-player=\"goPlayer\"
            />""",
        "box props template",
    )
    text = replace_once(
        text,
        ".match-detail-hero :deep(.series-game-tabs) {\n  margin-top: 28px;\n}",
        """.match-detail-hero :deep(.series-game-tabs) {
  margin-top: 22px;
}

.match-detail-hero :deep(.series-progress) {
  margin-top: 28px;
}""",
        "hero css",
    )
    PAGE.write_text(text, encoding="utf-8", newline="\n")
    print("page patched")


if __name__ == "__main__":
    main()
