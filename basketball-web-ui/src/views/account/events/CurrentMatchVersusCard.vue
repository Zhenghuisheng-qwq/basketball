<script setup lang="ts">
import { computed } from 'vue'
import EventTeamLogo from '@/components/account/tournament/EventTeamLogo.vue'
import type { PlayerMatchTaskItem } from '@/api/matchData'

const props = defineProps<{
  match: PlayerMatchTaskItem
  identityText: string
}>()

const homeName = computed(
  () => props.match.homeTeamNameSnapshot || props.match.homeTeamName || '未知球队',
)
const awayName = computed(
  () => props.match.awayTeamNameSnapshot || props.match.awayTeamName || '未知球队',
)

function formatTeamRecord(
  recordText?: string | null,
  winCount?: number | null,
  lossCount?: number | null,
): string {
  const text = recordText?.trim()
  if (text) return text
  const wins = Number.isFinite(Number(winCount)) ? Math.max(0, Number(winCount)) : 0
  const losses = Number.isFinite(Number(lossCount)) ? Math.max(0, Number(lossCount)) : 0
  return `${wins}胜${losses}负`
}

const homeRecordText = computed(() => formatTeamRecord(
  props.match.homeRecordText,
  props.match.homeWinCount,
  props.match.homeLossCount,
))
const awayRecordText = computed(() => formatTeamRecord(
  props.match.awayRecordText,
  props.match.awayWinCount,
  props.match.awayLossCount,
))

const isHomeMine = computed(
  () => Boolean(props.match.myEventTeamId && props.match.homeEventTeamId
    && props.match.myEventTeamId === props.match.homeEventTeamId),
)
const isAwayMine = computed(
  () => Boolean(props.match.myEventTeamId && props.match.awayEventTeamId
    && props.match.myEventTeamId === props.match.awayEventTeamId),
)

const metaLine = computed(() => {
  const parts = [
    props.match.stageName,
    props.match.roundNo ? `第 ${props.match.roundNo} 轮` : '',
    props.match.platformCode,
  ].filter(Boolean)
  return parts.join(' · ') || '-'
})

const scheduleLine = computed(() => {
  const parts = [props.match.matchDate, props.match.weekday, props.match.matchTime].filter(Boolean)
  return parts.join(' ') || '-'
})

const dataStatusText = computed(
  () => props.match.dataStatusText || props.match.statusText || '未提交',
)

const centerView = computed(() => {
  const row = props.match
  const dataText = dataStatusText.value
  if (row.matchStatus === 5) {
    return { mode: 'status' as const, text: '已取消' }
  }
  if (dataText.includes('争议') || dataText.includes('异常')) {
    return { mode: 'status' as const, text: dataText }
  }
  if (row.matchStatus === 3 && row.homeScore != null && row.awayScore != null) {
    return { mode: 'score' as const, home: row.homeScore, away: row.awayScore }
  }
  return { mode: 'vs' as const, text: 'VS' }
})

const matchPillClass = computed(() => {
  const text = props.match.matchStatusText || ''
  if (text.includes('取消') || text.includes('延期')) return 'versus-pill--danger'
  if (text.includes('进行')) return 'versus-pill--warning'
  if (text.includes('结束')) return 'versus-pill--success'
  return 'versus-pill--neutral'
})

const dataPillClass = computed(() => {
  const text = dataStatusText.value
  if (text.includes('驳回')) return 'versus-pill--danger'
  if (text.includes('争议') || text.includes('异常')) return 'versus-pill--warning'
  if (text.includes('确认') || text.includes('通过')) return 'versus-pill--success'
  if (text.includes('审核')) return 'versus-pill--warning'
  return 'versus-pill--neutral'
})

function teamBadgeText(name: string) {
  const trimmed = name.trim()
  if (!trimmed) return '--'
  const latinTail = trimmed.match(/([A-Za-z0-9]+)$/)?.[1]
  if (latinTail && latinTail.length <= 4) return latinTail.toUpperCase()
  if (trimmed.length <= 3) return trimmed.toUpperCase()
  return trimmed.slice(0, 2).toUpperCase()
}
</script>

