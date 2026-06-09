package com.bookcollection.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.prompt.Prompt;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ZhipuChatMemoryController 单元测试")
class ZhipuChatMemoryControllerTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private ZhipuChatMemoryController controller;

    @BeforeEach
    void setUp() {
        // 构造器调用链: chatClientBuilder.defaultAdvisors(...).build()
        lenient().when(chatClientBuilder.defaultAdvisors(any(Advisor[].class))).thenReturn(chatClientBuilder);
        lenient().when(chatClientBuilder.build()).thenReturn(chatClient);

        // ChatClient 调用链
        lenient().when(chatClient.prompt()).thenReturn(requestSpec);

        // RequestSpec fluent
        lenient().when(requestSpec.user(anyString())).thenReturn(requestSpec);
        lenient().when(requestSpec.advisors(any(Advisor[].class))).thenReturn(requestSpec);
        lenient().when(requestSpec.advisors(any(java.util.function.Consumer.class))).thenReturn(requestSpec);

        // call() → CallResponseSpec
        lenient().when(requestSpec.call()).thenReturn(callResponseSpec);

        controller = new ZhipuChatMemoryController(chatClientBuilder);
    }

    // ==================== simpleMessageChatMemoryAdvisor ====================

    @Nested
    @DisplayName("GET /chatMemory/simpleMessageChatMemoryAdvisor")
    class SimpleMessageChatMemoryAdvisor {

        @Test
        @DisplayName("带对话记忆的 Advisor 调用返回内容")
        void shouldCallWithMemoryAdvisor() {
            when(callResponseSpec.content()).thenReturn("根据历史记忆的回答");

            String result = controller.simpleMessageChatMemoryAdvisor("继续聊", "conv-001");

            assertEquals("根据历史记忆的回答", result);
            verify(requestSpec).user("继续聊");
            verify(requestSpec, atLeast(1)).advisors(any(Advisor[].class));
            verify(requestSpec, atLeast(1)).advisors(any(java.util.function.Consumer.class));
        }
    }

    // ==================== messageChatMemoryAdvisor ====================

    @Nested
    @DisplayName("GET /chatMemory/messageChatMemoryAdvisor")
    class MessageChatMemoryAdvisor {

        @Test
        @DisplayName("用 Lambda 设置 conversationId 并返回内容")
        void shouldSetConversationIdViaLambda() {
            when(callResponseSpec.content()).thenReturn("有记忆的回答");

            String result = controller.messageChatMemoryAdvisor("你在哪", "conv-002");

            assertEquals("有记忆的回答", result);
            verify(requestSpec).user("你在哪");
            verify(requestSpec).advisors(any(Advisor[].class));
        }
    }

    // ==================== main ====================

    @Nested
    @DisplayName("main — PromptTemplate 演示")
    class MainMethod {

        @Test
        @DisplayName("main 方法正常执行不抛异常")
        void shouldRunWithoutException() {
            assertDoesNotThrow(() -> ZhipuChatMemoryController.main(new String[]{}));
        }
    }
}
