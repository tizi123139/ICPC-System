<script setup lang="ts">
withDefaults(
  defineProps<{
    title?: string
    description?: string
  }>(),
  {
    title: '暂无数据',
    description: '当前条件下没有可展示的记录',
  },
)
</script>

<template>
  <div class="empty-state">
    <div class="illustration">
      <span class="dot dot-a" />
      <span class="dot dot-b" />
      <span class="bar" />
    </div>
    <h3>{{ title }}</h3>
    <p>{{ description }}</p>
    <slot name="action" />
  </div>
</template>

<style scoped>
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 56px 20px;
  color: var(--text-sub);
}

.illustration {
  position: relative;
  width: 96px;
  height: 72px;
  margin-bottom: 16px;
  border-radius: var(--radius-md);
  background: linear-gradient(160deg, var(--primary-lighter), #fdfbf4);
  border: 1px dashed var(--border-strong);
}

.dot {
  position: absolute;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: var(--primary-light);
}
.dot-a {
  top: 16px;
  left: 18px;
}
.dot-b {
  top: 16px;
  right: 20px;
  background: #efe3c8;
}

.bar {
  position: absolute;
  left: 18px;
  right: 18px;
  bottom: 18px;
  height: 26px;
  border-radius: 8px;
  background: #eaf4fc;
}

h3 {
  margin: 0 0 6px;
  font-size: 15px;
  color: var(--text-main);
}

p {
  margin: 0;
  font-size: 13px;
}
</style>
