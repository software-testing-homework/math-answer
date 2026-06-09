package com.bookcollection.controller;

import com.bookcollection.entity.Book;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ZhipuChatClientController 单元测试")
class ZhipuChatClientControllerTest {

    @Mock
    private ChatClient.Builder chatClientBuilder;

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    @Mock
    private ChatClient.StreamResponseSpec streamResponseSpec;

    private ZhipuChatClientController controller;

    @BeforeEach
    void setUp() {
        // builder.build() → chatClient
        lenient().when(chatClientBuilder.build()).thenReturn(chatClient);

        // ChatClient fluent chain
        lenient().when(chatClient.prompt(any(Prompt.class))).thenReturn(requestSpec);
        lenient().when(chatClient.prompt()).thenReturn(requestSpec);

        // RequestSpec fluent: system()/user()/options()/advisors() return same spec
        lenient().when(requestSpec.system(anyString())).thenReturn(requestSpec);
        lenient().when(requestSpec.user(anyString())).thenReturn(requestSpec);
        lenient().when(requestSpec.options(any())).thenReturn(requestSpec);
        lenient().when(requestSpec.advisors(any(org.springframework.ai.chat.client.advisor.api.Advisor[].class))).thenReturn(requestSpec);
        lenient().when(requestSpec.advisors(any(java.util.function.Consumer.class))).thenReturn(requestSpec);

        // call() → CallResponseSpec
        lenient().when(requestSpec.call()).thenReturn(callResponseSpec);

        // stream() → StreamResponseSpec
        lenient().when(requestSpec.stream()).thenReturn(streamResponseSpec);

        controller = new ZhipuChatClientController(chatClientBuilder);
    }

    // ==================== simple ====================

    @Nested
    @DisplayName("GET /chatclient/simple")
    class Simple {

        @Test
        @DisplayName("正常调用并返回 AI 内容")
        void shouldCallPromptAndReturnContent() {
            when(callResponseSpec.content()).thenReturn("AI回答：Java是...");

            String result = controller.simple("什么是Java");

            assertEquals("AI回答：Java是...", result);
            verify(chatClient).prompt(any(Prompt.class));
            verify(requestSpec).call();
            verify(callResponseSpec).content();
        }
    }

    // ==================== simple1 ====================

    @Nested
    @DisplayName("GET /chatclient/simple1")
    class Simple1 {

        @Test
        @DisplayName("用 fluent API 调用并返回内容")
        void shouldUseFluentApi() {
            when(callResponseSpec.content()).thenReturn("回答内容");

            String result = controller.simple1("查询");

            assertEquals("回答内容", result);
            verify(requestSpec).system("你是一个有用的ai助手。");
            verify(requestSpec).user("查询");
            verify(requestSpec).options(any());
        }
    }

    // ==================== chatResponse ====================

    @Nested
    @DisplayName("GET /chatclient/chatResponse")
    class ChatResponseEndpoint {

        @Test
        @DisplayName("正常返回 ChatResponse 对象")
        void shouldReturnChatResponse() {
            Generation generation = new Generation(new AssistantMessage("响应内容"));
            ChatResponse response = ChatResponse.builder()
                    .generations(List.of(generation))
                    .build();
            when(callResponseSpec.chatResponse()).thenReturn(response);

            ChatResponse result = controller.chatResponse("问题");

            assertNotNull(result);
            assertEquals("响应内容", result.getResult().getOutput().getText());
        }
    }

    // ==================== response (entity) ====================

    @Nested
    @DisplayName("GET /chatclient/entity")
    class ResponseEntityMapping {

        @Test
        @DisplayName("正常映射到 Book 实体")
        void shouldMapToBookEntity() {
            Book expected = new Book();
            expected.setName("测试书名");
            when(callResponseSpec.entity(Book.class)).thenReturn(expected);

            Book result = controller.response();

            assertNotNull(result);
            assertEquals("测试书名", result.getName());
        }
    }

    // ==================== advisor ====================

    @Nested
    @DisplayName("GET /chatclient/advisor")
    class Advisor {

        @Test
        @DisplayName("带 Advisor 调用并返回 Book")
        void shouldCallWithAdvisors() {
            Book expected = new Book();
            expected.setName("Advisor书名");
            when(callResponseSpec.entity(Book.class)).thenReturn(expected);

            Book result = controller.advisor();

            assertNotNull(result);
            assertEquals("Advisor书名", result.getName());
            // Verify advisors were set
            verify(requestSpec).advisors(any(org.springframework.ai.chat.client.advisor.api.Advisor[].class));
        }
    }

    // ==================== stream ====================

    @Nested
    @DisplayName("GET /chatclient/stream")
    class Stream {

        @Test
        @DisplayName("正常流式返回内容")
        void shouldStreamContent() {
            when(streamResponseSpec.content()).thenReturn(Flux.just("A", "B", "C"));

            Flux<String> result = controller.stream();

            List<String> chunks = result.collectList().block();
            assertNotNull(chunks);
            assertEquals(3, chunks.size());
            assertEquals("A", chunks.get(0));
            assertEquals("B", chunks.get(1));
            assertEquals("C", chunks.get(2));
        }

        @Test
        @DisplayName("空流正常完成")
        void shouldHandleEmptyStream() {
            when(streamResponseSpec.content()).thenReturn(Flux.empty());

            Flux<String> result = controller.stream();

            List<String> chunks = result.collectList().block();
            assertNotNull(chunks);
            assertTrue(chunks.isEmpty());
        }
    }

    // ==================== simpleMessageChatMemoryAdvisor ====================

    @Nested
    @DisplayName("GET /chatclient/simpleMessageChatMemoryAdvisor")
    class SimpleMessageChatMemoryAdvisor {

        @Test
        @DisplayName("带 memory advisor 调用并返回内容")
        void shouldCallWithMemoryAdvisor() {
            when(callResponseSpec.content()).thenReturn("带记忆的回答");

            String result = controller.simpleMessageChatMemoryAdvisor("继续刚才的话题", "conv-123");

            assertEquals("带记忆的回答", result);
            verify(requestSpec).user("继续刚才的话题");
            verify(requestSpec, atLeastOnce()).advisors(any(org.springframework.ai.chat.client.advisor.api.Advisor[].class));
        }
    }

}
