<script setup lang="ts">
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import {
  bindV4PersonalFormationTeam,
  confirmV4PersonalFormation,
  getV4PersonalFormationTasks,
} from '@/api/v4PlayerEvents'
import type { V4PersonalFormationTask } from '@/api/types/v4PlayerEvents'
import { useTeamStore } from '@/stores/team'

const teamStore = useTeamStore()
const loading = ref(false)
const tasks = ref<V4PersonalFormationTask[]>([])
const bindTeamId = ref('')
const actingTeamId = ref('')

function formationStatusLabel(status?: string) {
  switch ((status || '').toUpperCase()) {
    case 'CAPTAIN_ASSIGNED': return '已指定队长'
    case 'TEAM_FORMING': return '组队中'
    case 'READY_TO_CONFIRM': return '待确认'
    case 'CONFIRMED': return '已确认'
    case 'CONVERTED': return '已转换'
    default: return status || '进行中'
  }
}

async function loadTasks() {
  loading.value = true
  try {
    const res = await getV4PersonalFormationTasks()
    tasks.value = res.data || []
  } finally {
    loading.value = false
  }
}

async function handleBind(task: V4PersonalFormationTask) {
  if (!bindTeamId.value) {
    ElMessage.warning('请输入要绑定的长期球队 ID')
    return
  }
  actingTeamId.value = task.drawTeamId
  try {
    await bindV4PersonalFormationTeam({ drawTeamId: task.drawTeamId, longTermTeamId: bindTeamId.value })
    ElMessage.success('绑定成功')
    bindTeamId.value = ''
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

onMounted(async () => {
  await teamStore.bootstrap().catch(() => undefined)
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
      </p>
      <ul v-if="task.members?.length" class="formation-panel__members">
        <li v-for="member in task.members" :key="member.playerId">
          {{ member.playerName || member.playerId }}
          <span>{{ member.assignmentRole === 'CAPTAIN' ? '（队长）' : '' }}</span>
        </li>
      </ul>
      <div v-if="task.formationStatus !== 'CONVERTED'" class="formation-panel__actions">
        <input v-model="bindTeamId" class="account-input" placeholder="长期球队 ID" />
        <button type="button" class="account-btn-secondary" :disabled="actingTeamId === task.drawTeamId" @click="handleBind(task)">
          绑定球队
        </button>
        <button type="button" class="account-btn-primary" :disabled="actingTeamId === task.drawTeamId" @click="handleConfirm(task)">
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
    flex-wrap: wrap;
    gap: 8px;
    align-items: center;
  }
}
</style>