<template>
  <div class="match-overview">
    <div class="match-overview__meta-bar">
      <span class="match-overview__meta-left">{{ metaLine }}</span>
      <time class="match-overview__meta-time">{{ scheduleLine }}</time>
    </div>

    <div class="match-versus-card">
      <div
        class="team-side"
        :class="{ 'team-side--mine': isHomeMine }"
      >
        <div class="team-side__logo-wrap">
          <EventTeamLogo
            :logo-url="match.homeLogoUrl"
            :team-code="teamBadgeText(homeName)"
            size="xl"
            variant="versus"
          />
        </div>
        <div class="team-side__name" :title="homeName">{{ homeName }}</div>
        <div class="team-side__record" :title="`本届战绩：${homeRecordText}`">
          本届战绩：<span class="team-side__record-value">{{ homeRecordText }}</span>
        </div>
        <div class="team-side__tags">
          <span class="team-side__tag team-side__tag--role">主队</span>
          <span v-if="isHomeMine" class="team-side__tag team-side__tag--mine">我的球队</span>
          <span v-else-if="isAwayMine" class="team-side__tag team-side__tag--opponent">对手</span>
        </div>
      </div>

      <div class="versus-center">
        <div class="versus-center__glow" aria-hidden="true" />
        <template v-if="centerView.mode === 'score'">
          <div class="versus-center__score">
            <span>{{ centerView.home }}</span>
            <span class="versus-center__score-sep">:</span>
            <span>{{ centerView.away }}</span>
          </div>
        </template>
        <template v-else-if="centerView.mode === 'status'">
          <div class="versus-center__status-main">{{ centerView.text }}</div>
        </template>
        <template v-else>
          <div class="versus-center__vs">VS</div>
        </template>
        <div class="versus-center__pills">
          <span class="versus-pill" :class="matchPillClass">
            {{ match.matchStatusText || '-' }}
          </span>
          <span class="versus-pill" :class="dataPillClass">
            {{ dataStatusText }}
          </span>
        </div>
      </div>

      <div
        class="team-side team-side--away"
        :class="{ 'team-side--mine': isAwayMine }"
      >
        <div class="team-side__logo-wrap">
          <EventTeamLogo
            :logo-url="match.awayLogoUrl"
            :team-code="teamBadgeText(awayName)"
            size="xl"
            variant="versus"
          />
        </div>
        <div class="team-side__name" :title="awayName">{{ awayName }}</div>
        <div class="team-side__record" :title="`本届战绩：${awayRecordText}`">
          本届战绩：<span class="team-side__record-value">{{ awayRecordText }}</span>
        </div>
        <div class="team-side__tags">
          <span class="team-side__tag team-side__tag--role">客队</span>
          <span v-if="isAwayMine" class="team-side__tag team-side__tag--mine">我的球队</span>
          <span v-else-if="isHomeMine" class="team-side__tag team-side__tag--opponent">对手</span>
        </div>
      </div>
    </div>

    <dl class="match-overview__summary">
      <div class="match-overview__summary-item">
        <dt>赛事</dt>
        <dd :title="match.eventName || '-'">{{ match.eventName || '-' }}</dd>
      </div>
      <div class="match-overview__summary-item">
        <dt>赛段</dt>
        <dd :title="match.stageName || '-'">{{ match.stageName || '-' }}</dd>
      </div>
      <div class="match-overview__summary-item">
        <dt>轮次</dt>
        <dd>{{ match.roundNo ? `第 ${match.roundNo} 轮` : '-' }}</dd>
      </div>
      <div class="match-overview__summary-item">
        <dt>平台</dt>
        <dd>{{ match.platformCode || '-' }}</dd>
      </div>
      <div class="match-overview__summary-item">
        <dt>当前身份</dt>
        <dd>{{ identityText }}</dd>
      </div>
      <div class="match-overview__summary-item">
        <dt>我的球队</dt>
        <dd :title="match.myTeamNameSnapshot || '-'">{{ match.myTeamNameSnapshot || '-' }}</dd>
      </div>
      <div class="match-overview__summary-item">
        <dt>对手</dt>
        <dd :title="match.opponentTeamNameSnapshot || '-'">{{ match.opponentTeamNameSnapshot || '-' }}</dd>
      </div>
      <div class="match-overview__summary-item">
        <dt>数据状态</dt>
        <dd>{{ dataStatusText }}</dd>
      </div>
    </dl>
  </div>
