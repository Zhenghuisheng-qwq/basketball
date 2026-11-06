<script setup lang="ts">
import { computed, inject, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { getV4PublicTeamArchive } from '@/api/v4Public'
import HonorPlaque from '@/components/honor/HonorPlaque.vue'
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

interface TeamHonorItem {
  id: string
  honorTitle: string
  competitionName: string
  date: string
  dateIso: string
  tone: HonorTone
}

interface TeamNameHistoryItem {
  id: string
  oldName: string
  newName: string
  date: string
  dateIso: string
}

interface TeamLogoHistoryItem {
  id: string
  oldLogo: string
  newLogo: string
  date: string
  dateIso: string
}

interface NameCard extends TeamNameHistoryItem {
  displayName: string
  caption: string
  current: boolean
}

interface LogoCard extends TeamLogoHistoryItem {
  current: boolean
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

function formatArchiveDate(value?: string | null): { text: string; iso: string } {
  const iso = formatDateTime(value || '').date
  if (!iso || iso === '--') return { text: '', iso: '' }
  const parts = iso.split('-')
  if (parts.length !== 3 || parts.some((part) => !part)) return { text: '', iso: '' }
  return { text: parts.join('.'), iso }
}

function plaqueTone(tone: HonorTone): 'champion' | 'runner' | 'third' | 'award' {
  if (tone === 'champion' || tone === 'runner' || tone === 'third') return tone
  return 'award'
}

function honorTone(code?: string | null): HonorTone {
  const normalized = String(code || '').trim().toUpperCase()
  if (normalized === 'CHAMPION') return 'champion'
  if (normalized === 'RUNNER_UP') return 'runner'
  if (normalized === 'THIRD_PLACE') return 'third'
  return 'team'
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

const nameCards = computed<NameCard[]>(() => {
  const rows = [...nameHistory.value].reverse()
  return rows.map((item, index) => ({
    ...item,
    displayName: hasNamePair(item)
      ? item.newName
      : item.oldName || item.newName || t('publicPages.team.nameChange'),
    caption: hasNamePair(item)
      ? t('publicPages.team.renamedFrom', { name: item.oldName })
      : t('publicPages.team.nameChange'),
    current: index === rows.length - 1,
  }))
})

const logoCards = computed<LogoCard[]>(() => {
  const rows = [...logoHistory.value].reverse()
  return rows.map((item, index) => ({
    ...item,
    current: index === rows.length - 1,
  }))
})

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
      const dated = formatArchiveDate(item.awardDate)
      return {
        id: String(item.honorId || index),
        honorTitle: displayText(item.honorTitle, t('publicPages.team.honorFallback')),
        competitionName: rawText(item.competitionName),
        date: dated.text,
        dateIso: dated.iso,
        tone: honorTone(item.honorCode),
      }
    })
    nameHistory.value = (data?.nameHistory ?? []).map((item, index) => {
      const dated = formatArchiveDate(item.approvedAt || item.submittedAt)
      return {
        id: String(item.changeId || index),
        oldName: rawText(item.oldName),
        newName: rawText(item.newName),
        date: dated.text,
        dateIso: dated.iso,
      }
    })
    logoHistory.value = (data?.logoHistory ?? []).map((item, index) => {
      const dated = formatArchiveDate(item.approvedAt || item.submittedAt)
      return {
        id: String(item.changeId || index),
        oldLogo: rawText(item.oldLogoUrl),
        newLogo: rawText(item.newLogoUrl),
        date: dated.text,
        dateIso: dated.iso,
      }
    })
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
  if (key === 'names') return nameCards.value.length > 0
  return logoCards.value.length > 0
}
</script>

