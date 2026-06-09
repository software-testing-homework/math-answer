package com.bookcollection.advisor;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SimpleMessageChatMemoryAdvisor 单元测试")
class SimpleMessageChatMemoryAdvisorTest {

    @Mock
    private ChatClientRequest chatClientRequest;

    @Mock
    private ChatClientResponse chatClientResponse;

    @Mock
    private AdvisorChain advisorChain;

    @Mock
    private Prompt oldPrompt;

    @Mock
    private Prompt newPrompt;

    @Mock
    private ChatResponse chatResponse;

    @Mock
    private Generation generation;

    @Mock
    private ChatClientRequest.Builder requestBuilder;

    @Mock
    private ChatClientRequest newRequest;

    private SimpleMessageChatMemoryAdvisor advisor;

    // ---- 用反射清理 static chatMemory ----

    @BeforeEach
    void setUp() throws Exception {
        advisor = new SimpleMessageChatMemoryAdvisor();
        clearStaticChatMemory();
    }

    @AfterEach
    void tearDown() throws Exception {
        clearStaticChatMemory();
    }

    @SuppressWarnings("unchecked")
    private void clearStaticChatMemory() throws Exception {
        Field field = SimpleMessageChatMemoryAdvisor.class.getDeclaredField("chatMemory");
        field.setAccessible(true);
        Map<String, List<Message>> map = (Map<String, List<Message>>) field.get(null);
        map.clear();
    }

    // ==================== before ====================

    @Nested
    @DisplayName("before(ChatClientRequest, AdvisorChain)")
    class BeforeMethod {

        @Test
        @DisplayName("新会话：创建消息列表，返回更新后的请求")
        void shouldCreateNewMessageListForNewConversation() {
            // 1. Context
            Map<String, Object> context = new HashMap<>();
            context.put("conversationId", "conv-001");

            // 2. 真实消息
            UserMessage userMsg = new UserMessage("hello");
            List<Message> instructions = new ArrayList<>();
            instructions.add(userMsg);

            // 3. Prompt mutate 链
            Prompt.Builder promptBuilder = mock(Prompt.Builder.class);
            lenient().when(oldPrompt.getInstructions()).thenReturn(instructions);
            lenient().when(oldPrompt.mutate()).thenReturn(promptBuilder);
            lenient().when(promptBuilder.messages(anyList())).thenReturn(promptBuilder);
            lenient().when(promptBuilder.build()).thenReturn(newPrompt);

            // 4. ChatClientRequest mutate 链
            lenient().when(chatClientRequest.context()).thenReturn(context);
            lenient().when(chatClientRequest.prompt()).thenReturn(oldPrompt);
            lenient().when(chatClientRequest.mutate()).thenReturn(requestBuilder);
            lenient().when(requestBuilder.prompt(any(Prompt.class))).thenReturn(requestBuilder);
            lenient().when(requestBuilder.build()).thenReturn(newRequest);

            ChatClientRequest result = advisor.before(chatClientRequest, advisorChain);

            assertNotNull(result);
            assertSame(newRequest, result);

            // 验证 promptBuilder.messages 收到了包含用户消息的列表
            ArgumentCaptor<List<Message>> captor = ArgumentCaptor.forClass(List.class);
            verify(promptBuilder).messages(captor.capture());
            List<Message> captured = captor.getValue();
            assertEquals(1, captured.size());
        }

        @Test
        @DisplayName("已有历史消息：追加新消息到已有列表")
        void shouldAppendToExistingMessages() throws Exception {
            // 先执行一次 before 建立记忆
            Map<String, Object> ctx1 = new HashMap<>();
            ctx1.put("conversationId", "conv-001");

            UserMessage msg1 = new UserMessage("第一条消息");
            List<Message> instructions1 = List.of(msg1);

            Prompt prompt1 = mock(Prompt.class);
            Prompt.Builder pb1 = mock(Prompt.Builder.class);
            lenient().when(prompt1.getInstructions()).thenReturn(instructions1);
            lenient().when(prompt1.mutate()).thenReturn(pb1);
            lenient().when(pb1.messages(anyList())).thenReturn(pb1);
            lenient().when(pb1.build()).thenReturn(newPrompt);

            ChatClientRequest req1 = mock(ChatClientRequest.class);
            ChatClientRequest.Builder rb1 = mock(ChatClientRequest.Builder.class);
            lenient().when(req1.context()).thenReturn(ctx1);
            lenient().when(req1.prompt()).thenReturn(prompt1);
            lenient().when(req1.mutate()).thenReturn(rb1);
            lenient().when(rb1.prompt(any(Prompt.class))).thenReturn(rb1);
            lenient().when(rb1.build()).thenReturn(newRequest);
            advisor.before(req1, advisorChain);

            // 再执行第二次 before — 同一会话，追加新消息
            UserMessage msg2 = new UserMessage("第二条消息");
            List<Message> instructions2 = List.of(msg2);

            Prompt prompt2 = mock(Prompt.class);
            Prompt.Builder pb2 = mock(Prompt.Builder.class);
            lenient().when(prompt2.getInstructions()).thenReturn(instructions2);
            lenient().when(prompt2.mutate()).thenReturn(pb2);
            lenient().when(pb2.messages(anyList())).thenReturn(pb2);
            lenient().when(pb2.build()).thenReturn(newPrompt);

            Map<String, Object> ctx2 = new HashMap<>();
            ctx2.put("conversationId", "conv-001");

            ChatClientRequest req2 = mock(ChatClientRequest.class);
            ChatClientRequest.Builder rb2 = mock(ChatClientRequest.Builder.class);
            lenient().when(req2.context()).thenReturn(ctx2);
            lenient().when(req2.prompt()).thenReturn(prompt2);
            lenient().when(req2.mutate()).thenReturn(rb2);
            lenient().when(rb2.prompt(any(Prompt.class))).thenReturn(rb2);
            lenient().when(rb2.build()).thenReturn(newRequest);
            advisor.before(req2, advisorChain);

            // 验证 pb2.messages 收到 2 条消息（原有1条 + 新1条）
            ArgumentCaptor<List<Message>> captor = ArgumentCaptor.forClass(List.class);
            verify(pb2).messages(captor.capture());
            List<Message> captured = captor.getValue();
            assertEquals(2, captured.size());
        }

