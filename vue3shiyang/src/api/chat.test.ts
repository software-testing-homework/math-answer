import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'

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

function createReader(chunks: string[]) {
  let i = 0
  const encoder = new TextEncoder()
  return {
    async read() {
      if (i >= chunks.length) return { done: true as const, value: undefined }
      const value = encoder.encode(chunks[i]!)
      i++
      return { done: false as const, value }
    }
  }
}

describe('chatApi.chatStream', () => {
  beforeEach(() => {
    vi.stubGlobal('localStorage', new MemoryStorage())
    localStorage.clear()
  })

  afterEach(() => {
    vi.unstubAllGlobals()
  })

  it('解析 data: messageId:content 与纯 content', async () => {
    localStorage.setItem('token', 't')

    const fetchMock = vi.fn().mockResolvedValue({
      ok: true,
      body: {
        getReader() {
          return createReader(['data: 12:Hi\n', 'data: world\n', '\n'])
        }
      }
    })
    vi.stubGlobal('fetch', fetchMock as any)

    const { chatApi } = await import('./chat')

    const gen = chatApi.chatStream({ conversationId: 1, message: 'm' })
    const a = await gen.next()
    expect(a.value).toEqual({ content: 'Hi', done: false, messageId: 12 })
    const b = await gen.next()
    expect(b.value).toEqual({ content: 'world', done: false })
    const c = await gen.next()
    expect(c.value).toEqual({ content: '', done: true })
  })

  it('stopChat 会中断并清空 abortController', async () => {
    localStorage.setItem('token', 't')

    const aborted: { value: boolean } = { value: false }

    const fetchMock = vi.fn().mockImplementation(async (_url: string, init: any) => {
      init.signal.addEventListener('abort', () => {
        aborted.value = true
      })
      return {
        ok: true,
        body: {
          getReader() {
            return createReader(['data: 1:x\n'])
          }
        }
      }
    })

    vi.stubGlobal('fetch', fetchMock as any)

    const { chatApi } = await import('./chat')
    const gen = chatApi.chatStream({ conversationId: 1, message: 'm' })
    await gen.next()
    chatApi.stopChat()
    expect(aborted.value).toBe(true)
    expect(chatApi.abortController).toBe(null)
  })
})
