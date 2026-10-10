<script setup lang="ts">
import { reactive } from 'vue'
import { useI18n } from 'vue-i18n'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import PlayerAvatar from '@/components/player/PlayerAvatar.vue'
import type { TeamMemberItemDto } from '@/api/types/teamMember'
import { goToPlayerDetail } from '@/utils/playerNavigate'
import {
  formatTeamMemberJoinTime,
  isTeamCaptainMember,
  resolveMemberPlatformGameId,
  TEAM_MEMBER_PLATFORM_COLUMNS,
} from '@/utils/teamMemberUi'

const { t } = useI18n()
const KNOWN_POSITIONS = ['PG', 'SG', 'SF', 'PF', 'C'] as const

function positionFull(code?: string | null, name?: string | null) {
  if (!code) return t('accountTeam.position.unset')
  const normalized = code.trim().toUpperCase()
  if ((KNOWN_POSITIONS as readonly string[]).includes(normalized)) {
    return `${normalized} ${t(`accountTeam.position.${normalized}`)}`
  }
  return name ? `${normalized} ${name}` : normalized
}

function positionShort(code?: string | null) {
  return code?.trim() || t('accountTeam.position.unset')
}

function memberName(member: TeamMemberItemDto) {
  return member.nickname || member.gameNickname || member.realName || t('accountTeam.members.unnamed')
}

function memberRole(roleType?: string) {
  if (roleType === 'CAPTAIN') return t('accountTeam.members.captain')
  if (roleType === 'MEMBER') return t('accountTeam.members.playerRole')
  return roleType || '-'
}

function rosterLabel(rosterType?: string) {
  if (rosterType === 'OFFICIAL') return t('accountTeam.members.official')
  if (rosterType === 'TRIAL') return t('accountTeam.members.trial')
  return rosterType || '-'
}

function membershipLabel(status?: number, rosterType?: string) {
  if (status === 0) return t('accountTeam.members.left')
  if (status === 2) return t('accountTeam.members.removed')
  if (status === 1) return rosterLabel(rosterType)
  return '-'
}

const props = defineProps<{
  members: TeamMemberItemDto[]
  loading?: boolean
  isCaptain?: boolean
  teamId?: string
}>()

const emit = defineEmits<{
  save: [member: TeamMemberItemDto, payload: { rosterType: string }]
  remove: [member: TeamMemberItemDto]
}>()

const router = useRouter()
const memberEdits = reactive<Record<string, { rosterType: string }>>({})

function ensureMemberEdit(member: TeamMemberItemDto) {
  if (!memberEdits[member.teamMemberId]) {
    memberEdits[member.teamMemberId] = {
      rosterType: member.rosterType || 'OFFICIAL',
    }
  }
  return memberEdits[member.teamMemberId]
}

/** 队内私有资料详情（个人中心，需 token） */
function openProfile(member: TeamMemberItemDto) {
  if (!props.teamId) {
    ElMessage.warning(t('accountTeam.members.missingTeam'))
    return
  }
  if (!member.teamMemberId) {
    ElMessage.warning(t('accountTeam.members.missingMember'))
    return
  }
  router.push({
    name: 'AccountTeamMemberDetail',
    params: {
      teamId: props.teamId,
      teamMemberId: member.teamMemberId,
    },
  })
}

/** 公开球员生涯数据页（可不依赖 token） */
function openCareerData(member: TeamMemberItemDto) {
  if (!member.playerId) {
    ElMessage.warning(t('accountTeam.members.noProfile'))
    return
  }
  const extraQuery: Record<string, string> = { dataMode: 'career' }
  if (props.teamId) extraQuery.teamId = props.teamId
  goToPlayerDetail(router, member.playerId, 'data', extraQuery)
}

async function confirmRemove(member: TeamMemberItemDto) {
  try {
    await ElMessageBox.confirm(
      t('accountTeam.members.removeMessage'),
      t('accountTeam.members.removeTitle', { name: memberName(member) }),
      {
        type: 'warning',
        confirmButtonText: t('accountTeam.members.removeConfirm'),
        cancelButtonText: t('accountTeam.common.cancel'),
      },
    )
    emit('remove', member)
  } catch {
    // cancelled
  }
}

function handleSave(member: TeamMemberItemDto) {
  if (!props.isCaptain) return
  const edit = ensureMemberEdit(member)
  emit('save', member, { rosterType: edit.rosterType || 'OFFICIAL' })
}
</script>

