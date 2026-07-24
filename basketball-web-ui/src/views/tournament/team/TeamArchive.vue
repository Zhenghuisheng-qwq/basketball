<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import { getPublicTeamHonorPage } from '@/api/publicTeam'
import type { TeamHonor, TeamLogoHistoryItem, TeamNameHistoryItem } from '@/types/team'
import { displayText, formatDateTime } from '@/utils/publicDisplay'
import {
  PUBLIC_TEAM_PAGE_KEY,
  type PublicTeamPageContext,
} from './composables/usePublicTeamPage'

interface ArchiveBlock {
  key: 'honors' | 'names' | 'logos'
  title: string
  subtitle: string
  emptyTitle: string
  emptyDesc: string
  icon: 'trophy' | 'card' | 'image'
}

const archiveBlocks: ArchiveBlock[] = [
  {
    key: 'honors',
    title: '球队荣誉',
    subtitle: 'Team Honors',
    emptyTitle: '暂无荣誉记录',
    emptyDesc: '球队获得荣誉并发布后将在此展示',
    icon: 'trophy',
  },
  {
    key: 'names',
    title: '球队名称',
    subtitle: 'Name History',
    emptyTitle: '暂无球队名称变更记录',
    emptyDesc: '球队更名并通过赛事审核后将在此展示',
    icon: 'card',
  },
  {
    key: 'logos',
    title: '球队 Logo',
    subtitle: 'Logo History',
    emptyTitle: '暂无球队 Logo 变更记录',
    emptyDesc: '球队更换 Logo 并通过赛事审核后将在此展示',
    icon: 'image',
  },
]

const ctx = inject(PUBLIC_TEAM_PAGE_KEY) as PublicTeamPageContext
const honors = ref<TeamHonor[]>([])
const honorState = ref<'loading' | 'success' | 'empty' | 'error'>('loading')
const honorError = ref('')
let honorReq = 0

const nameHistory = computed<TeamNameHistoryItem[]>(() => {
  const team = ctx.teamView.value
  if (!team) return []
  return [
    {
      id: team.id,
      period: '当前',
      name: team.name,
      note: '当前使用名称',
      isCurrent: true,
    },
  ]
})

const logoHistory = computed<TeamLogoHistoryItem[]>(() => {
  const team = ctx.teamView.value
  if (!team) return []
  return [
    {
      id: team.id,
      period: '当前',
      label: team.name,
      logo: team.logoUrl,
      isCurrent: true,
    },
  ]
})

async function loadHonors(teamId: string) {
  const current = ++honorReq
  if (!teamId) {
    honors.value = []
    honorState.value = 'empty'
    return
  }
  honorState.value = 'loading'
  honorError.value = ''
  try {
    const res = await getPublicTeamHonorPage(teamId, { currentPage: 1, pageSize: 50 })
    if (current !== honorReq) return
    const records = res.data?.records ?? []
    honors.value = records.map((item, index) => ({
      id: String(item.honorId),
      competitionName: displayText(item.tournamentName || item.seasonName, '公开赛事'),
      honorTitle: displayText(item.honorTitle || item.honorName, '球队荣誉'),
      date: formatDateTime(item.awardDate).date,
      isLatest: index === 0,
    }))
    honorState.value = honors.value.length ? 'success' : 'empty'
  } catch (err) {
    if (current !== honorReq) return
    honors.value = []
    honorState.value = 'error'
    honorError.value = err instanceof Error ? err.message : '加载球队荣誉失败'
  }
}

watch(
  () => ctx.teamId.value,
  (teamId) => {
    void loadHonors(teamId)
  },
  { immediate: true },
)

function hasBlockData(key: ArchiveBlock['key']): boolean {
  if (key === 'honors') return honors.value.length > 0
  if (key === 'names') return nameHistory.value.length > 0
  return logoHistory.value.length > 0
}

function getHonorInitial(honor: TeamHonor): string {
  return honor.honorTitle.slice(0, 1).toUpperCase() || 'H'
}

function getNameInitial(item: TeamNameHistoryItem): string {
  return item.name.slice(0, 2).toUpperCase()
}

function getLogoInitial(item: TeamLogoHistoryItem): string {
  return item.label.slice(0, 2).toUpperCase() || 'LG'
}
</script>

