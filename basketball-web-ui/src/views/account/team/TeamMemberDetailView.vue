<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute, useRouter } from 'vue-router'
import AccountPageHeader from '@/components/account/AccountPageHeader.vue'
import AccountPanel from '@/components/account/AccountPanel.vue'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import { getTeamMemberDetail } from '@/api/teamMember'
import type { TeamMemberDetailDto } from '@/api/types/teamMember'
import { resolveMemberAvatarUrl } from '@/utils/memberAvatar'
import { goToPlayerDetail } from '@/utils/playerNavigate'
import { formatDateTime } from '@/utils/formatDate'

const { t } = useI18n()
const route = useRoute()
const router = useRouter()
const KNOWN_POSITIONS = ['PG', 'SG', 'SF', 'PF', 'C'] as const

function positionText(code?: string | null, name?: string | null) {
  if (!code) return name || t('accountTeam.position.unsetLong')
  const normalized = code.trim().toUpperCase()
  if ((KNOWN_POSITIONS as readonly string[]).includes(normalized)) {
    return `${normalized} ${t(`accountTeam.position.${normalized}`)}`
  }
  return name ? `${normalized} ${name}` : normalized
}

const loading = ref(false)
const loadError = ref('')
const detail = ref<TeamMemberDetailDto | null>(null)

const teamId = computed(() => String(route.params.teamId || ''))
const teamMemberId = computed(() => String(route.params.teamMemberId || ''))

const avatarUrl = computed(() => resolveMemberAvatarUrl(detail.value?.avatarUrl))
const gameIdLines = computed(() => {
  const items = detail.value?.platformGameIds || []
  if (items.length) {
    return items.map((item) => {
      const platform = item.platformCode || item.platformName || t('accountTeam.memberDetail.unknownPlatform')
      return t('accountTeam.common.platformId', { platform, id: item.gameId || '-' })
    })
  }
  const summary = detail.value?.gameIdSummary?.trim() || ''
  return summary.split(/\s*\/\s*/).filter((part) => /[:：]/.test(part))
})

const hasStatSummary = computed(() => {
  const d = detail.value
  if (!d) return false
  return [d.totalMatches, d.totalPoints, d.totalRebounds, d.totalAssists].some((v) => v != null)
})

const hasEventInfo = computed(() => {
  const d = detail.value
  if (!d) return false
  return Boolean(d.currentEventName || d.latestMatchSummary || d.participatedEventCount)
})

async function loadDetail() {
  if (!teamId.value || !teamMemberId.value) {
    loadError.value = t('accountTeam.memberDetail.missingParams')
    detail.value = null
    return
  }
  loading.value = true
  loadError.value = ''
  try {
    const res = await getTeamMemberDetail(teamId.value, teamMemberId.value)
    detail.value = res.data
  } catch (error) {
    detail.value = null
    loadError.value = error instanceof Error ? error.message : t('accountTeam.memberDetail.loadFailed')
  } finally {
    loading.value = false
  }
}

function goBackMembers() {
  router.push({ name: 'AccountMyTeam', query: { tab: 'members' } })
}

function openPublicCareer() {
  if (!detail.value?.playerId) return
  goToPlayerDetail(router, detail.value.playerId, 'data', { dataMode: 'career' })
}

function displayOrDash(value?: string | number | null) {
  if (value === undefined || value === null || value === '') return '-'
  return String(value)
}

watch([teamId, teamMemberId], () => {
  loadDetail()
})

onMounted(() => {
  loadDetail()
})
</script>

