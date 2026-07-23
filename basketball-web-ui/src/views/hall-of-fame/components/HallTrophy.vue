<script setup lang="ts">
withDefaults(
  defineProps<{
    size?: 'md' | 'lg' | 'hero'
    label?: string
  }>(),
  {
    size: 'md',
    label: 'TROPHY',
  },
)
</script>

<template>
  <div class="hall-trophy" :class="`hall-trophy--${size}`" aria-hidden="true">
    <div class="hall-trophy__cup">
      <span class="hall-trophy__handles hall-trophy__handles--left" />
      <span class="hall-trophy__handles hall-trophy__handles--right" />
      <span class="hall-trophy__rim" />
      <span class="hall-trophy__body" />
      <span class="hall-trophy__plate" />
      <span class="hall-trophy__stem" />
      <span class="hall-trophy__base" />
    </div>
    <span class="hall-trophy__label">{{ label }}</span>
  </div>
</template>

<style scoped lang="scss">
.hall-trophy {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 10px;
  filter: drop-shadow(0 0 18px rgba(231, 25, 34, 0.22));
}

.hall-trophy__cup {
  position: relative;
}

.hall-trophy__handles {
  position: absolute;
  top: 28%;
  width: 18%;
  height: 34%;
  border: 3px solid rgba(201, 163, 90, 0.82);
  border-radius: 999px;

  &--left {
    left: 4%;
    border-right: 0;
  }

  &--right {
    right: 4%;
    border-left: 0;
  }
}

.hall-trophy__rim {
  position: absolute;
  top: 8%;
  left: 50%;
  width: 72%;
  height: 10%;
  border: 3px solid rgba(201, 163, 90, 0.9);
  border-bottom: 0;
  border-radius: 10px 10px 0 0;
  transform: translateX(-50%);
  box-shadow: inset 0 2px 0 rgba(255, 255, 255, 0.18);
}

.hall-trophy__body {
  position: absolute;
  top: 16%;
  left: 50%;
  width: 56%;
  height: 46%;
  transform: translateX(-50%);
  background: linear-gradient(180deg, #f1d59a 0%, #c9a360 42%, #7a5a22 100%);
  border: 2px solid rgba(201, 163, 90, 0.95);
  border-radius: 0 0 20px 20px;
  box-shadow:
    inset 0 8px 16px rgba(255, 255, 255, 0.22),
    0 10px 28px rgba(201, 163, 90, 0.28);
}

.hall-trophy__plate {
  position: absolute;
  top: 38%;
  left: 50%;
  width: 30%;
  height: 12%;
  transform: translateX(-50%);
  background: linear-gradient(180deg, #1a1a1a 0%, #050505 100%);
  border: 1px solid rgba(201, 163, 90, 0.72);
  border-radius: 2px;
}

.hall-trophy__stem {
  position: absolute;
  top: 62%;
  left: 50%;
  width: 16%;
  height: 10%;
  transform: translateX(-50%);
  background: linear-gradient(180deg, #c9a360 0%, #6f5120 100%);
}

.hall-trophy__base {
  position: absolute;
  top: 72%;
  left: 50%;
  width: 68%;
  height: 12%;
  transform: translateX(-50%);
  background: linear-gradient(180deg, #2a2418 0%, #111 100%);
  border: 1px solid rgba(201, 163, 90, 0.55);
  border-radius: 2px;
}

.hall-trophy__label {
  color: rgba(241, 213, 154, 0.78);
  font-size: 11px;
  font-weight: 800;
  letter-spacing: 0.18em;
  text-transform: uppercase;
}

.hall-trophy--md .hall-trophy__cup {
  width: 88px;
  height: 108px;
}

.hall-trophy--lg .hall-trophy__cup {
  width: 128px;
  height: 156px;
}

.hall-trophy--hero .hall-trophy__cup {
  width: clamp(132px, 11vw, 176px);
  height: clamp(160px, 13vw, 214px);
}

.hall-trophy--hero .hall-trophy__label {
  font-size: 12px;
  letter-spacing: 0.22em;
}
</style>
