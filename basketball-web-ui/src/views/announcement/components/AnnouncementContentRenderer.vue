<script setup lang="ts">
import type { AnnouncementContentBlock } from '../types'

defineProps<{
  blocks: AnnouncementContentBlock[]
}>()
</script>

<template>
  <div class="announcement-content-renderer">
    <template v-for="(block, index) in blocks" :key="index">
      <p
        v-if="block.type === 'PARAGRAPH'"
        class="announcement-content-renderer__paragraph"
        :class="{
          'is-bold': block.bold,
          'is-emphasis': block.emphasis,
        }"
      >
        {{ block.text }}
      </p>

      <component
        v-else-if="block.type === 'HEADING'"
        :is="block.level === 2 ? 'h2' : 'h3'"
        class="announcement-content-renderer__heading"
      >
        <span class="announcement-content-renderer__heading-bar" aria-hidden="true" />
        {{ block.title }}
      </component>

      <section
        v-else-if="block.type === 'INFO_TABLE'"
        class="announcement-content-renderer__info-table"
      >
        <h3 v-if="block.title" class="announcement-content-renderer__info-title">
          <span class="announcement-content-renderer__heading-bar" aria-hidden="true" />
          {{ block.title }}
        </h3>
        <div class="announcement-content-renderer__info-grid">
          <div
            v-for="(row, rowIndex) in block.rows"
            :key="rowIndex"
            class="announcement-content-renderer__info-row"
          >
            <span class="announcement-content-renderer__info-label">{{ row.label }}</span>
            <span
              class="announcement-content-renderer__info-value"
              :class="{ 'is-highlight': row.highlight }"
            >
              {{ row.value }}
            </span>
          </div>
        </div>
      </section>

      <ol
        v-else-if="block.type === 'ORDERED_LIST'"
        class="announcement-content-renderer__list announcement-content-renderer__list--ordered"
      >
        <li v-for="(item, itemIndex) in block.items" :key="itemIndex">{{ item }}</li>
      </ol>

      <ul
        v-else-if="block.type === 'UNORDERED_LIST'"
        class="announcement-content-renderer__list announcement-content-renderer__list--unordered"
      >
        <li v-for="(item, itemIndex) in block.items" :key="itemIndex">{{ item }}</li>
      </ul>

      <router-link
        v-else-if="block.type === 'LINK' && block.url.startsWith('/')"
        :to="block.url"
        class="announcement-content-renderer__link"
      >
        {{ block.label }}
      </router-link>
      <a
        v-else-if="block.type === 'LINK'"
        :href="block.url"
        class="announcement-content-renderer__link"
        target="_blank"
        rel="noopener noreferrer"
      >
        {{ block.label }}
      </a>

      <a
        v-else-if="block.type === 'ATTACHMENT'"
        :href="block.url"
        class="announcement-content-renderer__attachment"
        download
      >
        <span class="announcement-content-renderer__attachment-icon" aria-hidden="true">↓</span>
        <span class="announcement-content-renderer__attachment-name">{{ block.fileName }}</span>
        <span class="announcement-content-renderer__attachment-size">{{ block.fileSize }}</span>
      </a>

      <hr v-else-if="block.type === 'DIVIDER'" class="announcement-content-renderer__divider" />

      <div
        v-else-if="block.type === 'NOTICE'"
        class="announcement-content-renderer__notice"
        :class="`announcement-content-renderer__notice--${block.tone ?? 'info'}`"
      >
        {{ block.text }}
      </div>
    </template>
  </div>
</template>

<style scoped lang="scss">
.announcement-content-renderer {
  color: #272727;
  font-size: 16px;
  line-height: 1.9;
}

.announcement-content-renderer__paragraph {
  margin: 0 0 24px;

  &.is-bold {
    font-weight: 800;
  }

  &.is-emphasis {
    color: var(--cnpa-red, #e10613);
    font-weight: 700;
  }
}

.announcement-content-renderer__heading,
.announcement-content-renderer__info-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 28px 0 16px;
  color: #111;
  font-size: 18px;
  font-weight: 900;
}

.announcement-content-renderer__heading-bar {
  width: 3px;
  height: 18px;
  background: var(--cnpa-red, #e10613);
  flex-shrink: 0;
}

.announcement-content-renderer__info-table {
  width: 100%;
  margin: 20px 0 38px;
}

.announcement-content-renderer__info-grid {
  border: 1px solid rgba(0, 0, 0, 0.12);
  background: rgba(255, 255, 255, 0.18);
}

.announcement-content-renderer__info-row {
  display: grid;
  grid-template-columns: minmax(160px, 200px) 1fr;
  gap: 16px;
  min-height: 56px;
  align-items: center;
  padding: 14px 22px;
  border-top: 1px solid rgba(0, 0, 0, 0.08);
  box-sizing: border-box;

  &:first-child {
    border-top: 0;
  }
}

.announcement-content-renderer__info-label {
  color: var(--cnpa-paper-text-secondary, #666);
  font-size: 14px;
}

.announcement-content-renderer__info-value {
  color: #111;
  font-weight: 700;

  &.is-highlight {
    color: var(--cnpa-red, #e10613);
  }
}

.announcement-content-renderer__list {
  margin: 0 0 18px;
  padding-left: 22px;

  li + li {
    margin-top: 8px;
  }
}

.announcement-content-renderer__link {
  display: inline-flex;
  margin: 0 0 16px;
  color: #111;
  font-weight: 700;
  text-decoration: underline;
  text-underline-offset: 3px;

  &:hover {
    color: var(--cnpa-red, #e10613);
  }
}

.announcement-content-renderer__attachment {
  display: inline-flex;
  align-items: center;
  gap: 10px;
  margin: 0 0 16px;
  padding: 12px 14px;
  border: 1px solid rgba(0, 0, 0, 0.12);
  background: rgba(255, 255, 255, 0.22);
  color: #111;
  text-decoration: none;
  transition: border-color 0.18s ease;

  &:hover {
    border-color: rgba(0, 0, 0, 0.28);
  }
}

.announcement-content-renderer__attachment-name {
  font-weight: 700;
}

.announcement-content-renderer__attachment-size {
  color: var(--cnpa-paper-text-secondary, #666);
  font-size: 13px;
}

.announcement-content-renderer__divider {
  margin: 24px 0;
  border: 0;
  border-top: 1px solid rgba(0, 0, 0, 0.1);
}

.announcement-content-renderer__notice {
  margin: 0 0 16px;
  padding: 12px 14px;
  border: 1px solid rgba(0, 0, 0, 0.1);
  background: rgba(255, 255, 255, 0.24);
  font-size: 14px;

  &--warning {
    border-color: rgba(142, 0, 8, 0.18);
    background: rgba(225, 6, 19, 0.06);
    color: #8e0008;
  }
}

@media (max-width: 767px) {
  .announcement-content-renderer__info-row {
    grid-template-columns: 1fr;
    gap: 6px;
  }
}
</style>