<template>
  <div class="account-view account-view--team-member-detail account-center-vars">
    <AccountPageHeader
      :zh-title="t('accountTeam.memberDetail.title')"
      :en-title="t('accountTeam.memberDetail.titleEn')"
      :breadcrumb-items="[
        { label: t('accountTeam.breadcrumb.home'), to: '/home' },
        { label: t('accountTeam.breadcrumb.account'), to: '/account/overview' },
        { label: t('accountTeam.breadcrumb.team'), to: '/account/team' },
        { label: t('accountTeam.breadcrumb.member') },
      ]"
    />

    <div v-if="loading" class="member-detail__state">{{ t('accountTeam.memberDetail.loading') }}</div>
    <AccountEmptyState v-else-if="loadError" :message="loadError" />

    <template v-else-if="detail">
      <section class="member-detail__hero">
        <div class="member-detail__hero-main">
          <div class="member-detail__avatar">
            <img :src="avatarUrl" :alt="detail.nickname || t('accountTeam.memberDetail.avatarAlt')" />
          </div>
          <div class="member-detail__hero-meta">
            <div class="member-detail__name-row">
              <h2>{{ detail.nickname || t('accountTeam.memberDetail.unnamed') }}</h2>
              <AccountStatusBadge
                v-if="detail.isCaptain"
                type="danger"
                :text="t('accountTeam.memberDetail.captain')"
              />
              <AccountStatusBadge
                v-else
                type="default"
                :text="detail.roleInTeamText || t('accountTeam.memberDetail.player')"
              />
            </div>
            <p class="member-detail__sub">{{ detail.playerNo || t('accountTeam.memberDetail.noPlayerNo') }}</p>
            <p class="member-detail__sub">
              {{ detail.teamName || '-' }}
              · {{ positionText(detail.positionCode, detail.positionName) }}
              · {{ detail.memberStatusText || '-' }}
            </p>
          </div>
        </div>
        <div class="member-detail__hero-actions">
          <button type="button" class="account-btn-secondary" @click="goBackMembers">{{ t('accountTeam.memberDetail.back') }}</button>
          <button
            v-if="detail.playerId"
            type="button"
            class="account-btn-secondary"
            @click="openPublicCareer"
          >
            {{ t('accountTeam.memberDetail.career') }}
          </button>
        </div>
      </section>

      <div class="member-detail__grid">
        <AccountPanel :title="t('accountTeam.memberDetail.basic')">
          <dl class="member-detail__dl">
            <div><dt>{{ t('accountTeam.memberDetail.nickname') }}</dt><dd>{{ displayOrDash(detail.nickname) }}</dd></div>
            <div><dt>{{ t('accountTeam.memberDetail.playerNo') }}</dt><dd>{{ displayOrDash(detail.playerNo) }}</dd></div>
            <div><dt>{{ t('accountTeam.memberDetail.position') }}</dt><dd>{{ detail.positionCode || detail.positionName ? positionText(detail.positionCode, detail.positionName) : '-' }}</dd></div>
            <div><dt>{{ t('accountTeam.memberDetail.joinedAt') }}</dt><dd>{{ detail.joinTime ? formatDateTime(detail.joinTime) : '-' }}</dd></div>
            <div><dt>{{ t('accountTeam.memberDetail.jerseyNo') }}</dt><dd>{{ displayOrDash(detail.jerseyNo) }}</dd></div>
            <div><dt>{{ t('accountTeam.memberDetail.role') }}</dt><dd>{{ displayOrDash(detail.roleInTeamText) }}</dd></div>
            <div><dt>{{ t('accountTeam.memberDetail.memberStatus') }}</dt><dd>{{ displayOrDash(detail.memberStatusText) }}</dd></div>
            <div><dt>{{ t('accountTeam.memberDetail.source') }}</dt><dd>{{ displayOrDash(detail.sourceTypeText || detail.sourceType) }}</dd></div>
          </dl>
        </AccountPanel>

        <AccountPanel :title="t('accountTeam.memberDetail.gameIds')">
          <ul v-if="gameIdLines.length" class="member-detail__gameids">
            <li v-for="line in gameIdLines" :key="line">{{ line }}</li>
          </ul>
          <p v-else class="member-detail__empty">{{ t('accountTeam.memberDetail.unbound') }}</p>
          <p class="member-detail__hint">{{ t('accountTeam.memberDetail.gameIdHint') }}</p>
        </AccountPanel>

        <AccountPanel :title="t('accountTeam.memberDetail.eventInfo')">
          <template v-if="hasEventInfo">
            <dl class="member-detail__dl">
              <div><dt>{{ t('accountTeam.memberDetail.currentEvent') }}</dt><dd>{{ displayOrDash(detail.currentEventName) }}</dd></div>
              <div><dt>{{ t('accountTeam.memberDetail.currentPlatform') }}</dt><dd>{{ displayOrDash(detail.currentEventPlatform) }}</dd></div>
              <div><dt>{{ t('accountTeam.memberDetail.rosterStatus') }}</dt><dd>{{ displayOrDash(detail.currentRosterStatus) }}</dd></div>
              <div><dt>{{ t('accountTeam.memberDetail.eventCount') }}</dt><dd>{{ displayOrDash(detail.participatedEventCount) }}</dd></div>
              <div><dt>{{ t('accountTeam.memberDetail.latestMatch') }}</dt><dd>{{ displayOrDash(detail.latestMatchSummary || detail.latestMatchTime) }}</dd></div>
            </dl>
          </template>
          <p v-else class="member-detail__empty">{{ t('accountTeam.memberDetail.eventEmpty') }}</p>
        </AccountPanel>

        <AccountPanel :title="t('accountTeam.memberDetail.stats')">
          <template v-if="hasStatSummary">
            <dl class="member-detail__dl member-detail__dl--stats">
              <div><dt>{{ t('accountTeam.memberDetail.matches') }}</dt><dd>{{ displayOrDash(detail.totalMatches) }}</dd></div>
              <div><dt>{{ t('accountTeam.memberDetail.points') }}</dt><dd>{{ displayOrDash(detail.totalPoints) }}</dd></div>
              <div><dt>{{ t('accountTeam.memberDetail.rebounds') }}</dt><dd>{{ displayOrDash(detail.totalRebounds) }}</dd></div>
              <div><dt>{{ t('accountTeam.memberDetail.assists') }}</dt><dd>{{ displayOrDash(detail.totalAssists) }}</dd></div>
              <div><dt>{{ t('accountTeam.memberDetail.avgPoints') }}</dt><dd>{{ displayOrDash(detail.avgPoints) }}</dd></div>
              <div><dt>{{ t('accountTeam.memberDetail.avgRebounds') }}</dt><dd>{{ displayOrDash(detail.avgRebounds) }}</dd></div>
              <div><dt>{{ t('accountTeam.memberDetail.avgAssists') }}</dt><dd>{{ displayOrDash(detail.avgAssists) }}</dd></div>
            </dl>
          </template>
          <p v-else class="member-detail__empty">{{ t('accountTeam.memberDetail.statsEmpty') }}</p>
        </AccountPanel>

        <AccountPanel :title="t('accountTeam.memberDetail.records')">
          <div class="member-detail__records">
            <div>
              <h3>{{ t('accountTeam.memberDetail.latestMatch') }}</h3>
              <p class="member-detail__empty">
                {{ detail.recentMatches?.length ? t('accountTeam.common.recordCount', { count: detail.recentMatches.length }) : t('accountTeam.memberDetail.noData') }}
              </p>
            </div>
            <div>
              <h3>{{ t('accountTeam.memberDetail.punishments') }}</h3>
              <p class="member-detail__empty">
                {{ detail.punishmentRecords?.length ? t('accountTeam.common.recordCount', { count: detail.punishmentRecords.length }) : t('accountTeam.memberDetail.noData') }}
              </p>
            </div>
            <div>
              <h3>{{ t('accountTeam.memberDetail.audits') }}</h3>
              <p class="member-detail__empty">
                {{ detail.auditRecords?.length ? t('accountTeam.common.recordCount', { count: detail.auditRecords.length }) : t('accountTeam.memberDetail.noData') }}
              </p>
            </div>
          </div>
        </AccountPanel>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
