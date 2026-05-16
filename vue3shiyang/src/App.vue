<script setup lang="ts">
import { RouterView } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { computed } from 'vue'
import Navbar from '@/components/Navbar.vue'

const userStore = useUserStore()
const isLoggedIn = computed(() => userStore.isLoggedIn)
const user = computed(() => userStore.user)

function handleLogout() {
  userStore.logout()
}
</script>

<template>
  <Navbar />
  <RouterView />
</template>

<style scoped>
.auth-layout {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100vh;
  position: relative;
  overflow: hidden;
}

.auth-bg-decoration {
  position: absolute;
  inset: 0;
  background: radial-gradient(circle at 10% 20%, rgba(0, 196, 143, 0.08) 0%, transparent 40%),
    radial-gradient(circle at 90% 80%, rgba(0, 196, 143, 0.06) 0%, transparent 40%);
  pointer-events: none;
}

.auth-container {
  width: 100%;
  max-width: 440px;
  background: var(--card-bg);
  border-radius: var(--radius);
  box-shadow: var(--shadow);
  padding: 48px 56px 40px;
  z-index: 1;
}

/* ---------- header ---------- */
.auth-header {
  text-align: center;
  margin-bottom: 32px;
}

.logo {
  width: 56px;
  height: 56px;
  margin-bottom: 16px;
}

.site-title {
  font-size: 28px;
  font-weight: 600;
  margin: 0 0 8px;
  color: var(--text-main);
}

.site-subtitle {
  font-size: 14px;
  color: var(--text-light);
  margin: 0;
}

/* ---------- 表单区域 ---------- */
.auth-content {
  margin-bottom: 24px;
}

/* ---------- 底部导航 ---------- */
.auth-nav {
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  color: var(--text-regular);
}

.auth-nav a {
  color: var(--primary);
  text-decoration: none;
  transition: color 0.2s;
}

.auth-nav a:hover {
  color: var(--primary-dark);
}

.nav-divider {
  margin: 0 8px;
  color: var(--border);
}

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
</style>
