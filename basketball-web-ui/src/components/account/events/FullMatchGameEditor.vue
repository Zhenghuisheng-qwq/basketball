<script setup lang="ts">
import { computed } from 'vue'
import { useI18n } from 'vue-i18n'
import type { V4MatchSubmissionEligiblePlayer, V4MatchSubmissionPlayerStat } from '@/api/types/v4MatchSubmission'
import type { V4MatchSubmissionPeriodScore } from '@/api/types/v4SeriesSubmission'
import { useAccountFilePreview } from '@/composables/useAccountFilePreview'
import {
  aggregateTeam,
  playerFieldErrors,
  requiresPeriodScores,
  shootingLine,
  validateGame,
} from '@/utils/fullMatchSubmissionRules'

const props = defineProps<{
  gameLabel: string
  homeName: string
  awayName: string
  homePlayers: V4MatchSubmissionEligiblePlayer[]
  awayPlayers: V4MatchSubmissionEligiblePlayer[]
  homeStats: V4MatchSubmissionPlayerStat[]
  awayStats: V4MatchSubmissionPlayerStat[]
  periods: V4MatchSubmissionPeriodScore[]
  materialFileIds: string[]
  readonly?: boolean
  requireComplete?: boolean
  gameMode?: string | null
}>()

const emit = defineEmits<{
  'update:homeStats': [value: V4MatchSubmissionPlayerStat[]]
  'update:awayStats': [value: V4MatchSubmissionPlayerStat[]]
  'update:periods': [value: V4MatchSubmissionPeriodScore[]]
  upload: []
  removeMaterial: [index: number]
  preview: [fileId: string]
}>()

const { t } = useI18n()

const STAT_COLS = [
  ['points', 'PTS'],
  ['rebound', 'REB'],
  ['assist', 'AST'],
  ['steal', 'STL'],
  ['block', 'BLK'],
  ['foul', 'PF'],
  ['turnover', 'TO'],
] as const

const homeAgg = computed(() => aggregateTeam(props.homeStats))
const awayAgg = computed(() => aggregateTeam(props.awayStats))
const validation = computed(() =>
  validateGame({
    gameLabel: props.gameLabel,
    homeName: props.homeName,
    awayName: props.awayName,
    homePlayers: props.homeStats,
    awayPlayers: props.awayStats,
    periods: props.periods,
    materialCount: props.materialFileIds.length,
    requireComplete: props.requireComplete !== false,
    gameMode: props.gameMode,
  }),
)
const showPeriods = computed(() => requiresPeriodScores(props.gameMode))
const materialIds = computed(() => props.materialFileIds || [])
const { urls: materialUrls, previewUrl, openPreview, closePreview } = useAccountFilePreview(materialIds)
const periodTouched = computed(() =>
  validation.value.homeScore + validation.value.awayScore + validation.value.homePeriod + validation.value.awayPeriod > 0,
)
const homePeriodOk = computed(() => validation.value.homePeriod === validation.value.homeScore)
const awayPeriodOk = computed(() => validation.value.awayPeriod === validation.value.awayScore)

defineExpose({ validation, homeAgg, awayAgg })

function playerLabel(player: V4MatchSubmissionEligiblePlayer) {
  const no = player.jerseyNo ? `#${player.jerseyNo}` : ''
  const name = player.playerName || player.gameId || t('accountEvents.common.unnamedPlayer')
  return [no, name].filter(Boolean).join(' ')
}

function findPlayer(side: 'home' | 'away', id: string | number | undefined) {
  const pool = side === 'home' ? props.homePlayers : props.awayPlayers
  return pool.find((player) => String(player.eventPlayerId) === String(id || ''))
}

function available(side: 'home' | 'away', index: number) {
  const rows = side === 'home' ? props.homeStats : props.awayStats
  const pool = side === 'home' ? props.homePlayers : props.awayPlayers
  const selected = new Set(rows.map((row, i) => (i === index ? '' : String(row.eventPlayerId || ''))).filter(Boolean))
  return pool.filter((player) => !selected.has(String(player.eventPlayerId)))
}

