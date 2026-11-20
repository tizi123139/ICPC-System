<script setup lang="ts">
defineProps<{
  title: string
  subtitle?: string
}>()
</script>

<template>
  <div class="page-header">
    <div class="heading">
      <h1>{{ title }}</h1>
      <p v-if="subtitle">{{ subtitle }}</p>
    </div>
    <div class="actions">
      <slot name="actions" />
    </div>
  </div>
</template>

<style scoped>
.page-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  /* 与下方 .page 的左右留白对齐（32px），避免页头贴边 */
  padding: 26px 32px 0;
  margin-bottom: 0;
}

.heading h1 {
  margin: 0;
  font-size: 23px;
  font-weight: 600;
  line-height: 1.4;
  letter-spacing: 0.3px;
  color: var(--text-main);
}

.heading p {
  margin: 9px 0 0;
  font-size: 13px;
  line-height: 1.6;
  color: var(--text-sub);
}

.actions {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-shrink: 0;
  padding-top: 2px;
}

@media (max-width: 768px) {
  .page-header {
    flex-direction: column;
    align-items: stretch;
    padding: 20px 18px 0;
  }
}
</style>
