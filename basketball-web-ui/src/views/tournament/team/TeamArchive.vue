<script setup lang="ts">
import { inject, ref, watch } from 'vue'
import { getV4PublicTeamArchive } from '@/api/v4Public'
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

interface TeamHonorItem {
  id: string
  honorTitle: string
  competitionName: string
  date: string
  isLatest?: boolean
}

interface TeamNameHistoryItem {
  id: string
  name: string
  period: string
  note: string
  isCurrent?: boolean
}

interface TeamLogoHistoryItem {
  id: string
  label: string
  period: string
  logo?: string
  oldLogo?: string
  isCurrent?: boolean
}

const archiveBlocks: ArchiveBlock[] = [
  {
    key: 'honors',
    title: '球队荣誉',
    subtitle: 'Team Honors',
    emptyTitle: '暂无球队荣誉',
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

const honorState = ref<'loading' | 'success' | 'empty' | 'error'>('empty')
const honorError = ref('')
const honors = ref<TeamHonorItem[]>([])
const nameHistory = ref<TeamNameHistoryItem[]>([])
const logoHistory = ref<TeamLogoHistoryItem[]>([])
let reqId = 0

function formatPeriod(value?: string | null): string {
  const dt = formatDateTime(value || '')
  return dt.date === '--' ? '' : dt.date
}

function hideBrokenLogo(event: Event) {
  const img = event.target as HTMLImageElement | null
  if (img) img.style.display = 'none'
}

async function loadArchive() {
  const teamId = ctx.teamId.value
  const current = ++reqId
  if (!teamId) {
    honors.value = []
    nameHistory.value = []
    logoHistory.value = []
    honorState.value = 'empty'
    return
  }
  honorState.value = 'loading'
  honorError.value = ''
  try {
    const res = await getV4PublicTeamArchive(teamId)
    if (current !== reqId) return
    const data = res.data
    honors.value = (data?.honors ?? []).map((item, index) => ({
      id: String(item.honorId || index),
      honorTitle: displayText(item.honorTitle, '荣誉'),
      competitionName: displayText(item.competitionName, ''),
      date: formatPeriod(item.awardDate),
      isLatest: index === 0,
    }))
    nameHistory.value = (data?.nameHistory ?? []).map((item, index) => ({
      id: String(item.changeId || index),
      name: `${displayText(item.oldName, '--')} → ${displayText(item.newName, '--')}`,
      period: formatPeriod(item.approvedAt || item.submittedAt),
      note: '审核通过后生效',
      isCurrent: index === 0,
    }))
    logoHistory.value = (data?.logoHistory ?? []).map((item, index) => ({
      id: String(item.changeId || index),
      label: 'Logo 变更',
      period: formatPeriod(item.approvedAt || item.submittedAt),
      logo: item.newLogoUrl || '',
      oldLogo: item.oldLogoUrl || '',
      isCurrent: index === 0,
    }))
    honorState.value = honors.value.length ? 'success' : 'empty'
  } catch (err) {
    if (current !== reqId) return
    honors.value = []
    nameHistory.value = []
    logoHistory.value = []
    honorState.value = 'error'
    honorError.value = err instanceof Error ? err.message : '加载球队档案失败'
  }
}

watch(() => ctx.teamId.value, () => {
  void loadArchive()
}, { immediate: true })

function hasBlockData(key: ArchiveBlock['key']): boolean {
  if (key === 'honors') return honors.value.length > 0
  if (key === 'names') return nameHistory.value.length > 0
  return logoHistory.value.length > 0
}

function getHonorInitial(honor: TeamHonorItem): string {
  return honor.honorTitle.trim().slice(0, 1) || '荣'
}

function getNameInitial(item: TeamNameHistoryItem): string {
  return item.name.trim().slice(0, 1) || '名'
}

function getLogoInitial(item: TeamLogoHistoryItem): string {
  return item.label.trim().slice(0, 1) || 'L'
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
            <img v-if="item.oldLogo" :src="item.oldLogo" alt="变更前 Logo" @error="hideBrokenLogo" />
            <span v-else>{{ getLogoInitial(item) }}</span>
          </div>
          <span class="archive-item__arrow" aria-hidden="true">→</span>
          <div class="archive-item__logo">
            <img v-if="item.logo" :src="item.logo" alt="变更后 Logo" @error="hideBrokenLogo" />
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

.archive-item__arrow {
  color: rgba(0, 0, 0, 0.4);
  font-size: 16px;
  font-weight: 800;
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
