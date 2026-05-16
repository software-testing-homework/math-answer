<template>
  <div class="chat-container">
    <aside class="chat-sidebar">
      <div class="sidebar-header">
        <h2>AI 对话</h2>
        <button class="new-chat-btn" @click="startNewChat">
          <span class="icon">+</span> 新建对话
        </button>
      </div>

      <div class="conversation-list">
        <div v-for="conv in sortedConversations" :key="conv.id" class="conversation-item"
          :class="{ active: chatStore.currentConversationId === conv.id }" @click="selectConversation(conv.id)">
          <div class="conv-content">
            <span class="conv-icon">{{ conv.isStar ? '⭐' : '💬' }}</span>
            <div class="conv-info">
              <span class="conv-title">{{ conv.title }}</span>
              <span class="conv-last-message">{{ conv.lastMessage || '暂无消息' }}</span>
            </div>
          </div>
          <div class="conv-actions" @click.stop>
            <button class="action-btn" :class="{ starred: conv.isStar }" @click="toggleStar(conv.id)">
              {{ conv.isStar ? '❎' : '🔝' }}
            </button>
            <button class="action-btn delete" @click="deleteConversation(conv.id)">🗑️</button>
          </div>
        </div>

        <div v-if="chatStore.conversations.length === 0 && !chatStore.isLoading" class="empty-list">
          暂无对话记录
        </div>

        <div v-if="chatStore.isLoading" class="loading">
          加载中...
        </div>
      </div>
    </aside>

    <main class="chat-main">
      <div class="chat-header">
        <div class="chat-title">
          <h3>{{ chatStore.conversationTitle }}</h3>
        </div>
      </div>

      <div class="messages-container" ref="messagesContainer" :key="messageKey">
        <div v-for="message in localMessages" :key="message.id" class="message" :class="message.role">
          <div class="message-avatar">
            <img v-if="message.role === 'assistant'" src="@/assets/ai-avatar.svg" alt="AI" />
            <img v-else src="@/assets/user-avatar.svg" alt="User" />
          </div>
          <div class="message-content">
            <div class="message-bubble" v-html="formatMessage(message.content)"></div>
          </div>
        </div>

        <div v-if="chatStore.isStreaming" class="message assistant streaming">
          <div class="message-avatar">
            <img src="@/assets/ai-avatar.svg" alt="AI" />
          </div>
          <div class="message-content">
            <div class="typing-indicator">
              <span></span><span></span><span></span>
            </div>
          </div>
        </div>
      </div>

      <div class="input-area">
        <div class="input-container">
          <textarea v-model="inputMessage" placeholder="输入消息与AI对话..." @keydown.enter.exact.prevent="sendMessage"
            :disabled="chatStore.isStreaming" rows="1" ref="inputRef"></textarea>
          <button class="send-btn" :class="{ stopping: chatStore.isStreaming }"
            @click="chatStore.isStreaming ? stopStreaming() : sendMessage()"
            :disabled="!inputMessage.trim() && !chatStore.isStreaming">
            <svg v-if="!chatStore.isStreaming" viewBox="0 0 24 24" width="20" height="20">
              <path fill="currentColor" d="M2.01 21L23 12 2.01 3 2 10l15 2-15 2z" />
            </svg>
            <svg v-else viewBox="0 0 24 24" width="20" height="20">
              <rect x="6" y="6" width="12" height="12" fill="currentColor" rx="2" />
            </svg>
          </button>
        </div>
        <div class="input-hint">
          按 Enter 发送，Shift+Enter 换行
        </div>
      </div>
    </main>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, nextTick, watch, getCurrentInstance, triggerRef, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useChatStore } from '@/stores/chat'
import { useUserStore } from '@/stores/user'
import { chatApi } from '@/api/chat'
import { marked } from 'marked'
import katex from 'katex'
import 'katex/dist/katex.min.css'

interface Message {
  id: number
  role: 'user' | 'assistant' | 'system'
  content: string
  contentType?: string
  createTime?: string
}

const chatStore = useChatStore()
const userStore = useUserStore()
const router = useRouter()

