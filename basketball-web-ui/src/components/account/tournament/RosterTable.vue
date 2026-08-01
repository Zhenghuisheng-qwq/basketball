<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import { getTeamMemberPage } from '@/api/teamMember'
import type { RosterMember } from '@/types/eventTeam'
import { formatPositionLabel } from '@/constants/rosterRules'
import { formatDate } from '@/utils/formatDate'
import { goToPlayerDetail } from '@/utils/playerNavigate'
import {
  TEAM_MEMBER_PLATFORM_COLUMNS,
  type TeamMemberPlatformCode,
  resolveMemberPlatformGameId,
} from '@/utils/teamMemberUi'

const props = defineProps<{
  members: RosterMember[]
  teamId?: string
  loading?: boolean
}>()

const router = useRouter()
const teamMemberIdMap = ref<Record<string, string>>({})
const resolvingProfile = ref(false)

function isCaptainMember(member: RosterMember) {
  return member.role === 'CAPTAIN'
}

function resolveDisplayName(member: RosterMember) {
  return member.nickname || member.playerNo || '未命名球员'
}

function resolveAvatarFallback(member: RosterMember) {
  const name = resolveDisplayName(member)
  if (name && name !== '未命名球员') return name.slice(0, 1).toUpperCase()
  const tail = member.playerNo?.replace(/\D/g, '').slice(-2)
  return tail || '?'
}

function formatPositionShort(position?: string) {
  if (!position || position === '-') return '未设置'
  return position.trim().toUpperCase()
}

function formatPositionTitle(position?: string) {
  if (!position || position === '-') return '未设置'
  return formatPositionLabel(position)
}

function formatRoleText(member: RosterMember) {
  if (member.roleText) return member.roleText
  return isCaptainMember(member) ? '赛事队长' : '队员'
}

function formatEffectiveTime(value?: string) {
  if (!value) return '-'
  return formatDate(value)
}

function buildGameIdSummary(member: RosterMember): string {
  const gameId = (member.gameId || '').trim()
  if (!gameId || gameId === '-' || gameId === '未绑定') return ''
  if (/^(PS5?|PC|XBOX(?:\sONE)?)\s*[:：]/i.test(gameId) || gameId.includes('/')) {
    return gameId
  }
  const platform = (member.platform || '').trim()
  if (platform && platform !== '-') {
    return `${platform}: ${gameId}`
  }
  return gameId
}

function resolvePlatformGameId(member: RosterMember, platform: TeamMemberPlatformCode) {
  return resolveMemberPlatformGameId(
    {
      platformGameIds: [],
      gameIdSummary: buildGameIdSummary(member),
    },
    platform,
  )
}

async function ensureTeamMemberIdMap() {
  if (!props.teamId) return {}
  if (Object.keys(teamMemberIdMap.value).length) return teamMemberIdMap.value

  const res = await getTeamMemberPage(props.teamId, { currentPage: 1, pageSize: 100 })
  const records = res.data?.records || []
  const next: Record<string, string> = {}
  for (const item of records) {
    if (item.playerId && item.teamMemberId) {
      next[String(item.playerId)] = item.teamMemberId
    }
  }
  teamMemberIdMap.value = next
  return next
}

/** 队内私有资料详情（个人中心，需 token） */
async function openProfile(member: RosterMember) {
  if (!props.teamId) {
    ElMessage.warning('缺少球队上下文，无法查看队内资料')
    return
  }
  if (!member.playerId) {
    ElMessage.warning('缺少球员标识')
    return
  }
  if (resolvingProfile.value) return

  resolvingProfile.value = true
  try {
    const map = await ensureTeamMemberIdMap()
    const teamMemberId = map[String(member.playerId)]
    if (!teamMemberId) {
      ElMessage.warning('未找到对应队内成员资料')
      return
    }
    await router.push({
      name: 'AccountTeamMemberDetail',
      params: {
        teamId: props.teamId,
        teamMemberId,
      },
    })
  } catch {
    ElMessage.error('加载队内成员资料失败')
  } finally {
    resolvingProfile.value = false
  }
}