function emitStats(side: 'home' | 'away', rows: V4MatchSubmissionPlayerStat[]) {
  if (side === 'home') emit('update:homeStats', rows)
  else emit('update:awayStats', rows)
}

function updateRow(side: 'home' | 'away', index: number, field: string, value: string | number) {
  const rows = (side === 'home' ? props.homeStats : props.awayStats).map((row, i) =>
    i === index ? { ...row, [field]: value } : row,
  )
  emitStats(side, rows)
}

function addRow(side: 'home' | 'away') {
  const empty: V4MatchSubmissionPlayerStat = {
    eventPlayerId: '', points: 0, rebound: 0, assist: 0, steal: 0, block: 0, foul: 0, turnover: 0,
    fieldGoalMade: 0, fieldGoalAttempt: 0, threePointMade: 0, threePointAttempt: 0, freeThrowMade: 0, freeThrowAttempt: 0,
  }
  emitStats(side, [...(side === 'home' ? props.homeStats : props.awayStats), empty])
}

function removeRow(side: 'home' | 'away', index: number) {
  const rows = [...(side === 'home' ? props.homeStats : props.awayStats)]
  if (rows.length <= 1) return
  rows.splice(index, 1)
  emitStats(side, rows)
}

function updatePeriod(index: number, field: 'homeScore' | 'awayScore', value: number) {
  emit('update:periods', props.periods.map((row, i) => (i === index ? { ...row, [field]: Number.isFinite(value) ? value : 0 } : row)))
}

function num(event: Event) {
  const raw = (event.target as HTMLInputElement).value
  const value = raw === '' ? 0 : Number(raw)
  return Number.isFinite(value) && value >= 0 ? Math.floor(value) : 0
}

function fieldErr(side: 'home' | 'away', index: number) {
  return playerFieldErrors((side === 'home' ? props.homeStats : props.awayStats)[index] || {})
}
</script>

