<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import { useAuthStore } from '@/stores/auth'
import type { LoginRequest } from '@/types'

const router = useRouter()
const route = useRoute()
const auth = useAuthStore()

const loading = ref(false)
const formRef = ref<FormInstance>()
const form = reactive<LoginRequest>({
  // 默认使用会长账号，演示时可直接切换学号
  studentId: '1006',
  password: '123456',
})

/** 演示账号：每个角色保留一个账号，学号按 1001-1007 连续编号 */
const demoAccounts = [
  { studentId: '1001', role: '观察会员' },
  { studentId: '1002', role: '正式会员' },
  { studentId: '1003', role: '队长' },
  { studentId: '1004', role: '干事' },
  { studentId: '1005', role: '助理教练' },
  { studentId: '1006', role: '会长/副会长' },
  { studentId: '1007', role: '指导老师' },
]

function fillAccount(studentId: string) {
  form.studentId = studentId
}

const rules: FormRules<LoginRequest> = {
  studentId: [
    { required: true, message: '请输入学号', trigger: 'blur' },
    { pattern: /^\d{4,12}$/, message: '学号格式不正确', trigger: 'blur' },
  ],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleSubmit() {
  if (!formRef.value) return
  const valid = await formRef.value.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    // TODO(backend): 登录成功后后端返回 token + user，由 auth store 统一持久化
    await auth.login(form)
    ElMessage.success('登录成功，欢迎回来')
    const redirect = (route.query.redirect as string) || '/dashboard'
    router.push(redirect)
  } catch (error) {
    // HTTP 错误由 Axios 拦截器统一提示；这里兜底处理非 Axios 异常
    if (error instanceof Error && !('isAxiosError' in error)) ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <section class="hero">
      <div class="hero-inner">
        <div class="brand">
          <div class="logo">IC</div>
          <div>
            <strong>ICPC 协会管理系统</strong>
            <span>ICPC Association Management System</span>
          </div>
        </div>

        <h1>让每一次训练<br />都有迹可循</h1>
      </div>

      <div class="decor decor-a" />
      <div class="decor decor-b" />
    </section>

    <!-- 右侧登录区 -->
    <section class="panel">
      <div class="login-card">
        <header>
          <h2>登录</h2>
          <p>使用学号与密码进入系统</p>
        </header>

        <el-form ref="formRef" :model="form" :rules="rules" label-position="top" @submit.prevent>
          <el-form-item label="学号" prop="studentId">
            <el-input
              v-model="form.studentId"
              placeholder="请输入学号"
              size="large"
              :prefix-icon="'User'"
              clearable
            />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="form.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              :prefix-icon="'Lock'"
              show-password
              @keyup.enter="handleSubmit"
            />
          </el-form-item>

          <div class="form-row">
            <el-checkbox>记住学号</el-checkbox>
            <span class="link">忘记密码？</span>
          </div>

          <el-button
            class="submit"
            type="primary"
            size="large"
            :loading="loading"
            @click="handleSubmit"
          >
            登 录
          </el-button>
        </el-form>

        <div class="demo-tip">
          <el-icon><InfoFilled /></el-icon>
          <div class="demo-content">
            <p>演示环境：密码任意，学号决定登录身份（点击学号直接填入）。</p>
            <div class="demo-accounts">
              <button
                v-for="account in demoAccounts"
                :key="account.studentId"
                type="button"
                class="demo-account"
                :class="{ active: form.studentId === account.studentId }"
                @click="fillAccount(account.studentId)"
              >
                <span class="mono">{{ account.studentId }}</span>
                {{ account.role }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <footer>© 2026 中南民族大学 ICPC 协会 · Aurora 4</footer>
    </section>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh;
  display: grid;
  grid-template-columns: 1fr 1.1fr;
  background: var(--bg-cream);
}

/* ========== 左侧 ========== */
.hero {
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  padding: 64px 72px;
  background:
    radial-gradient(800px 500px at 85% 15%, #ffffff 0%, transparent 55%),
    linear-gradient(150deg, #e8f3fc 0%, #f3f9fe 50%, #fdfbf4 100%);
}

.hero-inner {
  position: relative;
  z-index: 2;
  max-width: 520px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 56px;
}

.logo {
  width: 44px;
  height: 44px;
  border-radius: 14px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  color: #fff;
  background: linear-gradient(140deg, #6fa9dd, #3d7cb0);
  box-shadow: 0 8px 18px rgba(91, 155, 213, 0.35);
}

.brand strong {
  display: block;
  font-size: 16px;
  color: var(--text-main);
}

.brand span {
  font-size: 11px;
  letter-spacing: 0.6px;
  color: var(--text-muted);
}

.hero h1 {
  margin: 0;
  font-size: 52px;
  line-height: 1.4;
  font-weight: 650;
  letter-spacing: 0.5px;
  color: #23404f;
}

.decor {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}

.decor-a {
  width: 320px;
  height: 320px;
  right: -80px;
  bottom: -90px;
  background: radial-gradient(circle at 30% 30%, #ffffff, #dcebf8 70%);
  opacity: 0.7;
}

.decor-b {
  width: 180px;
  height: 180px;
  right: 120px;
  top: -60px;
  background: radial-gradient(circle at 40% 40%, #fdf6e6, #f5ead1 70%);
  opacity: 0.65;
}

/* ========== 右侧 ========== */
.panel {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 20px;
  padding: 48px 40px;
  background: #fdfcf7;
}

.login-card {
  width: 100%;
  max-width: 500px;
  padding: 48px 48px 36px;
  background: #fff;
  border: 1px solid var(--border);
  border-radius: 20px;
  box-shadow: 0 20px 50px rgba(91, 155, 213, 0.1);
}

.login-card header {
  margin-bottom: 30px;
}

.login-card h2 {
  margin: 0 0 8px;
  font-size: 26px;
  font-weight: 650;
  color: var(--text-main);
}

.login-card header p {
  margin: 0;
  font-size: 13px;
  color: var(--text-sub);
}

.form-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin: -4px 0 18px;
  font-size: 13px;
}

.link {
  color: var(--primary-deep);
  cursor: pointer;
}

.submit {
  width: 100%;
  height: 48px;
  font-size: 16px;
  letter-spacing: 4px;
  border-radius: 10px;
}

.demo-tip {
  display: flex;
  align-items: flex-start;
  gap: 8px;
  margin-top: 22px;
  padding: 14px 16px;
  border-radius: var(--radius-md);
  background: var(--primary-lighter);
  color: var(--primary-deep);
  font-size: 12px;
  line-height: 1.7;
}

.demo-content p {
  margin: 0;
}

.demo-accounts {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 10px;
}

.demo-account {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 3px 8px;
  border: 1px solid rgba(91, 155, 213, 0.28);
  border-radius: var(--radius-pill);
  background: #fff;
  color: var(--primary-deep);
  font-size: 11px;
  cursor: pointer;
  transition:
    background 0.16s ease,
    border-color 0.16s ease;
}

.demo-account:hover,
.demo-account.active {
  background: var(--primary-light);
  border-color: var(--primary);
  color: var(--primary-deep);
}

.demo-account .mono {
  font-weight: 600;
}

footer {
  font-size: 12px;
  color: var(--text-muted);
}

@media (max-width: 960px) {
  .login-page {
    grid-template-columns: 1fr;
  }
  .hero {
    display: none;
  }
  .panel {
    min-height: 100vh;
  }
}
</style>
