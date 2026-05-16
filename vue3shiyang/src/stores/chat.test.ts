import { createPinia, setActivePinia } from 'pinia'
import { beforeAll, beforeEach, describe, expect, it, vi } from 'vitest'

import { chatApi } from '@/api/chat'

vi.mock('@/api/chat', () => {
  return {
    chatApi: {
      createConversation: vi.fn(),
      getConversationList: vi.fn(),
      getConversation: vi.fn(),
      deleteConversation: vi.fn(),
      toggleStar: vi.fn(),
      chatStream: vi.fn()
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

async function flush() {
  await Promise.resolve()
  await Promise.resolve()
}

describe('useChatStore', () => {
  beforeAll(() => {
    vi.stubGlobal('localStorage', new MemoryStorage())
  })

  beforeEach(() => {
    setActivePinia(createPinia())
    localStorage.clear()
    vi.clearAllMocks()
  })

  it('createConversation：会创建会话、设置当前会话、并刷新会话列表', async () => {
    ;(chatApi.createConversation as any).mockResolvedValue({
      code: 200,
      data: { id: 101, title: '第一条消息' }
    })
    ;(chatApi.getConversationList as any).mockResolvedValue({
      code: 200,
      data: [
        {
          id: 101,
          title: '第一条消息',
          messageCount: 1,
          lastMessage: '第一条消息',
          isStar: 0
        }
      ]
    })

    const { useChatStore } = await import('./chat')
    const store = useChatStore()

    const id = await store.createConversation('第一条消息')
    expect(id).toBe(101)
    expect(store.currentConversationId).toBe(101)
    expect(store.conversationTitle).toBe('第一条消息')
    expect(store.conversations).toHaveLength(1)

    expect(chatApi.createConversation).toHaveBeenCalledTimes(1)
    expect(chatApi.getConversationList).toHaveBeenCalledTimes(1)
    expect(localStorage.getItem('currentConversationId')).toBe('101')
  })

  it('loadConversation：会把接口消息映射到 store.messages', async () => {
    ;(chatApi.getConversation as any).mockResolvedValue({
      code: 200,
      data: {
        id: 202,
        userId: 1,
        title: '某个对话',
        messageCount: 2,
        createTime: '2026-01-01',
        isStar: 0,
        messages: [
          {
            id: 1,
            role: 'user',
            content: '你好',
            contentType: 'text',
            createTime: '2026-01-01'
          },
          {
            id: 2,
            role: 'assistant',
            content: '你好呀',
            contentType: 'text',
            createTime: '2026-01-01'
          }
        ]
      }
    })

    const { useChatStore } = await import('./chat')
    const store = useChatStore()

    await store.loadConversation(202)
    expect(store.currentConversationId).toBe(202)
    expect(store.conversationTitle).toBe('某个对话')
    expect(store.messages).toHaveLength(2)
    expect(store.messages[0]?.role).toBe('user')
    expect(store.messages[1]?.role).toBe('assistant')
  })

  it('sendMessage：会追加用户消息，并把流式内容拼成一条 assistant 消息', async () => {
    ;(chatApi.createConversation as any).mockResolvedValue({
      code: 200,
      data: { id: 303, title: '你好' }
    })
    ;(chatApi.getConversationList as any).mockResolvedValue({
      code: 200,
      data: []
    })

    async function* mockStream() {
      yield { content: 'Hi', done: false }
      yield { content: '!', done: false }
      return { content: '', done: true }
    }

    ;(chatApi.chatStream as any).mockReturnValue(mockStream())

    const { useChatStore } = await import('./chat')
    const store = useChatStore()

    await store.sendMessage('  hello  ')
    await flush()
    await flush()

    expect(store.currentConversationId).toBe(303)
    expect(store.messages.some(m => m.role === 'user' && m.content === 'hello')).toBe(true)

    const lastAssistant = [...store.messages].reverse().find(m => m.role === 'assistant')
    expect(lastAssistant?.content).toBe('Hi!')
  })
})