        @Test
        @DisplayName("不同会话 ID 的消息互不干扰")
        void shouldIsolateByConversationId() throws Exception {
            // 会话 A：1 条消息
            Map<String, Object> ctxA = new HashMap<>();
            ctxA.put("conversationId", "conv-A");
            UserMessage msgA = new UserMessage("消息A");
            List<Message> listA = List.of(msgA);

            Prompt pA = mock(Prompt.class);
            Prompt.Builder pbA = mock(Prompt.Builder.class);
            lenient().when(pA.getInstructions()).thenReturn(listA);
            lenient().when(pA.mutate()).thenReturn(pbA);
            lenient().when(pbA.messages(anyList())).thenReturn(pbA);
            lenient().when(pbA.build()).thenReturn(newPrompt);

            ChatClientRequest reqA = mock(ChatClientRequest.class);
            ChatClientRequest.Builder rbA = mock(ChatClientRequest.Builder.class);
            lenient().when(reqA.context()).thenReturn(ctxA);
            lenient().when(reqA.prompt()).thenReturn(pA);
            lenient().when(reqA.mutate()).thenReturn(rbA);
            lenient().when(rbA.prompt(any(Prompt.class))).thenReturn(rbA);
            lenient().when(rbA.build()).thenReturn(newRequest);
            advisor.before(reqA, advisorChain);

            // 会话 B：1 条消息
            Map<String, Object> ctxB = new HashMap<>();
            ctxB.put("conversationId", "conv-B");
            UserMessage msgB = new UserMessage("消息B");
            List<Message> listB = List.of(msgB);

            Prompt pB = mock(Prompt.class);
            Prompt.Builder pbB = mock(Prompt.Builder.class);
            lenient().when(pB.getInstructions()).thenReturn(listB);
            lenient().when(pB.mutate()).thenReturn(pbB);
            lenient().when(pbB.messages(anyList())).thenReturn(pbB);
            lenient().when(pbB.build()).thenReturn(newPrompt);

            ChatClientRequest reqB = mock(ChatClientRequest.class);
            ChatClientRequest.Builder rbB = mock(ChatClientRequest.Builder.class);
            lenient().when(reqB.context()).thenReturn(ctxB);
            lenient().when(reqB.prompt()).thenReturn(pB);
            lenient().when(reqB.mutate()).thenReturn(rbB);
            lenient().when(rbB.prompt(any(Prompt.class))).thenReturn(rbB);
            lenient().when(rbB.build()).thenReturn(newRequest);
            advisor.before(reqB, advisorChain);

            // 会话 B 只应有自己的 1 条消息
            ArgumentCaptor<List<Message>> captor = ArgumentCaptor.forClass(List.class);
            verify(pbB).messages(captor.capture());
            List<Message> captured = captor.getValue();
            assertEquals(1, captured.size());
        }
    }

    // ==================== after ====================

    @Nested
    @DisplayName("after(ChatClientResponse, AdvisorChain)")
    class AfterMethod {

        @Test
        @DisplayName("正常将 AI 回复消息加入记忆")
        void shouldStoreAssistantMessage() {
            // Context
            Map<String, Object> context = new HashMap<>();
            context.put("conversationId", "conv-001");

            AssistantMessage aiMsg = new AssistantMessage("AI 的回答");

            lenient().when(chatClientResponse.context()).thenReturn(context);
            lenient().when(chatClientResponse.chatResponse()).thenReturn(chatResponse);
            lenient().when(chatResponse.getResult()).thenReturn(generation);
            lenient().when(generation.getOutput()).thenReturn(aiMsg);

            ChatClientResponse result = advisor.after(chatClientResponse, advisorChain);

            assertNotNull(result);
            assertSame(chatClientResponse, result);
            // 不做深度验证记忆内容（static 字段），只验证方法正常返回
        }