<template>
  <div class="team-archive">
    <header class="archive-intro">
      <h2 class="archive-intro__title">
        {{ t('publicPages.team.archiveTitle') }}
        <span>Team Archive</span>
      </h2>
      <p class="archive-intro__desc">{{ t('publicPages.team.archiveDesc') }}</p>
    </header>

    <div v-if="archiveState === 'loading'" class="archive-status" role="status">
      {{ t('publicPages.team.loading') }}
    </div>
    <div v-else-if="archiveState === 'error'" class="archive-status archive-status--error" role="alert">
      <strong>{{ t('publicPages.team.loadFailedShort') }}</strong>
      <span>{{ archiveError }}</span>
    </div>

    <template v-else>
      <section
        v-for="block in archiveBlocks"
        :key="block.key"
        class="archive-block"
      >
        <div class="archive-block__head">
          <div class="archive-block__title">
            <h3>{{ block.title }}</h3>
            <span>{{ block.subtitle }}</span>
          </div>
          <p v-if="block.key === 'honors' && honors.length" class="archive-block__meta">
            {{ t('publicPages.team.honorCount', { count: honors.length }) }}
          </p>
        </div>

        <div v-if="!hasBlockData(block.key)" class="archive-empty" role="status">
          <strong>{{ block.emptyTitle }}</strong>
          <span>{{ block.emptyDesc }}</span>
        </div>

        <div v-else-if="block.key === 'honors'" class="honor-list">
          <HonorPlaque
            v-for="honor in honors"
            :key="honor.id"
            :season="honor.competitionName"
            :title="honor.honorTitle"
            :date="honor.date"
            :tone="plaqueTone(honor.tone)"
          />
        </div>

        <ol v-else-if="block.key === 'names'" class="archive-timeline">
          <li v-for="item in nameCards" :key="item.id" class="archive-timeline__item">
            <div class="archive-timeline__rail" aria-hidden="true">
              <span class="archive-timeline__dot" />
            </div>
            <article class="archive-timeline__card" :class="{ 'is-current': item.current }">
              <span v-if="item.current" class="archive-timeline__tag">{{ t('publicPages.team.nameInUse') }}</span>
              <time v-if="item.date" class="archive-timeline__date" :datetime="item.dateIso">{{ item.date }}</time>
              <strong class="archive-timeline__name">{{ item.displayName }}</strong>
              <span class="archive-timeline__desc">{{ item.caption }}</span>
            </article>
          </li>
        </ol>

        <ol v-else class="archive-timeline">
          <li v-for="item in logoCards" :key="item.id" class="archive-timeline__item">
            <div class="archive-timeline__rail" aria-hidden="true">
              <span class="archive-timeline__dot" />
            </div>
            <article class="archive-timeline__card" :class="{ 'is-current': item.current }">
              <span v-if="item.current" class="archive-timeline__tag">{{ t('publicPages.team.logoInUse') }}</span>
              <time v-if="item.date" class="archive-timeline__date" :datetime="item.dateIso">{{ item.date }}</time>
              <div class="archive-timeline__logos">
                <img
                  v-if="item.oldLogo"
                  :src="item.oldLogo"
                  :alt="t('publicPages.team.logoBefore')"
                  :class="{ 'is-previous': Boolean(item.newLogo) }"
                  @error="hideBrokenLogo"
                />
                <span v-if="item.oldLogo && item.newLogo" class="archive-timeline__arrow" aria-hidden="true">→</span>
                <img
                  v-if="item.newLogo"
                  :src="item.newLogo"
                  :alt="t('publicPages.team.logoAfter')"
                  @error="hideBrokenLogo"
                />
                <span v-if="!item.oldLogo && !item.newLogo" class="archive-timeline__swatch">CN</span>
              </div>
              <span class="archive-timeline__desc">{{ t('publicPages.team.logoUpdated') }}</span>
            </article>
          </li>
        </ol>
      </section>

      <p class="archive-note">
        <svg viewBox="0 0 16 16" aria-hidden="true">
          <circle cx="8" cy="8" r="6.25" fill="none" stroke="currentColor" stroke-width="1.2" />
          <path d="M8 7.2v4.2" fill="none" stroke="currentColor" stroke-width="1.2" stroke-linecap="round" />
          <circle cx="8" cy="4.7" r="0.7" fill="currentColor" />
        </svg>
        <span>{{ t('publicPages.team.archiveNote') }}</span>
      </p>
    </template>
  </div>
