<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { getV4PublicTeamArchive } from '@/api/v4Public'
import cupIcon from '@/assets/icon/home/cup.png'
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
}

interface TeamHonorItem {
  id: string
  honorTitle: string
  competitionName: string
  date: string
}

interface TeamNameHistoryItem {
  id: string
  oldName: string
  newName: string
  date: string
}

interface TeamLogoHistoryItem {
  id: string
  oldLogo: string
  newLogo: string
  date: string
}

const ctx = inject(PUBLIC_TEAM_PAGE_KEY) as PublicTeamPageContext
const { t } = useI18n()

const archiveBlocks = computed<ArchiveBlock[]>(() => [
  {
    key: 'honors',
    title: t('publicPages.team.honors'),
    subtitle: 'Team Honors',
    emptyTitle: t('publicPages.team.noHonors'),
    emptyDesc: t('publicPages.team.noHonorsHint'),
  },
  {
    key: 'names',
    title: t('publicPages.team.names'),
    subtitle: 'Name History',
    emptyTitle: t('publicPages.team.noNames'),
    emptyDesc: t('publicPages.team.noNamesHint'),
  },
  {
    key: 'logos',
    title: t('publicPages.team.logos'),
    subtitle: 'Logo History',
    emptyTitle: t('publicPages.team.noLogos'),
    emptyDesc: t('publicPages.team.noLogosHint'),
  },
])

const archiveState = ref<'loading' | 'success' | 'empty' | 'error'>('empty')
const archiveError = ref('')
const honors = ref<TeamHonorItem[]>([])
const nameHistory = ref<TeamNameHistoryItem[]>([])
const logoHistory = ref<TeamLogoHistoryItem[]>([])
let reqId = 0

function formatArchiveDate(value?: string | null): string {
  const dt = formatDateTime(value || '')
  return dt.date === '--' ? '' : dt.date
}

function rawText(value?: string | null): string {
  return value == null ? '' : String(value).trim()
}

function hideBrokenLogo(event: Event) {
  const img = event.target as HTMLImageElement | null
  if (img) img.style.display = 'none'
}

function hasNamePair(item: TeamNameHistoryItem): boolean {
  return Boolean(item.oldName && item.newName)
}

function singleName(item: TeamNameHistoryItem): string {
  return item.oldName || item.newName || t('publicPages.team.nameChange')
}

