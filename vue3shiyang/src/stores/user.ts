import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { userApi, type UserResponse } from '@/api/user'

export const useUserStore = defineStore('user', () => {
  const user = ref<UserResponse | null>(null)
  const token = ref<string | null>(null)

  const isLoggedIn = computed(() => !!token.value)
  const isAdmin = computed(() => user.value?.role === 1)

  function setUser(userData: UserResponse) {
    user.value = userData
    token.value = userData.token
    localStorage.setItem('token', userData.token)
    localStorage.setItem('user', JSON.stringify(userData))
  }

  function updateUser(userData: Partial<UserResponse>) {
    if (user.value) {
      user.value = { ...user.value, ...userData }
      localStorage.setItem('user', JSON.stringify(user.value))
    }
  }

  function logout() {
    user.value = null
    token.value = null
    localStorage.removeItem('token')
    localStorage.removeItem('user')
  }

  function initFromStorage() {
    const storedToken = localStorage.getItem('token')
    const storedUser = localStorage.getItem('user')
    if (storedToken && storedUser) {
      token.value = storedToken
      user.value = JSON.parse(storedUser)
    }
  }

  async function login(username: string, password: string) {
    try {
      const response = await userApi.login({ username, password })
      if (response.code === 200) {
        setUser(response.data)
        return true
      }
      throw new Error(response.message || '登录失败')
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || error.message || '登录失败'
      throw new Error(errorMessage)
    }
  }

  async function register(username: string, password: string, email?: string, phone?: string) {
    try {
      const response = await userApi.register({ username, password, email, phone })
      if (response.code === 200) {
        setUser(response.data)
        return true
      }
      throw new Error(response.message || '注册失败')
    } catch (error: any) {
      const errorMessage = error.response?.data?.message || error.message || '注册失败'
      throw new Error(errorMessage)
    }
  }

  return {
    user,
    token,
    isLoggedIn,
    isAdmin,
    setUser,
    updateUser,
    logout,
    initFromStorage,
    login,
    register
  }
})
