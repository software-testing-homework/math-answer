package com.bookcollection.controller;

import com.bookcollection.dto.*;
import com.bookcollection.mapper.ChatConversationMapper;
import com.bookcollection.mapper.ChatMessageMapper;
import com.bookcollection.service.ChatService;
import com.bookcollection.utils.JwtUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import reactor.core.publisher.Flux;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("ChatController 单元测试")
class ChatControllerTest {

    // ==================== Stub Service ====================

    private static final class StubChatService extends ChatService {
        private ConversationResponse createConversationResult;
        private RuntimeException createConversationException;

        private List<ConversationListResponse> getConversationListResult;
        private RuntimeException getConversationListException;

        private ConversationResponse getConversationResult;
        private RuntimeException getConversationException;

        private RuntimeException deleteConversationException;

        private ConversationResponse toggleStarResult;
        private RuntimeException toggleStarException;

        private Flux<String> chatStreamResult;
        private RuntimeException chatStreamException;

        // To verify userId from token
        Long lastCreateConversationUserId;
        Long lastGetConversationListUserId;
        Long lastGetConversationUserId;
        Long lastDeleteConversationUserId;
        Long lastToggleStarUserId;

        private StubChatService() {
            super(null, (ChatConversationMapper) null, (ChatMessageMapper) null);
        }

        @Override
        public ConversationResponse createConversation(Long userId, ConversationCreateRequest request) {
            lastCreateConversationUserId = userId;
            if (createConversationException != null) throw createConversationException;
            return createConversationResult;
        }

        @Override
        public List<ConversationListResponse> getConversationList(Long userId) {
            lastGetConversationListUserId = userId;
            if (getConversationListException != null) throw getConversationListException;
            return getConversationListResult;
        }

        @Override
        public ConversationResponse getConversation(Long conversationId, Long userId) {
            lastGetConversationUserId = userId;
            if (getConversationException != null) throw getConversationException;
            return getConversationResult;
        }

        @Override
        public void deleteConversation(Long conversationId, Long userId) {
            lastDeleteConversationUserId = userId;
            if (deleteConversationException != null) throw deleteConversationException;
        }

        @Override
        public ConversationResponse toggleStar(Long conversationId, Long userId) {
            lastToggleStarUserId = userId;
            if (toggleStarException != null) throw toggleStarException;
            return toggleStarResult;
        }

        @Override
        public Flux<String> chatStream(ChatRequest request, Long userId) {
            if (chatStreamException != null) throw chatStreamException;
            return chatStreamResult != null ? chatStreamResult : Flux.empty();
        }
    }

    // ==================== Helpers ====================

    private ConversationResponse buildConversationResponse(Long id, String title) {
        ConversationResponse r = new ConversationResponse();
        r.setId(id);
        r.setTitle(title);
        return r;
    }

    private ConversationListResponse buildListResponse(Long id, String title) {
        ConversationListResponse r = new ConversationListResponse();
        r.setId(id);
        r.setTitle(title);
        return r;
    }

    // ==================== createConversation ====================

    @Nested
    @DisplayName("POST /api/chat/conversations")
    class CreateConversation {

        @Test
        @DisplayName("正常创建 → 200")
        void shouldReturn200() {
            StubChatService service = new StubChatService();
            ChatController controller = new ChatController(service);

            ConversationResponse cr = buildConversationResponse(1L, "数学问题");
            service.createConversationResult = cr;

            ConversationCreateRequest request = new ConversationCreateRequest();
            request.setTitle("数学问题");

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<ConversationResponse>> resp =
                    controller.createConversation(request, token);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertEquals(200, resp.getBody().getCode());
            assertEquals("数学问题", resp.getBody().getData().getTitle());
            assertEquals(100L, service.lastCreateConversationUserId);
        }

        @Test
        @DisplayName("服务异常 → 500")
        void shouldReturn500OnException() {
            StubChatService service = new StubChatService();
            ChatController controller = new ChatController(service);
            service.createConversationException = new RuntimeException("DB error");

            ConversationCreateRequest request = new ConversationCreateRequest();

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<ConversationResponse>> resp =
                    controller.createConversation(request, token);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
            assertTrue(resp.getBody().getMessage().contains("创建对话失败"));
        }
    }

    // ==================== getConversationList ====================

