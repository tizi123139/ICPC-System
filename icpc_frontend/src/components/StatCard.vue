<script setup lang="ts">
defineProps<{
  label: string
  value: string | number
  icon: string
  hint?: string
  tone?: 'blue' | 'cream' | 'green' | 'amber'
}>()
</script>

<template>
  <div class="stat-card ic-card ic-card--hover">
    <div class="content">
      <span class="label">{{ label }}</span>
      <strong class="value">{{ value }}</strong>
      <span v-if="hint" class="hint">{{ hint }}</span>
    </div>
    <div class="icon-wrap" :class="`tone-${tone || 'blue'}`">
      <component :is="icon" />
    </div>
  </div>
</template>

<style scoped>
.stat-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 20px 22px;
  min-height: 108px;
}

.content {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.label {
  font-size: 13px;
  color: var(--text-sub);
}

.value {
  font-size: 26px;
  font-weight: 650;
  color: var(--text-main);
  line-height: 1.2;
}

.hint {
  font-size: 12px;
  color: var(--text-muted);
}

.icon-wrap {
  width: 46px;
  height: 46px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 22px;
  background: var(--primary-lighter);
  color: var(--primary-deep);
}

.tone-cream {
  background: #fbf6e8;
  color: #b58a3a;
}
.tone-green {
  background: #e9f6f1;
  color: var(--success);
}
.tone-amber {
  background: #fdf3e2;
  color: var(--warning);
}
</style>
