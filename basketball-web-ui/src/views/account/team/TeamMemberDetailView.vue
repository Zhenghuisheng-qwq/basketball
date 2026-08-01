<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
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

const route = useRoute()
const router = useRouter()

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
      const platform = item.platformCode || item.platformName || '未知'
      return `${platform}：${item.gameId || '-'}`
    })
  }
  if (detail.value?.gameIdSummary && detail.value.gameIdSummary !== '未绑定') {
    return detail.value.gameIdSummary.split(/\s*\/\s*/).filter(Boolean)
  }
  return []
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
    loadError.value = '缺少球队或成员参数'
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
    loadError.value = error instanceof Error ? error.message : '加载成员资料失败'
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
      zh-title="队员资料"
      en-title="TEAM MEMBER DETAIL"
      :breadcrumb-items="[
        { label: '首页', to: '/home' },
        { label: '个人中心', to: '/account/overview' },
        { label: '我的球队', to: '/account/team' },
        { label: '队员资料' },
      ]"
    />

    <div v-if="loading" class="member-detail__state">成员资料加载中...</div>
    <AccountEmptyState v-else-if="loadError" :message="loadError" />

    <template v-else-if="detail">
      <section class="member-detail__hero">
        <div class="member-detail__hero-main">
          <div class="member-detail__avatar">
            <img :src="avatarUrl" :alt="detail.nickname || '队员'" />
          </div>
          <div class="member-detail__hero-meta">
            <div class="member-detail__name-row">
              <h2>{{ detail.nickname || '未设置昵称' }}</h2>
              <AccountStatusBadge
                v-if="detail.isCaptain"
                type="danger"
                text="队长"
              />
              <AccountStatusBadge
                v-else
                type="default"
                :text="detail.roleInTeamText || '队员'"
              />
            </div>
            <p class="member-detail__sub">{{ detail.playerNo || '暂无球员编号' }}</p>
            <p class="member-detail__sub">
              {{ detail.teamName || '-' }}
              · {{ detail.positionName || detail.positionCode || '未设置位置' }}
              · {{ detail.memberStatusText || '-' }}
            </p>
          </div>
        </div>
        <div class="member-detail__hero-actions">
          <button type="button" class="account-btn-secondary" @click="goBackMembers">返回成员管理</button>
          <button
            v-if="detail.playerId"
            type="button"
            class="account-btn-secondary"
            @click="openPublicCareer"
          >
            生涯数据
          </button>
        </div>
      </section>

      <div class="member-detail__grid">
        <AccountPanel title="基础资料">
          <dl class="member-detail__dl">
            <div><dt>球员昵称</dt><dd>{{ displayOrDash(detail.nickname) }}</dd></div>
            <div><dt>CNPA 编号</dt><dd>{{ displayOrDash(detail.playerNo) }}</dd></div>
            <div><dt>常用位置</dt><dd>{{ displayOrDash(detail.positionName || detail.positionCode) }}</dd></div>
            <div><dt>加入时间</dt><dd>{{ detail.joinTime ? formatDateTime(detail.joinTime) : '-' }}</dd></div>
            <div><dt>球衣号</dt><dd>{{ displayOrDash(detail.jerseyNo) }}</dd></div>
            <div><dt>成员身份</dt><dd>{{ displayOrDash(detail.roleInTeamText) }}</dd></div>
            <div><dt>成员状态</dt><dd>{{ displayOrDash(detail.memberStatusText) }}</dd></div>
            <div><dt>加入来源</dt><dd>{{ displayOrDash(detail.sourceTypeText || detail.sourceType) }}</dd></div>
          </dl>
        </AccountPanel>

        <AccountPanel title="游戏 ID">
          <ul v-if="gameIdLines.length" class="member-detail__gameids">
            <li v-for="line in gameIdLines" :key="line">{{ line }}</li>
          </ul>
          <p v-else class="member-detail__empty">未绑定</p>
          <p class="member-detail__hint">游戏 ID 用于确认球员在各平台中的身份，不代表球员归属某一平台。</p>
        </AccountPanel>

        <AccountPanel title="本队参赛信息">
          <template v-if="hasEventInfo">
            <dl class="member-detail__dl">
              <div><dt>当前赛事</dt><dd>{{ displayOrDash(detail.currentEventName) }}</dd></div>
              <div><dt>当前赛事平台</dt><dd>{{ displayOrDash(detail.currentEventPlatform) }}</dd></div>
              <div><dt>当前阵容状态</dt><dd>{{ displayOrDash(detail.currentRosterStatus) }}</dd></div>
              <div><dt>参赛赛事数</dt><dd>{{ displayOrDash(detail.participatedEventCount) }}</dd></div>
              <div><dt>最近比赛</dt><dd>{{ displayOrDash(detail.latestMatchSummary || detail.latestMatchTime) }}</dd></div>
            </dl>
          </template>
          <p v-else class="member-detail__empty">暂无本队参赛记录</p>
        </AccountPanel>

        <AccountPanel title="数据摘要">
          <template v-if="hasStatSummary">
            <dl class="member-detail__dl member-detail__dl--stats">
              <div><dt>场次</dt><dd>{{ displayOrDash(detail.totalMatches) }}</dd></div>
              <div><dt>得分</dt><dd>{{ displayOrDash(detail.totalPoints) }}</dd></div>
              <div><dt>篮板</dt><dd>{{ displayOrDash(detail.totalRebounds) }}</dd></div>
              <div><dt>助攻</dt><dd>{{ displayOrDash(detail.totalAssists) }}</dd></div>
              <div><dt>场均得分</dt><dd>{{ displayOrDash(detail.avgPoints) }}</dd></div>
              <div><dt>场均篮板</dt><dd>{{ displayOrDash(detail.avgRebounds) }}</dd></div>
              <div><dt>场均助攻</dt><dd>{{ displayOrDash(detail.avgAssists) }}</dd></div>
            </dl>
          </template>
          <p v-else class="member-detail__empty">暂无比赛数据</p>
        </AccountPanel>

        <AccountPanel title="记录">
          <div class="member-detail__records">
            <div>
              <h3>最近比赛</h3>
              <p class="member-detail__empty">
                {{ detail.recentMatches?.length ? `共 ${detail.recentMatches.length} 条` : '暂无数据' }}
              </p>
            </div>
            <div>
              <h3>处罚记录</h3>
              <p class="member-detail__empty">
                {{ detail.punishmentRecords?.length ? `共 ${detail.punishmentRecords.length} 条` : '暂无数据' }}
              </p>
            </div>
            <div>
              <h3>审核记录</h3>
              <p class="member-detail__empty">
                {{ detail.auditRecords?.length ? `共 ${detail.auditRecords.length} 条` : '暂无数据' }}
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