    @Nested
    @DisplayName("GET /api/chat/conversations")
    class GetConversationList {

        @Test
        @DisplayName("正常返回对话列表 → 200")
        void shouldReturn200() {
            StubChatService service = new StubChatService();
            ChatController controller = new ChatController(service);

            service.getConversationListResult = List.of(buildListResponse(1L, "对话1"));

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<List<ConversationListResponse>>> resp =
                    controller.getConversationList(token);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertEquals(200, resp.getBody().getCode());
            assertEquals(1, resp.getBody().getData().size());
            assertEquals(100L, service.lastGetConversationListUserId);
        }

        @Test
        @DisplayName("空列表 → 200")
        void shouldReturnEmptyList() {
            StubChatService service = new StubChatService();
            ChatController controller = new ChatController(service);

            service.getConversationListResult = List.of();

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<List<ConversationListResponse>>> resp =
                    controller.getConversationList(token);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertTrue(resp.getBody().getData().isEmpty());
        }

        @Test
        @DisplayName("服务异常 → 500")
        void shouldReturn500OnException() {
            StubChatService service = new StubChatService();
            ChatController controller = new ChatController(service);
            service.getConversationListException = new RuntimeException("DB error");

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<List<ConversationListResponse>>> resp =
                    controller.getConversationList(token);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        }
    }

    // ==================== getConversation ====================

    @Nested
    @DisplayName("GET /api/chat/conversations/{conversationId}")
    class GetConversation {

        @Test
        @DisplayName("正常返回对话详情 → 200")
        void shouldReturn200() {
            StubChatService service = new StubChatService();
            ChatController controller = new ChatController(service);

            ConversationResponse cr = buildConversationResponse(1L, "数学问题");
            service.getConversationResult = cr;

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<ConversationResponse>> resp =
                    controller.getConversation(1L, token);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertEquals(200, resp.getBody().getCode());
            assertEquals(100L, service.lastGetConversationUserId);
        }

        @Test
        @DisplayName("对话不存在 → 404")
        void shouldReturn404WhenNotFound() {
            StubChatService service = new StubChatService();
            ChatController controller = new ChatController(service);
            service.getConversationResult = null; // service returns null

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<ConversationResponse>> resp =
                    controller.getConversation(999L, token);

            assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
            assertEquals("对话不存在", resp.getBody().getMessage());
        }

        @Test
        @DisplayName("服务异常 → 500")
        void shouldReturn500OnException() {
            StubChatService service = new StubChatService();
            ChatController controller = new ChatController(service);
            service.getConversationException = new RuntimeException("DB error");

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<ConversationResponse>> resp =
                    controller.getConversation(1L, token);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        }
    }

    // ==================== deleteConversation ====================

    @Nested
    @DisplayName("DELETE /api/chat/conversations/{conversationId}")
    class DeleteConversation {

        @Test
        @DisplayName("正常删除 → 200")
        void shouldReturn200() {
            StubChatService service = new StubChatService();
            ChatController controller = new ChatController(service);

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<Void>> resp =
                    controller.deleteConversation(1L, token);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertEquals(200, resp.getBody().getCode());
            assertEquals(100L, service.lastDeleteConversationUserId);
        }

        @Test
        @DisplayName("服务异常 → 500")
        void shouldReturn500OnException() {
            StubChatService service = new StubChatService();
            ChatController controller = new ChatController(service);
            service.deleteConversationException = new RuntimeException("DB error");

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<Void>> resp =
                    controller.deleteConversation(1L, token);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        }
    }

    // ==================== toggleStar ====================

    @Nested
    @DisplayName("POST /api/chat/conversations/{conversationId}/star")
    class ToggleStar {

        @Test
        @DisplayName("正常切换星标 → 200")
        void shouldReturn200() {
            StubChatService service = new StubChatService();
            ChatController controller = new ChatController(service);

            ConversationResponse cr = buildConversationResponse(1L, "对话");
            cr.setIsStar(1);
            service.toggleStarResult = cr;

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<ConversationResponse>> resp =
                    controller.toggleStar(1L, token);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertEquals(200, resp.getBody().getCode());
            assertEquals(1, resp.getBody().getData().getIsStar());
            assertEquals(100L, service.lastToggleStarUserId);
        }

