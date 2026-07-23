<script setup lang="ts">
import { computed } from 'vue'
import type { TeamInvitation } from '@/types/account'
import { formatDateTime } from '@/utils/formatDate'

const props = defineProps<{
  modelValue: boolean
  invitation: TeamInvitation | null
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  accept: [id: string]
  reject: [id: string]
}>()

const open = computed({
  get: () => props.modelValue,
  set: (value: boolean) => emit('update:modelValue', value),
})

function close() {
  open.value = false
}
</script>

<template>
  <Teleport to="body">
    <div v-if="open && invitation" class="invitation-dialog">
      <button type="button" class="invitation-dialog__backdrop" aria-label="关闭" @click="close" />
      <div class="invitation-dialog__panel" role="dialog" aria-modal="true" aria-label="处理球队邀请">
        <h3>球队邀请</h3>
        <dl class="account-info-list">
          <div class="account-field-row"><dt>球队名称</dt><dd>{{ invitation.teamName }}</dd></div>
          <div class="account-field-row"><dt>赛事名称</dt><dd>{{ invitation.tournamentName }}</dd></div>
          <div class="account-field-row"><dt>邀请位置</dt><dd>{{ invitation.inviteRole }}</dd></div>
          <div class="account-field-row"><dt>邀请时间</dt><dd>{{ formatDateTime(invitation.createdAt) }}</dd></div>
          <div class="account-field-row"><dt>过期时间</dt><dd>{{ formatDateTime(invitation.expiresAt) }}</dd></div>
        </dl>
        <div class="invitation-dialog__actions">
          <button type="button" class="account-btn-secondary" @click="emit('reject', invitation.id); close()">
            拒绝邀请
          </button>
          <button type="button" class="account-btn-primary" @click="emit('accept', invitation.id); close()">
            接受邀请
          </button>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped lang="scss">
@import '@/views/account/styles/account-center.scss';

.invitation-dialog {
  position: fixed;
  inset: 0;
  z-index: 3100;
  display: grid;
  place-items: center;
  padding: 20px;
}

.invitation-dialog__backdrop {
  position: absolute;
  inset: 0;
  border: 0;
  background: rgba(0, 0, 0, 0.5);
  cursor: pointer;
}

.invitation-dialog__panel {
  position: relative;
  z-index: 1;
  width: min(460px, 100%);
  padding: 24px;
  border: 1px solid var(--account-border);
  border-radius: 3px;
  background: var(--cnpa-paper-bg);

  h3 {
    margin: 0 0 16px;
    font-size: 18px;
    font-weight: 900;
  }
}

.invitation-dialog__actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 16px;
}
</style>