<template>
  <div class="team-member-list">
    <div v-if="loading" class="team-member-list__loading">{{ t('accountTeam.members.loading') }}</div>

    <AccountEmptyState
      v-else-if="!members.length"
      :message="t('accountTeam.members.empty')"
      :subtitle="t('accountTeam.members.emptyHint')"
    />

    <template v-else>
      <div class="team-member-list__table" role="table" :aria-label="t('accountTeam.members.tableAria')">
        <div class="team-member-list__head" role="row">
          <div class="team-member-list__cell team-member-list__cell--player" role="columnheader">{{ t('accountTeam.members.player') }}</div>
          <div class="team-member-list__cell team-member-list__cell--position" role="columnheader">{{ t('accountTeam.members.position') }}</div>
          <div
            v-for="platform in TEAM_MEMBER_PLATFORM_COLUMNS"
            :key="platform"
            class="team-member-list__cell team-member-list__cell--platform"
            role="columnheader"
          >
            {{ platform === 'XBOX' ? 'Xbox' : platform }}
          </div>
          <div class="team-member-list__cell team-member-list__cell--role" role="columnheader">{{ t('accountTeam.members.role') }}</div>
          <div class="team-member-list__cell team-member-list__cell--status" role="columnheader">{{ t('accountTeam.members.status') }}</div>
          <div class="team-member-list__cell team-member-list__cell--join" role="columnheader">{{ t('accountTeam.members.joinedAt') }}</div>
          <div class="team-member-list__cell team-member-list__cell--actions" role="columnheader">{{ t('accountTeam.members.actions') }}</div>
        </div>

        <div
          v-for="member in members"
          :key="member.teamMemberId"
          class="team-member-list__row"
          :class="{ 'team-member-list__row--captain': isTeamCaptainMember(member) }"
          role="row"
        >
          <div class="team-member-list__cell team-member-list__cell--player" role="cell">
            <div class="team-member-list__player">
              <PlayerAvatar
                :name="memberName(member)"
                :avatar-url="member.avatarUrl"
                :player-no="member.playerNo"
                :player-id="member.playerId"
                size="sm"
              />
              <div class="team-member-list__player-meta">
                <div class="team-member-list__player-name">
                  <strong>{{ memberName(member) }}</strong>
                  <AccountStatusBadge
                    v-if="isTeamCaptainMember(member)"
                    type="danger"
                    :text="t('accountTeam.members.captain')"
                    class="team-member-list__captain-badge"
                  />
                </div>
                <span class="team-member-list__player-no">{{ member.playerNo || t('accountTeam.members.noPlayerNo') }}</span>
              </div>
            </div>
          </div>

          <div class="team-member-list__cell team-member-list__cell--position" role="cell">
            <span class="team-member-list__position" :title="positionFull(member.positionCode, member.positionName)">
              {{ positionShort(member.positionCode) }}
            </span>
          </div>

          <div
            v-for="platform in TEAM_MEMBER_PLATFORM_COLUMNS"
            :key="`${member.teamMemberId}-${platform}`"
            class="team-member-list__cell team-member-list__cell--platform"
            role="cell"
          >
            <span
              class="team-member-list__gameid"
              :title="resolveMemberPlatformGameId(member, platform)"
            >
              {{ resolveMemberPlatformGameId(member, platform) }}
            </span>
          </div>

          <div class="team-member-list__cell team-member-list__cell--role" role="cell">
            {{ memberRole(member.roleType) }}
          </div>

          <div class="team-member-list__cell team-member-list__cell--status" role="cell">
            <template v-if="isCaptain">
              <select v-model="ensureMemberEdit(member).rosterType" class="team-member-list__select">
                <option value="OFFICIAL">{{ t('accountTeam.members.official') }}</option>
                <option value="TRIAL">{{ t('accountTeam.members.trial') }}</option>
              </select>
            </template>
            <template v-else>
              {{ membershipLabel(member.membershipStatus, member.rosterType) }}
            </template>
          </div>

          <div class="team-member-list__cell team-member-list__cell--join" role="cell">
            {{ formatTeamMemberJoinTime(member.joinTime) }}
          </div>

          <div class="team-member-list__cell team-member-list__cell--actions" role="cell">
            <div class="team-member-list__actions">
              <button
                v-if="isCaptain"
                type="button"
                class="team-member-list__link"
                @click="handleSave(member)"
              >
                {{ t('accountTeam.members.save') }}
              </button>
              <button type="button" class="team-member-list__link" @click="openProfile(member)">
                {{ t('accountTeam.members.profile') }}
              </button>
              <button type="button" class="team-member-list__link" @click="openCareerData(member)">
                {{ t('accountTeam.members.career') }}
              </button>
              <button
                v-if="isCaptain && !isTeamCaptainMember(member)"
                type="button"
                class="team-member-list__link team-member-list__link--danger"
                @click="confirmRemove(member)"
              >
                {{ t('accountTeam.members.remove') }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="team-member-list__cards" :aria-label="t('accountTeam.members.cardsAria')">
        <article
          v-for="member in members"
          :key="`card-${member.teamMemberId}`"
          class="team-member-list__card"
          :class="{ 'team-member-list__card--captain': isTeamCaptainMember(member) }"
        >
          <div class="team-member-list__card-head">
            <PlayerAvatar
              :name="memberName(member)"
              :avatar-url="member.avatarUrl"
              :player-no="member.playerNo"
              :player-id="member.playerId"
              size="sm"
            />
            <div>
              <div class="team-member-list__player-name">
                <strong>{{ memberName(member) }}</strong>
                <AccountStatusBadge
                  v-if="isTeamCaptainMember(member)"
                  type="danger"
                  :text="t('accountTeam.members.captain')"
                  class="team-member-list__captain-badge"
                />
              </div>
              <span class="team-member-list__player-no">{{ member.playerNo || t('accountTeam.members.noPlayerNo') }}</span>
            </div>
          </div>

          <dl class="team-member-list__card-meta">
            <div><dt>{{ t('accountTeam.members.position') }}</dt><dd :title="positionFull(member.positionCode, member.positionName)">{{ positionShort(member.positionCode) }}</dd></div>
            <div
              v-for="platform in TEAM_MEMBER_PLATFORM_COLUMNS"
              :key="`card-${member.teamMemberId}-${platform}`"
            >
              <dt>{{ platform === 'XBOX' ? 'Xbox' : platform }}</dt>
              <dd>{{ resolveMemberPlatformGameId(member, platform) }}</dd>
            </div>
            <div><dt>{{ t('accountTeam.members.role') }}</dt><dd>{{ memberRole(member.roleType) }}</dd></div>
            <div>
              <dt>{{ t('accountTeam.members.status') }}</dt>
              <dd>
                <select
                  v-if="isCaptain"
                  v-model="ensureMemberEdit(member).rosterType"
                  class="team-member-list__select"
                >
                  <option value="OFFICIAL">{{ t('accountTeam.members.official') }}</option>
                  <option value="TRIAL">{{ t('accountTeam.members.trial') }}</option>
                </select>
                <template v-else>{{ membershipLabel(member.membershipStatus, member.rosterType) }}</template>
              </dd>
            </div>
            <div><dt>{{ t('accountTeam.members.joinedAt') }}</dt><dd>{{ formatTeamMemberJoinTime(member.joinTime) }}</dd></div>
          </dl>

          <div class="team-member-list__actions team-member-list__actions--card">
            <button
              v-if="isCaptain"
              type="button"
              class="team-member-list__link"
              @click="handleSave(member)"
            >
              {{ t('accountTeam.members.save') }}
            </button>
            <button type="button" class="team-member-list__link" @click="openProfile(member)">
              {{ t('accountTeam.members.profile') }}
            </button>
            <button type="button" class="team-member-list__link" @click="openCareerData(member)">
              {{ t('accountTeam.members.career') }}
            </button>
            <button
              v-if="isCaptain && !isTeamCaptainMember(member)"
              type="button"
              class="team-member-list__link team-member-list__link--danger"
              @click="confirmRemove(member)"
            >
              {{ t('accountTeam.members.remove') }}
            </button>
          </div>
        </article>
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/account-center.scss';

.team-member-list {
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
    min-width: 900px;
    grid-template-columns:
      minmax(168px, 1.15fr)
      minmax(52px, 0.38fr)
      minmax(72px, 0.52fr)
      minmax(72px, 0.52fr)
      minmax(72px, 0.52fr)
      minmax(68px, 0.48fr)
      minmax(84px, 0.58fr)
      minmax(84px, 0.58fr)
      minmax(188px, 1.2fr);
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

  &__select {
    width: 100%;
    max-width: 108px;
    min-height: 36px;
    padding: 0 10px;
    border: 1px solid rgba(20, 20, 20, 0.18);
    border-radius: 3px;
    background: rgba(255, 255, 255, 0.72);
    font: inherit;
    font-size: 15px;
    box-sizing: border-box;
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

  &__link--danger {
    color: #b42318;

    &:hover {
      color: #991b1b;
    }
  }

  &__cards {
    display: none;
  }
}

@media (max-width: 1180px) {
  .team-member-list__head,
  .team-member-list__row {
    grid-template-columns:
      minmax(168px, 1.2fr)
      minmax(52px, 0.38fr)
      minmax(68px, 0.48fr)
      minmax(68px, 0.48fr)
      minmax(68px, 0.48fr)
      minmax(68px, 0.48fr)
      minmax(84px, 0.58fr)
      minmax(188px, 1.15fr);
  }

  .team-member-list__cell--join {
    display: none;
  }
}

@media (max-width: 960px) {
  .team-member-list__table {
    display: none;
  }

  .team-member-list__cards {
    display: flex;
    flex-direction: column;
    gap: 16px;
  }

  .team-member-list__card {
    padding: 18px;
    border: 1px solid rgba(0, 0, 0, 0.12);
    border-radius: 4px;
    background: rgba(255, 255, 255, 0.36);

    &--captain {
      border-color: rgba(215, 25, 32, 0.22);
      background: rgba(215, 25, 32, 0.03);
    }
  }

  .team-member-list__card-head {
    display: flex;
    align-items: center;
    gap: 12px;
    margin-bottom: 14px;
  }

  .team-member-list__card-meta {
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
    }
  }

  .team-member-list__actions--card {
    flex-wrap: wrap;
    gap: 8px 0;
  }
}
</style>