        @Test
        @DisplayName("对话不存在 → 404")
        void shouldReturn404WhenNotFound() {
            StubChatService service = new StubChatService();
            ChatController controller = new ChatController(service);
            service.toggleStarResult = null;

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<ConversationResponse>> resp =
                    controller.toggleStar(999L, token);

            assertEquals(HttpStatus.NOT_FOUND, resp.getStatusCode());
            assertEquals("对话不存在", resp.getBody().getMessage());
        }

        @Test
        @DisplayName("服务异常 → 500")
        void shouldReturn500OnException() {
            StubChatService service = new StubChatService();
            ChatController controller = new ChatController(service);
            service.toggleStarException = new RuntimeException("DB error");

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<ConversationResponse>> resp =
                    controller.toggleStar(1L, token);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        }
    }

    // ==================== chatStream ====================

    @Nested
    @DisplayName("POST /api/chat/stream")
    class ChatStream {

        @Test
        @DisplayName("正常流式返回 → Flux<String>")
        void shouldReturnFlux() {
            StubChatService service = new StubChatService();
            ChatController controller = new ChatController(service);

            service.chatStreamResult = Flux.just("data: 0:Hello", "data: 1:World");

            ChatRequest request = new ChatRequest();
            request.setConversationId(1L);
            request.setMessage("hi");

            String token = JwtUtils.generateToken(100L, "user");
            Flux<String> result = controller.chatStream(request, token);

            assertNotNull(result);
            List<String> chunks = result.collectList().block();
            assertNotNull(chunks);
            assertEquals(2, chunks.size());
            assertEquals("data: 0:Hello", chunks.get(0));
            assertEquals("data: 1:World", chunks.get(1));
        }

        @Test
        @DisplayName("空流 → Flux 正常完成")
        void shouldReturnEmptyFlux() {
            StubChatService service = new StubChatService();
            ChatController controller = new ChatController(service);

            service.chatStreamResult = Flux.empty();

            ChatRequest request = new ChatRequest();
            request.setConversationId(1L);
            request.setMessage("hi");

            String token = JwtUtils.generateToken(100L, "user");
            Flux<String> result = controller.chatStream(request, token);

            List<String> chunks = result.collectList().block();
            assertNotNull(chunks);
            assertTrue(chunks.isEmpty());
        }

        @Test
        @DisplayName("token 无效时返回 Flux.error")
        void shouldReturnErrorFluxOnException() {
            StubChatService service = new StubChatService();
            ChatController controller = new ChatController(service);

            ChatRequest request = new ChatRequest();
            request.setConversationId(1L);
            request.setMessage("hi");

            // Invalid token causes parseToken to throw → Flux.error
            Flux<String> result = controller.chatStream(request, "invalid-jwt");

            assertNotNull(result);
            // collect the error
            List<String> chunks = result.onErrorResume(e -> Flux.just("ERROR"))
                    .collectList().block();
            assertNotNull(chunks);
            assertEquals(1, chunks.size());
            assertEquals("ERROR", chunks.get(0));
        }
    }

    // ==================== sendMessage ====================

    @Nested
    @DisplayName("POST /api/chat/message")
    class SendMessage {

        @Test
        @DisplayName("正常发送消息 → 200")
        void shouldReturn200() {
            StubChatService service = new StubChatService();
            ChatController controller = new ChatController(service);

            ConversationResponse cr = buildConversationResponse(1L, "对话");
            service.getConversationResult = cr;

            ChatRequest request = new ChatRequest();
            request.setConversationId(1L);
            request.setMessage("hello");

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<ConversationResponse>> resp =
                    controller.sendMessage(request, token);

            assertEquals(HttpStatus.OK, resp.getStatusCode());
            assertEquals(200, resp.getBody().getCode());
        }

        @Test
        @DisplayName("服务异常 → 500")
        void shouldReturn500OnException() {
            StubChatService service = new StubChatService();
            ChatController controller = new ChatController(service);
            service.getConversationException = new RuntimeException("DB error");

            ChatRequest request = new ChatRequest();
            request.setConversationId(1L);

            String token = JwtUtils.generateToken(100L, "user");
            ResponseEntity<Result<ConversationResponse>> resp =
                    controller.sendMessage(request, token);

            assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, resp.getStatusCode());
        }
    }
}