@import '../styles/account-center.scss';

.account-view--team-member-detail {
  width: 100%;
  min-width: 0;
}

.member-detail__state {
  padding: 32px 0;
  color: #696969;
  font-size: 15px;
}

.member-detail__hero {
  display: flex;
  flex-wrap: wrap;
  justify-content: space-between;
  gap: 18px;
  margin-bottom: 20px;
  padding: 22px 24px;
  border: 1px solid rgba(0, 0, 0, 0.12);
  border-radius: 4px;
  background: linear-gradient(135deg, rgba(20, 20, 20, 0.92) 0%, rgba(80, 12, 16, 0.88) 100%);
  color: #fff;
}

.member-detail__hero-main {
  display: flex;
  align-items: center;
  gap: 16px;
  min-width: 0;
}

.member-detail__avatar {
  width: 72px;
  height: 72px;
  border-radius: 50%;
  overflow: hidden;
  border: 2px solid rgba(215, 25, 32, 0.65);
  flex: 0 0 auto;
  background: #222;

  img {
    width: 100%;
    height: 100%;
    object-fit: cover;
  }
}

.member-detail__name-row {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;

  h2 {
    margin: 0;
    font-size: 24px;
    font-weight: 800;
  }
}

.member-detail__sub {
  margin: 6px 0 0;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.78);
}

.member-detail__hero-actions {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  align-items: center;
}

.member-detail__grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 16px;
}

.member-detail__dl {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 18px;
  margin: 0;

  dt {
    margin: 0 0 4px;
    font-size: 12px;
    font-weight: 700;
    color: #777;
  }

  dd {
    margin: 0;
    font-size: 15px;
    color: #111;
    word-break: break-word;
  }
}

.member-detail__dl--stats {
  grid-template-columns: repeat(3, minmax(0, 1fr));
}

.member-detail__gameids {
  margin: 0;
  padding: 0;
  list-style: none;

  li {
    padding: 8px 0;
    border-bottom: 1px solid rgba(0, 0, 0, 0.06);
    font-size: 15px;
    font-weight: 600;

    &:last-child {
      border-bottom: 0;
    }
  }
}

.member-detail__empty {
  margin: 0;
  color: #777;
  font-size: 14px;
}

.member-detail__hint {
  margin: 12px 0 0;
  font-size: 12px;
  color: #888;
  line-height: 1.5;
}

.member-detail__records {
  display: grid;
  gap: 16px;

  h3 {
    margin: 0 0 6px;
    font-size: 14px;
    font-weight: 700;
  }
}

@media (max-width: 960px) {
  .member-detail__grid {
    grid-template-columns: 1fr;
  }

  .member-detail__dl,
  .member-detail__dl--stats {
    grid-template-columns: 1fr;
  }
}
</style>
