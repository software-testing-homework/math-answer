import { createPinia, setActivePinia } from 'pinia'
import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'

import { userApi } from '@/api/user'

vi.mock('@/api/user', () => {
  return {
    userApi: {
      login: vi.fn(),
      register: vi.fn()
    }
  }
})

class MemoryStorage {
  private store = new Map<string, string>()

  getItem(key: string) {
    return this.store.get(key) ?? null
  }

  setItem(key: string, value: string) {
    this.store.set(key, String(value))
  }

  removeItem(key: string) {
    this.store.delete(key)
  }

  clear() {
    this.store.clear()
  }
}

describe('useUserStore', () => {
  beforeAll(() => {
    vi.stubGlobal('localStorage', new MemoryStorage())
  })

  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('setUser：会设置 user/token，并写入 localStorage', async () => {
    const { useUserStore } = await import('./user')
    const store = useUserStore()

    store.setUser({
      id: 1,
      username: 'u',
      nickname: 'n',
      avatar: '',
      email: '',
      phone: '',
      bio: '',
      role: 0,
      token: 't1'
    })

    expect(store.token).toBe('t1')
    expect(store.user?.username).toBe('u')
    expect(store.isLoggedIn).toBe(true)
    expect(localStorage.getItem('token')).toBe('t1')
    expect(JSON.parse(localStorage.getItem('user') || '{}')?.username).toBe('u')
  })

  it('updateUser：会合并更新 user 并回写 localStorage', async () => {
    const { useUserStore } = await import('./user')
    const store = useUserStore()

    store.setUser({
      id: 1,
      username: 'u',
      nickname: 'n',
      avatar: '',
      email: '',
      phone: '',
      bio: '',
      role: 0,
      token: 't1'
    })

    store.updateUser({ nickname: 'n2' })
    expect(store.user?.nickname).toBe('n2')
    expect(JSON.parse(localStorage.getItem('user') || '{}')?.nickname).toBe('n2')
  })

  it('initFromStorage：会从 localStorage 恢复 user/token', async () => {
    localStorage.setItem('token', 't2')
    localStorage.setItem(
      'user',
      JSON.stringify({
        id: 2,
        username: 'u2',
        nickname: 'n2',
        avatar: '',
        email: '',
        phone: '',
        bio: '',
        role: 1,
        token: 't2'
      })
    )

    const { useUserStore } = await import('./user')
    const store = useUserStore()

    store.initFromStorage()
    expect(store.token).toBe('t2')
    expect(store.user?.id).toBe(2)
    expect(store.isAdmin).toBe(true)
  })

  it('logout：会清空 user/token 并移除 localStorage', async () => {
    const { useUserStore } = await import('./user')
    const store = useUserStore()

    store.setUser({
      id: 1,
      username: 'u',
      nickname: 'n',
      avatar: '',
      email: '',
      phone: '',
      bio: '',
      role: 0,
      token: 't1'
    })

    store.logout()
    expect(store.user).toBe(null)
    expect(store.token).toBe(null)
    expect(store.isLoggedIn).toBe(false)
    expect(localStorage.getItem('token')).toBe(null)
    expect(localStorage.getItem('user')).toBe(null)
  })

  it('login：接口 code=200 时返回 true，并写入 user/token', async () => {
    ;(userApi.login as any).mockResolvedValue({
      code: 200,
      message: 'ok',
      data: {
        id: 3,
        username: 'u3',
        nickname: 'n3',
        avatar: '',
        email: '',
        phone: '',
        bio: '',
        role: 0,
        token: 't3'
      }
    })

    const { useUserStore } = await import('./user')
    const store = useUserStore()

    const ok = await store.login('u3', 'p')
    expect(ok).toBe(true)
    expect(store.token).toBe('t3')
    expect(localStorage.getItem('token')).toBe('t3')
  })

  it('login：接口异常时，会抛出更友好的 message', async () => {
    ;(userApi.login as any).mockRejectedValue({
      response: {
        data: {
          message: '用户名或密码错误'
        }
      }
    })

    const { useUserStore } = await import('./user')
    const store = useUserStore()

    await expect(store.login('u', 'bad')).rejects.toThrow('用户名或密码错误')
  })
})

