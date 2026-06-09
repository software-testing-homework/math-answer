package com.bookcollection.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookcollection.dto.*;
import com.bookcollection.entity.ChatConversation;
import com.bookcollection.entity.ChatMessage;
import com.bookcollection.mapper.ChatConversationMapper;
import com.bookcollection.mapper.ChatMessageMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ChatService 单元测试")
class ChatServiceTest {

    @Mock
    private ChatClient chatClient;
    @Mock
    private ChatConversationMapper conversationMapper;
    @Mock
    private ChatMessageMapper messageMapper;

    @InjectMocks
    private ChatService chatService;

    // Spring AI ChatClient 调用链 mock（仅 ChatStream 测试使用，lenient 避免 UnnecessaryStubbing）
    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;
    @Mock
    private ChatClient.StreamResponseSpec streamSpec;

    @BeforeEach
    void setUp() {
        lenient().when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        lenient().when(requestSpec.stream()).thenReturn(streamSpec);
    }

    // ==================== 辅助方法 ====================

    private ChatConversation buildConversation(Long id, Long userId, String title, Integer isStar) {
        ChatConversation conv = new ChatConversation();
        conv.setId(id);
        conv.setUserId(userId);
        conv.setTitle(title);
        conv.setDescription("描述");
        conv.setMessageCount(0);
        conv.setTokenCount(0);
        conv.setIsStar(isStar);
        conv.setStatus(1);
        conv.setCreateTime(LocalDateTime.now());
        conv.setLastMessageTime(LocalDateTime.now());
        return conv;
    }

    private ChatMessage buildMessage(Long id, Long conversationId, String role, String content) {
        ChatMessage msg = new ChatMessage();
        msg.setId(id);
        msg.setConversationId(conversationId);
        msg.setRole(role);
        msg.setContent(content);
        msg.setContentType("text");
        msg.setStatus(1);
        msg.setCreateTime(LocalDateTime.now());
        return msg;
    }

    /**
     * 模拟 MyBatis-Plus 在 insert 后自动回填 ID（模拟 AUTO 策略）。
     * 否则 entity.getId() 仍为 null，导致后续逻辑 selectById(null) 失败。
     */
    private void stubInsertWithIdBackfill(Long id) {
        doAnswer(inv -> {
            ChatConversation c = inv.getArgument(0);
            c.setId(id);
            return 1;
        }).when(conversationMapper).insert(any(ChatConversation.class));
    }

    // ==================== 1. createConversation ====================

    @Nested
    @DisplayName("createConversation — 创建对话")
    class CreateConversation {

        @Test
        @DisplayName("正常创建对话")
        void shouldCreateConversation() {
            ConversationCreateRequest request = new ConversationCreateRequest();
            request.setTitle("数学问题");

            stubInsertWithIdBackfill(1L);

            ConversationResponse response = chatService.createConversation(1L, request);

            assertNotNull(response);
            assertEquals("数学问题", response.getTitle());
            assertEquals(1L, response.getUserId());
            assertEquals(0, response.getMessageCount());
            assertEquals(0, response.getIsStar());
            verify(conversationMapper, times(1)).insert(any(ChatConversation.class));
        }

        @Test
        @DisplayName("标题为空时默认使用「新对话」")
        void shouldUseDefaultTitleWhenBlank() {
            ConversationCreateRequest request = new ConversationCreateRequest();
            request.setTitle("");

            stubInsertWithIdBackfill(1L);

            ConversationResponse response = chatService.createConversation(1L, request);

            assertEquals("新对话", response.getTitle());
        }

        @Test
        @DisplayName("标题为 null 时默认使用「新对话」")
        void shouldUseDefaultTitleWhenNull() {
            ConversationCreateRequest request = new ConversationCreateRequest();
            request.setTitle(null);

            stubInsertWithIdBackfill(1L);

            ConversationResponse response = chatService.createConversation(1L, request);

            assertEquals("新对话", response.getTitle());
        }
    }

    // ==================== 2. getConversationList ====================