async function loadArchive() {
  const teamId = ctx.teamId.value
  const current = ++reqId
  if (!teamId) {
    honors.value = []
    nameHistory.value = []
    logoHistory.value = []
    archiveState.value = 'empty'
    return
  }
  archiveState.value = 'loading'
  archiveError.value = ''
  try {
    const res = await getV4PublicTeamArchive(teamId)
    if (current !== reqId) return
    const data = res.data
    honors.value = (data?.honors ?? []).map((item, index) => ({
      id: String(item.honorId || index),
      honorTitle: displayText(item.honorTitle, t('publicPages.team.honorFallback')),
      competitionName: rawText(item.competitionName),
      date: formatArchiveDate(item.awardDate),
    }))
    nameHistory.value = (data?.nameHistory ?? []).map((item, index) => ({
      id: String(item.changeId || index),
      oldName: rawText(item.oldName),
      newName: rawText(item.newName),
      date: formatArchiveDate(item.approvedAt || item.submittedAt),
    }))
    logoHistory.value = (data?.logoHistory ?? []).map((item, index) => ({
      id: String(item.changeId || index),
      oldLogo: rawText(item.oldLogoUrl),
      newLogo: rawText(item.newLogoUrl),
      date: formatArchiveDate(item.approvedAt || item.submittedAt),
    }))
    archiveState.value =
      honors.value.length || nameHistory.value.length || logoHistory.value.length
        ? 'success'
        : 'empty'
  } catch (err) {
    if (current !== reqId) return
    honors.value = []
    nameHistory.value = []
    logoHistory.value = []
    archiveState.value = 'error'
    archiveError.value = err instanceof Error ? err.message : t('publicPages.team.loadArchiveFailed')
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
</script>

<template>
  <div class="team-archive">
    <header class="archive-header">
      <div class="archive-title">
        <h2 class="archive-title__cn">{{ t('publicPages.team.archiveTitle') }}</h2>
        <span class="archive-title__en">Team Archive</span>
      </div>
      <p class="archive-header__desc">{{ t('publicPages.team.archiveDesc') }}</p>
    </header>

    <section v-for="block in archiveBlocks" :key="block.key" class="archive-block">
      <div class="block-title">
        <h3 class="block-title__cn">{{ block.title }}</h3>
        <span class="block-title__en">{{ block.subtitle }}</span>
      </div>

      <div
        v-if="archiveState === 'loading'"
        class="empty-row"
        role="status"
      >
        <div class="empty-row__text">
          <strong>{{ t('publicPages.team.loading') }}</strong>
        </div>
      </div>
      <div
        v-else-if="archiveState === 'error'"
        class="empty-row empty-row--error"
        role="alert"
      >
        <div class="empty-row__text">
          <strong>{{ t('publicPages.team.loadFailedShort') }}</strong>
          <span>{{ archiveError }}</span>
        </div>
      </div>
      <div
        v-else-if="!hasBlockData(block.key)"
        class="empty-row"
        role="status"
      >
        <div class="empty-row__text">
          <strong>{{ block.emptyTitle }}</strong>
          <span>{{ block.emptyDesc }}</span>
        </div>
      </div>

      <div v-else-if="block.key === 'honors'" class="honor-list">
        <article
          v-for="honor in honors"
          :key="honor.id"
          class="honor-card"
        >
          <img class="honor-card__cup" :src="cupIcon" alt="" />
          <div class="honor-card__body">
            <span v-if="honor.competitionName" class="honor-card__event">{{ honor.competitionName }}</span>
            <strong class="honor-card__title">{{ honor.honorTitle }}</strong>
            <span v-if="honor.date" class="honor-card__date">{{ honor.date }}</span>
          </div>
        </article>
      </div>

      <div v-else-if="block.key === 'names'" class="archive-cards">
        <article v-for="item in nameHistory" :key="item.id" class="archive-card">
          <time v-if="item.date">{{ item.date }}</time>
          <strong>{{ hasNamePair(item) ? item.newName : singleName(item) }}</strong>
          <span v-if="hasNamePair(item)">{{ item.oldName }} → {{ item.newName }}</span>
          <span v-else>{{ t('publicPages.team.nameChange') }}</span>
        </article>
      </div>

      <div v-else class="archive-cards">
        <article v-for="item in logoHistory" :key="item.id" class="archive-card archive-card--logo">
          <div class="archive-card__logos">
            <img
              v-if="item.oldLogo"
              :src="item.oldLogo"
              :alt="t('publicPages.team.logoBefore')"
              @error="hideBrokenLogo"
            />
            <img
              v-if="item.newLogo"
              :src="item.newLogo"
              :alt="t('publicPages.team.logoAfter')"
              @error="hideBrokenLogo"
            />
            <span v-if="!item.oldLogo && !item.newLogo" class="archive-card__swatch">CN</span>
          </div>
          <time v-if="item.date">{{ item.date }}</time>
          <span>{{ t('publicPages.team.logoUpdated') }}</span>
        </article>
      </div>
    </section>

    <p class="archive-note">{{ t('publicPages.team.archiveNote') }}</p>
  </div>
</template>

<style scoped lang="scss">
.team-archive {
  background: transparent;
  color: var(--cnpa-paper-text);
}

.archive-header {
  margin-bottom: 24px;
}

.archive-title {
  display: flex;
  align-items: flex-end;
  flex-wrap: wrap;
  gap: 12px;
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
  margin: 10px 0 0;
  color: rgba(0, 0, 0, 0.6);
  font-size: 14px;
  font-weight: 600;
}

.archive-block + .archive-block {
  margin-top: 32px;
}

.block-title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 14px;

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
  line-height: 1;
}

.block-title__en {
  color: rgba(0, 0, 0, 0.45);
  font-size: 12px;
  font-weight: 800;
  letter-spacing: 0.08em;
  text-transform: uppercase;
  line-height: 1;
}

.empty-row {
  display: flex;
  align-items: center;
  min-height: 76px;
  padding: 14px 16px;
  border: 1px solid rgba(17, 17, 17, 0.08);
  background: rgba(243, 242, 241, 0.72);
  box-sizing: border-box;
}

.empty-row--error {
  border-color: rgba(225, 6, 19, 0.18);
}

.empty-row__text {
  display: flex;
  flex-direction: column;
  gap: 4px;
  min-width: 0;

  strong {
    color: rgba(17, 17, 17, 0.78);
    font-size: 14px;
    font-weight: 800;
    line-height: 1.3;
  }

  span {
    color: rgba(17, 17, 17, 0.48);
    font-size: 12px;
    font-weight: 600;
    line-height: 1.45;
  }
}

.honor-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.honor-card {
  display: flex;
  align-items: center;
  gap: 14px;
  min-height: 118px;
  padding: 16px 18px;
  border: 1px solid rgba(201, 163, 90, 0.48);
  background: linear-gradient(180deg, #1c1a16 0%, #0f0e0c 100%);
  box-sizing: border-box;
}

.honor-card__cup {
  width: 52px;
  height: 52px;
  object-fit: contain;
  flex-shrink: 0;
}

.honor-card__body {
  min-width: 0;
}

.honor-card__event,
.honor-card__date {
  display: block;
  color: rgba(243, 227, 194, 0.62);
  font-size: 12px;
  font-weight: 700;
}

.honor-card__title {
  display: block;
  margin: 6px 0;
  color: #e8c56a;
  font-size: 22px;
  font-weight: 900;
  line-height: 1.15;
}

.archive-cards {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.archive-card {
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 8px;
  min-height: 148px;
  padding: 18px;
  border: 1px solid rgba(17, 17, 17, 0.08);
  background: rgba(255, 255, 255, 0.78);
  box-sizing: border-box;

  time,
  span {
    color: rgba(17, 17, 17, 0.5);
    font-size: 12px;
    font-weight: 700;
  }

  strong {
    color: #111;
    font-size: 22px;
    font-weight: 900;
    line-height: 1.2;
    word-break: break-word;
  }
}

.archive-card__logos {
  display: flex;
  align-items: center;
  gap: 10px;
  min-height: 64px;

  img {
    width: 64px;
    height: 64px;
    object-fit: contain;
  }
}

.archive-card__swatch {
  display: grid;
  place-items: center;
  width: 64px;
  height: 64px;
  background: #151515;
  color: #fff !important;
  font-size: 16px;
  font-weight: 900;
}

@media (max-width: 991px) {
  .honor-list,
  .archive-cards {
    grid-template-columns: 1fr;
  }
}

.archive-timeline {
  margin: 0;
  padding: 0;
  list-style: none;
}

.timeline-item {
  display: flex;
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid rgba(17, 17, 17, 0.06);

  &:last-child {
    border-bottom: 0;
    padding-bottom: 2px;
  }
}

.timeline-item__rail {
  display: flex;
  justify-content: center;
  width: 12px;
  flex-shrink: 0;
  position: relative;

  &::after {
    content: '';
    position: absolute;
    top: 16px;
    bottom: -14px;
    left: 50%;
    width: 1px;
    background: rgba(17, 17, 17, 0.1);
    transform: translateX(-50%);
  }
}

.timeline-item:last-child .timeline-item__rail::after {
  display: none;
}

.timeline-item__dot {
  position: relative;
  z-index: 1;
  width: 8px;
  height: 8px;
  margin-top: 5px;
  border-radius: 50%;
  background: var(--cnpa-red);
  box-shadow: 0 0 0 3px rgba(225, 6, 19, 0.08);
}

.timeline-item__body {
  min-width: 0;
  flex: 1;
  padding: 2px 8px 2px 0;
  border-radius: 0;
  transition: background 0.18s ease;
}

.timeline-item:hover .timeline-item__body {
  background: rgba(255, 255, 255, 0.55);
}

.timeline-item__date {
  display: block;
  color: rgba(17, 17, 17, 0.45);
  font-size: 12px;
  font-weight: 700;
}

.timeline-item__change {
  display: flex;
  flex-wrap: wrap;
  align-items: baseline;
  gap: 8px;
  margin-top: 4px;
  color: rgba(17, 17, 17, 0.62);
  font-size: 15px;
  font-weight: 700;

  strong {
    color: #111;
    font-weight: 800;
  }
}

.timeline-item__title {
  display: block;
  margin-top: 4px;
  color: #111;
  font-size: 15px;
  font-weight: 800;
}

.timeline-item__arrow {
  color: rgba(17, 17, 17, 0.35);
  font-size: 14px;
  font-weight: 800;
}

.timeline-item__caption {
  display: block;
  margin-top: 6px;
  color: rgba(17, 17, 17, 0.48);
  font-size: 12px;
  font-weight: 600;
}

.logo-compare {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
  margin-top: 8px;
}

.logo-compare__mark {
  display: grid;
  place-items: center;
  width: 48px;
  height: 48px;
  flex-shrink: 0;
  overflow: hidden;
  border: 1px solid rgba(17, 17, 17, 0.08);
  background: #fff;

  img {
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
}

.archive-note {
  margin: 24px 0 0;
  color: rgba(0, 0, 0, 0.42);
  font-size: 12px;
}

@media (max-width: 640px) {
  .empty-row {
    min-height: 70px;
    padding: 12px 14px;
  }

  .timeline-item--logo .logo-compare {
    flex-direction: column;
    align-items: flex-start;
  }

  .timeline-item--logo .timeline-item__arrow {
    transform: rotate(90deg);
    margin-left: 16px;
  }
}
</style>
