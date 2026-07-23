<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import HallPlayerSilhouette from './HallPlayerSilhouette.vue'
import { goToPlayerDetail } from '@/utils/playerNavigate'
import type { ChampionRosterMember } from '../types'

const props = defineProps<{
  members: ChampionRosterMember[]
}>()

const router = useRouter()

const starters = computed(() =>
  props.members.filter((item) => item.role === 'STARTER').sort((a, b) => a.sortOrder - b.sortOrder),
)
const substitutes = computed(() =>
  props.members.filter((item) => item.role !== 'STARTER').sort((a, b) => a.sortOrder - b.sortOrder),
)

function openPlayer(playerId: number) {
  goToPlayerDetail(router, playerId, 'overview')
}
</script>

<template>
  <div class="champion-roster">
    <div class="champion-roster__head">
      <h3 class="champion-roster__title">冠军阵容</h3>
      <span class="champion-roster__sub">CHAMPION ROSTER</span>
    </div>

    <div class="champion-roster__body">
      <section v-if="starters.length" class="champion-roster__group">
        <h4 class="champion-roster__group-title">首发</h4>
        <div class="champion-roster__grid">
          <button
            v-for="member in starters"
            :key="member.id"
            type="button"
            class="champion-roster__item"
            @click="openPlayer(member.playerId)"
          >
            <HallPlayerSilhouette
              :name="member.playerName"
              :avatar-url="member.playerAvatarUrl"
              size="roster"
            />
            <div class="champion-roster__info">
              <strong>
                {{ member.playerName }}
                <span v-if="member.captain" class="champion-roster__captain">队长</span>
              </strong>
              <span>{{ member.position }} · 出场 {{ member.appearances }} 次</span>
            </div>
          </button>
        </div>
      </section>

      <section v-if="substitutes.length" class="champion-roster__group">
        <h4 class="champion-roster__group-title">替补</h4>
        <div class="champion-roster__grid">
          <button
            v-for="member in substitutes"
            :key="member.id"
            type="button"
            class="champion-roster__item"
            @click="openPlayer(member.playerId)"
          >
            <HallPlayerSilhouette
              :name="member.playerName"
              :avatar-url="member.playerAvatarUrl"
              size="roster"
            />
            <div class="champion-roster__info">
              <strong>
                {{ member.playerName }}
                <span v-if="member.captain" class="champion-roster__captain">队长</span>
              </strong>
              <span>{{ member.position }} · 出场 {{ member.appearances }} 次</span>
            </div>
          </button>
        </div>
      </section>
    </div>
  </div>
</template>

<style scoped lang="scss">
.champion-roster {
  margin-top: 0;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-top: 0;
  background: #111;
  overflow: hidden;
}

.champion-roster__head {
  display: flex;
  align-items: baseline;
  gap: 12px;
  min-height: 48px;
  padding: 0 20px;
  background: #0a0a0a;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.champion-roster__title {
  margin: 0;
  color: #fff;
  font-size: 16px;
  font-weight: 800;
}

.champion-roster__sub {
  color: var(--hall-red, #e71922);
  font-size: 12px;
  font-weight: 700;
  font-style: italic;
  letter-spacing: 0.08em;
}

.champion-roster__body {
  padding: 18px 20px 20px;
}

.champion-roster__group + .champion-roster__group {
  margin-top: 18px;
}

.champion-roster__group-title {
  margin: 0 0 12px;
  color: rgba(255, 255, 255, 0.72);
  font-size: 13px;
  font-weight: 800;
  letter-spacing: 0.08em;
}

.champion-roster__grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 12px;
}

.champion-roster__item {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
  margin: 0;
  padding: 10px;
  border: 1px solid rgba(255, 255, 255, 0.08);
  background: rgba(255, 255, 255, 0.03);
  color: #fff;
  text-align: left;
  cursor: pointer;
  transition: border-color 0.18s ease, background 0.18s ease;

  &:hover {
    border-color: rgba(231, 25, 34, 0.55);
    background: rgba(231, 25, 34, 0.08);
  }
}

.champion-roster__info {
  min-width: 0;

  strong {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 14px;
  }

  span {
    display: block;
    margin-top: 4px;
    color: rgba(255, 255, 255, 0.56);
    font-size: 12px;
  }
}

.champion-roster__captain {
  padding: 1px 6px;
  border: 1px solid rgba(210, 166, 72, 0.65);
  background: rgba(210, 166, 72, 0.14);
  color: var(--hall-gold, #d2a648);
  font-size: 10px;
  font-weight: 800;
  letter-spacing: 0.04em;
}

@media (max-width: 1199px) {
  .champion-roster__grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 767px) {
  .champion-roster__grid {
    grid-template-columns: 1fr;
  }
}
</style>
