<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const username = ref('')
const password = ref('')
const confirmPassword = ref('')
const email = ref('')
const phone = ref('')
const errorMessage = ref('')
const loading = ref(false)

async function handleRegister() {
  errorMessage.value = ''

  if (!username.value || !password.value) {
    errorMessage.value = '请填写必填项'
    setTimeout(() => {
      errorMessage.value = ''
    }, 2000)
    return
  }

  if (password.value !== confirmPassword.value) {
    errorMessage.value = '两次输入的密码不一致'
    setTimeout(() => {
      errorMessage.value = ''
    }, 2000)
    return
  }

  if (password.value.length < 6) {
    errorMessage.value = '密码长度至少6位'
    setTimeout(() => {
      errorMessage.value = ''
    }, 2000)
    return
  }

  loading.value = true

  try {
    await userStore.register(username.value, password.value, email.value, phone.value)
    router.push('/login')
  } catch (error: any) {
    errorMessage.value = error.message || '注册失败'
    setTimeout(() => {
      errorMessage.value = ''
    }, 2000)
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="auth-layout">
    <div class="auth-bg-decoration"></div>
    <div class="auth-container">
      <div class="auth-header">
        <img alt="Vue logo" class="logo" src="@/assets/logo.svg" />
      </div>

      <div class="auth-content">
        <div class="register-form">
          <h2>注册</h2>
          <form @submit.prevent="handleRegister">
            <div class="form-group">
              <label for="username">用户名 <span class="required">*</span></label>
              <input id="username" v-model="username" type="text" placeholder="请输入用户名" required />
            </div>
            <div class="form-group">
              <label for="password">密码 <span class="required">*</span></label>
              <input id="password" v-model="password" type="password" placeholder="请输入密码（至少6位）" required />
            </div>
            <div class="form-group">
              <label for="confirmPassword">确认密码 <span class="required">*</span></label>
              <input id="confirmPassword" v-model="confirmPassword" type="password" placeholder="请再次输入密码" required />
            </div>
            <div class="form-group">
              <label for="email">邮箱</label>
              <input id="email" v-model="email" type="email" placeholder="请输入邮箱（可选）" />
            </div>
            <div class="form-group">
              <label for="phone">手机号</label>
              <input id="phone" v-model="phone" type="tel" placeholder="请输入手机号（可选）" />
            </div>
            <div v-if="errorMessage" class="error-message">
              {{ errorMessage }}
            </div>
            <button type="submit" :disabled="loading" class="register-btn">
              {{ loading ? '注册中...' : '注册' }}
            </button>
          </form>
          <div class="login-link">
            已有账号？<router-link to="/login">立即登录</router-link>
          </div>
        </div>
      </div>

    </div>
  </div>
</template>

<style scoped>
.auth-layout {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8eb 100%);
}

.auth-bg-decoration {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 10% 20%, rgba(76, 175, 80, 0.08) 0%, transparent 40%),
    radial-gradient(circle at 90% 80%, rgba(76, 175, 80, 0.06) 0%, transparent 40%);
  pointer-events: none;
}

.auth-container {
  width: 100%;
  max-width: 460px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.08);
  padding: 48px 56px 40px;
  z-index: 1;
  border: 2px solid #e5e7eb;
}

/* ---------- header ---------- */
.auth-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo {
  width: 64px;
  height: 64px;
  margin-bottom: 20px;
}


/* ---------- 表单区域 ---------- */
.auth-content {
  margin-bottom: 24px;
}

/* ---------- 底部导航 ---------- */

.welcome {
  margin-right: 12px;
  color: var(--text-regular);
}

/* ---------- 响应式微调 ---------- */
@media (max-width: 480px) {
  .auth-container {
    padding: 36px 24px;
  }
}

.register-form {
  width: 100%;
}

.register-form h2 {
  text-align: center;
  margin-bottom: 2.5rem;
  color: #1e293b;
  font-size: 2rem;
  font-weight: 700;
  letter-spacing: -0.5px;
  background: linear-gradient(135deg, #4CAF50 0%, #2E7D32 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}

.form-group {
  margin-bottom: 1.75rem;
  position: relative;
}

.form-group label {
  display: block;
  margin-bottom: 0.75rem;
  color: #475569;
  font-size: 1rem;
  font-weight: 500;
}

.required {
  color: #ef4444;
  margin-left: 0.25rem;
}

.form-group input {
  width: 100%;
  padding: 1rem 1.5rem;
  border: 2px solid #e2e8f0;
  border-radius: 12px;
  font-size: 1rem;
  font-weight: 400;
  box-sizing: border-box;
  transition: all 0.3s ease;
  background: #f9fafb;
}

.form-group input:focus {
  outline: none;
  border-color: #4CAF50;
  background: white;
  box-shadow: 0 0 0 4px rgba(76, 175, 80, 0.1);
  transform: translateY(-1px);
}

.error-message {
  color: #ef4444;
  font-size: 0.9rem;
  margin-bottom: 2rem;
  text-align: center;
  padding: 1rem;
  background: rgba(239, 68, 68, 0.05);
  border: 1px solid rgba(239, 68, 68, 0.2);
  border-radius: 10px;
}

.register-btn {
  width: 100%;
  padding: 1.125rem 2rem;
  background: linear-gradient(135deg, #4CAF50 0%, #45a049 100%);
  color: white;
  border: none;
  border-radius: 12px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.3s ease;
  box-shadow: 0 4px 15px rgba(76, 175, 80, 0.3);
  margin-bottom: 1.5rem;
}

.register-btn:hover:not(:disabled) {
  transform: translateY(-2px);
  box-shadow: 0 8px 25px rgba(76, 175, 80, 0.4);
  background: linear-gradient(135deg, #45a049 0%, #3d8b40 100%);
}

.register-btn:disabled {
  background: #cbd5e1;
  cursor: not-allowed;
  transform: none;
  box-shadow: none;
  opacity: 0.6;
}

.login-link {
  margin-top: 1.5rem;
  text-align: center;
  font-size: 1rem;
  color: #64748b;
}

.login-link a {
  color: #4CAF50;
  text-decoration: none;
  font-weight: 600;
  transition: all 0.3s ease;
  padding: 0.25rem 0.5rem;
  border-radius: 4px;
}

.login-link a:hover {
  color: #45a049;
  background: rgba(76, 175, 80, 0.05);
}
</style>
