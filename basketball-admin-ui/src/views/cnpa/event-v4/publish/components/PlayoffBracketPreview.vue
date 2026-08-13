<template>
  <div v-if="model" class="playoff-bracket-wrap">
    <div class="playoff-bracket-meta">
      <span>种子来源：{{ model.seedSourceLabel }}</span>
    </div>
    <p class="playoff-bracket-hint">首轮根据种子生成固定淘汰树，生成后上下半区固定，后续胜者沿既定路径晋级。</p>
    <div class="playoff-bracket-scroll">
      <div class="playoff-bracket" :style="{ width: Math.max(model.width, teamCount >= 16 ? 1380 : model.width) + 'px', height: model.height + 'px' }">
        <div
          v-for="(round, ri) in model.rounds"
          :key="round.size"
          class="bracket-round-head"
          :style="{ left: round.x + 'px', width: model.matchWidth + 'px' }"
        >
          <div class="bracket-round-name">{{ round.name }}</div>
          <div class="bracket-round-sub">{{ round.size }} → {{ round.advance }}</div>
          <el-tag size="small" type="danger" effect="plain">BO{{ round.bestOf }}</el-tag>
        </div>

        <div
          v-for="(c, ci) in model.connectors"
          :key="'c-' + ci"
          class="bracket-connector"
          :class="'bracket-connector--' + c.type"
          :style="connectorStyle(c)"
        />

        <div
          v-for="round in model.rounds"
          :key="'m-' + round.size"
          class="bracket-round"
        >
          <div
            v-for="match in round.matches"
            :key="round.size + '-' + match.matchNo"
            class="bracket-match"
            :style="{ left: round.x + 'px', top: match.y + 'px', width: model.matchWidth + 'px', height: model.matchHeight + 'px' }"
          >
            <div v-if="match.label" class="bracket-match-label">{{ match.label }}</div>
            <div class="bracket-slot">
              <span class="bracket-seed">{{ slotSeed(match.top) }}</span>
              <span class="bracket-team">{{ match.top.label }}</span>
            </div>
            <div class="bracket-slot">
              <span class="bracket-seed">{{ slotSeed(match.bottom) }}</span>
              <span class="bracket-team">{{ match.bottom.label }}</span>
            </div>
          </div>
        </div>

        <div
          class="bracket-champion"
          :style="{ left: model.champion.x + 'px', top: model.champion.y + 'px', height: model.matchHeight + 'px' }"
        >
          <span class="bracket-champion-cup">冠军</span>
        </div>
      </div>
    </div>
  </div>
  <el-alert
    v-else
    type="warning"
    :closable="false"
    show-icon
    title="当前标准季后赛支持 4 / 8 / 16 支队伍。其它人数后续结合 BYE 处理。"
  />
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  buildPlayoffBracketModel,
  type PlayoffBracketConnector,
  type V4PlayoffSeedSource
} from '@/utils/v4PlayoffFormat'

const props = defineProps<{
  teamCount: number
  bestOfList?: number[]
  roundNames?: string[]
  seedSource?: V4PlayoffSeedSource
}>()

const model = computed(() => buildPlayoffBracketModel({
  teamCount: props.teamCount,
  bestOfList: props.bestOfList,
  roundNames: props.roundNames,
  seedSource: props.seedSource
}))

function connectorStyle(c: PlayoffBracketConnector) {
  if (c.type === 'h') {
    return {
      left: c.x + 'px',
      top: c.y + 'px',
      width: Math.max(c.width, 1) + 'px',
      height: '0px'
    }
  }
  return {
    left: c.x + 'px',
    top: c.y + 'px',
    width: '0px',
    height: Math.max(c.height, 1) + 'px'
  }
}

function slotSeed(slot: { seedNo?: number; sourceMatchNo?: number; kind: string }) {
  if (slot.kind === 'seed' && slot.seedNo != null) return String(slot.seedNo)
  if (slot.sourceMatchNo != null) return 'W' + slot.sourceMatchNo
  return ''
}
</script>

<style scoped>
.playoff-bracket-wrap {
  width: 100%;
}
.playoff-bracket-meta {
  font-size: 13px;
  color: var(--el-text-color-regular);
  margin-bottom: 4px;
}
.playoff-bracket-hint {
  margin: 0 0 10px;
  font-size: 12px;
  color: var(--el-text-color-placeholder);
  line-height: 1.5;
}
.playoff-bracket-scroll {
  overflow-x: auto;
  overflow-y: visible;
  width: 100%;
}
.playoff-bracket {
  position: relative;
  min-width: 720px;
}
.bracket-round-head {
  position: absolute;
  top: 0;
  display: flex;
  flex-direction: column;
  gap: 2px;
  align-items: flex-start;
}
.bracket-round-name {
  font-size: 14px;
  font-weight: 600;
  color: var(--el-text-color-primary);
  line-height: 1.3;
}
.bracket-round-sub {
  font-size: 12px;
  color: var(--el-text-color-secondary);
}
.bracket-connector {
  position: absolute;
  pointer-events: none;
  z-index: 1;
}
.bracket-connector--h {
  border-top: 2px solid #c41e3a;
}
.bracket-connector--v {
  border-left: 2px solid #c41e3a;
}
.bracket-match {
  position: absolute;
  z-index: 2;
  display: flex;
  flex-direction: column;
  border: 1px solid var(--el-border-color);
  border-radius: 4px;
  overflow: hidden;
  background: var(--el-bg-color);
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.04);
}
.bracket-match-label {
  position: absolute;
  left: 0;
  top: -16px;
  font-size: 11px;
  color: var(--el-text-color-secondary);
}
.bracket-slot {
  display: flex;
  align-items: center;
  height: 50%;
  min-height: 0;
  font-size: 12px;
  line-height: 1.2;
}
.bracket-slot + .bracket-slot {
  border-top: 1px solid var(--el-border-color-lighter);
}
.bracket-seed {
  flex: 0 0 28px;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: var(--el-fill-color-light);
  color: var(--el-text-color-secondary);
  font-weight: 600;
  font-size: 11px;
}
.bracket-team {
  flex: 1;
  padding: 0 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.bracket-champion {
  position: absolute;
  z-index: 2;
  display: flex;
  align-items: center;
  justify-content: center;
  min-width: 88px;
  padding: 0 12px;
  border: 1px solid #c41e3a;
  border-radius: 4px;
  background: #fff5f5;
}
.bracket-champion-cup {
  font-size: 13px;
  font-weight: 700;
  color: #c41e3a;
}
</style>
