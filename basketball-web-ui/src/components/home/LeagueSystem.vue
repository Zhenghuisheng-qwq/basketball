<script setup lang="ts">
import { computed } from 'vue'
import TornEdge from '@/components/home/TornEdge.vue'
import leagueCollageImg from '@/assets/icon/home/data.png'
import type { PublicSeasonSummaryVo } from '@/api/types/public'

type SectionState = 'loading' | 'success' | 'empty' | 'error'

const props = defineProps<{
  summary: PublicSeasonSummaryVo | null
  state: SectionState
  errorMessage?: string
}>()

const emit = defineEmits<{
  retry: []
}>()

type LeagueStatItem = {
  key: string
  value: string
  label: string
}

function formatStatValue(raw: unknown): string {
  if (raw === null || raw === undefined || raw === '') return '0'
  const numeric = Number(raw)
  if (!Number.isFinite(numeric)) return '0'
  if (Number.isInteger(numeric)) return String(numeric)
  return String(numeric)
}

const stats = computed<LeagueStatItem[]>(() => {
  const loadingOrError = props.state === 'loading' || props.state === 'error'
  const summary = props.summary

  const items = [
    { key: 'teams', raw: summary?.teamCount, label: '支参赛战队' },
    { key: 'players', raw: summary?.playerStatCount, label: '球员数据记录' },
    { key: 'matches', raw: summary?.finishedMatchCount, label: '已结束比赛' },
    { key: 'standings', raw: summary?.standingCount, label: '积分榜记录' },
  ]

  return items.map((item) => ({
    key: item.key,
    label: item.label,
    value: loadingOrError ? '--' : formatStatValue(item.raw),
  }))
})

const showStatError = computed(() => props.state === 'error')
</script>

<template>
  <section class="site-section league-system-section">
    <div class="site-shell">
      <div class="paper-band paper-band-light">
        <TornEdge position="top" tone="light" variant="soft" />
        <div class="paper-band-inner league-system-paper">
          <div class="league-system-layout">
            <div class="league-system-copy">
              <h2 class="league-system-title">
                <span class="league-system-title__line">关于 CNPA</span>
                <span class="league-system-title__accent">联赛体系</span>
              </h2>
              <p class="league-system-description">
                CNPA 中国职业电竞篮球联赛，致力于打造专业、公平、热血的线上篮球竞技舞台。通过全年多级赛事体系，推动电竞篮球运动的发展与普及。
              </p>
            </div>

            <div class="league-system-stats" aria-label="联赛统计数据">
              <div
                v-for="item in stats"
                :key="item.key"
                class="league-stat-item"
              >
                <strong>{{ item.value }}</strong>
                <span>{{ item.label }}</span>
              </div>
              <p v-if="showStatError" class="league-system-stats__error">
                {{ errorMessage || '数据加载失败' }}
                <button type="button" @click="emit('retry')">重试</button>
              </p>
            </div>

            <div class="league-system-visual">
              <img
                class="league-visual-collage"
                :src="leagueCollageImg"
                alt=""
                loading="lazy"
                draggable="false"
              />
            </div>
          </div>
        </div>
        <TornEdge position="bottom" tone="light" variant="sharp" />
      </div>
    </div>
  </section>
</template>