<template>
  <div class="fm">
    <section class="fm-board">
      <div class="fm-board__team">
        <span>HOME</span>
        <strong>{{ homeName }}</strong>
      </div>
      <div class="fm-board__score">
        <b>{{ homeAgg.score }}</b>
        <em>:</em>
        <b>{{ awayAgg.score }}</b>
      </div>
      <div class="fm-board__team is-away">
        <span>AWAY</span>
        <strong>{{ awayName }}</strong>
      </div>
      <p class="fm-board__hint">{{ t('accountEvents.editor.autoScore') }}</p>
    </section>

    <section v-if="showPeriods" class="fm-period">
      <div class="fm-sec-head">
        <strong>{{ t('accountEvents.editor.periods') }}</strong>
        <small>{{ t('accountEvents.editor.periodHint') }}</small>
      </div>
      <div class="fm-period__grid">
        <div v-for="(period, index) in periods" :key="period.periodNo" class="fm-q">
          <div class="fm-q__label">Q{{ period.periodNo }}</div>
          <div class="fm-q__inputs">
            <input type="number" min="0" :disabled="readonly" :value="period.homeScore ?? 0" @input="updatePeriod(index, 'homeScore', num($event))" />
            <span>:</span>
            <input type="number" min="0" :disabled="readonly" :value="period.awayScore ?? 0" @input="updatePeriod(index, 'awayScore', num($event))" />
          </div>
        </div>
      </div>
      <div class="fm-period__totals">
        <div :class="['fm-ptotal', !periodTouched ? 'is-wait' : homePeriodOk ? 'is-ok' : 'is-bad']">
          <span>{{ t('accountEvents.editor.periodTotal', { name: homeName, total: validation.homePeriod }) }}</span>
          <b v-if="!periodTouched">{{ t('accountEvents.editor.pending') }}</b>
          <b v-else-if="homePeriodOk">{{ t('accountEvents.editor.periodMatch', { score: validation.homeScore }) }}</b>
          <b v-else>{{ t('accountEvents.editor.periodMismatch', { period: validation.homePeriod, score: validation.homeScore }) }}</b>
        </div>
        <div :class="['fm-ptotal', !periodTouched ? 'is-wait' : awayPeriodOk ? 'is-ok' : 'is-bad']">
          <span>{{ t('accountEvents.editor.periodTotal', { name: awayName, total: validation.awayPeriod }) }}</span>
          <b v-if="!periodTouched">{{ t('accountEvents.editor.pending') }}</b>
          <b v-else-if="awayPeriodOk">{{ t('accountEvents.editor.periodMatch', { score: validation.awayScore }) }}</b>
          <b v-else>{{ t('accountEvents.editor.periodMismatch', { period: validation.awayPeriod, score: validation.awayScore }) }}</b>
        </div>
      </div>
    </section>

    <section class="fm-teams">
      <article v-for="pack in [{ name: homeName, t: homeAgg }, { name: awayName, t: awayAgg }]" :key="pack.name" class="fm-tcard">
        <header>
          <strong>{{ pack.name }}</strong>
          <em>{{ t('accountEvents.editor.autoTotal') }}</em>
        </header>
        <div class="fm-tcard__stats">
          <div><small>PTS</small><b>{{ pack.t.score }}</b></div>
          <div><small>REB</small><b>{{ pack.t.rebound }}</b></div>
          <div><small>AST</small><b>{{ pack.t.assist }}</b></div>
          <div><small>STL</small><b>{{ pack.t.steal }}</b></div>
          <div><small>BLK</small><b>{{ pack.t.block }}</b></div>
          <div><small>PF</small><b>{{ pack.t.foul }}</b></div>
          <div><small>TO</small><b>{{ pack.t.turnover }}</b></div>
        </div>
        <div class="fm-tcard__shoot">
          <div><small>FG</small><b>{{ shootingLine(pack.t.fieldGoalMade, pack.t.fieldGoalAttempt) }}</b></div>
          <div><small>3PT</small><b>{{ shootingLine(pack.t.threePointMade, pack.t.threePointAttempt) }}</b></div>
          <div><small>FT</small><b>{{ shootingLine(pack.t.freeThrowMade, pack.t.freeThrowAttempt) }}</b></div>
        </div>
      </article>
    </section>

    <section :class="['fm-valid', `is-${validation.status}`]">
      <strong v-if="validation.status === 'pass'">{{ t('accountEvents.editor.validPass') }}</strong>
      <strong v-else-if="validation.status === 'empty'">{{ t('accountEvents.editor.validEmpty') }}</strong>
      <strong v-else>{{ t('accountEvents.editor.validFail') }}</strong>
      <p v-if="validation.status === 'empty'">{{ showPeriods ? t('accountEvents.editor.fillEmptyPeriods') : t('accountEvents.editor.fillEmpty') }}</p>
      <ul v-else-if="validation.errors.length">
        <li v-for="item in validation.errors.slice(0, 6)" :key="item">{{ item }}</li>
      </ul>
    </section>

    <section v-for="side in (['home', 'away'] as const)" :key="side" class="fm-plist">
      <div class="fm-plist__head">
        <strong>{{ t('accountEvents.editor.playerData', { name: side === 'home' ? homeName : awayName }) }}</strong>
        <button v-if="!readonly" type="button" class="account-btn-secondary" @click="addRow(side)">{{ t('accountEvents.editor.addPlayer') }}</button>
      </div>
      <div class="fm-plist__card">
        <div class="fm-table-wrap">
          <table class="fm-table">
            <thead>
              <tr>
                <th class="is-player">{{ t('accountEvents.common.player') }}</th>
                <th v-for="col in STAT_COLS" :key="col[0]">{{ col[1] }}</th>
                <th :title="t('accountEvents.editor.fgmTitle')">FGM/FGA</th>
                <th>3PM/3PA</th>
                <th>FTM/FTA</th>
                <th v-if="!readonly" class="is-op">{{ t('accountEvents.editor.action') }}</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, index) in (side === 'home' ? homeStats : awayStats)" :key="`${side}-${index}`">
                <td class="is-player">
                  <select
                    :class="{ 'is-err': fieldErr(side, index).player }"
                    :disabled="readonly"
                    :value="String(row.eventPlayerId || '')"
                    @change="updateRow(side, index, 'eventPlayerId', ($event.target as HTMLSelectElement).value)"
                  >
                    <option value="">{{ t('accountEvents.common.selectPlayer') }}</option>
                    <option v-for="player in available(side, index)" :key="String(player.eventPlayerId)" :value="String(player.eventPlayerId)">
                      {{ playerLabel(player) }}{{ player.isCaptain ? t('accountEvents.common.captainSuffix') : '' }}
                    </option>
                  </select>
                  <small v-if="findPlayer(side, row.eventPlayerId)?.isCaptain" class="fm-cap">{{ t('accountEvents.common.captain') }}</small>
                </td>
                <td v-for="col in STAT_COLS" :key="col[0]">
                  <input
                    type="number"
                    min="0"
                    :class="{ 'is-err': col[0] === 'points' && fieldErr(side, index).points }"
                    :disabled="readonly"
                    :value="(row as any)[col[0]] ?? 0"
                    @input="updateRow(side, index, col[0], num($event))"
                  />
                </td>
                <td>
                  <div :class="['fm-pair', { 'is-err': fieldErr(side, index).fieldGoal }]">
                    <input type="number" min="0" :disabled="readonly" :value="row.fieldGoalMade ?? 0" @input="updateRow(side, index, 'fieldGoalMade', num($event))" />
                    <span>/</span>
                    <input type="number" min="0" :disabled="readonly" :value="row.fieldGoalAttempt ?? 0" @input="updateRow(side, index, 'fieldGoalAttempt', num($event))" />
                  </div>
                </td>
                <td>
                  <div :class="['fm-pair', { 'is-err': fieldErr(side, index).threePoint }]">
                    <input type="number" min="0" :disabled="readonly" :value="row.threePointMade ?? 0" @input="updateRow(side, index, 'threePointMade', num($event))" />
                    <span>/</span>
                    <input type="number" min="0" :disabled="readonly" :value="row.threePointAttempt ?? 0" @input="updateRow(side, index, 'threePointAttempt', num($event))" />
                  </div>
                </td>
                <td>
                  <div :class="['fm-pair', { 'is-err': fieldErr(side, index).freeThrow }]">
                    <input type="number" min="0" :disabled="readonly" :value="row.freeThrowMade ?? 0" @input="updateRow(side, index, 'freeThrowMade', num($event))" />
                    <span>/</span>
                    <input type="number" min="0" :disabled="readonly" :value="row.freeThrowAttempt ?? 0" @input="updateRow(side, index, 'freeThrowAttempt', num($event))" />
                  </div>
                </td>
                <td v-if="!readonly" class="is-op">
                  <button type="button" class="fm-del" @click="removeRow(side, index)">{{ t('accountEvents.common.delete') }}</button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </div>
    </section>

    <section class="fm-shots">
      <div class="fm-sec-head">
        <strong>{{ t('accountEvents.editor.screenshots') }}</strong>
        <button v-if="!readonly" type="button" class="account-btn-secondary" @click="emit('upload')">{{ t('accountEvents.editor.upload') }}</button>
      </div>
      <p class="fm-shots__hint">{{ t('accountEvents.editor.screenshotHint') }}</p>
      <div class="fm-shots__grid">
        <figure v-for="(fileId, index) in materialFileIds" :key="fileId">
          <button type="button" @click="openPreview(fileId); emit('preview', fileId)">
            <img v-if="materialUrls[fileId]" :src="materialUrls[fileId]" :alt="t('accountEvents.common.screenshot')" />
            <span v-else class="fm-shot-pending">{{ t('accountEvents.common.loadingShort') }}</span>
          </button>
          <button v-if="!readonly" type="button" class="fm-del" @click="emit('removeMaterial', index)">{{ t('accountEvents.common.delete') }}</button>
        </figure>
        <button v-if="!readonly" type="button" class="fm-shots__add" @click="emit('upload')">{{ t('accountEvents.editor.addScreenshot') }}</button>
      </div>
    </section>
    <div v-if="previewUrl" class="fm-preview" @click.self="closePreview">
      <img :src="previewUrl" :alt="t('accountEvents.common.screenshotPreview')" />
    </div>
  </div>
