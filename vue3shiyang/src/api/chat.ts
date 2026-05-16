import api, { type UserResponse } from './user'

export interface ChatRequest {
  conversationId?: number
  message: string
  modelType?: number
}

export interface ConversationCreateRequest {
  title?: string
  firstMessage?: string
}

export interface ConversationResponse {
  id: number
  userId: number
  title: string
  description?: string
  messageCount: number
  createTime: string
  lastMessageTime?: string
  isStar: number
  messages?: MessageResponse[]
}

export interface ConversationListResponse {
  id: number
  title: string
  description?: string
  messageCount: number
  lastMessage: string
  isStar: number
}

export interface MessageResponse {
  id: number
  role: string
  content: string
  contentType: string
  createTime: string
}

export interface ChatMessage {
  id: number
  role: 'user' | 'assistant' | 'system'
  content: string
  contentType?: string
  createTime?: string
}

export const chatApi = {
  createConversation(data: ConversationCreateRequest) {
    return api.post('/chat/conversations', data)
  },

  getConversationList() {
    return api.get<any, { code: number; message: string; data: ConversationListResponse[] }>('/chat/conversations')
  },

  getConversation(conversationId: number) {
    return api.get<any, { code: number; message: string; data: ConversationResponse }>(`/chat/conversations/${conversationId}`)
  },

  deleteConversation(conversationId: number) {
    return api.delete(`/chat/conversations/${conversationId}`)
  },

  toggleStar(conversationId: number) {
    return api.post(`/chat/conversations/${conversationId}/star`)
  },

  abortController: null as AbortController | null,

  stopChat() {
    if (this.abortController) {
      this.abortController.abort()
      this.abortController = null
    }
  },

  async *chatStream(data: ChatRequest): AsyncGenerator<{ content: string; done: boolean; messageId?: number }> {
    const token = localStorage.getItem('token')
    const baseURL = 'http://localhost:8081/api'
    
    this.abortController = new AbortController()
    
    try {
      const response = await fetch(`${baseURL}/chat/stream`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'Authorization': `Bearer ${token}`
        },
        body: JSON.stringify(data),
        signal: this.abortController.signal
      })

      if (!response.ok) {
        throw new Error(`HTTP error! status: ${response.status}`)
      }

      if (!response.body) {
        throw new Error('Response body is null')
      }

      const reader = response.body.getReader()
      const decoder = new TextDecoder()
      let buffer = ''

      while (true) {
        const { done, value } = await reader.read()
        
        if (done) {
          yield { content: '', done: true }
          break
        }

        buffer += decoder.decode(value, { stream: true })
        const lines = buffer.split('\n')
        buffer = lines.pop() || ''

        for (const line of lines) {
          if (line.startsWith('data: ')) {
            const dataStr = line.slice(6).trim()
            if (dataStr) {
              try {
                const parts = dataStr.split(':', 2)
                if (parts.length === 2 && parts[0] !== undefined && parts[1] !== undefined) {
                  const messageId = parseInt(parts[0], 10)
                  const content = parts[1]
                  yield { content, done: false, messageId }
                } else {
                  yield { content: dataStr, done: false }
                }
              } catch {
                yield { content: dataStr, done: false }
              }
            }
          }
        }
      }
    } finally {
      this.abortController = null
    }
  }
}

export default chatApi
