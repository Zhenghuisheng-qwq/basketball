<script setup lang="ts">
import type { AnnouncementListItem } from '../types'

defineProps<{
  item: AnnouncementListItem
}>()

const emit = defineEmits<{
  click: [id: string]
}>()

function handleClick(id: string) {
  emit('click', id)
}
</script>

<template>
  <article
    class="announcement-list-item"
    :class="{ 'announcement-list-item--pinned': item.pinned }"
    role="button"
    tabindex="0"
    @click="handleClick(item.id)"
    @keyup.enter="handleClick(item.id)"
  >
    <div class="announcement-list-item__tags">
      <span v-if="item.pinned" class="announcement-list-item__pin">置顶</span>
      <span
        class="announcement-list-item__category"
        :class="`announcement-list-item__category--${item.categoryCode.toLowerCase()}`"
      >
        {{ item.categoryName }}
      </span>
    </div>

    <div class="announcement-list-item__content">
      <h3 class="announcement-list-item__title">{{ item.title }}</h3>
      <p v-if="item.summary" class="announcement-list-item__scope">{{ item.summary }}</p>
    </div>

    <time class="announcement-list-item__date">{{ item.publishDateText }}</time>

    <span class="announcement-list-item__arrow" aria-hidden="true">›</span>
  </article>
</template>

<style scoped lang="scss">
.announcement-list-item {
  display: grid;
  grid-template-columns: 100px minmax(0, 1fr) 100px 24px;
  gap: 18px 20px;
  align-items: center;
  min-height: 80px;
  padding: 14px 0;
  background: var(--cnpa-paper-bg, #e9e7e7);
  cursor: pointer;
  transition: background 0.18s ease;

  &:nth-child(even) {
    background: rgba(0, 0, 0, 0.018);
  }

  &:hover {
    background: rgba(0, 0, 0, 0.034);

    .announcement-list-item__title {
      color: var(--cnpa-red, #e10613);
    }

    .announcement-list-item__arrow {
      transform: translateX(2px);
    }
  }

  &--pinned {
    background: rgba(255, 255, 255, 0.2);
    box-shadow: inset 3px 0 0 var(--cnpa-red, #e10613);

    &:nth-child(even) {
      background: rgba(255, 255, 255, 0.26);
    }

    &:hover {
      background: rgba(255, 255, 255, 0.32);
    }
  }
}

.announcement-list-item__tags {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}

.announcement-list-item__pin,
.announcement-list-item__category {
  display: inline-flex;
  align-items: center;
  height: 24px;
  padding: 0 8px;
  font-size: 12px;
  font-weight: 700;
  line-height: 1;
  white-space: nowrap;
}

.announcement-list-item__pin {
  background: var(--cnpa-red, #e10613);
  color: #fff;
}

.announcement-list-item__category {
  color: #fff;

  &--event {
    background: #8e0008;
  }

  &--rule {
    background: #111;
  }

  &--notice {
    background: #9a7420;
  }

  &--other {
    background: #555;
  }
}

.announcement-list-item__content {
  min-width: 0;
}

.announcement-list-item__title {
  margin: 0;
  max-width: 760px;
  color: #171717;
  font-size: 17px;
  font-weight: 700;
  line-height: 1.4;
  transition: color 0.18s ease;
}

.announcement-list-item__scope {
  margin: 6px 0 0;
  color: #777;
  font-size: 12px;
  line-height: 1.4;
}

.announcement-list-item__date {
  color: #777;
  font-size: 13px;
  white-space: nowrap;
}

.announcement-list-item__arrow {
  color: #171717;
  font-size: 22px;
  line-height: 1;
  text-align: center;
  transition: transform 0.18s ease;
}

@media (max-width: 767px) {
  .announcement-list-item {
    grid-template-columns: 1fr auto;
    gap: 10px 12px;
    min-height: 0;
    padding: 14px 0;
  }

  .announcement-list-item__tags {
    grid-column: 1;
    grid-row: 1;
    flex-direction: row;
    flex-wrap: wrap;
  }

  .announcement-list-item__arrow {
    grid-column: 2;
    grid-row: 1;
    justify-self: end;
    align-self: center;
  }

  .announcement-list-item__content {
    grid-column: 1 / -1;
    grid-row: 2;
  }

  .announcement-list-item__date {
    grid-column: 1 / -1;
    grid-row: 3;
  }
}
</style>
