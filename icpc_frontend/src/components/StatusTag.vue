<script setup lang="ts">
defineProps<{
  label: string
  tone?: 'primary' | 'success' | 'warning' | 'danger' | 'info'
}>()
</script>

<template>
  <el-tag
    class="status-tag"
    :type="tone || 'primary'"
    effect="light"
    size="small"
    round
  >
    {{ label }}
  </el-tag>
</template>

<style scoped>
.status-tag {
  padding: 0 10px;
  font-size: 12px;
  line-height: 22px;
}
</style>
