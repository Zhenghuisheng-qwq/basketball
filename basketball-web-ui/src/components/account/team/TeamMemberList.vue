<script setup lang="ts">
import { reactive } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import AccountEmptyState from '@/components/account/AccountEmptyState.vue'
import AccountStatusBadge from '@/components/account/AccountStatusBadge.vue'
import type { TeamMemberItemDto } from '@/api/types/teamMember'
import { goToPlayerDetail } from '@/utils/playerNavigate'
import {
  formatTeamMemberJoinTime,
  formatTeamMemberMembershipStatus,
  formatTeamMemberPosition,
  formatTeamMemberPositionShort,
  formatTeamMemberRole,
  isTeamCaptainMember,
  resolveTeamMemberAvatarFallback,
  resolveTeamMemberDisplayName,
} from '@/utils/teamMemberUi'

const props = defineProps<{
  members: TeamMemberItemDto[]
  loading?: boolean
  isCaptain?: boolean
  teamId?: string
}>()

const emit = defineEmits<{
  save: [member: TeamMemberItemDto, payload: { jerseyNo: string; rosterType: string }]
  remove: [member: TeamMemberItemDto]
}>()

const router = useRouter()
const memberEdits = reactive<Record<string, { jerseyNo: string; rosterType: string }>>({})

function ensureMemberEdit(member: TeamMemberItemDto) {
  if (!memberEdits[member.teamMemberId]) {
    memberEdits[member.teamMemberId] = {
      jerseyNo: member.jerseyNo || '',
      rosterType: member.rosterType || 'OFFICIAL',
    }
  }
  return memberEdits[member.teamMemberId]
}

function openProfile(member: TeamMemberItemDto) {
  if (!member.playerId) {
    ElMessage.warning('暂无球员档案')
    return
  }
  goToPlayerDetail(router, member.playerId, 'overview')
}

function openCareerData(member: TeamMemberItemDto) {
  if (!member.playerId) {
    ElMessage.warning('暂无球员档案')
    return
  }
  const extraQuery: Record<string, string> = { dataMode: 'career' }
  if (props.teamId) extraQuery.teamId = props.teamId
  goToPlayerDetail(router, member.playerId, 'data', extraQuery)
}

