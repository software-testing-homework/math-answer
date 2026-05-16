import { beforeEach, describe, expect, it, vi } from 'vitest'

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

let axiosCreateMock: any
let instance: any
let requestInterceptor: ((config: any) => any) | undefined
let responseFulfilled: ((res: any) => any) | undefined
let responseRejected: ((err: any) => any) | undefined

vi.mock('axios', () => {
  instance = {
    interceptors: {
      request: {
        use: vi.fn((fulfilled: any) => {
          requestInterceptor = fulfilled
        })
      },
      response: {
        use: vi.fn((fulfilled: any, rejected: any) => {
          responseFulfilled = fulfilled
          responseRejected = rejected
        })
      }
    },
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
    delete: vi.fn()
  }

  axiosCreateMock = vi.fn(() => instance)

  return {
    default: {
      create: axiosCreateMock
    }
  }
})

describe('userApi & axios instance', () => {
  beforeEach(() => {
    vi.resetModules()
    requestInterceptor = undefined
    responseFulfilled = undefined
    responseRejected = undefined
    vi.stubGlobal('localStorage', new MemoryStorage())
    vi.stubGlobal('window', { location: { href: '' } } as any)
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('axios.create 的基础配置正确', async () => {
    await import('./user')
    expect(axiosCreateMock).toHaveBeenCalledTimes(1)

    const config = axiosCreateMock.mock.calls[0]?.[0]
    expect(config?.baseURL).toBe('http://localhost:8081/api')
    expect(config?.timeout).toBe(10000)
    expect(config?.headers?.['Content-Type']).toBe('application/json')
  })

  it('请求拦截器：有 token 时注入 Authorization', async () => {
    localStorage.setItem('token', 't')
    await import('./user')
    expect(typeof requestInterceptor).toBe('function')

    const config = { headers: {} as any }
    const out = requestInterceptor!(config)
    expect(out.headers.Authorization).toBe('Bearer t')
  })

  it('响应拦截器：成功时返回 response.data', async () => {
    await import('./user')
    expect(typeof responseFulfilled).toBe('function')
    expect(responseFulfilled!({ data: { ok: 1 } })).toEqual({ ok: 1 })
  })

  it('响应拦截器：401 且非 login 请求时清 token 并跳转 /login', async () => {
    localStorage.setItem('token', 't')
    localStorage.setItem('user', '{"id":1}')
    await import('./user')
    expect(typeof responseRejected).toBe('function')

    await expect(
      responseRejected!({
        response: { status: 401 },
        config: { url: '/user/profile' }
      })
    ).rejects.toBeTruthy()

    expect(localStorage.getItem('token')).toBe(null)
    expect(localStorage.getItem('user')).toBe(null)
    expect((window as any).location.href).toBe('/login')
  })

  it('userApi.login/register 调用正确 endpoint', async () => {
    const { userApi } = await import('./user')
    userApi.login({ username: 'u', password: 'p' })
    expect(instance.post).toHaveBeenCalledWith('/user/login', { username: 'u', password: 'p' })

    userApi.register({ username: 'u2', password: 'p2' })
    expect(instance.post).toHaveBeenCalledWith('/user/register', { username: 'u2', password: 'p2' })
  })
})