        @Test
        @DisplayName("after 先于 before 调用时不抛异常")
        void shouldNotThrowWhenAfterCalledFirst() {
            Map<String, Object> context = new HashMap<>();
            context.put("conversationId", "conv-new");

            AssistantMessage aiMsg = new AssistantMessage("独立回答");

            lenient().when(chatClientResponse.context()).thenReturn(context);
            lenient().when(chatClientResponse.chatResponse()).thenReturn(chatResponse);
            lenient().when(chatResponse.getResult()).thenReturn(generation);
            lenient().when(generation.getOutput()).thenReturn(aiMsg);

            assertDoesNotThrow(() -> advisor.after(chatClientResponse, advisorChain));
        }
    }

    // ==================== getOrder ====================

    @Nested
    @DisplayName("getOrder()")
    class GetOrderMethod {

        @Test
        @DisplayName("返回 0")
        void shouldReturnZero() {
            assertEquals(0, advisor.getOrder());
        }

        @Test
        @DisplayName("多次调用返回一致")
        void shouldBeConsistent() {
            assertEquals(advisor.getOrder(), advisor.getOrder());
        }
    }

    // ==================== 综合 ====================

    @Nested
    @DisplayName("before-after 联动")
    class BeforeAfterIntegration {

        @Test
        @DisplayName("before → after → before 完整对话流程")
        void shouldHandleFullConversation() throws Exception {
            // ---- before: 用户发消息 ----
            Map<String, Object> ctx1 = new HashMap<>();
            ctx1.put("conversationId", "conv-full");

            UserMessage userMsg = new UserMessage("你好");
            List<Message> instructions = List.of(userMsg);

            Prompt prompt1 = mock(Prompt.class);
            Prompt.Builder pb1 = mock(Prompt.Builder.class);
            lenient().when(prompt1.getInstructions()).thenReturn(instructions);
            lenient().when(prompt1.mutate()).thenReturn(pb1);
            lenient().when(pb1.messages(anyList())).thenReturn(pb1);
            lenient().when(pb1.build()).thenReturn(newPrompt);

            ChatClientRequest req1 = mock(ChatClientRequest.class);
            ChatClientRequest.Builder rb1 = mock(ChatClientRequest.Builder.class);
            lenient().when(req1.context()).thenReturn(ctx1);
            lenient().when(req1.prompt()).thenReturn(prompt1);
            lenient().when(req1.mutate()).thenReturn(rb1);
            lenient().when(rb1.prompt(any(Prompt.class))).thenReturn(rb1);
            lenient().when(rb1.build()).thenReturn(newRequest);
            assertNotNull(advisor.before(req1, advisorChain));

            // ---- after: AI 回复 ----
            Map<String, Object> respCtx = new HashMap<>();
            respCtx.put("conversationId", "conv-full");

            AssistantMessage aiMsg = new AssistantMessage("你好！有什么可以帮你的？");

            lenient().when(chatClientResponse.context()).thenReturn(respCtx);
            lenient().when(chatClientResponse.chatResponse()).thenReturn(chatResponse);
            lenient().when(chatResponse.getResult()).thenReturn(generation);
            lenient().when(generation.getOutput()).thenReturn(aiMsg);
            assertNotNull(advisor.after(chatClientResponse, advisorChain));

            // ---- before: 第二轮用户消息 ----
            UserMessage userMsg2 = new UserMessage("今天天气怎么样");
            List<Message> instructions2 = List.of(userMsg2);

            Prompt prompt2 = mock(Prompt.class);
            Prompt.Builder pb2 = mock(Prompt.Builder.class);
            lenient().when(prompt2.getInstructions()).thenReturn(instructions2);
            lenient().when(prompt2.mutate()).thenReturn(pb2);
            lenient().when(pb2.messages(anyList())).thenReturn(pb2);
            lenient().when(pb2.build()).thenReturn(newPrompt);

            Map<String, Object> ctx2 = new HashMap<>();
            ctx2.put("conversationId", "conv-full");

            ChatClientRequest req2 = mock(ChatClientRequest.class);
            ChatClientRequest.Builder rb2 = mock(ChatClientRequest.Builder.class);
            lenient().when(req2.context()).thenReturn(ctx2);
            lenient().when(req2.prompt()).thenReturn(prompt2);
            lenient().when(req2.mutate()).thenReturn(rb2);
            lenient().when(rb2.prompt(any(Prompt.class))).thenReturn(rb2);
            lenient().when(rb2.build()).thenReturn(newRequest);
            assertNotNull(advisor.before(req2, advisorChain));

            // 验证第二轮 before 收到了 3 条消息（用户1 + AI回复 + 用户2）
            ArgumentCaptor<List<Message>> captor = ArgumentCaptor.forClass(List.class);
            verify(pb2).messages(captor.capture());
            List<Message> captured = captor.getValue();
            assertEquals(3, captured.size());
        }
    }
}
