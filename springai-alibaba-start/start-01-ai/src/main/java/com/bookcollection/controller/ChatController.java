package com.bookcollection.controller;

import com.bookcollection.dto.*;
import com.bookcollection.service.ChatService;
import com.bookcollection.utils.JwtUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/conversations")
    public ResponseEntity<Result<ConversationResponse>> createConversation(
            @RequestBody ConversationCreateRequest request,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = JwtUtils.getUserId(token);
            ConversationResponse response = chatService.createConversation(userId, request);
            return ResponseEntity.ok(Result.success(response));
        } catch (Exception e) {
            log.error("创建对话失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Result.error("创建对话失败: " + e.getMessage()));
        }
    }

    @GetMapping("/conversations")
    public ResponseEntity<Result<List<ConversationListResponse>>> getConversationList(
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = JwtUtils.getUserId(token);
            List<ConversationListResponse> list = chatService.getConversationList(userId);
            return ResponseEntity.ok(Result.success(list));
        } catch (Exception e) {
            log.error("获取对话列表失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Result.error("获取对话列表失败: " + e.getMessage()));
        }
    }

    @GetMapping("/conversations/{conversationId}")
    public ResponseEntity<Result<ConversationResponse>> getConversation(
            @PathVariable Long conversationId,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = JwtUtils.getUserId(token);
            ConversationResponse response = chatService.getConversation(conversationId, userId);
            if (response == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Result.error("对话不存在"));
            }
            return ResponseEntity.ok(Result.success(response));
        } catch (Exception e) {
            log.error("获取对话详情失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Result.error("获取对话详情失败: " + e.getMessage()));
        }
    }

    @DeleteMapping("/conversations/{conversationId}")
    public ResponseEntity<Result<Void>> deleteConversation(
            @PathVariable Long conversationId,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = JwtUtils.getUserId(token);
            chatService.deleteConversation(conversationId, userId);
            return ResponseEntity.ok(Result.success(null));
        } catch (Exception e) {
            log.error("删除对话失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Result.error("删除对话失败: " + e.getMessage()));
        }
    }

    @PostMapping("/conversations/{conversationId}/star")
    public ResponseEntity<Result<ConversationResponse>> toggleStar(
            @PathVariable Long conversationId,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = JwtUtils.getUserId(token);
            ConversationResponse response = chatService.toggleStar(conversationId, userId);
            if (response == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(Result.error("对话不存在"));
            }
            return ResponseEntity.ok(Result.success(response));
        } catch (Exception e) {
            log.error("切换星标失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Result.error("操作失败: " + e.getMessage()));
        }
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(
            @RequestBody ChatRequest request,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = JwtUtils.getUserId(token);
            return chatService.chatStream(request, userId)
                    .doOnError(e -> log.error("流式响应错误", e));
        } catch (Exception e) {
            log.error("处理聊天请求失败", e);
            return Flux.error(e);
        }
    }

    @PostMapping("/message")
    public ResponseEntity<Result<ConversationResponse>> sendMessage(
            @RequestBody ChatRequest request,
            @RequestHeader("Authorization") String token) {
        try {
            Long userId = JwtUtils.getUserId(token);
            ConversationResponse response = chatService.getConversation(request.getConversationId(), userId);
            return ResponseEntity.ok(Result.success(response));
        } catch (Exception e) {
            log.error("发送消息失败", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Result.error("发送消息失败: " + e.getMessage()));
        }
    }
}
