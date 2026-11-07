<script setup lang="ts">
import { computed } from 'vue'
import championTrophy from '@/assets/icon/team/champion-trophy.png'

const props = withDefaults(
  defineProps<{
    season?: string
    title: string
    date?: string
    tone?: 'champion' | 'runner' | 'third' | 'award'
    mark?: 'trophy' | 'shield' | 'star'
    iconUrl?: string
    clickable?: boolean
  }>(),
  {
    season: '',
    date: '',
    tone: 'award',
    mark: 'trophy',
    iconUrl: '',
    clickable: false,
  },
)

const emit = defineEmits<{
  select: []
}>()

const dateLabel = computed(() => {
  const raw = props.date?.trim()
  if (!raw || raw === '--') return ''
  const matched = raw.match(/^(\d{4})[-/.](\d{2})[-/.](\d{2})/)
  const text = matched ? `${matched[1]}.${matched[2]}.${matched[3]}` : raw.replace(/^—\s*|\s*—$/g, '')
  return `— ${text} —`
})

function onSelect() {
  if (props.clickable) emit('select')
}
</script>

<template>
  <article
    class="honor-plaque"
    :class="[`honor-plaque--${tone}`, { 'is-clickable': clickable }]"
    :role="clickable ? 'button' : undefined"
    :tabindex="clickable ? 0 : undefined"
    @click="onSelect"
    @keydown.enter="onSelect"
  >
    <div class="honor-plaque__emblem" aria-hidden="true">
      <img v-if="iconUrl" class="honor-plaque__icon" :src="iconUrl" alt="" />
      <svg v-else class="honor-plaque__laurel" viewBox="0 0 120 96">
        <path d="M28 74c-2-18 4-34 16-46 2 10 2 22-2 34-6 4-10 8-14 12z" />
        <path d="M22 68c2-3 6-8 12-12 1 6 0 12-2 18-4 0-7-2-10-6z" />
        <path d="M92 74c2-18-4-34-16-46-2 10-2 22 2 34 6 4 10 8 14 12z" />
        <path d="M98 68c-2-3-6-8-12-12-1 6 0 12 2 18 4 0 7-2 10-6z" />
      </svg>
      <img v-if="!iconUrl && mark === 'trophy'" class="honor-plaque__trophy" :src="championTrophy" alt="" />
      <svg v-else-if="!iconUrl && mark === 'shield'" class="honor-plaque__mark" viewBox="0 0 72 72">
        <path d="M36 8l20 8v16c0 12-8 20-20 24C24 52 16 44 16 32V16z" fill="#1a140c" stroke="#e8c56a" stroke-width="2" />
        <path d="M28 30h16v8c0 6-4 10-8 12-4-2-8-6-8-12z" fill="#e8c56a" />
        <text x="36" y="28" text-anchor="middle" fill="#f3e3c2" font-size="7" font-weight="700">CNPA</text>
      </svg>
      <svg v-else-if="!iconUrl" class="honor-plaque__mark" viewBox="0 0 72 72">
        <circle cx="36" cy="34" r="16" fill="none" stroke="#e8c56a" stroke-width="2" />
        <path d="M36 22l3.2 7 7.6.6-5.8 4.8 1.8 7.4L36 37.8 29.2 41.8l1.8-7.4-5.8-4.8 7.6-.6z" fill="#f0d48a" />
      </svg>
    </div>
    <div class="honor-plaque__copy">
      <span v-if="season" class="honor-plaque__season">{{ season }}</span>
      <strong class="honor-plaque__title">{{ title }}</strong>
      <span v-if="dateLabel" class="honor-plaque__date">{{ dateLabel }}</span>
    </div>
  </article>
</template>

<style scoped lang="scss">
.honor-plaque {
  display: grid;
  grid-template-columns: 112px minmax(0, 1fr);
  align-items: center;
  min-height: 156px;
  padding: 14px 18px 14px 8px;
  border: 1px solid rgba(212, 175, 90, 0.78);
  background:
    radial-gradient(80% 120% at 0% 50%, rgba(232, 197, 106, 0.14), transparent 60%),
    linear-gradient(180deg, #1a140e 0%, #0c0b09 100%);
  box-shadow:
    inset 0 0 0 1px rgba(255, 226, 160, 0.08),
    0 8px 18px rgba(0, 0, 0, 0.12);
  box-sizing: border-box;
  color: #f3e3c2;
}

.honor-plaque--runner {
  border-color: rgba(186, 196, 208, 0.7);
  background:
    radial-gradient(80% 120% at 0% 50%, rgba(206, 214, 224, 0.12), transparent 60%),
    linear-gradient(180deg, #17191d 0%, #0c0d10 100%);
}

.honor-plaque--third {
  border-color: rgba(196, 132, 82, 0.72);
  background:
    radial-gradient(80% 120% at 0% 50%, rgba(208, 138, 88, 0.14), transparent 60%),
    linear-gradient(180deg, #1c1410 0%, #0c0b09 100%);
}

.honor-plaque.is-clickable {
  cursor: pointer;

  &:hover {
    border-color: rgba(225, 6, 19, 0.75);
  }
}

.honor-plaque:focus-visible {
  outline: 2px solid rgba(225, 6, 19, 0.45);
  outline-offset: 3px;
}

.honor-plaque__emblem {
  position: relative;
  display: grid;
  place-items: center;
  height: 118px;
}

.honor-plaque__laurel {
  position: absolute;
  inset: 4px 0;
  width: 100%;
  height: 108px;
  fill: #e4c36a;
  opacity: 0.9;
}

.honor-plaque__icon {
  position: relative;
  z-index: 1;
  width: 96px;
  height: 96px;
  object-fit: contain;
  filter: drop-shadow(0 8px 10px rgba(0, 0, 0, 0.45));
}

.honor-plaque__trophy,
.honor-plaque__mark {
  position: relative;
  z-index: 1;
  width: 78px;
  height: 78px;
  object-fit: contain;
  filter: drop-shadow(0 8px 10px rgba(0, 0, 0, 0.45));
}

.honor-plaque__copy {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-width: 0;
  text-align: center;
}

.honor-plaque__season,
.honor-plaque__date {
  color: rgba(243, 227, 194, 0.68);
  font-size: 12px;
  font-weight: 700;
  letter-spacing: 0.03em;
}

.honor-plaque__title {
  display: -webkit-box;
  margin: 8px 0;
  overflow: hidden;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  color: #f2d78a;
  font-size: clamp(22px, 1.7vw, 28px);
  font-weight: 900;
  line-height: 1.15;
  letter-spacing: 0.04em;
}

.honor-plaque--runner .honor-plaque__title {
  color: #d7dee6;
}

.honor-plaque--third .honor-plaque__title {
  color: #e0a06a;
}

@media (max-width: 640px) {
  .honor-plaque {
    grid-template-columns: 92px minmax(0, 1fr);
    min-height: 132px;
    padding-right: 12px;
  }
}
</style>
