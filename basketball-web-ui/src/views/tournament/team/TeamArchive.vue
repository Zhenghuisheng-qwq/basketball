<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
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
}

type HonorTone = 'champion' | 'runner' | 'third' | 'team'

interface HonorMetal {
  light: string
  mid: string
  deep: string
}

interface TeamHonorItem {
  id: string
  honorTitle: string
  competitionName: string
  date: string
  dateIso: string
  tone: HonorTone
  codeLabel: string
  metal: HonorMetal
}

const HONOR_METAL: Record<HonorTone, HonorMetal> = {
  champion: { light: '#fff4d4', mid: '#e4c36a', deep: '#7a561c' },
  runner: { light: '#f7f9fc', mid: '#c5ced8', deep: '#5c6774' },
  third: { light: '#ffe4cc', mid: '#d08a58', deep: '#6b3b24' },
  team: { light: '#f3ecdf', mid: '#c4b39c', deep: '#5e5144' },
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

function formatHonorDate(value?: string | null): { text: string; iso: string } {
  const iso = formatArchiveDate(value)
  if (!iso) return { text: '', iso: '' }
  const parts = iso.split('-')
  if (parts.length !== 3 || parts.some((part) => !part)) return { text: '', iso: '' }
  return { text: parts.join('.'), iso }
}

function honorTone(code?: string | null): HonorTone {
  const normalized = String(code || '').trim().toUpperCase()
  if (normalized === 'CHAMPION') return 'champion'
  if (normalized === 'RUNNER_UP') return 'runner'
  if (normalized === 'THIRD_PLACE') return 'third'
  return 'team'
}

function honorCodeLabel(code?: string | null): string {
  const normalized = String(code || '').trim().toUpperCase()
  if (normalized === 'CHAMPION') return 'CHAMPION'
  if (normalized === 'RUNNER_UP') return 'RUNNER-UP'
  if (normalized === 'THIRD_PLACE') return 'THIRD PLACE'
  if (normalized === 'TEAM_HONOR') return 'TEAM HONOR'
  if (!normalized) return ''
  return normalized.replace(/_/g, ' ')
}

function metalId(index: number): string {
  return `team-honor-metal-${index}`
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
    honors.value = (data?.honors ?? []).map((item, index) => {
      const dated = formatHonorDate(item.awardDate)
      const tone = honorTone(item.honorCode)
      return {
        id: String(item.honorId || index),
        honorTitle: displayText(item.honorTitle, t('publicPages.team.honorFallback')),
        competitionName: rawText(item.competitionName),
        date: dated.text,
        dateIso: dated.iso,
        tone,
        codeLabel: honorCodeLabel(item.honorCode),
        metal: HONOR_METAL[tone],
      }
    })
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

    <section
      v-for="block in archiveBlocks"
      :key="block.key"
      class="archive-block"
      :class="{ 'archive-block--honors': block.key === 'honors' }"
    >
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
        :class="{ 'empty-row--honor': block.key === 'honors' }"
        role="status"
      >
        <div class="empty-row__text">
          <strong>{{ block.emptyTitle }}</strong>
          <span>{{ block.emptyDesc }}</span>
        </div>
      </div>

      <div v-else-if="block.key === 'honors'" class="honor-list">
        <article
          v-for="(honor, index) in honors"
          :key="honor.id"
          class="honor-card"
          :class="`honor-card--${honor.tone}`"
        >
          <svg class="honor-card__watermark" viewBox="0 0 160 188" aria-hidden="true">
            <path
              v-if="honor.tone === 'team'"
              d="M80 28a40 40 0 1 0 0 80 40 40 0 0 0 0-80zm0 16a24 24 0 1 1 0 48 24 24 0 0 1 0-48zM58 96l-12 62 22-18 12 28 12-28 22 18-12-62"
            />
            <path
              v-else
              d="M48 40h64c2 34-10 58-22 72-6 6-10 8-10 8s-4-2-10-8C58 98 46 74 48 40zm-8 8c-18 6-26 24-22 40 4 14 18 18 30 12M120 48c18 6 26 24 22 40-4 14-18 18-30 12M70 122h20l8 24H62zM46 148h68l6 10H40z"
            />
          </svg>

          <svg
            v-if="honor.tone === 'team'"
            class="honor-card__mark"
            viewBox="0 0 160 188"
            aria-hidden="true"
          >
            <defs>
              <linearGradient :id="metalId(index)" x1="0" y1="0" x2="0" y2="1">
                <stop offset="0%" :stop-color="honor.metal.light" />
                <stop offset="48%" :stop-color="honor.metal.mid" />
                <stop offset="100%" :stop-color="honor.metal.deep" />
              </linearGradient>
            </defs>
            <path
              d="M62 86 46 158l24-20 10 28 10-28 24 20-16-72"
              :fill="`url(#${metalId(index)})`"
              opacity="0.92"
            />
            <circle cx="80" cy="68" r="38" :fill="`url(#${metalId(index)})`" />
            <circle cx="80" cy="68" r="30" fill="none" stroke="#1a140e" stroke-width="1.4" opacity="0.35" />
            <circle cx="80" cy="68" r="22" fill="none" stroke="#fff8ea" stroke-width="1.2" opacity="0.45" />
            <path d="M80 52l4.2 9.2 10 .9-7.6 6.4 2.3 9.7L80 73.2 71.1 78.2l2.3-9.7-7.6-6.4 10-.9z" fill="#1a140e" opacity="0.55" />
            <path d="M58 52c6-10 14-16 22-16" fill="none" stroke="#fff8ea" stroke-width="3" stroke-linecap="round" opacity="0.55" />
          </svg>
          <svg v-else class="honor-card__mark" viewBox="0 0 160 188" aria-hidden="true">
            <defs>
              <linearGradient :id="metalId(index)" x1="0.15" y1="0" x2="0.85" y2="1">
                <stop offset="0%" :stop-color="honor.metal.light" />
                <stop offset="42%" :stop-color="honor.metal.mid" />
                <stop offset="100%" :stop-color="honor.metal.deep" />
              </linearGradient>
            </defs>
            <path
              d="M49 50c-20 5-30 24-25 42 4 15 20 20 34 13"
              fill="none"
              :stroke="`url(#${metalId(index)})`"
              stroke-width="7"
              stroke-linecap="round"
            />
            <path
              d="M111 50c20 5 30 24 25 42-4 15-20 20-34 13"
              fill="none"
              :stroke="`url(#${metalId(index)})`"
              stroke-width="7"
              stroke-linecap="round"
            />
            <path
              d="M46 42h68c1 32-10 56-22 70-6 7-10 9-12 9s-6-2-12-9C56 98 45 74 46 42z"
              :fill="`url(#${metalId(index)})`"
            />
            <ellipse cx="80" cy="42" rx="36" ry="10" :fill="`url(#${metalId(index)})`" />
            <ellipse cx="80" cy="42" rx="23" ry="5.5" fill="#1a140e" opacity="0.38" />
            <path
              d="M62 54c3 20 7 38 16 52"
              fill="none"
              stroke="#fff8ea"
              stroke-width="3.5"
              stroke-linecap="round"
              opacity="0.55"
            />
            <rect x="67" y="84" width="26" height="16" rx="1.5" fill="#120e0a" opacity="0.72" />
            <polygon
              v-if="honor.tone === 'champion'"
              points="80,87 82.2,92.2 87.8,92.6 83.4,96 84.8,101.4 80,98.4 75.2,101.4 76.6,96 72.2,92.6 77.8,92.2"
              fill="#fff6dc"
            />
            <rect v-else x="72" y="90" width="16" height="2.5" rx="1" fill="#fff6dc" opacity="0.7" />
            <path d="M72 122h16l7 24H65z" :fill="`url(#${metalId(index)})`" />
            <path d="M54 146h52l6 10H48z" fill="#16120c" />
            <rect x="40" y="156" width="80" height="14" rx="2" :fill="`url(#${metalId(index)})`" />
            <rect x="40" y="156" width="80" height="3" fill="#fff8ea" opacity="0.38" />
          </svg>

          <div class="honor-card__copy">
            <h4 class="honor-card__title" :title="honor.honorTitle">{{ honor.honorTitle }}</h4>
            <p v-if="honor.competitionName" class="honor-card__event" :title="honor.competitionName">
              {{ honor.competitionName }}
            </p>
            <time v-if="honor.date" class="honor-card__date" :datetime="honor.dateIso">{{ honor.date }}</time>
            <span v-if="honor.codeLabel" class="honor-card__code">{{ honor.codeLabel }}</span>
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
  margin-top: 36px;
}

.archive-block--honors .block-title {
  margin-bottom: 20px;
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

.empty-row--honor {
  min-height: 64px;
  padding: 12px 16px;
  background: rgba(255, 255, 255, 0.62);
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
  gap: 22px;
  align-items: stretch;
}

.honor-card {
  --ink: #f0e7d8;
  --muted: rgba(228, 216, 196, 0.72);
  --faint: rgba(228, 216, 196, 0.5);
  --metal-mid: #c4b39c;
  --border: rgba(168, 150, 124, 0.42);
  --border-hot: rgba(214, 196, 164, 0.78);
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  min-height: 228px;
  padding: 18px 16px 14px;
  overflow: hidden;
  border: 1px solid var(--border);
  background:
    linear-gradient(118deg, transparent 40%, rgba(255, 244, 226, 0.05) 50%, transparent 60%),
    radial-gradient(90% 52% at 50% 0%, rgba(196, 179, 156, 0.14), transparent 64%),
    linear-gradient(180deg, #1a1816 0%, #0e0d0c 100%);
  box-shadow: inset 0 1px 0 rgba(255, 244, 220, 0.12);
  box-sizing: border-box;
  text-align: center;
  transition: border-color 0.2s ease, box-shadow 0.2s ease;

  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 18px;
    right: 18px;
    height: 1px;
    background: linear-gradient(90deg, transparent, var(--metal-mid), transparent);
    opacity: 0.75;
    pointer-events: none;
  }

  &:hover {
    border-color: var(--border-hot);
    box-shadow:
      inset 0 1px 0 rgba(255, 244, 220, 0.2),
      0 8px 18px rgba(12, 9, 6, 0.22);
  }
}

.honor-card--champion {
  --ink: #f8efd8;
  --muted: rgba(243, 227, 194, 0.74);
  --faint: rgba(243, 227, 194, 0.5);
  --metal-mid: #e4c36a;
  --border: rgba(201, 163, 90, 0.52);
  --border-hot: rgba(232, 197, 106, 0.88);
  background:
    linear-gradient(118deg, transparent 38%, rgba(255, 236, 196, 0.08) 49%, transparent 60%),
    radial-gradient(90% 52% at 50% 0%, rgba(228, 195, 106, 0.18), transparent 62%),
    linear-gradient(180deg, #221c14 0%, #100e0c 100%);
}

.honor-card--runner {
  --ink: #f4f7fb;
  --muted: rgba(226, 232, 240, 0.76);
  --faint: rgba(226, 232, 240, 0.52);
  --metal-mid: #c5ced8;
  --border: rgba(170, 180, 194, 0.5);
  --border-hot: rgba(214, 222, 232, 0.86);
  background:
    linear-gradient(118deg, transparent 38%, rgba(255, 255, 255, 0.07) 49%, transparent 60%),
    radial-gradient(90% 52% at 50% 0%, rgba(206, 214, 224, 0.16), transparent 62%),
    linear-gradient(180deg, #1a1c20 0%, #0e0f12 100%);
}

.honor-card--third {
  --ink: #f8e6d6;
  --muted: rgba(240, 210, 188, 0.76);
  --faint: rgba(240, 210, 188, 0.52);
  --metal-mid: #d08a58;
  --border: rgba(184, 122, 78, 0.5);
  --border-hot: rgba(214, 156, 112, 0.86);
  background:
    linear-gradient(118deg, transparent 38%, rgba(255, 214, 180, 0.07) 49%, transparent 60%),
    radial-gradient(90% 52% at 50% 0%, rgba(208, 138, 88, 0.16), transparent 62%),
    linear-gradient(180deg, #211814 0%, #100e0c 100%);
}

.honor-card__watermark {
  position: absolute;
  z-index: 0;
  top: -12px;
  right: -42px;
  width: 176px;
  height: 206px;
  fill: var(--metal-mid);
  opacity: 0.09;
  pointer-events: none;
}

.honor-card__mark {
  position: relative;
  z-index: 1;
  width: 96px;
  height: 112px;
  flex-shrink: 0;
  filter: drop-shadow(0 8px 10px rgba(0, 0, 0, 0.32));
}

.honor-card__copy {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 100%;
  min-width: 0;
  flex: 1;
  margin-top: 8px;
}

.honor-card__title {
  display: -webkit-box;
  margin: 0;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  color: var(--ink);
  font-size: 22px;
  font-weight: 900;
  line-height: 1.2;
  overflow-wrap: anywhere;
}

.honor-card__event {
  display: -webkit-box;
  margin: 6px 0 0;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  color: var(--muted);
  font-size: 13px;
  font-weight: 700;
  line-height: 1.35;
  overflow-wrap: anywhere;
}

.honor-card__date {
  margin-top: 6px;
  color: var(--faint);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.06em;
}

.honor-card__code {
  margin-top: auto;
  padding-top: 10px;
  color: var(--metal-mid);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.18em;
  line-height: 1;
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

@media (max-width: 1024px) {
  .honor-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 991px) {
  .archive-cards {
    grid-template-columns: 1fr;
  }
}

@media (max-width: 640px) {
  .honor-list {
    grid-template-columns: minmax(0, 1fr);
    gap: 16px;
  }

  .honor-card {
    min-height: 212px;
  }
}

@media (prefers-reduced-motion: reduce) {
  .honor-card {
    transition: none;
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
