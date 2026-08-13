<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import {
  bindV4PersonalFormationTeam,
  confirmV4PersonalFormation,
  getV4PersonalFormationAvailableTeams,
  getV4PersonalFormationTasks,
} from '@/api/v4PlayerEvents'
import type { V4PersonalFormationAvailableTeam, V4PersonalFormationTask } from '@/api/types/v4PlayerEvents'

const router = useRouter()
const loading = ref(false)
const tasks = ref<V4PersonalFormationTask[]>([])
const selectedTeamId = ref<Record<string, string>>({})
const availableTeamsMap = ref<Record<string, V4PersonalFormationAvailableTeam[]>>({})
const loadingTeamsMap = ref<Record<string, boolean>>({})
const actingTeamId = ref('')

function isVersionedFormation(task: V4PersonalFormationTask) {
  return task.rosterWorkflowMode === 'VERSIONED' || task.needsLongTermBind === false
}

function formationStatusLabel(status?: string) {
  switch ((status || '').toUpperCase()) {
    case 'CAPTAIN_ASSIGNED': return '已指定队长'
    case 'TEAM_FORMING': return '组队中'
    case 'READY_TO_CONFIRM': return '待确认'
    case 'CONFIRMED': return '已确认'
    case 'CONVERTED': return '已转换'
    case 'DRAFT': return '已分配临时球队'
    default: return status || '进行中'
  }
}

async function loadTasks() {
  loading.value = true
  try {
    const res = await getV4PersonalFormationTasks()
    tasks.value = res.data || []
    for (const task of tasks.value) {
      if (task.formationStatus !== 'CONVERTED' && task.longTermTeamId) {
        selectedTeamId.value[task.drawTeamId] = task.longTermTeamId
      }
    }
  } finally {
    loading.value = false
  }
}

async function loadAvailableTeams(task: V4PersonalFormationTask) {
  if (availableTeamsMap.value[task.drawTeamId]?.length) return
  loadingTeamsMap.value[task.drawTeamId] = true
  try {
    const res = await getV4PersonalFormationAvailableTeams(task.drawTeamId)
    availableTeamsMap.value[task.drawTeamId] = res.data || []
    if (task.longTermTeamId) {
      selectedTeamId.value[task.drawTeamId] = task.longTermTeamId
    }
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '加载候选球队失败')
  } finally {
    loadingTeamsMap.value[task.drawTeamId] = false
  }
}

function selectedAvailableTeam(task: V4PersonalFormationTask) {
  const list = availableTeamsMap.value[task.drawTeamId] || []
  const id = selectedTeamId.value[task.drawTeamId]
  return list.find(item => item.teamId === id)
}

async function handleBind(task: V4PersonalFormationTask) {
  const teamId = selectedTeamId.value[task.drawTeamId]
  const selected = selectedAvailableTeam(task)
  if (!teamId || !selected) {
    ElMessage.warning('请选择要绑定的长期球队')
    return
  }
  if (!selected.canBind) {
    ElMessage.warning(selected.unavailableReason || '所选球队不可绑定')
    return
  }
  actingTeamId.value = task.drawTeamId
  try {
    await bindV4PersonalFormationTeam({ drawTeamId: task.drawTeamId, longTermTeamId: teamId })
    ElMessage.success('绑定成功')
    availableTeamsMap.value[task.drawTeamId] = []
    await loadTasks()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '绑定失败')
  } finally {
    actingTeamId.value = ''
  }
}

async function handleConfirm(task: V4PersonalFormationTask) {
  actingTeamId.value = task.drawTeamId
  try {
    await confirmV4PersonalFormation(task.drawTeamId)
    ElMessage.success('成队确认成功')
    await loadTasks()
  } catch (error) {
    ElMessage.error(error instanceof Error ? error.message : '确认失败')
  } finally {
    actingTeamId.value = ''
  }
}

function goCreateTeam() {
  router.push('/account/teams')
}

onMounted(async () => {
  await loadTasks()
})
</script>

