<script setup lang="ts">
import { computed } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

const currentUser = computed(() => userStore.user)
const avatarUrl = computed(() => {
  if (!userStore.user?.avatar) return ''
  if (userStore.user.avatar.startsWith('http')) {
    return userStore.user.avatar
  }
  return 'http://localhost:8081' + userStore.user.avatar
})

const isLoggedIn = computed(() => userStore.isLoggedIn)
const isAdmin = computed(() => userStore.user?.role === 1)
const currentRoute = computed(() => router.currentRoute.value.path)

const showNavbar = computed(() => {
  return currentRoute.value !== '/login' &&
    currentRoute.value !== '/register'
})

const isHomePage = computed(() => {
  return currentRoute.value === '/'
})

function navigateTo(path: string) {
  if (currentRoute.value !== path) {
    router.push(path)
  }
}

function handleLogout() {
  userStore.logout()
  router.push('/login')
}
</script>

<template>
  <nav v-if="showNavbar" class="navbar">
    <div class="nav-container">
      <div class="nav-brand">
        <img alt="Logo" class="logo" src="@/assets/logo.svg" />
        <span class="brand-name">数学问答系统</span>
      </div>

      <div class="nav-menu">
        <template v-if="isLoggedIn">
          <div class="nav-section">
            <router-link to="/" class="nav-link" active-class="active">
              <span class="nav-icon">🏠</span>
              <span>首页</span>
            </router-link>

            <router-link to="/articles" class="nav-link" active-class="active">
              <span class="nav-icon">📚</span>
              <span>问题资讯</span>
            </router-link>
          </div>

          <div class="nav-divider"></div>

          <div class="nav-section">
            <router-link to="/chat" class="nav-link" active-class="active">
              <span class="nav-icon">💬</span>
              <span>AI对话</span>
            </router-link>

            <router-link to="/history" class="nav-link" active-class="active">
              <span class="nav-icon">❓</span>
              <span>创建问题</span>
            </router-link>

            <router-link to="/favorites" class="nav-link" active-class="active">
              <span class="nav-icon">⭐</span>
              <span>问题收藏</span>
            </router-link>

            <router-link to="/notifications" class="nav-link" active-class="active">
              <span class="nav-icon">🔔</span>
              <span>通知中心</span>
            </router-link>
          </div>

          <div class="nav-divider"></div>

          <div class="nav-section">
            <router-link v-if="isAdmin" to="/admin" class="nav-link admin-link" active-class="active">
              <span class="nav-icon">⚙️</span>
              <span>管理后台</span>
            </router-link>
          </div>
        </template>

        <template v-if="!isLoggedIn && !isHomePage">
          <div class="nav-section">
            <router-link to="/login" class="nav-link" active-class="active">
              <span class="nav-icon">🔐</span>
              <span>登录</span>
            </router-link>

            <router-link to="/register" class="nav-link" active-class="active">
              <span class="nav-icon">📝</span>
              <span>注册</span>
            </router-link>
          </div>
        </template>
      </div>

      <div v-if="isLoggedIn" class="nav-user">
        <div class="user-dropdown">
          <div class="user-info" @click="navigateTo('/profile')">
            <div class="user-avatar">
              <img v-if="avatarUrl" :src="avatarUrl" alt="头像" />
              <span v-else>{{ currentUser?.nickname?.[0] || currentUser?.username?.[0] || 'U' }}</span>
            </div>
            <span class="user-name">{{ currentUser?.nickname || currentUser?.username }}</span>
          </div>
          <div class="dropdown-menu">
            <div class="dropdown-item" @click="navigateTo('/profile')">
              <span>👤</span>
              <span>个人中心</span>
            </div>
            <div class="dropdown-divider"></div>
            <div class="dropdown-item logout" @click="handleLogout">
              <span>🚪</span>
              <span>退出登录</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  </nav>
</template>

<style scoped>
.navbar {
  background: #fff;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  position: sticky;
  top: 0;
  z-index: 100;
}

.nav-container {
  max-width: 1200px;
  margin: 0 auto;
  padding: 0 2rem;
  display: flex;
  justify-content: space-between;
  align-items: center;
  height: 64px;
}

.nav-brand {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  text-decoration: none;
}

.logo {
  width: 36px;
  height: 36px;
}

.brand-name {
  font-size: 1.25rem;
  font-weight: 600;
  color: #333;
}

.nav-menu {
  display: flex;
  align-items: center;
  gap: 0.5rem;
}

.nav-section {
  display: flex;
  align-items: center;
  gap: 0.25rem;
}

.nav-divider {
  width: 1px;
  height: 24px;
  background: #e8e8e8;
  margin: 0 0.5rem;
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 0.35rem;
  padding: 0.4rem 0.85rem;
  text-decoration: none;
  color: #666;
  border-radius: 6px;
  transition: all 0.2s ease;
  font-size: 0.9rem;
  white-space: nowrap;
}

.nav-link:hover {
  background: #f5f5f5;
  color: #667eea;
}

.nav-link.active {
  color: #667eea;
  background: rgba(102, 126, 234, 0.1);
  font-weight: 500;
}

.nav-link.admin-link {
  color: #f59e0b;
}

.nav-link.admin-link:hover {
  background: #fef3c7;
  color: #d97706;
}

.nav-link.admin-link.active {
  color: #d97706;
  background: rgba(245, 158, 11, 0.08);
}

.nav-icon {
  font-size: 1.1rem;
}

.nav-user {
  position: relative;
}

.user-dropdown {
  position: relative;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 0.5rem;
  padding: 0.5rem 1rem;
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.3s;
}

.user-info:hover {
  background: #f5f5f5;
}

.user-avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  overflow: hidden;
  background: linear-gradient(135deg, #52c4dc 0%, #3ebd93 100%);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
}

.user-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-name {
  font-size: 0.9rem;
  color: #333;
  font-weight: 500;
}

.dropdown-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  background: #fff;
  border-radius: 12px;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
  min-width: 180px;
  overflow: hidden;
  opacity: 0;
  visibility: hidden;
  transform: translateY(-10px);
  transition: all 0.3s;
}

.user-dropdown:hover .dropdown-menu {
  opacity: 1;
  visibility: visible;
  transform: translateY(0);
}

.dropdown-item {
  display: flex;
  align-items: center;
  gap: 0.75rem;
  padding: 0.75rem 1rem;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 0.9rem;
  color: #333;
}

.dropdown-item:hover {
  background: #f5f5f5;
  color: #667eea;
}

.dropdown-item.logout {
  color: #f56565;
}

.dropdown-item.logout:hover {
  background: #fee2e2;
  color: #c53030;
}

.dropdown-divider {
  height: 1px;
  background: #e8e8e8;
  margin: 0;
}

@media (max-width: 1024px) {
  .nav-container {
    padding: 0 1.5rem;
  }

  .nav-link {
    padding: 0.35rem 0.75rem;
    font-size: 0.85rem;
  }

  .nav-icon {
    font-size: 1rem;
  }
}

@media (max-width: 768px) {
  .nav-container {
    padding: 0 1rem;
  }

  .brand-name {
    font-size: 1rem;
  }

  .nav-menu {
    gap: 0.25rem;
  }

  .nav-section {
    gap: 0.15rem;
  }

  .nav-divider {
    height: 20px;
    margin: 0 0.35rem;
  }

  .nav-link {
    padding: 0.35rem 0.65rem;
    font-size: 0.85rem;
  }

  .nav-link span:not(.nav-icon) {
    display: none;
  }

  .user-name {
    display: none;
  }
}
</style>