const inputMessage = ref('')
const inputRef = ref<HTMLTextAreaElement | null>(null)
const messagesContainer = ref<HTMLElement | null>(null)
const localMessages = ref<Message[]>([])
const messageKey = ref(0)

const currentUser = computed(() => userStore.user)

const sortedConversations = computed(() => {
  return [...chatStore.conversations].sort((a, b) => {
    if (a.isStar && !b.isStar) return -1
    if (!a.isStar && b.isStar) return 1
    return 0
  })
})


onMounted(async () => {
  if (!userStore.isLoggedIn) return
  await chatStore.loadConversations()

  if (chatStore.currentConversationId) {
    await chatStore.loadConversation(chatStore.currentConversationId)
    localMessages.value = (chatStore.messages || []).map(m => ({
      id: m.id,
      role: m.role,
      content: m.content,
      contentType: m.contentType,
      createTime: m.createTime
    }))
    scrollToBottom()
  }
})

function startNewChat() {
  chatStore.clearCurrentConversation()
  localMessages.value = []
  inputMessage.value = ''
  nextTick(() => {
    inputRef.value?.focus()
  })
}

async function selectConversation(conversationId: number) {
  await chatStore.loadConversation(conversationId)
  localMessages.value = (chatStore.messages || []).map(m => ({
    id: m.id,
    role: m.role,
    content: m.content,
    contentType: m.contentType,
    createTime: m.createTime
  }))
  scrollToBottom()
}

async function deleteConversation(conversationId: number) {
  if (confirm('确定要删除这个对话吗？')) {
    await chatStore.deleteConversation(conversationId)
  }
}

async function toggleStar(conversationId: number) {
  await chatStore.toggleStar(conversationId)
}

function stopStreaming() {
  chatApi.stopChat()
  chatStore.isStreaming = false
}

async function sendMessage() {
  const message = inputMessage.value.trim()
  if (!message || chatStore.isStreaming) return

  inputMessage.value = ''

  const tempId = Date.now()
  const newMsg = {
    id: tempId,
    role: 'user' as const,
    content: message
  }
  localMessages.value = [...localMessages.value, newMsg]
  triggerRef(localMessages)

  let conversationId: number | undefined = chatStore.currentConversationId || undefined
  if (!conversationId) {
    conversationId = await chatStore.createConversation(message)
  }

  chatStore.isStreaming = true

  try {
    const generator = chatApi.chatStream({
      conversationId,
      message
    })

    let fullContent = ''
    let assistantMsgId = tempId + 1000000
    let chunkCount = 0
    let isStopped = false

    const processResult = async () => {
      if (isStopped) return

      try {
        const result = await generator.next()

        if (isStopped) return

        if (result.done) {
          chatStore.isStreaming = false

          await nextTick()
          requestAnimationFrame(() => {
            scrollToBottom()
          })

          await chatStore.loadConversations()
          await chatStore.loadConversation(conversationId)
          const savedMessages = chatStore.messages || []

          localMessages.value = savedMessages.map(m => ({
            id: m.id,
            role: m.role,
            content: m.content,
            contentType: m.contentType,
            createTime: m.createTime
          }))
          messageKey.value++
          triggerRef(localMessages)

          await nextTick()
          scrollToBottom()

          return
        }

        chunkCount++
        fullContent += result.value.content
        assistantMsgId++

        const streamingMsg = {
          id: assistantMsgId,
          role: 'assistant' as const,
          content: fullContent || '正在思考...'
        }
        localMessages.value = [...localMessages.value.filter(m => m.id === tempId), streamingMsg]
        messageKey.value++
        triggerRef(localMessages)
        chatStore.messages = [...localMessages.value]

        await nextTick()
        scrollToBottom()
        processResult()
      } catch (error: any) {
        if (error.name === 'AbortError' || error.message.includes('abort')) {
          isStopped = true
          chatStore.isStreaming = false
          const stoppedMsg = {
            id: assistantMsgId,
            role: 'assistant' as const,
            content: fullContent || '已停止回答'
          }
          localMessages.value = [...localMessages.value.filter(m => m.id === tempId), stoppedMsg]
          messageKey.value++
          triggerRef(localMessages)
        } else {
          console.error('发送消息失败:', error)
          localMessages.value = localMessages.value.filter(m => m.id === tempId)
          chatStore.isStreaming = false
        }
      }
    }

    processResult()
  } catch (error) {
    console.error('发送消息失败:', error)
    localMessages.value = localMessages.value.filter(m => m.id === tempId)
    chatStore.isStreaming = false
    throw error
  }
}

