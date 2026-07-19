<script setup lang="ts">
import { computed } from 'vue'
import type { TeamArchiveData, TeamHonor, TeamLogoHistoryItem, TeamNameHistoryItem } from '@/types/team'

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
    emptyTitle: '暂无球队荣誉',
    emptyDesc: '球队获得冠军荣誉后将在此展示',
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

const archiveData = computed<TeamArchiveData>(() => ({
  honors: [],
  nameHistory: [],
  logoHistory: [],
}))

function hasBlockData(key: ArchiveBlock['key']): boolean {
  if (key === 'honors') return archiveData.value.honors.length > 0
  if (key === 'names') return archiveData.value.nameHistory.length > 0
  return archiveData.value.logoHistory.length > 0
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

      <div v-if="!hasBlockData(block.key)" class="empty-card">
        <div class="empty-icon" :class="`empty-icon--${block.icon}`" aria-hidden="true" />
        <h4>{{ block.emptyTitle }}</h4>
        <p>{{ block.emptyDesc }}</p>
      </div>

      <div v-else-if="block.key === 'honors'" class="archive-list">
        <article
          v-for="honor in archiveData.honors"
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
          v-for="item in archiveData.nameHistory"
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
          v-for="item in archiveData.logoHistory"
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
  color: var(--cnpa-paper-text);
  font-size: clamp(22px, 2vw, 24px);
  line-height: 1;
  font-weight: 900;
  letter-spacing: -0.02em;
}

.block-title__en {
  color: var(--cnpa-red);
  font-size: clamp(12px, 1.1vw, 13px);
  font-weight: 900;
  font-style: italic;
  text-transform: uppercase;
  letter-spacing: 0.02em;
  white-space: nowrap;
}

.empty-card {
  min-height: 168px;
  padding: 28px 20px;
  border: 1px solid rgba(0, 0, 0, 0.18);
  background: rgba(255, 255, 255, 0.22);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  text-align: center;
  border-radius: 3px;
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.38);
  box-sizing: border-box;
}

.empty-icon {
  position: relative;
  width: 54px;
  height: 50px;
  margin-bottom: 14px;
  color: rgba(0, 0, 0, 0.32);
}

.empty-icon--trophy::before {
  content: '';
  position: absolute;
  left: 18px;
  top: 3px;
  width: 20px;
  height: 24px;
  border: 4px solid currentColor;
  border-top-width: 3px;
  border-radius: 3px 3px 9px 9px;
}

.empty-icon--trophy::after {
  content: '';
  position: absolute;
  left: 10px;
  top: 26px;
  width: 34px;
  height: 20px;
  border-bottom: 4px solid currentColor;
  border-left: 4px solid transparent;
  border-right: 4px solid transparent;
}

.empty-icon--card::before {
  content: '';
  position: absolute;
  left: 4px;
  top: 7px;
  width: 46px;
  height: 31px;
  border: 4px solid currentColor;
  border-radius: 2px;
}

.empty-icon--card::after {
  content: '';
  position: absolute;
  left: 13px;
  top: 17px;
  width: 28px;
  height: 12px;
  border-top: 4px solid currentColor;
  border-bottom: 4px solid currentColor;
}

.empty-icon--image::before {
  content: '';
  position: absolute;
  left: 5px;
  top: 5px;
  width: 44px;
  height: 36px;
  border: 4px solid currentColor;
  border-radius: 2px;
}

.empty-icon--image::after {
  content: '';
  position: absolute;
  left: 13px;
  top: 22px;
  width: 28px;
  height: 13px;
  border-left: 4px solid currentColor;
  border-bottom: 4px solid currentColor;
  transform: skewX(-26deg) rotate(-8deg);
}

.empty-card h4 {
  margin: 0;
  font-size: 19px;
  line-height: 1.2;
  color: var(--cnpa-paper-text);
  font-weight: 900;
}

.empty-card p {
  margin: 11px 0 0;
  font-size: 13px;
  color: rgba(0, 0, 0, 0.62);
  font-weight: 600;
}

.archive-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.archive-list--logos {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(240px, 1fr));
  gap: 14px;
}

.archive-item {
  display: flex;
  align-items: center;
  gap: 16px;
  min-height: 88px;
  padding: 16px 18px;
  border: 1px solid var(--cnpa-paper-border);
  border-radius: 3px;
  background: rgba(255, 255, 255, 0.28);
  box-shadow: inset 0 1px 0 rgba(255, 255, 255, 0.38);
  box-sizing: border-box;
}

.archive-item__badge,
.archive-item__logo {
  flex-shrink: 0;
  width: 52px;
  height: 52px;
  display: grid;
  place-items: center;
  border: 1px solid rgba(0, 0, 0, 0.12);
  border-radius: 3px;
  background: linear-gradient(135deg, #1a1a1a 0%, #3a3a3a 100%);
  color: #fff;
  font-size: 14px;
  font-weight: 900;
  letter-spacing: 0.04em;
}

.archive-item__logo img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  border-radius: 2px;
}

.archive-item__content {
  flex: 1;
  min-width: 0;
}

.archive-item__title {
  display: block;
  font-size: 16px;
  font-weight: 900;
  color: var(--cnpa-paper-text);
}

.archive-item__meta {
  display: block;
  margin-top: 6px;
  font-size: 13px;
  font-weight: 600;
  color: rgba(0, 0, 0, 0.58);
}

.archive-item__tag {
  flex-shrink: 0;
  height: 24px;
  padding: 0 10px;
  display: inline-flex;
  align-items: center;
  border: 1px solid rgba(225, 6, 19, 0.35);
  border-radius: 2px;
  background: rgba(225, 6, 19, 0.08);
  color: var(--cnpa-red);
  font-size: 12px;
  font-weight: 800;
}

.archive-note {
  margin: 17px 0 0;
  color: rgba(0, 0, 0, 0.56);
  font-size: 12px;
  font-weight: 600;
}

@media (max-width: 768px) {
  .archive-title {
    align-items: baseline;
  }

  .archive-list--logos {
    grid-template-columns: 1fr;
  }
}
</style>