/** 公开球员生涯数据页 */
function openCareerData(member: RosterMember) {
  if (!member.playerId) {
    ElMessage.warning('暂无球员档案')
    return
  }
  const extraQuery: Record<string, string> = { dataMode: 'career' }
  if (props.teamId) extraQuery.teamId = props.teamId
  goToPlayerDetail(router, member.playerId, 'data', extraQuery)
}
</script>

<template>
  <div class="event-roster-list">
    <div v-if="loading" class="event-roster-list__loading">阵容加载中...</div>

    <AccountEmptyState
      v-else-if="!members.length"
      message="暂无赛事阵容"
      subtitle="该赛事球队暂无生效阵容，可能尚未完成报名审核或阵容生成。"
    />

    <template v-else>
      <div class="event-roster-list__table" role="table" aria-label="当前赛事阵容">
        <div class="event-roster-list__head" role="row">
          <div class="event-roster-list__cell event-roster-list__cell--player" role="columnheader">球员</div>
          <div class="event-roster-list__cell event-roster-list__cell--position" role="columnheader">位置</div>
          <div
            v-for="platform in TEAM_MEMBER_PLATFORM_COLUMNS"
            :key="platform"
            class="event-roster-list__cell event-roster-list__cell--platform"
            role="columnheader"
          >
            {{ platform === 'XBOX' ? 'Xbox' : platform }}
          </div>
          <div class="event-roster-list__cell event-roster-list__cell--role" role="columnheader">球队角色</div>
          <div class="event-roster-list__cell event-roster-list__cell--join" role="columnheader">生效时间</div>
          <div class="event-roster-list__cell event-roster-list__cell--status" role="columnheader">当前状态</div>
          <div class="event-roster-list__cell event-roster-list__cell--actions" role="columnheader">操作</div>
        </div>

        <div
          v-for="member in members"
          :key="member.playerId"
          class="event-roster-list__row"
          :class="{ 'event-roster-list__row--captain': isCaptainMember(member) }"
          role="row"
        >
          <div class="event-roster-list__cell event-roster-list__cell--player" role="cell">
            <div class="event-roster-list__player">
              <div
                class="event-roster-list__avatar"
                :class="{ 'event-roster-list__avatar--image': Boolean(member.avatarUrl) }"
              >
                <img v-if="member.avatarUrl" :src="member.avatarUrl" :alt="resolveDisplayName(member)" />
                <span v-else>{{ resolveAvatarFallback(member) }}</span>
              </div>
              <div class="event-roster-list__player-meta">
                <div class="event-roster-list__player-name">
                  <strong>{{ resolveDisplayName(member) }}</strong>
                  <AccountStatusBadge
                    v-if="isCaptainMember(member)"
                    type="danger"
                    text="队长"
                    class="event-roster-list__captain-badge"
                  />
                </div>
                <span class="event-roster-list__player-no">{{ member.playerNo || '暂无球员编号' }}</span>
              </div>
            </div>
          </div>

          <div class="event-roster-list__cell event-roster-list__cell--position" role="cell">
            <span class="event-roster-list__position" :title="formatPositionTitle(member.position)">
              {{ formatPositionShort(member.position) }}
            </span>
          </div>

          <div
            v-for="platform in TEAM_MEMBER_PLATFORM_COLUMNS"
            :key="`${member.playerId}-${platform}`"
            class="event-roster-list__cell event-roster-list__cell--platform"
            role="cell"
          >
            <span
              class="event-roster-list__gameid"
              :title="resolvePlatformGameId(member, platform)"
            >
              {{ resolvePlatformGameId(member, platform) }}
            </span>
          </div>

          <div class="event-roster-list__cell event-roster-list__cell--role" role="cell">
            {{ formatRoleText(member) }}
          </div>

          <div class="event-roster-list__cell event-roster-list__cell--join" role="cell">
            {{ formatEffectiveTime(member.joinTime) }}
          </div>

          <div class="event-roster-list__cell event-roster-list__cell--status" role="cell">
            {{ member.statusText || '-' }}
          </div>

          <div class="event-roster-list__cell event-roster-list__cell--actions" role="cell">
            <div class="event-roster-list__actions">
              <button type="button" class="event-roster-list__link" @click="openProfile(member)">
                查看资料
              </button>
              <button type="button" class="event-roster-list__link" @click="openCareerData(member)">
                生涯数据
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="event-roster-list__cards" aria-label="当前赛事阵容卡片列表">
        <article
          v-for="member in members"
          :key="`card-${member.playerId}`"
          class="event-roster-list__card"
          :class="{ 'event-roster-list__card--captain': isCaptainMember(member) }"
        >
          <div class="event-roster-list__card-head">
            <div
              class="event-roster-list__avatar event-roster-list__avatar--compact"
              :class="{ 'event-roster-list__avatar--image': Boolean(member.avatarUrl) }"
            >
              <img v-if="member.avatarUrl" :src="member.avatarUrl" :alt="resolveDisplayName(member)" />
              <span v-else>{{ resolveAvatarFallback(member) }}</span>
            </div>
            <div>
              <div class="event-roster-list__player-name">
                <strong>{{ resolveDisplayName(member) }}</strong>
                <AccountStatusBadge
                  v-if="isCaptainMember(member)"
                  type="danger"
                  text="队长"
                  class="event-roster-list__captain-badge"
                />
              </div>
              <span class="event-roster-list__player-no">{{ member.playerNo || '暂无球员编号' }}</span>
            </div>
          </div>

          <dl class="event-roster-list__card-meta">
            <div>
              <dt>位置</dt>
              <dd :title="formatPositionTitle(member.position)">{{ formatPositionShort(member.position) }}</dd>
            </div>
            <div
              v-for="platform in TEAM_MEMBER_PLATFORM_COLUMNS"
              :key="`card-${member.playerId}-${platform}`"
            >
              <dt>{{ platform === 'XBOX' ? 'Xbox' : platform }}</dt>
              <dd>{{ resolvePlatformGameId(member, platform) }}</dd>
            </div>
            <div><dt>球队角色</dt><dd>{{ formatRoleText(member) }}</dd></div>
            <div><dt>生效时间</dt><dd>{{ formatEffectiveTime(member.joinTime) }}</dd></div>
            <div><dt>当前状态</dt><dd>{{ member.statusText || '-' }}</dd></div>
          </dl>

          <div class="event-roster-list__actions event-roster-list__actions--card">
            <button type="button" class="event-roster-list__link" @click="openProfile(member)">
              查看资料
            </button>
            <button type="button" class="event-roster-list__link" @click="openCareerData(member)">
              生涯数据
            </button>
          </div>
        </article>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/account-center.scss';