function scrollToBottom() {
  nextTick(() => {
    if (messagesContainer.value) {
      messagesContainer.value.scrollTop = messagesContainer.value.scrollHeight
    }
  })
}

function formatMessage(content: string): string {
  if (!content) return ''

  const escapeHtml = (text: string): string => {
    return text
      .replace(/&/g, '&amp;')
      .replace(/</g, '&lt;')
      .replace(/>/g, '&gt;')
  }

  let processed = escapeHtml(content)

  processed = processed.replace(/\$\$([\s\S]*?)\$\$/g, (_, math) => {
    try {
      return `<span class="katex-block">${katex.renderToString(math.trim(), { displayMode: true })}</span>`
    } catch {
      return `$$${math}$$`
    }
  })

  processed = processed.replace(/\$([^\$\n]+?)\$/g, (_, math) => {
    try {
      return `<span class="katex-inline">${katex.renderToString(math.trim(), { displayMode: false })}</span>`
    } catch {
      return `$${math}$`
    }
  })

  try {
    processed = marked.parse(processed) as string
  } catch {
    processed = processed.replace(/\n/g, '<br>')
  }

  return processed
}

watch(chatStore.messages, (newMessages) => {
  if (newMessages && newMessages.length > 0 && localMessages.value.length === 0) {
    localMessages.value = newMessages.map(m => ({
      id: m.id,
      role: m.role,
      content: m.content,
      contentType: m.contentType,
      createTime: m.createTime
    }))
  }
}, { immediate: true })

watch(() => chatStore.messages.length, () => {
  scrollToBottom()
})
</script>