    @Nested
    @DisplayName("getConversationList — 获取对话列表")
    class GetConversationList {

        @Test
        @DisplayName("正常返回对话列表（含最后一条消息）")
        void shouldReturnConversationListWithLastMessage() {
            ChatConversation conv = buildConversation(1L, 100L, "数学问题", 0);
            when(conversationMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(conv));

            ChatMessage lastMsg = buildMessage(1L, 1L, "assistant", "这是解答内容，比较长的解答...");
            when(messageMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(lastMsg);

            List<ConversationListResponse> result = chatService.getConversationList(100L);

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("数学问题", result.get(0).getTitle());
            assertTrue(result.get(0).getLastMessage().contains("..."));
            assertTrue(result.get(0).getLastMessage().length() <= 53); // 50 + "..."
        }

        @Test
        @DisplayName("无最后一条消息时 lastMessage 为空字符串")
        void shouldReturnEmptyLastMessageWhenNoMessages() {
            ChatConversation conv = buildConversation(1L, 100L, "新对话", 0);
            when(conversationMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(conv));
            when(messageMapper.selectOne(any(LambdaQueryWrapper.class)))
                    .thenReturn(null);

            List<ConversationListResponse> result = chatService.getConversationList(100L);

            assertEquals("", result.get(0).getLastMessage());
        }

        @Test
        @DisplayName("无对话时返回空列表")
        void shouldReturnEmptyList() {
            when(conversationMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of());

            List<ConversationListResponse> result = chatService.getConversationList(100L);

            assertNotNull(result);
            assertTrue(result.isEmpty());
        }
    }

    // ==================== 3. getConversation ====================

    @Nested
    @DisplayName("getConversation — 获取单个对话详情")
    class GetConversation {

        @Test
        @DisplayName("正常返回对话详情（含消息列表）")
        void shouldReturnConversationWithMessages() {
            ChatConversation conv = buildConversation(1L, 100L, "数学问题", 0);
            when(conversationMapper.selectById(1L)).thenReturn(conv);

            ChatMessage userMsg = buildMessage(1L, 1L, "user", "1+1等于几");
            ChatMessage assistantMsg = buildMessage(2L, 1L, "assistant", "等于2");
            when(messageMapper.selectList(any(LambdaQueryWrapper.class)))
                    .thenReturn(List.of(userMsg, assistantMsg));

            ConversationResponse response = chatService.getConversation(1L, 100L);

            assertNotNull(response);
            assertEquals(1L, response.getId());
            assertEquals("数学问题", response.getTitle());
            assertNotNull(response.getMessages());
            assertEquals(2, response.getMessages().size());
            assertEquals("user", response.getMessages().get(0).getRole());
            assertEquals("assistant", response.getMessages().get(1).getRole());
        }

        @Test
        @DisplayName("对话不存在时返回 null")
        void shouldReturnNullWhenNotFound() {
            when(conversationMapper.selectById(999L)).thenReturn(null);

            ConversationResponse response = chatService.getConversation(999L, 100L);

            assertNull(response);
        }

        @Test
        @DisplayName("用户ID不匹配时返回 null")
        void shouldReturnNullWhenUserMismatch() {
            ChatConversation conv = buildConversation(1L, 100L, "数学问题", 0);
            when(conversationMapper.selectById(1L)).thenReturn(conv);

            ConversationResponse response = chatService.getConversation(1L, 999L);

            assertNull(response);
            verify(messageMapper, never()).selectList(any(LambdaQueryWrapper.class));
        }
    }

    // ==================== 4. deleteConversation ====================

    @Nested
    @DisplayName("deleteConversation — 删除对话")
    class DeleteConversation {