</template>

<style scoped lang="scss">
.team-archive {
  color: var(--cnpa-paper-text);
}

.archive-intro {
  margin-bottom: 28px;
}

.archive-intro__title {
  display: flex;
  align-items: baseline;
  flex-wrap: wrap;
  gap: 12px;
  margin: 0;
  color: var(--cnpa-paper-text);
  font-size: clamp(28px, 2.6vw, 36px);
  font-weight: 900;
  letter-spacing: 0.02em;
  line-height: 1;

  span {
    color: var(--cnpa-red);
    font-size: 0.42em;
    font-style: italic;
    font-weight: 800;
    letter-spacing: 0.14em;
    text-transform: uppercase;
  }
}

.archive-intro__desc {
  margin: 10px 0 0;
  max-width: 640px;
  color: var(--cnpa-paper-text-secondary);
  font-size: 13px;
  font-weight: 600;
  line-height: 1.6;
}

.archive-status {
  padding: 28px 16px;
  border: 1px solid rgba(17, 17, 17, 0.08);
  background: rgba(255, 255, 255, 0.72);
  color: var(--cnpa-paper-text-secondary);
  font-size: 14px;
  font-weight: 700;
  text-align: center;
}

.archive-status--error {
  display: flex;
  flex-direction: column;
  gap: 4px;
  border-color: rgba(225, 6, 19, 0.18);
  color: var(--cnpa-red);

  span {
    color: var(--cnpa-paper-text-secondary);
    font-size: 12px;
    font-weight: 600;
  }
}

.archive-block + .archive-block {
  margin-top: 40px;
}

.archive-block__head {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.archive-block__title {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 10px;

  &::before {
    content: '';
    width: 4px;
    height: 22px;
    flex-shrink: 0;
    background: var(--cnpa-red);
  }

  h3 {
    margin: 0;
    color: var(--cnpa-paper-text);
    font-size: 22px;
    font-weight: 900;
    line-height: 1;
  }

  span {
    color: var(--cnpa-red);
    font-size: 12px;
    font-style: italic;
    font-weight: 800;
    letter-spacing: 0.1em;
    line-height: 1;
    text-transform: uppercase;
  }
}

.archive-block__meta {
  margin: 0;
  color: var(--cnpa-paper-text-secondary);
  font-size: 13px;
  font-weight: 700;
}

.archive-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 6px;
  min-height: 88px;
  padding: 20px 18px;
  border: 1px solid rgba(17, 17, 17, 0.08);
  background: rgba(255, 255, 255, 0.72);
  text-align: center;

  strong {
    color: var(--cnpa-paper-text);
    font-size: 15px;
    font-weight: 800;
  }

  span {
    color: var(--cnpa-paper-text-secondary);
    font-size: 12px;
    font-weight: 600;
    line-height: 1.5;
  }
}

.honor-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.archive-timeline {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 0 16px;
  margin: 0;
  padding: 0;
  list-style: none;
}

.archive-timeline__item {
  position: relative;
  min-width: 0;
  padding-top: 28px;
}

.archive-timeline__rail {
  position: absolute;
  top: 8px;
  right: 0;
  left: 0;
  height: 2px;
  background: rgba(17, 17, 17, 0.12);
}

.archive-timeline__dot {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 12px;
  height: 12px;
  border: 2px solid #171717;
  border-radius: 50%;
  background: #fff;
  transform: translate(-50%, -50%);
}

.archive-timeline__item:first-child .archive-timeline__rail {
  left: 50%;
}

.archive-timeline__item:last-child .archive-timeline__rail {
  right: 50%;
}

.archive-timeline__item:only-child .archive-timeline__rail {
  background: transparent;
}