<template>
  <div class="team-archive">
    <header class="archive-header">
      <div class="archive-title">
        <h2 class="archive-title__cn">球队档案</h2>
        <span class="archive-title__en">Team Archive</span>
      </div>
      <p class="archive-header__desc">记录球队参赛以来的冠军荣誉与品牌变化</p>
    </header>

    <section v-for="block in archiveBlocks" :key="block.key" class="archive-block">
      <div class="block-title">
        <h3 class="block-title__cn">{{ block.title }}</h3>
        <span class="block-title__en">{{ block.subtitle }}</span>
      </div>

      <div
        v-if="block.key === 'honors' && honorState === 'loading'"
        class="empty-card"
      >
        <h4>加载中...</h4>
      </div>
      <div
        v-else-if="block.key === 'honors' && honorState === 'error'"
        class="empty-card"
      >
        <h4>加载失败</h4>
        <p>{{ honorError }}</p>
      </div>
      <div v-else-if="!hasBlockData(block.key)" class="empty-card">
        <div class="empty-icon" :class="`empty-icon--${block.icon}`" aria-hidden="true" />
        <h4>{{ block.emptyTitle }}</h4>
        <p>{{ block.emptyDesc }}</p>
      </div>

      <div v-else-if="block.key === 'honors'" class="archive-list">
        <article
          v-for="honor in honors"
          :key="honor.id"
          class="archive-item archive-item--honor"
        >
          <div class="archive-item__badge">{{ getHonorInitial(honor) }}</div>
          <div class="archive-item__content">
            <strong class="archive-item__title">{{ honor.honorTitle }}</strong>
            <span class="archive-item__meta">{{ honor.competitionName }} · {{ honor.date }}</span>
          </div>
          <span v-if="honor.isLatest" class="archive-item__tag">最新</span>
        </article>
      </div>

      <div v-else-if="block.key === 'names'" class="archive-list">
        <article
          v-for="item in nameHistory"
          :key="item.id"
          class="archive-item archive-item--name"
        >
          <div class="archive-item__badge">{{ getNameInitial(item) }}</div>
          <div class="archive-item__content">
            <strong class="archive-item__title">{{ item.name }}</strong>
            <span class="archive-item__meta">{{ item.period }} · {{ item.note }}</span>
          </div>
          <span v-if="item.isCurrent" class="archive-item__tag">当前</span>
        </article>
      </div>

      <div v-else class="archive-list archive-list--logos">
        <article
          v-for="item in logoHistory"
          :key="item.id"
          class="archive-item archive-item--logo"
        >
          <div class="archive-item__logo">
            <img v-if="item.logo" :src="item.logo" :alt="item.label" />
            <span v-else>{{ getLogoInitial(item) }}</span>
          </div>
          <div class="archive-item__content">
            <strong class="archive-item__title">{{ item.label }}</strong>
            <span class="archive-item__meta">{{ item.period }}</span>
          </div>
          <span v-if="item.isCurrent" class="archive-item__tag">当前</span>
        </article>
      </div>
    </section>

    <p class="archive-note">ⓘ 仅展示赛事审核通过并实际使用过的球队历史记录</p>
  </div>
</template>

<style scoped lang="scss">
.team-archive {
  background: transparent;
  color: var(--cnpa-paper-text);
}

.archive-header {
  margin-bottom: 30px;
}

.archive-title {
  display: flex;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 15px;
}

.archive-title__cn {
  margin: 0;
  font-size: clamp(30px, 3vw, 34px);
  line-height: 1;
  font-weight: 900;
  letter-spacing: -0.02em;
  color: var(--cnpa-paper-text);
}

.archive-title__en {
  color: var(--cnpa-red);
  font-size: clamp(14px, 1.3vw, 16px);
  font-weight: 900;
  font-style: italic;
  text-transform: uppercase;
  letter-spacing: 0.02em;
  white-space: nowrap;
}

.archive-header__desc {
  margin: 13px 0 0;
  color: rgba(0, 0, 0, 0.6);
  font-size: 14px;
  font-weight: 600;
}

.archive-block {
  margin-top: 26px;
}

.block-title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;

  &::before {
    content: '';
    width: 4px;
    height: 26px;
    background: var(--cnpa-red);
    flex-shrink: 0;
  }
}

.block-title__cn {
  margin: 0;
  font-size: 22px;
  font-weight: 900;
}

.block-title__en {
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
}

.empty-card {
  padding: 28px 22px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: rgba(255, 255, 255, 0.28);

  h4 {
    margin: 0 0 8px;
    font-size: 16px;
  }

  p {
    margin: 0;
    color: rgba(0, 0, 0, 0.55);
    font-size: 13px;
  }
}

.empty-icon {
  width: 36px;
  height: 36px;
  margin-bottom: 12px;
  border-radius: 50%;
  background: rgba(225, 6, 19, 0.12);
}

.archive-list {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.archive-item {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 14px 16px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  background: rgba(255, 255, 255, 0.22);
}

.archive-item__badge,
.archive-item__logo {
  display: grid;
  place-items: center;
  width: 44px;
  height: 44px;
  flex-shrink: 0;
  background: #111;
  color: #fff;
  font-size: 13px;
  font-weight: 900;
  overflow: hidden;

  img {
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
}

.archive-item__content {
  min-width: 0;
  flex: 1;
}

.archive-item__title {
  display: block;
  color: #111;
  font-size: 15px;
  font-weight: 800;
}

.archive-item__meta {
  display: block;
  margin-top: 4px;
  color: rgba(0, 0, 0, 0.55);
  font-size: 12px;
}

.archive-item__tag {
  color: var(--cnpa-red);
  font-size: 12px;
  font-weight: 800;
}

.archive-note {
  margin: 28px 0 0;
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
}
</style>
