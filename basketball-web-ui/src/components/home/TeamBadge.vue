<script setup lang="ts">
defineProps<{
  type: string
  tiny?: boolean
}>()
</script>

<template>
  <div class="badge" :class="[type, { tiny: tiny }]">
    <div v-if="type === 'titan'" class="shield" />
    <div v-else-if="type === 'phoenix'" class="flame" />
    <div v-else-if="type === 'wolves'" class="wolf" />
    <div v-else-if="type === 'dragon'" class="dragon" />
    <div v-else-if="type === 'legend'" class="crown" />
    <div v-else-if="type === 'rangers'" class="ranger" />
  </div>
</template>

<style scoped lang="scss">
.badge {
  width: 64px;
  height: 64px;
  position: relative;
  display: grid;
  place-items: center;

  &.tiny {
    transform: scale(0.34);
  }
}

.shield {
  width: 48px;
  height: 58px;
  clip-path: polygon(50% 0, 90% 13%, 82% 76%, 50% 100%, 18% 76%, 10% 13%);
  background: linear-gradient(135deg, #e8d7ad, #7b6132);
  border: 2px solid #111;

  &::after {
    content: 'T';
    display: grid;
    place-items: center;
    color: #111;
    font-size: 28px;
    font-weight: 950;
    height: 100%;
  }
}

.flame {
  width: 58px;
  height: 58px;
  background: conic-gradient(from 215deg, #ffd45c, #ff3a16, #5b0300, #ff8c15, #ffd45c);
  clip-path: polygon(58% 0, 79% 18%, 70% 39%, 100% 37%, 74% 60%, 83% 100%, 51% 75%, 18% 97%, 30% 62%, 0 42%, 36% 40%, 32% 17%);
  filter: drop-shadow(0 0 8px rgba(255, 58, 22, 0.5));
}

.wolf {
  width: 58px;
  height: 56px;
  background: linear-gradient(135deg, #e8f1ff, #3c76c4 45%, #151c2f 70%);
  clip-path: polygon(13% 23%, 39% 5%, 49% 22%, 78% 9%, 93% 33%, 75% 76%, 47% 98%, 17% 71%, 0 42%);
}

.dragon {
  width: 60px;
  height: 56px;
  background: linear-gradient(135deg, #d8ec62, #12a56a, #044d35);
  clip-path: polygon(12% 54%, 25% 22%, 52% 6%, 47% 25%, 79% 16%, 64% 39%, 95% 50%, 60% 57%, 52% 87%, 38% 62%, 8% 83%, 28% 58%);
  filter: drop-shadow(0 0 8px rgba(22, 198, 99, 0.42));
}

.crown {
  width: 60px;
  height: 60px;
  background: linear-gradient(180deg, #cc59ff, #3b155d);
  clip-path: polygon(9% 32%, 23% 12%, 37% 33%, 50% 5%, 64% 33%, 79% 12%, 93% 32%, 84% 82%, 16% 82%);
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 12px;
  font-weight: 950;

  &::after {
    content: 'LEG';
    margin-top: 16px;
  }
}

.ranger {
  width: 60px;
  height: 60px;
  background: linear-gradient(145deg, #ff3b24, #2b0605 65%, #111);
  clip-path: polygon(50% 0, 82% 18%, 93% 62%, 72% 96%, 25% 96%, 4% 62%, 17% 18%);
  display: grid;
  place-items: center;
  color: #fff;
  font-size: 10px;
  font-weight: 950;

  &::after {
    content: 'R';
    font-size: 36px;
  }
}
</style>
