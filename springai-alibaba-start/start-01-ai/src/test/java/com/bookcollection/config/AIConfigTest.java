package com.bookcollection.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AIConfig 单元测试")
class AIConfigTest {

    @Mock
    private ChatClient.Builder builder;

    @Mock
    private ChatClient chatClient;

    @Nested
    @DisplayName("chatClient Bean")
    class ChatClientBean {

        @Test
        @DisplayName("正常构建 ChatClient")
        void shouldBuildChatClient() {
            when(builder.build()).thenReturn(chatClient);

            AIConfig config = new AIConfig();
            ChatClient result = config.chatClient(builder);

            assertNotNull(result);
            assertSame(chatClient, result);
            verify(builder).build();
        }

        @Test
        @DisplayName("builder 为 null 时抛出 NPE")
        void shouldThrowNpeWhenBuilderIsNull() {
            AIConfig config = new AIConfig();

            assertThrows(NullPointerException.class,
                    () -> config.chatClient(null));
        }
    }
}