<style scoped>
.chat-container {
  display: flex;
  height: calc(100vh - 64px);
  background: linear-gradient(135deg, #f5f7fa 0%, #e4e8eb 100%);
}

.chat-sidebar {
  width: 280px;
  background: white;
  border-right: 2px solid #e5e7eb;
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  box-shadow: 2px 0 8px rgba(0, 0, 0, 0.04);
}

.sidebar-header {
  padding: 24px 20px;
  border-bottom: 2px solid #f1f5f9;
}

.sidebar-header h2 {
  margin: 0 0 20px 0;
  font-size: 22px;
  font-weight: 700;
  color: #1e293b;
}

.new-chat-btn {
  width: 100%;
  padding: 14px 20px;
  background: linear-gradient(135deg, #4CAF50 0%, #45a049 100%);
  color: #fff;
  border: none;
  border-radius: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  font-size: 15px;
  font-weight: 600;
  transition: all 0.3s ease;
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.3);
}

.new-chat-btn:hover {
  box-shadow: 0 6px 16px rgba(76, 175, 80, 0.4);
  transform: translateY(-2px);
}

.new-chat-btn .icon {
  font-size: 20px;
  font-weight: bold;
}

.conversation-list {
  flex: 1;
  overflow-y: auto;
  padding: 16px 12px;
}

.conversation-item {
  padding: 16px 14px;
  border-radius: 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
  transition: all 0.3s ease;
  border: 2px solid transparent;
  background: #f9fafb;
}

.conversation-item:hover {
  background: #f0fdf4;
  border-color: #e5e7eb;
}

.conversation-item.active {
  background: linear-gradient(135deg, #e8f5e9 0%, #c8e6c9 100%);
  border-color: #4CAF50;
  box-shadow: 0 4px 12px rgba(76, 175, 80, 0.2);
}


.conv-content {
  display: flex;
  align-items: center;
  gap: 14px;
  flex: 1;
  min-width: 0;
}

.conv-icon {
  font-size: 20px;
  flex-shrink: 0;
}

.conv-info {
  display: flex;
  flex-direction: column;
  min-width: 0;
  flex: 1;
}

.conv-title {
  font-size: 15px;
  font-weight: 600;
  color: #1e293b;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.conv-last-message {
  font-size: 13px;
  color: #94a3b8;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  margin-top: 6px;
}

.conv-actions {
  display: flex;
  gap: 8px;
  opacity: 0;
  transition: opacity 0.3s ease;
}

.conversation-item:hover .conv-actions {
  opacity: 1;
}

.action-btn {
  background: none;
  border: none;
  cursor: pointer;
  padding: 8px;
  font-size: 15px;
  border-radius: 8px;
  transition: all 0.3s ease;
  color: #94a3b8;
}

.action-btn:hover {
  background: #e5e7eb;
  color: #1e293b;
}

.action-btn.starred {
  color: #faad14;
}

.action-btn.delete:hover {
  background: #fee2e2;
  color: #f5222d;
}

.empty-list,
.loading {
  text-align: center;
  padding: 80px 24px;
  color: #94a3b8;
  font-size: 15px;
}

.chat-main {
  flex: 1;
  display: flex;
  flex-direction: column;
  background: white;
  min-width: 0;
}

.chat-header {
  padding: 20px 40px;
  border-bottom: 2px solid #e5e7eb;
  background: white;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.chat-title {
  display: flex;
  align-items: center;
  gap: 20px;
}

.chat-header h3 {
  margin: 0;
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
}

.chat-title h3 {
  font-size: 20px;
  font-weight: 700;
  color: #1e293b;
}


.icon-btn {
  width: 40px;
  height: 40px;
  border-radius: 10px;
  background: #f0fdf4;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #94a3b8;
  transition: all 0.3s ease;
}

.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 40px;
  background: #f8fafc;
}

.message {
  display: flex;
  gap: 20px;
  margin-bottom: 32px;
  max-width: 1000px;
  margin-left: auto;
  margin-right: auto;
}

.message.user {
  flex-direction: row-reverse;
}

.message-avatar {
  width: 48px;
  height: 48px;
  border-radius: 50%;
  background: #e5e7eb;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

.message.user .message-avatar {
  background: linear-gradient(135deg, #4CAF50 0%, #45a049 100%);
  border: 2px solid #4CAF50;
}

.message.assistant .message-avatar {
  background: linear-gradient(135deg, #73d13d 0%, #52c41a 100%);
  border: 2px solid #73d13d;
}

.message-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.message-content {
  max-width: 75%;
  display: flex;
  flex-direction: column;
}

.message.assistant .message-content {
  align-items: flex-start;
}

.message.user .message-content {
  align-items: flex-end;
}

.message-name {
  font-size: 13px;
  color: #94a3b8;
  margin-bottom: 8px;
  font-weight: 600;
}

.message.user .message-name {
  text-align: right;
}

.message-bubble {
  padding: 16px 24px;
  border-radius: 20px;
  background: #fff;
  color: #1e293b;
  line-height: 1.8;
  font-size: 16px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  border: 2px solid #e5e7eb;
}

.message.user .message-bubble {
  background: linear-gradient(135deg, #4CAF50 0%, #45a049 100%);
  color: #fff;
  border: none;
  box-shadow: 0 6px 16px rgba(76, 175, 80, 0.3);
}

.message.assistant .message-bubble {
  background: #fff;
}

.message-bubble :deep(p) {
  margin: 0 0 12px 0;
}

.message-bubble :deep(p:last-child) {
  margin-bottom: 0;
}

.message-bubble :deep(pre) {
  background: #1e293b;
  color: #e2e8f0;
  padding: 20px;
  border-radius: 12px;
  overflow-x: auto;
  margin: 16px 0;
  font-size: 14px;
  line-height: 1.6;
  border: 2px solid #e5e7eb;
}

.message-bubble :deep(code) {
  font-family: 'Fira Code', 'Consolas', 'Monaco', monospace;
  font-size: 14px;
}

.message-bubble :deep(pre code) {
  background: transparent;
  padding: 0;
}

.message-bubble :deep(.katex-block) {
  margin: 20px 0;
  padding: 20px;
  background: #f8fafc;
  border-radius: 12px;
  overflow-x: auto;
  border: 2px solid #e5e7eb;
}

.message-bubble :deep(.katex-inline) {
  background: #f0fdf4;
  padding: 4px 10px;
  border-radius: 6px;
}

.typing-indicator {
  display: flex;
  gap: 6px;
  padding: 20px 24px;
  background: #fff;
  border-radius: 20px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
  border: 2px solid #e5e7eb;
}

.typing-indicator span {
  width: 12px;
  height: 12px;
  background: #4CAF50;
  border-radius: 50%;
  animation: typing 1.4s infinite;
}

.typing-indicator span:nth-child(2) {
  animation-delay: 0.2s;
  background: #73d13d;
}

.typing-indicator span:nth-child(3) {
  animation-delay: 0.4s;
  background: #40a9ff;
}

@keyframes typing {

  0%,
  60%,
  100% {
    transform: translateY(0);
    opacity: 0.4;
  }

  30% {
    transform: translateY(-12px);
    opacity: 1;
  }
}

.input-area {
  padding: 24px 40px 28px;
  border-top: 2px solid #e5e7eb;
  background: white;
}

.input-container {
  display: flex;
  align-items: flex-end;
  gap: 20px;
  background: #f8fafc;
  border-radius: 16px;
  padding: 16px 20px 16px 24px;
  border: 2px solid #e5e7eb;
  transition: all 0.3s ease;
  min-height: 60px;
}

.input-container:focus-within {
  border-color: #4CAF50;
  box-shadow: 0 0 0 4px rgba(76, 175, 80, 0.15);
  background: white;
}

.input-container textarea {
  flex: 1;
  width: 0;
  display: block;
  min-height: 32px;
  height: auto;
  border: none;
  background: transparent;
  resize: none;
  font-size: 16px;
  line-height: 32px;
  max-height: 140px;
  outline: none;
  color: #1e293b;
  align-self: center;
}

.input-container textarea::placeholder {
  color: #cbd5e1;
}

.send-btn {
  width: 52px;
  height: 52px;
  border-radius: 16px;
  color: #fff;
  border: none;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.3s ease;
  flex-shrink: 0;
}

.send-btn:not(.stopping):not(:disabled) {
  background: linear-gradient(135deg, #4CAF50 0%, #45a049 100%);
  box-shadow: 0 6px 16px rgba(76, 175, 80, 0.3);
}

.send-btn.stopping {
  background: linear-gradient(135deg, #ff7875 0%, #f5222d 100%);
  box-shadow: 0 6px 16px rgba(245, 34, 45, 0.3);
}

.send-btn:hover:not(:disabled) {
  transform: translateY(-2px);
}

.send-btn:not(.stopping):hover:not(:disabled) {
  box-shadow: 0 8px 24px rgba(76, 175, 80, 0.4);
}

.send-btn.stopping:hover:not(:disabled) {
  box-shadow: 0 8px 24px rgba(245, 34, 45, 0.4);
}

.send-btn:disabled {
  background: #cbd5e1;
  cursor: not-allowed;
  box-shadow: none;
  transform: none;
}

.send-btn.stopping:disabled {
  background: linear-gradient(135deg, #ff7875 0%, #f5222d 100%);
}

.send-btn svg {
  width: 24px;
  height: 24px;
}

.input-hint {
  margin-top: 12px;
  font-size: 13px;
  color: #94a3b8;
  text-align: center;
}

@media (max-width: 768px) {
  .chat-sidebar {
    width: 240px;
  }

  .chat-header {
    padding: 16px 24px;
  }

  .chat-title h3 {
    font-size: 16px;
    max-width: 140px;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
  }

  .chat-status {
    display: none;
  }

  .messages-container {
    padding: 24px;
  }

  .message-content {
    max-width: 85%;
  }

  .input-area {
    padding: 20px 24px;
  }

  .input-container {
    padding: 12px 16px 12px 20px;
  }
}
</style>