        @Test
        @DisplayName("正常删除对话及关联消息")
        void shouldDeleteConversationAndMessages() {
            ChatConversation conv = buildConversation(1L, 100L, "数学问题", 0);
            when(conversationMapper.selectById(1L)).thenReturn(conv);
            when(conversationMapper.deleteById(1L)).thenReturn(1);
            when(messageMapper.delete(any(LambdaQueryWrapper.class))).thenReturn(2);

            assertDoesNotThrow(() -> chatService.deleteConversation(1L, 100L));

            verify(conversationMapper, times(1)).deleteById(1L);
            verify(messageMapper, times(1)).delete(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("对话不存在时静默跳过")
        void shouldSilentlySkipWhenNotFound() {
            when(conversationMapper.selectById(999L)).thenReturn(null);

            assertDoesNotThrow(() -> chatService.deleteConversation(999L, 100L));

            verify(conversationMapper, never()).deleteById(anyLong());
            verify(messageMapper, never()).delete(any(LambdaQueryWrapper.class));
        }

        @Test
        @DisplayName("用户ID不匹配时静默跳过")
        void shouldSilentlySkipWhenUserMismatch() {
            ChatConversation conv = buildConversation(1L, 100L, "数学问题", 0);
            when(conversationMapper.selectById(1L)).thenReturn(conv);

            assertDoesNotThrow(() -> chatService.deleteConversation(1L, 999L));

            verify(conversationMapper, never()).deleteById(anyLong());
        }
    }

    // ==================== 5. toggleStar ====================

    @Nested
    @DisplayName("toggleStar — 切换收藏状态")
    class ToggleStar {

        @Test
        @DisplayName("从未收藏切换为收藏 (0 → 1)")
        void shouldStarConversation() {
            ChatConversation conv = buildConversation(1L, 100L, "数学问题", 0);
            when(conversationMapper.selectById(1L)).thenReturn(conv);
            when(conversationMapper.updateById(any(ChatConversation.class))).thenReturn(1);

            ConversationResponse response = chatService.toggleStar(1L, 100L);

            assertNotNull(response);
            assertEquals(1, response.getIsStar());
            verify(conversationMapper, times(1)).updateById(any(ChatConversation.class));
        }

        @Test
        @DisplayName("从收藏切换为未收藏 (1 → 0)")
        void shouldUnstarConversation() {
            ChatConversation conv = buildConversation(1L, 100L, "数学问题", 1);
            when(conversationMapper.selectById(1L)).thenReturn(conv);
            when(conversationMapper.updateById(any(ChatConversation.class))).thenReturn(1);

            ConversationResponse response = chatService.toggleStar(1L, 100L);

            assertNotNull(response);
            assertEquals(0, response.getIsStar());
        }

        @Test
        @DisplayName("对话不存在时返回 null")
        void shouldReturnNullWhenNotFound() {
            when(conversationMapper.selectById(999L)).thenReturn(null);

            ConversationResponse response = chatService.toggleStar(999L, 100L);

            assertNull(response);
            verify(conversationMapper, never()).updateById(any());
        }

        @Test
        @DisplayName("用户ID不匹配时返回 null")
        void shouldReturnNullWhenUserMismatch() {
            ChatConversation conv = buildConversation(1L, 100L, "数学问题", 0);
            when(conversationMapper.selectById(1L)).thenReturn(conv);

            ConversationResponse response = chatService.toggleStar(1L, 999L);

            assertNull(response);
        }
    }

    // ==================== 6. chatStream ====================

    @Nested
    @DisplayName("chatStream — 流式聊天")
    class ChatStream {

        @Test
        @DisplayName("正常流式对话（已有对话）")
        void shouldChatWithExistingConversation() {
            ChatConversation conv = buildConversation(1L, 100L, "数学问题", 0);
            when(conversationMapper.selectById(1L)).thenReturn(conv);

            // 模拟 AI 流式返回
            when(streamSpec.content()).thenReturn(Flux.just("答案", "是", "4"));

            // 保存用户消息 + 助手消息
            lenient().when(messageMapper.insert(any(ChatMessage.class))).thenReturn(1);
            // 查历史消息
            when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
            // 统计消息数
            when(messageMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
            // 更新对话统计
            when(conversationMapper.updateById(any(ChatConversation.class))).thenReturn(1);

            ChatRequest request = new ChatRequest();
            request.setConversationId(1L);
            request.setMessage("2+2等于几");

            List<String> result = chatService.chatStream(request, 100L)
                    .collectList()
                    .block();

            assertNotNull(result);
            assertEquals(3, result.size());
            assertTrue(result.get(0).startsWith("data: 0:"));
            assertTrue(result.get(1).startsWith("data: 1:"));
            assertTrue(result.get(2).startsWith("data: 2:"));

            // 验证用户消息和助手消息均已保存
            ArgumentCaptor<ChatMessage> msgCaptor = ArgumentCaptor.forClass(ChatMessage.class);
            verify(messageMapper, atLeast(2)).insert(msgCaptor.capture());
            List<ChatMessage> saved = msgCaptor.getAllValues();
            assertTrue(saved.stream().anyMatch(m -> "user".equals(m.getRole())));
            assertTrue(saved.stream().anyMatch(m -> "assistant".equals(m.getRole())));
        }

        @Test
        @DisplayName("conversationId 为 null 时自动创建新对话")
        void shouldAutoCreateConversationWhenIdIsNull() {
            // insert 后回填 ID，避免 response.getId() 为 null
            stubInsertWithIdBackfill(1L);

            ChatConversation newConv = buildConversation(1L, 100L, "2+2等于几...", 0);
            when(conversationMapper.selectById(1L)).thenReturn(newConv);

            when(streamSpec.content()).thenReturn(Flux.just("答案是4"));
            lenient().when(messageMapper.insert(any(ChatMessage.class))).thenReturn(1);
            when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
            when(messageMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
            when(conversationMapper.updateById(any(ChatConversation.class))).thenReturn(1);

            ChatRequest request = new ChatRequest();
            request.setConversationId(null);
            request.setMessage("2+2等于几");

            List<String> result = chatService.chatStream(request, 100L)
                    .collectList()
                    .block();

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(conversationMapper, atLeastOnce()).insert(any(ChatConversation.class));
        }

        @Test
        @DisplayName("conversationId 为 0 时自动创建新对话")
        void shouldAutoCreateConversationWhenIdIsZero() {
            stubInsertWithIdBackfill(1L);

            ChatConversation newConv = buildConversation(1L, 100L, "hello...", 0);
            when(conversationMapper.selectById(1L)).thenReturn(newConv);

            when(streamSpec.content()).thenReturn(Flux.just("OK"));
            lenient().when(messageMapper.insert(any(ChatMessage.class))).thenReturn(1);
            when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
            when(messageMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(2L);
            when(conversationMapper.updateById(any(ChatConversation.class))).thenReturn(1);

            ChatRequest request = new ChatRequest();
            request.setConversationId(0L);
            request.setMessage("hello");

            List<String> result = chatService.chatStream(request, 100L)
                    .collectList()
                    .block();

            assertNotNull(result);
        }

        @Test
        @DisplayName("空 AI 响应时不保存助手消息")
        void shouldNotSaveEmptyAssistantMessage() {
            ChatConversation conv = buildConversation(1L, 100L, "数学问题", 0);
            when(conversationMapper.selectById(1L)).thenReturn(conv);

            // AI 返回空流
            when(streamSpec.content()).thenReturn(Flux.empty());

            lenient().when(messageMapper.insert(any(ChatMessage.class))).thenReturn(1);
            when(messageMapper.selectList(any(LambdaQueryWrapper.class))).thenReturn(List.of());
            when(messageMapper.selectCount(any(LambdaQueryWrapper.class))).thenReturn(1L);
            when(conversationMapper.updateById(any(ChatConversation.class))).thenReturn(1);

            ChatRequest request = new ChatRequest();
            request.setConversationId(1L);
            request.setMessage("test");

            List<String> result = chatService.chatStream(request, 100L)
                    .collectList()
                    .block();

            assertNotNull(result);
            assertTrue(result.isEmpty());

            // 只插入了一条用户消息，没有助手消息
            verify(messageMapper, times(1)).insert(any(ChatMessage.class));
        }
    }
}