async function confirmRemove(member: TeamMemberItemDto) {
  try {
    await ElMessageBox.confirm(
      '移除后该成员将不再属于当前球队，但历史比赛数据会保留。',
      `确认移除 ${resolveTeamMemberDisplayName(member)}？`,
      {
        type: 'warning',
        confirmButtonText: '确认移除',
        cancelButtonText: '取消',
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
  const jerseyNo = edit.jerseyNo.trim()
  if (!jerseyNo) {
    ElMessage.warning('球衣号不能为空')
    return
  }
  if (!/^\d{1,3}$/.test(jerseyNo)) {
    ElMessage.warning('球衣号格式不正确，请输入 1-3 位数字')
    return
  }
  emit('save', member, { jerseyNo, rosterType: edit.rosterType || 'OFFICIAL' })
}
</script>

<template>
  <div class="team-member-list">
    <div v-if="loading" class="team-member-list__loading">成员列表加载中...</div>

    <AccountEmptyState
      v-else-if="!members.length"
      message="当前球队暂无成员"
      subtitle="你可以通过邀请球员或处理加入申请来组建球队"
    />

    <template v-else>
      <div class="team-member-list__table" role="table" aria-label="球队成员列表">
        <div class="team-member-list__head" role="row">
          <div class="team-member-list__cell team-member-list__cell--player" role="columnheader">球员</div>
          <div class="team-member-list__cell team-member-list__cell--position" role="columnheader">位置</div>
          <div class="team-member-list__cell team-member-list__cell--role" role="columnheader">球队角色</div>
          <div class="team-member-list__cell team-member-list__cell--status" role="columnheader">成员状态</div>
          <div class="team-member-list__cell team-member-list__cell--jersey" role="columnheader">球衣号</div>
          <div class="team-member-list__cell team-member-list__cell--join" role="columnheader">加入时间</div>
          <div class="team-member-list__cell team-member-list__cell--actions" role="columnheader">操作</div>
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
              <div
                class="team-member-list__avatar"
                :class="{ 'team-member-list__avatar--image': Boolean(member.avatarUrl) }"
              >
                <img v-if="member.avatarUrl" :src="member.avatarUrl" :alt="resolveTeamMemberDisplayName(member)" />
                <span v-else>{{ resolveTeamMemberAvatarFallback(member) }}</span>
              </div>
              <div class="team-member-list__player-meta">
                <div class="team-member-list__player-name">
                  <strong>{{ resolveTeamMemberDisplayName(member) }}</strong>
                  <AccountStatusBadge
                    v-if="isTeamCaptainMember(member)"
                    type="danger"
                    text="队长"
                    class="team-member-list__captain-badge"
                  />
                </div>
                <span class="team-member-list__player-no">{{ member.playerNo || '暂无球员编号' }}</span>
              </div>
            </div>
          </div>

          <div class="team-member-list__cell team-member-list__cell--position" role="cell">
            <span class="team-member-list__position" :title="formatTeamMemberPosition(member)">
              {{ formatTeamMemberPositionShort(member.positionCode) }}
            </span>
          </div>

          <div class="team-member-list__cell team-member-list__cell--role" role="cell">
            {{ formatTeamMemberRole(member.roleType) }}
          </div>

          <div class="team-member-list__cell team-member-list__cell--status" role="cell">
            <template v-if="isCaptain">
              <select v-model="ensureMemberEdit(member).rosterType" class="team-member-list__select">
                <option value="OFFICIAL">正式</option>
                <option value="TRIAL">试训</option>
              </select>
            </template>
            <template v-else>
              {{ formatTeamMemberMembershipStatus(member.membershipStatus, member.rosterType) }}
            </template>
          </div>

          <div class="team-member-list__cell team-member-list__cell--jersey" role="cell">
            <input
              v-if="isCaptain"
              v-model="ensureMemberEdit(member).jerseyNo"
              type="text"
              maxlength="3"
              inputmode="numeric"
              class="team-member-list__jersey-input"
              placeholder="号码"
            />
            <span v-else>{{ member.jerseyNo || '-' }}</span>
          </div>

          <div class="team-member-list__cell team-member-list__cell--join" role="cell">
            {{ formatTeamMemberJoinTime(member.joinTime) }}
          </div>

          <div class="team-member-list__cell team-member-list__cell--actions" role="cell">
            <div class="team-member-list__actions">
              <button
                v-if="isCaptain"
                type="button"
                class="account-btn-secondary team-member-list__btn"
                @click="handleSave(member)"
              >
                保存
              </button>
              <button
                type="button"
                class="account-btn-secondary team-member-list__btn"
                @click="openProfile(member)"
              >
                查看资料
              </button>
              <button
                type="button"
                class="account-btn-secondary team-member-list__btn"
                @click="openCareerData(member)"
              >
                生涯数据
              </button>
              <button
                v-if="isCaptain && !isTeamCaptainMember(member)"
                type="button"
                class="account-btn-secondary team-member-list__btn team-member-list__btn--danger"
                @click="confirmRemove(member)"
              >
                移除
              </button>
            </div>
          </div>
        </div>
      </div>

      <div class="team-member-list__cards" aria-label="球队成员卡片列表">
        <article
          v-for="member in members"
          :key="`card-${member.teamMemberId}`"
          class="team-member-list__card"
          :class="{ 'team-member-list__card--captain': isTeamCaptainMember(member) }"
        >
          <div class="team-member-list__card-head">
            <div
              class="team-member-list__avatar team-member-list__avatar--compact"
              :class="{ 'team-member-list__avatar--image': Boolean(member.avatarUrl) }"
            >
              <img v-if="member.avatarUrl" :src="member.avatarUrl" :alt="resolveTeamMemberDisplayName(member)" />
              <span v-else>{{ resolveTeamMemberAvatarFallback(member) }}</span>
            </div>
            <div>
              <div class="team-member-list__player-name">
                <strong>{{ resolveTeamMemberDisplayName(member) }}</strong>
                <AccountStatusBadge
                  v-if="isTeamCaptainMember(member)"
                  type="danger"
                  text="队长"
                  class="team-member-list__captain-badge"
                />
              </div>
              <span class="team-member-list__player-no">{{ member.playerNo || '暂无球员编号' }}</span>
            </div>
          </div>

          <dl class="team-member-list__card-meta">
            <div><dt>位置</dt><dd :title="formatTeamMemberPosition(member)">{{ formatTeamMemberPositionShort(member.positionCode) }}</dd></div>
            <div><dt>球队角色</dt><dd>{{ formatTeamMemberRole(member.roleType) }}</dd></div>
            <div>
              <dt>成员状态</dt>
              <dd>
                <select
                  v-if="isCaptain"
                  v-model="ensureMemberEdit(member).rosterType"
                  class="team-member-list__select"
                >
                  <option value="OFFICIAL">正式</option>
                  <option value="TRIAL">试训</option>
                </select>
                <template v-else>{{ formatTeamMemberMembershipStatus(member.membershipStatus, member.rosterType) }}</template>
              </dd>
            </div>
            <div>
              <dt>球衣号</dt>
              <dd>
                <input
                  v-if="isCaptain"
                  v-model="ensureMemberEdit(member).jerseyNo"
                  type="text"
                  maxlength="3"
                  inputmode="numeric"
                  class="team-member-list__jersey-input"
                  placeholder="号码"
                />
                <template v-else>{{ member.jerseyNo || '-' }}</template>
              </dd>
            </div>
            <div><dt>加入时间</dt><dd>{{ formatTeamMemberJoinTime(member.joinTime) }}</dd></div>
          </dl>

          <div class="team-member-list__actions team-member-list__actions--card">
            <button
              v-if="isCaptain"
              type="button"
              class="account-btn-secondary team-member-list__btn"
              @click="handleSave(member)"
            >
              保存
            </button>
            <button type="button" class="account-btn-secondary team-member-list__btn" @click="openProfile(member)">
              查看资料
            </button>
            <button type="button" class="account-btn-secondary team-member-list__btn" @click="openCareerData(member)">
              生涯数据
            </button>
            <button
              v-if="isCaptain && !isTeamCaptainMember(member)"
              type="button"
              class="account-btn-secondary team-member-list__btn team-member-list__btn--danger"
              @click="confirmRemove(member)"
            >
              移除
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
    overflow: hidden;
  }

  &__head,
  &__row {
    display: grid;
    grid-template-columns:
      minmax(220px, 1.6fr)
      minmax(72px, 0.55fr)
      minmax(88px, 0.65fr)
      minmax(108px, 0.75fr)
      minmax(88px, 0.6fr)
      minmax(108px, 0.75fr)
      minmax(280px, 1.8fr);
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

  &__select,
  &__jersey-input {
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

  &__jersey-input {
    max-width: 72px;
    text-align: center;
  }

  &__actions {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
    justify-content: flex-start;
  }

  &__btn {
    min-height: 36px;
    padding: 0 12px;
    font-size: 14px;
    white-space: nowrap;
  }

  &__btn--danger {
    border-color: rgba(200, 10, 16, 0.35);
    color: #b42318;

    &:hover {
      border-color: rgba(200, 10, 16, 0.55);
      background: rgba(200, 10, 16, 0.06);
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
      minmax(200px, 1.5fr)
      minmax(64px, 0.5fr)
      minmax(80px, 0.6fr)
      minmax(96px, 0.7fr)
      minmax(80px, 0.55fr)
      minmax(240px, 1.4fr);
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
    gap: 10px;
  }

  .team-member-list__btn {
    flex: 1 1 calc(50% - 5px);
    min-width: 0;
  }
}
</style>
