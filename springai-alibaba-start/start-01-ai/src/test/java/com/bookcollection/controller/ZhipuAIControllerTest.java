package com.bookcollection.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("ZhipuAIController 单元测试")
class ZhipuAIControllerTest {

    @Mock
    private ChatModel chatModel;

    @InjectMocks
    private ZhipuAIController controller;

    // ==================== chat ====================

    @Nested
    @DisplayName("POST /api/zhipuai/chat")
    class Chat {

        @Test
        @DisplayName("返回 hello world")
        void shouldReturnHelloWorld() {
            assertEquals("hello world", controller.chat());
        }
    }

    // ==================== simple ====================

    @Nested
    @DisplayName("GET /api/zhipuai/simple")
    class Simple {

        @Test
        @DisplayName("正常调用 ChatModel 并返回结果")
        void shouldCallChatModel() {
            when(chatModel.call("什么是Java")).thenReturn("Java是一门编程语言");

            String result = controller.simple("什么是Java");

            assertEquals("Java是一门编程语言", result);
        }
    }

    // ==================== message ====================

    @Nested
    @DisplayName("GET /api/zhipuai/message")
    class Message {

        @Test
        @DisplayName("正常调用并返回结果")
        void shouldCallWithSystemAndUserMessage() {
            when(chatModel.call("什么是Java")).thenReturn("Java是一门编程语言");

            String result = controller.message("什么是Java");

            assertEquals("Java是一门编程语言", result);
        }
    }

    // ==================== chatOptions ====================

    @Nested
    @DisplayName("GET /api/zhipuai/chatOptions")
    class ChatOptions {

        @Test
        @DisplayName("带 options 正常返回 ChatResponse")
        void shouldReturnChatResponseWithOptions() {
            Generation generation = new Generation(new AssistantMessage("AI回答内容"));
            ChatResponse response = ChatResponse.builder()
                    .generations(List.of(generation))
                    .build();
            when(chatModel.call(any(Prompt.class))).thenReturn(response);

            ChatResponse result = controller.chatOptions("测试问题");

            assertNotNull(result);
            assertEquals("AI回答内容",
                    result.getResult().getOutput().getText());
        }
    }

    // ==================== chatResponse ====================

    @Nested
    @DisplayName("GET /api/zhipuai/chatResponse")
    class ChatResponseEndpoint {

        @Test
        @DisplayName("正常返回文本内容")
        void shouldReturnTextContent() {
            Generation generation = new Generation(new AssistantMessage("提取的文本"));
            ChatResponse response = ChatResponse.builder()
                    .generations(List.of(generation))
                    .build();
            when(chatModel.call(any(Prompt.class))).thenReturn(response);

            String result = controller.chatResponse("问题");

            assertEquals("提取的文本", result);
        }
    }

    // ==================== stream ====================

    @Nested
    @DisplayName("GET /api/zhipuai/stream")
    class Stream {

        @Test
        @DisplayName("正常流式返回")
        void shouldReturnStream() {
            when(chatModel.stream("查询")).thenReturn(Flux.just("A", "B", "C"));

            Flux<String> result = controller.stream("查询");

            List<String> chunks = result.collectList().block();
            assertNotNull(chunks);
            assertEquals(3, chunks.size());
            assertEquals("A", chunks.get(0));
            assertEquals("B", chunks.get(1));
            assertEquals("C", chunks.get(2));
        }

        @Test
        @DisplayName("空流正常完成")
        void shouldReturnEmptyStream() {
            when(chatModel.stream("查询")).thenReturn(Flux.empty());

            Flux<String> result = controller.stream("查询");

            List<String> chunks = result.collectList().block();
            assertNotNull(chunks);
            assertTrue(chunks.isEmpty());
        }
    }

}