</template>

<style scoped lang="scss">
.match-overview {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.match-overview__meta-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 8px 16px;
  padding: 0 2px;
}

.match-overview__meta-left {
  font-size: 13px;
  color: #666;
  letter-spacing: 0.02em;
}

.match-overview__meta-time {
  font-size: 13px;
  font-weight: 700;
  color: #333;
  letter-spacing: 0.01em;
}

.match-versus-card {
  position: relative;
  display: grid;
  grid-template-columns: minmax(0, 1fr) minmax(200px, 260px) minmax(0, 1fr);
  align-items: center;
  gap: 12px;
  min-height: 220px;
  max-height: 260px;
  padding: 22px 18px;
  overflow: hidden;
  border: 1px solid rgba(255, 255, 255, 0.06);
  border-radius: 4px;
  background:
    repeating-linear-gradient(
      -45deg,
      rgba(255, 255, 255, 0.015) 0,
      rgba(255, 255, 255, 0.015) 1px,
      transparent 1px,
      transparent 8px
    ),
    radial-gradient(ellipse 80% 60% at 50% 0%, rgba(225, 6, 19, 0.14) 0%, transparent 70%),
    linear-gradient(180deg, #141414 0%, #090909 52%, #0c0505 100%);
  box-shadow:
    inset 0 1px 0 rgba(255, 255, 255, 0.05),
    inset 0 -12px 32px rgba(225, 6, 19, 0.04),
    0 10px 28px rgba(0, 0, 0, 0.18);

  &::before {
    content: '';
    position: absolute;
    inset: 0 auto auto 0;
    width: 28px;
    height: 28px;
    border-top: 2px solid rgba(225, 6, 19, 0.45);
    border-left: 2px solid rgba(225, 6, 19, 0.45);
    pointer-events: none;
  }

  &::after {
    content: '';
    position: absolute;
    top: 0;
    left: 12%;
    right: 12%;
    height: 1px;
    background: linear-gradient(
      90deg,
      transparent 0%,
      rgba(225, 6, 19, 0.55) 50%,
      transparent 100%
    );
    pointer-events: none;
  }
}

.team-side {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  min-width: 0;
  text-align: center;
  z-index: 1;
}

.team-side__logo-wrap {
  position: relative;
  padding: 3px;
  border-radius: 18px;
  transition: box-shadow 0.2s ease;
}

.team-side--mine .team-side__logo-wrap {
  box-shadow: 0 0 0 1px rgba(225, 6, 19, 0.55);
}

.team-side--mine .team-side__logo-wrap::after {
  content: '我';
  position: absolute;
  top: -4px;
  right: -4px;
  width: 18px;
  height: 18px;
  display: grid;
  place-items: center;
  border-radius: 2px;
  background: #e10613;
  color: #fff;
  font-size: 10px;
  font-weight: 900;
  line-height: 1;
  box-shadow: 0 2px 8px rgba(225, 6, 19, 0.45);
}

.team-side__name {
  max-width: 100%;
  font-size: 15px;
  font-weight: 800;
  color: #f5f5f5;
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-side__record {
  max-width: 100%;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.55);
  line-height: 1.35;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.team-side__record-value {
  font-weight: 700;
  color: rgba(255, 255, 255, 0.72);
}

.team-side__tags {
  display: flex;
  flex-wrap: nowrap;
  justify-content: center;
  gap: 6px;
  max-width: 100%;
}

.team-side__tag {
  display: inline-flex;
  align-items: center;
  padding: 2px 8px;
  border-radius: 999px;
  font-size: 10px;
  font-weight: 700;
  letter-spacing: 0.03em;
  white-space: nowrap;
}

.team-side__tag--role {
  border: 1px solid rgba(255, 255, 255, 0.14);
  color: rgba(255, 255, 255, 0.65);
  background: rgba(255, 255, 255, 0.04);
}

.team-side__tag--mine {
  border: 1px solid rgba(225, 6, 19, 0.45);
  color: #ff8a90;
  background: rgba(225, 6, 19, 0.12);
}

.team-side__tag--opponent {
  border: 1px solid rgba(255, 255, 255, 0.12);
  color: rgba(255, 255, 255, 0.55);
  background: rgba(0, 0, 0, 0.2);
}

.versus-center {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-height: 160px;
  padding: 0 8px;
  z-index: 1;

  &::before,
  &::after {
    content: '';
    position: absolute;
    top: 18%;
    bottom: 18%;
    width: 1px;
    background: linear-gradient(
      180deg,
      transparent 0%,
      rgba(225, 6, 19, 0.35) 50%,
      transparent 100%
    );
    pointer-events: none;
  }

  &::before {
    left: 0;
  }

  &::after {
    right: 0;
  }
}

.versus-center__glow {
  position: absolute;
  inset: 20% 10%;
  background: radial-gradient(circle, rgba(225, 6, 19, 0.12) 0%, transparent 70%);
  pointer-events: none;
}

.versus-center__vs {
  font-size: 48px;
  font-weight: 900;
  letter-spacing: 0.12em;
  color: rgba(255, 255, 255, 0.92);
  line-height: 1;
  text-shadow: 0 0 24px rgba(225, 6, 19, 0.35);
}

.versus-center__score {
  display: flex;
  align-items: baseline;
  gap: 8px;
  font-size: 44px;
  font-weight: 900;
  color: #fff;
  line-height: 1;
  text-shadow: 0 4px 18px rgba(0, 0, 0, 0.35);
}

.versus-center__score-sep {
  color: rgba(225, 6, 19, 0.75);
  font-weight: 800;
}

.versus-center__status-main {
  font-size: 18px;
  font-weight: 800;
  color: #ff8a90;
  text-align: center;
  line-height: 1.35;
}

.versus-center__pills {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
}

.versus-pill {
  display: inline-flex;
  align-items: center;
  padding: 3px 10px;
  border-radius: 999px;
  font-size: 11px;
  font-weight: 700;
  letter-spacing: 0.02em;
  border: 1px solid transparent;
}

.versus-pill--neutral {
  color: rgba(255, 255, 255, 0.78);
  background: rgba(255, 255, 255, 0.06);
  border-color: rgba(255, 255, 255, 0.1);
}

.versus-pill--success {
  color: #8ce6a8;
  background: rgba(35, 148, 71, 0.14);
  border-color: rgba(35, 148, 71, 0.35);
}

.versus-pill--warning {
  color: #f0c56d;
  background: rgba(214, 138, 22, 0.14);
  border-color: rgba(214, 138, 22, 0.35);
}

.versus-pill--danger {
  color: #ff9a9a;
  background: rgba(200, 10, 16, 0.14);
  border-color: rgba(200, 10, 16, 0.35);
}

.match-overview__summary {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px 16px;
  margin: 0;
  padding: 16px 4px 4px;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
}

.match-overview__summary-item {
  min-width: 0;
  padding: 10px 12px;
  border-radius: 2px;
  background: rgba(0, 0, 0, 0.02);
  border: 1px solid rgba(0, 0, 0, 0.04);

  dt {
    margin: 0 0 6px;
    font-size: 11px;
    font-weight: 700;
    color: #999;
    letter-spacing: 0.04em;
    text-transform: none;
  }

  dd {
    margin: 0;
    font-size: 14px;
    font-weight: 700;
    color: #1a1a1a;
    line-height: 1.45;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
}

@media (max-width: 992px) {
  .match-versus-card {
    grid-template-columns: minmax(0, 1fr) minmax(160px, 220px) minmax(0, 1fr);
    padding: 18px 12px;
  }

  .versus-center__vs {
    font-size: 40px;
  }

  .versus-center__score {
    font-size: 36px;
  }
}

@media (max-width: 767px) {
  .match-versus-card {
    grid-template-columns: 1fr;
    max-height: none;
    min-height: 0;
    padding: 20px 14px;
    gap: 16px;
  }

  .versus-center {
    order: -1;
    min-height: 120px;
    padding: 8px 0 4px;

    &::before,
    &::after {
      display: none;
    }
  }

  .match-overview__summary {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 480px) {
  .match-overview__summary {
    grid-template-columns: 1fr;
  }
}
</style>