<template>
  <section v-if="loading" class="formation-panel">加载组队任务中…</section>
  <section v-else-if="tasks.length" class="formation-panel">
    <h3 class="formation-panel__title">个人成队任务</h3>
    <article v-for="task in tasks" :key="task.drawTeamId" class="formation-panel__item">
      <div class="formation-panel__head">
        <strong>{{ task.temporaryTeamName || task.temporaryTeamCode }}</strong>
        <span>{{ task.divisionName }}</span>
      </div>
      <p class="formation-panel__meta">
        状态：{{ formationStatusLabel(task.formationStatus) }}
        · 成员 {{ task.actualMemberCount ?? 0 }}/{{ task.expectedMemberCount ?? '-' }}
        <span v-if="task.longTermTeamName"> · 已绑定：{{ task.longTermTeamName }}</span>
      </p>
      <ul v-if="task.members?.length" class="formation-panel__members">
        <li v-for="member in task.members" :key="member.playerId">
          {{ member.playerName || member.playerId }}
          <span>{{ member.assignmentRole === 'CAPTAIN' ? '（队长）' : '' }}</span>
        </li>
      </ul>
      <div v-if="isVersionedFormation(task)" class="formation-panel__meta">
        已分配临时球队，锁定后可进入本队比赛提交。
      </div>
      <div v-else-if="task.formationStatus !== 'CONVERTED'" class="formation-panel__actions">
        <button type="button" class="account-btn-secondary" @click="loadAvailableTeams(task)">
          {{ loadingTeamsMap[task.drawTeamId] ? '加载中…' : '加载候选球队' }}
        </button>
        <template v-if="availableTeamsMap[task.drawTeamId]?.length">
          <div class="formation-panel__team-list">
            <label
              v-for="team in availableTeamsMap[task.drawTeamId]"
              :key="team.teamId"
              class="formation-panel__team-option"
              :class="{ 'is-disabled': !team.canBind }"
            >
              <input
                v-model="selectedTeamId[task.drawTeamId]"
                type="radio"
                :value="team.teamId"
                :disabled="!team.canBind"
              />
              <div>
                <strong>{{ team.teamName }}</strong>
                <p>
                  成员 {{ team.currentMemberCount ?? 0 }}
                  · 已匹配 {{ team.matchedAssignedMemberCount ?? 0 }}
                  · 缺失 {{ team.missingAssignedMemberCount ?? 0 }}
                  · 额外 {{ team.extraMemberCount ?? 0 }}
                </p>
                <p v-if="team.canBind" class="ok">可绑定</p>
                <p v-else class="warn">{{ team.unavailableReason || '不可绑定' }}</p>
              </div>
            </label>
          </div>
        </template>
        <p v-else-if="availableTeamsMap[task.drawTeamId] && !availableTeamsMap[task.drawTeamId].length" class="formation-panel__empty">
          暂无可用长期球队，
          <button type="button" class="link-btn" @click="goCreateTeam">创建长期球队</button>
        </p>
        <button
          type="button"
          class="account-btn-secondary"
          :disabled="actingTeamId === task.drawTeamId"
          @click="handleBind(task)"
        >
          绑定球队
        </button>
        <button
          type="button"
          class="account-btn-primary"
          :disabled="actingTeamId === task.drawTeamId"
          @click="handleConfirm(task)"
        >
          确认成队
        </button>
      </div>
    </article>
  </section>
</template>

<style scoped lang="scss">
.formation-panel {
  margin-bottom: 24px;
  &__title {
    margin: 0 0 12px;
    font-size: 16px;
  }
  &__item {
    padding: 16px 20px;
    margin-bottom: 12px;
    border: 1px solid rgba(0, 0, 0, 0.12);
    border-radius: 4px;
    background: rgba(255, 255, 255, 0.35);
  }
  &__head {
    display: flex;
    justify-content: space-between;
    gap: 12px;
    margin-bottom: 8px;
  }
  &__meta {
    margin: 0 0 8px;
    color: rgba(0, 0, 0, 0.65);
    font-size: 13px;
  }
  &__members {
    margin: 0 0 12px;
    padding-left: 18px;
    font-size: 14px;
  }
  &__actions {
    display: flex;
    flex-direction: column;
    gap: 12px;
    align-items: stretch;
  }
  &__team-list {
    display: flex;
    flex-direction: column;
    gap: 8px;
  }
  &__team-option {
    display: flex;
    gap: 10px;
    padding: 10px 12px;
    border: 1px solid rgba(0, 0, 0, 0.12);
    border-radius: 4px;
    cursor: pointer;
    &.is-disabled {
      opacity: 0.65;
      cursor: not-allowed;
    }
    p {
      margin: 4px 0 0;
      font-size: 12px;
      color: rgba(0, 0, 0, 0.65);
    }
    .ok { color: #067647; }
    .warn { color: #c80a10; }
  }
  &__empty {
    margin: 0;
    font-size: 13px;
    color: rgba(0, 0, 0, 0.65);
  }
}
.link-btn {
  background: none;
  border: none;
  padding: 0;
  color: var(--account-primary, #d71920);
  cursor: pointer;
  text-decoration: underline;
}
</style>
