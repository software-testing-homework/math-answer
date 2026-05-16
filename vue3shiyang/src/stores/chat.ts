import { defineStore } from 'pinia'
import { ref, computed, nextTick, watch } from 'vue'
import { chatApi, type ChatMessage, type ConversationListResponse } from '@/api/chat'

export const useChatStore = defineStore('chat', () => {
  const conversations = ref<ConversationListResponse[]>([])
  const currentConversationId = ref<number | null>(parseInt(localStorage.getItem('currentConversationId') || '0') || null)
  const messages = ref<ChatMessage[]>([])
  const isLoading = ref(false)
  const isStreaming = ref(false)
  const conversationTitle = ref('新对话')

  watch(currentConversationId, (newId) => {
    if (newId) {
      localStorage.setItem('currentConversationId', newId.toString())
    } else {
      localStorage.removeItem('currentConversationId')
    }
  })

  const hasCurrentConversation = computed(() => currentConversationId.value !== null)

  async function loadConversations() {
    try {
      isLoading.value = true
      const res = await chatApi.getConversationList()
      if (res.code === 200) {
        conversations.value = res.data
      }
    } catch (error) {
      console.error('加载对话列表失败:', error)
      throw error
    } finally {
      isLoading.value = false
    }
  }

  async function createConversation(firstMessage?: string) {
    try {
      isLoading.value = true
      const res = await chatApi.createConversation({
        title: firstMessage ? firstMessage.slice(0, 20) : undefined,
        firstMessage
      })
      if (res.code === 200) {
        const newConversation = res.data
        currentConversationId.value = newConversation.id
        conversationTitle.value = newConversation.title
        messages.value = []
        await loadConversations()
        return newConversation.id
      }
    } catch (error) {
      console.error('创建对话失败:', error)
      throw error
    } finally {
      isLoading.value = false
    }
  }

  async function loadConversation(conversationId: number) {
    try {
      isLoading.value = true
      const res = await chatApi.getConversation(conversationId)
      if (res.code === 200) {
        currentConversationId.value = conversationId
        conversationTitle.value = res.data.title
        messages.value = (res.data.messages || []).map(m => ({
          id: m.id,
          role: m.role as 'user' | 'assistant' | 'system',
          content: m.content,
          contentType: m.contentType,
          createTime: m.createTime
        }))
      }
    } catch (error) {
      console.error('加载对话详情失败:', error)
      throw error
    } finally {
      isLoading.value = false
    }
  }

  async function deleteConversation(conversationId: number) {
    try {
      const res = await chatApi.deleteConversation(conversationId)
      if (res.code === 200) {
        conversations.value = conversations.value.filter(c => c.id !== conversationId)
        if (currentConversationId.value === conversationId) {
          currentConversationId.value = null
          messages.value = []
          conversationTitle.value = '新对话'
        }
        await loadConversations()
      }
    } catch (error) {
      console.error('删除对话失败:', error)
      throw error
    }
  }

  async function toggleStar(conversationId: number) {
    try {
      const res = await chatApi.toggleStar(conversationId)
      if (res.code === 200) {
        const conversation = conversations.value.find(c => c.id === conversationId)
        if (conversation) {
          conversation.isStar = res.data.isStar
        }
      }
    } catch (error) {
      console.error('切换星标失败:', error)
      throw error
    }
  }

  async function sendMessage(content: string) {
    if (!content.trim() || isStreaming.value) return

    const userMessage = content.trim()
    let conversationId = currentConversationId.value
    if (!conversationId) {
      conversationId = await createConversation(userMessage)
    }

    const tempUserId = Date.now()
    messages.value.push({
      id: tempUserId,
      role: 'user',
      content: userMessage
    })

    isStreaming.value = true

    try {
      const tempAssistantId = tempUserId + 1
      messages.value.push({
        id: tempAssistantId,
        role: 'assistant',
        content: ''
      })

      const generator = chatApi.chatStream({
        conversationId,
        message: userMessage
      })

      let fullContent = ''
      let hasContent = false
      let chunkCount = 0

      const processChunk = async (result: IteratorResult<{ content: string; done: boolean; messageId?: number }>) => {
        if (result.done) {
          hasContent = true
          messages.value = messages.value.filter(m => m.id !== tempAssistantId)
          messages.value = [
            ...messages.value,
            {
              id: tempAssistantId,
              role: 'assistant',
              content: fullContent
            }
          ]
          return
        }

        chunkCount++
        fullContent += result.value.content
        messages.value = messages.value.filter(m => m.id !== tempAssistantId)
        messages.value = [
          ...messages.value,
          {
            id: tempAssistantId + chunkCount,
            role: 'assistant',
            content: fullContent
          }
        ]
        await nextTick()
        processNext()
      }

      const processNext = async () => {
        const result = await generator.next()
        await processChunk(result)
      }

      processNext()
    } catch (error) {
      console.error('发送消息失败:', error)
      messages.value = messages.value.filter(m => m.id < tempUserId || m.id >= tempUserId + 1)
      throw error
    } finally {
      isStreaming.value = false
    }
  }

  function clearCurrentConversation() {
    currentConversationId.value = null
    messages.value = []
    conversationTitle.value = '新对话'
  }

  function selectConversation(conversationId: number) {
    loadConversation(conversationId)
  }

  return {
    conversations,
    currentConversationId,
    messages,
    isLoading,
    isStreaming,
    conversationTitle,
    hasCurrentConversation,
    loadConversations,
    createConversation,
    loadConversation,
    deleteConversation,
    toggleStar,
    sendMessage,
    clearCurrentConversation,
    selectConversation
  }
})
