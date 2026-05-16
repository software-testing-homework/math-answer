import { beforeEach, describe, expect, it, vi } from 'vitest'

let lastRouterOptions: any
let beforeEachGuard: any
let mockStore: any

vi.mock('vue-router', () => {
  return {
    createWebHistory: vi.fn(() => ({})),
    createRouter: vi.fn((options: any) => {
      lastRouterOptions = options
      return {
        beforeEach(fn: any) {
          beforeEachGuard = fn
        }
      }
    })
  }
})

vi.mock('@/stores/user', () => {
  return {
    useUserStore: () => mockStore
  }
})

describe('router', () => {
  beforeEach(() => {
    vi.resetModules()
    lastRouterOptions = undefined
    beforeEachGuard = undefined
    mockStore = { isLoggedIn: false, user: null }
  })

  it('包含关键路由 meta 配置', async () => {
    await import('./index')
    const routes = lastRouterOptions?.routes ?? []

    const chat = routes.find((r: any) => r.path === '/chat')
    expect(chat?.meta?.requiresAuth).toBe(true)

    const admin = routes.find((r: any) => r.path === '/admin')
    expect(admin?.meta?.requiresAuth).toBe(true)
    expect(admin?.meta?.requiresAdmin).toBe(true)
  })

  it('requiresAuth 且未登录时跳转 /login', async () => {
    await import('./index')
    const next = vi.fn()
    mockStore = { isLoggedIn: false, user: null }

    beforeEachGuard({ meta: { requiresAuth: true } }, {}, next)
    expect(next).toHaveBeenCalledWith('/login')
  })

  it('requiresAdmin 且非管理员时跳转 /', async () => {
    await import('./index')
    const next = vi.fn()
    mockStore = { isLoggedIn: true, user: { role: 0 } }

    beforeEachGuard({ meta: { requiresAdmin: true } }, {}, next)
    expect(next).toHaveBeenCalledWith('/')
  })

  it('满足权限时放行 next()', async () => {
    await import('./index')
    const next = vi.fn()
    mockStore = { isLoggedIn: true, user: { role: 1 } }

    beforeEachGuard({ meta: { requiresAuth: true, requiresAdmin: true } }, {}, next)
    expect(next).toHaveBeenCalledWith()
  })
})