.archive-timeline__card {
  position: relative;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 168px;
  padding: 22px 18px 18px;
  border: 1px solid rgba(17, 17, 17, 0.08);
  background: rgba(255, 255, 255, 0.86);
  box-sizing: border-box;
  text-align: center;

  &.is-current {
    border-color: rgba(201, 163, 90, 0.78);
    box-shadow: inset 0 0 0 1px rgba(201, 163, 90, 0.22);
  }
}

.archive-timeline__tag {
  position: absolute;
  top: 10px;
  right: 10px;
  padding: 3px 8px;
  background: #171717;
  color: #e8c56a;
  font-size: 11px;
  font-weight: 800;
  line-height: 1.2;
}

.archive-timeline__date {
  display: block;
  margin-bottom: 12px;
  color: var(--cnpa-paper-text-secondary);
  font-size: 13px;
  font-weight: 700;
}

.archive-timeline__name {
  display: block;
  margin-bottom: 10px;
  color: var(--cnpa-paper-text);
  font-size: 22px;
  font-weight: 900;
  line-height: 1.2;
  word-break: break-word;
}

.archive-timeline__desc {
  color: var(--cnpa-paper-text-secondary);
  font-size: 13px;
  font-weight: 700;
}

.archive-timeline__logos {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  min-height: 72px;
  margin-bottom: 10px;

  img {
    width: 64px;
    height: 64px;
    object-fit: contain;
  }

  img.is-previous {
    width: 40px;
    height: 40px;
    opacity: 0.55;
  }
}

.archive-timeline__arrow {
  color: rgba(17, 17, 17, 0.35);
  font-size: 16px;
  font-weight: 800;
}

.archive-timeline__swatch {
  display: grid;
  place-items: center;
  width: 64px;
  height: 64px;
  background: #151515;
  color: #fff;
  font-size: 16px;
  font-weight: 900;
}

.archive-note {
  display: flex;
  align-items: flex-start;
  gap: 6px;
  margin: 18px 0 0;
  color: var(--cnpa-paper-text-secondary);
  font-size: 12px;
  font-weight: 600;
  line-height: 1.5;

  svg {
    width: 14px;
    height: 14px;
    flex: 0 0 14px;
    margin-top: 1px;
  }
}

@media (max-width: 1100px) {
  .honor-list,
  .archive-timeline {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .archive-timeline {
    gap: 18px 16px;
  }

  .archive-timeline__item:nth-child(odd) .archive-timeline__rail {
    left: 50%;
  }

  .archive-timeline__item:nth-child(even) .archive-timeline__rail {
    right: 50%;
    left: 0;
  }

  .archive-timeline__item:last-child:nth-child(odd) .archive-timeline__rail {
    background: transparent;
  }
}

@media (max-width: 720px) {
  .archive-block__head {
    flex-direction: column;
    align-items: flex-start;
  }

  .honor-list,
  .archive-timeline {
    grid-template-columns: minmax(0, 1fr);
  }

  .archive-timeline {
    gap: 16px;
  }

  .archive-timeline__item {
    padding-top: 0;
    padding-left: 28px;
  }

  .archive-timeline__rail {
    top: 0;
    right: auto;
    bottom: -16px;
    left: 5px;
    width: 2px;
    height: auto;
  }

  .archive-timeline__item:nth-child(odd) .archive-timeline__rail,
  .archive-timeline__item:nth-child(even) .archive-timeline__rail,
  .archive-timeline__item:first-child .archive-timeline__rail,
  .archive-timeline__item:last-child .archive-timeline__rail {
    right: auto;
    left: 5px;
  }

  .archive-timeline__dot {
    top: 28px;
    left: 0;
  }

  .archive-timeline__item:first-child .archive-timeline__rail {
    top: 28px;
  }

  .archive-timeline__item:last-child .archive-timeline__rail {
    bottom: auto;
    height: 28px;
  }

  .archive-timeline__item:only-child .archive-timeline__rail {
    display: none;
  }
}
</style>
