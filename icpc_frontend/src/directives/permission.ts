/**
 * 按钮级权限指令：v-permission="['OFFICER', 'PRESIDENT']"
 * 与路由守卫、菜单过滤共同构成 RBAC 三层控制。
 * TODO(backend): 权限码如与后端不一致，只需调整此处的角色列表来源。
 */
import type { Directive, DirectiveBinding } from 'vue'
import { useAuthStore } from '@/stores/auth'
import type { RoleCode } from '@/types'

function apply(el: HTMLElement, binding: DirectiveBinding<RoleCode[]>) {
  const auth = useAuthStore()
  const allowed = binding.value
  el.style.display = auth.hasRole(allowed) ? '' : 'none'
}

export const permission: Directive<HTMLElement, RoleCode[]> = {
  mounted: apply,
  updated: apply,
}