.event-roster-list {
  width: 100%;
  min-width: 0;

  &__loading {
    padding: 28px 0;
    color: #696969;
    font-size: 15px;
  }

  &__table {
    display: flex;
    flex-direction: column;
    width: 100%;
    border: 1px solid rgba(0, 0, 0, 0.12);
    border-radius: 4px;
    background: rgba(255, 255, 255, 0.36);
    overflow-x: auto;
  }

  &__head,
  &__row {
    display: grid;
    min-width: 960px;
    grid-template-columns:
      minmax(168px, 1.2fr)
      minmax(52px, 0.38fr)
      minmax(72px, 0.52fr)
      minmax(72px, 0.52fr)
      minmax(72px, 0.52fr)
      minmax(72px, 0.52fr)
      minmax(92px, 0.62fr)
      minmax(84px, 0.55fr)
      minmax(148px, 0.95fr);
    gap: 12px;
    align-items: center;
    padding: 0 18px;
  }

  &__head {
    min-height: 52px;
    border-bottom: 1px solid rgba(0, 0, 0, 0.12);
    background: rgba(255, 255, 255, 0.42);
    font-size: 14px;
    font-weight: 700;
    color: #444;
  }

  &__row {
    min-height: 72px;
    border-bottom: 1px solid rgba(0, 0, 0, 0.08);
    transition: background-color 0.18s ease;

    &:hover {
      background: rgba(255, 255, 255, 0.42);
    }

    &:last-child {
      border-bottom: 0;
    }

    &--captain {
      background: rgba(215, 25, 32, 0.03);
    }
  }

  &__cell {
    min-width: 0;
    font-size: 15px;
    line-height: 1.5;
    color: #111;
  }

  &__gameid {
    display: block;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 13px;
    line-height: 1.4;
    color: #333;
  }

  &__cell--platform {
    font-size: 13px;
  }

  &__player {
    display: flex;
    align-items: center;
    gap: 14px;
    min-width: 0;
  }

  &__avatar {
    flex: 0 0 auto;
    width: 48px;
    height: 48px;
    border-radius: 50%;
    display: grid;
    place-items: center;
    overflow: hidden;
    border: 2px solid rgba(215, 25, 32, 0.35);
    background: linear-gradient(180deg, #2a2a2a 0%, #111 100%);
    color: #fff;
    font-size: 18px;
    font-weight: 800;

    img {
      width: 100%;
      height: 100%;
      object-fit: cover;
    }

    &--compact {
      width: 40px;
      height: 40px;
      font-size: 16px;
    }

    &--image {
      border-color: rgba(0, 0, 0, 0.12);
      background: #ddd;
    }
  }

  &__player-meta {
    min-width: 0;
  }

  &__player-name {
    display: flex;
    flex-wrap: wrap;
    align-items: center;
    gap: 8px;

    strong {
      font-size: 16px;
      font-weight: 800;
      word-break: break-word;
    }
  }

  &__player-no {
    display: block;
    margin-top: 4px;
    font-size: 13px;
    color: #666;
    word-break: break-all;
  }

  &__captain-badge {
    transform: scale(0.92);
    transform-origin: left center;
  }

  &__position {
    display: inline-block;
    font-weight: 700;
    cursor: default;
    border-bottom: 1px dotted rgba(20, 20, 20, 0.28);
  }

  &__actions {
    display: flex;
    flex-wrap: nowrap;
    align-items: center;
    gap: 0;
    min-width: 0;
  }

  &__link {
    appearance: none;
    margin: 0;
    padding: 0 8px;
    border: 0;
    border-right: 1px solid rgba(20, 20, 20, 0.16);
    background: transparent;
    color: #1f4b99;
    font: inherit;
    font-size: 13px;
    line-height: 1.2;
    white-space: nowrap;
    cursor: pointer;

    &:first-child {
      padding-left: 0;
    }

    &:last-child {
      border-right: 0;
      padding-right: 0;
    }

    &:hover {
      color: #d71920;
      text-decoration: underline;
    }
  }

  &__cards {
    display: none;
  }
}

@media (max-width: 1180px) {
  .event-roster-list__head,
  .event-roster-list__row {
    grid-template-columns:
      minmax(168px, 1.2fr)
      minmax(52px, 0.38fr)
      minmax(68px, 0.48fr)
      minmax(68px, 0.48fr)
      minmax(68px, 0.48fr)
      minmax(72px, 0.52fr)
      minmax(84px, 0.55fr)
      minmax(148px, 0.95fr);
  }

  .event-roster-list__cell--join {
    display: none;
  }
}

@media (max-width: 960px) {
  .event-roster-list__table {
    display: none;
  }

  .event-roster-list__cards {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .event-roster-list__card {
    padding: 18px;
    border: 1px solid rgba(0, 0, 0, 0.12);
    border-radius: 4px;
    background: rgba(255, 255, 255, 0.36);

    &--captain {
      border-color: rgba(215, 25, 32, 0.22);
      background: rgba(215, 25, 32, 0.03);
    }
  }

  .event-roster-list__card-head {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 14px;
  }

  .event-roster-list__card-meta {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    gap: 12px 16px;
    margin: 0 0 16px;

    div {
      min-width: 0;
    }

    dt {
      margin: 0 0 4px;
      font-size: 12px;
      font-weight: 700;
      color: #777;
    }

    dd {
      margin: 0;
      font-size: 15px;
      line-height: 1.5;
      color: #111;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }

  .event-roster-list__actions--card {
    flex-wrap: wrap;
    gap: 8px 0;
  }
}
</style>