</template>

<style scoped lang="scss">
.fm {
  display: flex;
  flex-direction: column;
  gap: 22px;
}

.fm-board {
  display: grid;
  grid-template-columns: 1fr auto 1fr;
  align-items: center;
  gap: 12px 20px;
  padding: 22px 24px 16px;
  background: #141414;
  color: #f4efe8;
  border-radius: 8px;
}
.fm-board__team {
  display: flex;
  flex-direction: column;
  gap: 4px;
  span { color: #9a9188; font-size: 11px; letter-spacing: .08em; }
  strong { font-size: 20px; line-height: 1.2; }
  &.is-away { text-align: right; }
}
.fm-board__score {
  display: flex;
  align-items: baseline;
  gap: 14px;
  b { font-size: 56px; line-height: .9; color: #fff; font-weight: 800; }
  em { font-size: 36px; color: #b90000; font-style: normal; font-weight: 800; }
}
.fm-board__hint {
  grid-column: 1 / -1;
  margin: 0;
  text-align: center;
  color: #8d8680;
  font-size: 12px;
}

.fm-sec-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  gap: 12px;
  margin-bottom: 10px;
  strong { font-size: 15px; }
  small { color: #6f6862; }
}

.fm-period__grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 10px;
}
.fm-q {
  background: #fff;
  border: 1px solid #ddd7d1;
  border-radius: 6px;
  padding: 10px 8px;
  text-align: center;
}
.fm-q__label { font-weight: 800; margin-bottom: 8px; }
.fm-q__inputs { display: flex; justify-content: center; align-items: center; gap: 6px; }
.fm-period__totals { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-top: 10px; }
.fm-ptotal {
  padding: 8px 10px;
  border-radius: 6px;
  font-size: 13px;
  display: flex;
  flex-direction: column;
  gap: 2px;
  &.is-ok { background: #eef8ee; color: #176d1b; }
  &.is-bad { background: #fff0ed; color: #a61908; }
  &.is-wait { background: #fff8e8; color: #8a5a00; }
}

.fm-teams {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
}
.fm-tcard {
  background: #fff;
  border: 1px solid #ddd7d1;
  border-radius: 8px;
  padding: 14px 16px;
  header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 12px;
    strong { font-size: 16px; }
    em {
      font-style: normal;
      font-size: 12px;
      color: #148518;
      background: #e8f6e8;
      border-radius: 99px;
      padding: 2px 8px;
    }
  }
}
.fm-tcard__stats {
  display: grid;
  grid-template-columns: repeat(7, minmax(0, 1fr));
  text-align: center;
  gap: 6px;
  small { display: block; color: #7a736c; font-size: 11px; }
  b { font-size: 20px; }
}
.fm-tcard__shoot {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 8px;
  margin-top: 12px;
  padding-top: 12px;
  border-top: 1px solid #eee8e2;
  small { display: block; color: #7a736c; font-size: 11px; }
  b { font-size: 13px; }
}

.fm-valid {
  padding: 10px 14px;
  border-radius: 6px;
  font-size: 13px;
  strong { display: block; margin-bottom: 4px; }
  ul { margin: 6px 0 0; padding-left: 18px; }
  &.is-empty { background: #fff8e8; color: #8a5a00; }
  &.is-pass { background: #eef8ee; color: #176d1b; }
  &.is-error { background: #fff0ed; color: #a61908; }
}

.fm-plist { display: flex; flex-direction: column; gap: 8px; }
.fm-plist__head {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.fm-plist__card {
  background: #fff;
  border: 1px solid #ddd7d1;
  border-radius: 8px;
  overflow: hidden;
}
.fm-table-wrap { overflow-x: auto; }
.fm-table {
  width: 100%;
  min-width: 980px;
  border-collapse: collapse;
  font-size: 12px;
  th, td { border-bottom: 1px solid #eee8e2; padding: 7px 6px; text-align: center; }
  th { background: #f7f4f1; color: #5c564f; font-weight: 700; }
  .is-player { min-width: 168px; text-align: left; }
  .is-op { width: 56px; }
}

.fm-table input,
.fm-q input,
.fm-pair input {
  width: 100%;
  height: 34px;
  padding: 0 4px;
  border: 1px solid #d8d2cc;
  border-radius: 4px;
  background: #f7f5f2;
  text-align: center;
  font-size: 13px;
  outline: none;
}
.fm-table select {
  width: 100%;
  min-width: 150px;
  height: 34px;
  padding: 0 8px;
  border: 1px solid #d8d2cc;
  border-radius: 4px;
  background: #f7f5f2;
  outline: none;
}
.fm-table input:focus,
.fm-table select:focus,
.fm-q input:focus,
.fm-pair input:focus {
  border-color: #b90000;
  background: #fff;
  box-shadow: 0 0 0 2px rgba(185, 0, 0, .12);
}
.fm-table input.is-err,
.fm-table select.is-err,
.fm-pair.is-err {
  border-color: #c42314;
  background: #fff1ee;
}
.fm-pair {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 2px;
  border-radius: 4px;
  input { width: 42px; }
  span { color: #8a837c; }
}
.fm-cap {
  display: inline-block;
  margin-top: 4px;
  color: #b90000;
  font-size: 11px;
}
.fm-del {
  border: 0;
  background: none;
  color: #b42318;
  cursor: pointer;
}

.fm-shots__hint { margin: 0 0 10px; color: #6f6862; font-size: 13px; }
.fm-shots__grid { display: flex; flex-wrap: wrap; gap: 10px; }
.fm-shots figure {
  margin: 0;
  width: 132px;
  button { display: block; padding: 0; border: 0; background: none; cursor: pointer; }
  img { width: 132px; height: 88px; object-fit: cover; border-radius: 6px; border: 1px solid #ddd7d1; }
}
.fm-shots__add {
  width: 132px;
  height: 88px;
  border: 1px dashed #c8c1ba;
  background: #faf7f4;
  color: #6f6862;
  border-radius: 6px;
  cursor: pointer;
}
.fm-shot-pending {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 132px;
  height: 88px;
  border-radius: 6px;
  border: 1px solid #ddd7d1;
  color: #8a837c;
  font-size: 12px;
}
.fm-preview {
  position: fixed;
  inset: 0;
  z-index: 40;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.72);
  img { max-width: 92vw; max-height: 88vh; object-fit: contain; }
}

@media (max-width: 1100px) {
  .fm-teams, .fm-period__totals { grid-template-columns: 1fr; }
  .fm-board__score b { font-size: 42px; }
}
@media (max-width: 760px) {
  .fm-board { grid-template-columns: 1fr; text-align: center; }
  .fm-board__team, .fm-board__team.is-away { text-align: center; }
  .fm-board__score { justify-content: center; }
  .fm-period__grid { grid-template-columns: 1fr 1fr; }
  .fm-tcard__stats { grid-template-columns: repeat(4, 1fr); }
}
</style>
