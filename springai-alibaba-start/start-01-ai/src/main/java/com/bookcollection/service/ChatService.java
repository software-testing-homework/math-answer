package com.bookcollection.service;

import cn.hutool.core.util.StrUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.bookcollection.dto.*;
import com.bookcollection.entity.ChatConversation;
import com.bookcollection.entity.ChatMessage;
import com.bookcollection.mapper.ChatConversationMapper;
import com.bookcollection.mapper.ChatMessageMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.reactive.TransactionSynchronization;
import org.springframework.transaction.reactive.TransactionSynchronizationManager;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final ChatClient chatClient;
    private final ChatConversationMapper conversationMapper;
    private final ChatMessageMapper messageMapper;

    private static final Map<Long, List<Message>> userChatMemory = new ConcurrentHashMap<>();

    @Transactional
    public ConversationResponse createConversation(Long userId, ConversationCreateRequest request) {
        ChatConversation conversation = new ChatConversation();
        conversation.setUserId(userId);
        conversation.setTitle(StrUtil.isNotBlank(request.getTitle()) ? request.getTitle() : "新对话");
        conversation.setMessageCount(0);
        conversation.setTokenCount(0);
        conversation.setIsStar(0);
        conversation.setStatus(1);
        conversationMapper.insert(conversation);

        ConversationResponse response = new ConversationResponse();
        response.setId(conversation.getId());
        response.setUserId(userId);
        response.setTitle(conversation.getTitle());
        response.setMessageCount(conversation.getMessageCount());
        response.setCreateTime(conversation.getCreateTime());
        response.setLastMessageTime(conversation.getLastMessageTime());
        response.setIsStar(conversation.getIsStar());
        return response;
    }

    public List<ConversationListResponse> getConversationList(Long userId) {
        LambdaQueryWrapper<ChatConversation> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatConversation::getUserId, userId)
                .eq(ChatConversation::getStatus, 1)
                .orderByDesc(ChatConversation::getLastMessageTime);
        List<ChatConversation> conversations = conversationMapper.selectList(wrapper);

        return conversations.stream().map(c -> {
            ConversationListResponse resp = new ConversationListResponse();
            resp.setId(c.getId());
            resp.setTitle(c.getTitle());
            resp.setDescription(c.getDescription());
            resp.setMessageCount(c.getMessageCount());
            resp.setIsStar(c.getIsStar());

            ChatMessage lastMsg = messageMapper.selectOne(
                    new LambdaQueryWrapper<ChatMessage>()
                            .eq(ChatMessage::getConversationId, c.getId())
                            .orderByDesc(ChatMessage::getCreateTime)
                            .last("LIMIT 1")
            );
            resp.setLastMessage(lastMsg != null ? truncateContent(lastMsg.getContent(), 50) : "");
            return resp;
        }).collect(Collectors.toList());
    }

    public ConversationResponse getConversation(Long conversationId, Long userId) {
        ChatConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            return null;
        }

        LambdaQueryWrapper<ChatMessage> msgWrapper = new LambdaQueryWrapper<>();
        msgWrapper.eq(ChatMessage::getConversationId, conversationId)
                .eq(ChatMessage::getStatus, 1)
                .orderByAsc(ChatMessage::getCreateTime);
        List<ChatMessage> messages = messageMapper.selectList(msgWrapper);

        List<MessageResponse> messageResponses = messages.stream().map(m -> {
            MessageResponse resp = new MessageResponse();
            resp.setId(m.getId());
            resp.setRole(m.getRole());
            resp.setContent(m.getContent());
            resp.setContentType(m.getContentType());
            resp.setCreateTime(m.getCreateTime());
            return resp;
        }).collect(Collectors.toList());

        ConversationResponse response = new ConversationResponse();
        response.setId(conversation.getId());
        response.setUserId(conversation.getUserId());
        response.setTitle(conversation.getTitle());
        response.setDescription(conversation.getDescription());
        response.setMessageCount(conversation.getMessageCount());
        response.setCreateTime(conversation.getCreateTime());
        response.setLastMessageTime(conversation.getLastMessageTime());
        response.setIsStar(conversation.getIsStar());
        response.setMessages(messageResponses);
        return response;
    }

    @Transactional
    public void deleteConversation(Long conversationId, Long userId) {
        ChatConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation != null && conversation.getUserId().equals(userId)) {
            conversationMapper.deleteById(conversationId);

            LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(ChatMessage::getConversationId, conversationId);
            messageMapper.delete(wrapper);

            userChatMemory.remove(conversationId);
        }
    }

    @Transactional
    public ConversationResponse toggleStar(Long conversationId, Long userId) {
        ChatConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation != null && conversation.getUserId().equals(userId)) {
            conversation.setIsStar(conversation.getIsStar() == 1 ? 0 : 1);
            conversationMapper.updateById(conversation);

            ConversationResponse response = new ConversationResponse();
            response.setId(conversation.getId());
            response.setIsStar(conversation.getIsStar());
            return response;
        }
        return null;
    }

    @Transactional
    public Flux<String> chatStream(ChatRequest request, Long userId) {
        String userMessage = request.getMessage();

        Long conversationId = request.getConversationId();
        if (conversationId == null || conversationId == 0) {
            ConversationCreateRequest createRequest = new ConversationCreateRequest();
            createRequest.setTitle(truncateContent(userMessage, 20));
            ConversationResponse response = createConversation(userId, createRequest);
            conversationId = response.getId();
        }

        final Long finalConversationId = conversationId;
        ChatConversation conversation = conversationMapper.selectById(finalConversationId);
        if (conversation == null || !conversation.getUserId().equals(userId)) {
            return Flux.error(new RuntimeException("对话不存在或无权限"));
        }

        saveUserMessage(finalConversationId, userMessage);

        List<Message> historyMessages = getConversationHistory(finalConversationId);

        SystemMessage systemMessage = new SystemMessage("我是数学问答助手，能简洁明了地回答用户的数学问题。");

        ChatOptions options = ChatOptions.builder()
                .model("glm-4.7")
                .temperature(0.0)
                .maxTokens(2048)
                .build();

        List<Message> allMessages = new ArrayList<>();
        allMessages.add(systemMessage);
        allMessages.addAll(historyMessages);

        Prompt prompt = new Prompt(allMessages, options);

        StringBuilder fullContent = new StringBuilder();
        java.util.concurrent.atomic.AtomicInteger counter = new java.util.concurrent.atomic.AtomicInteger(0);

        Flux<String> streamFlux = chatClient.prompt(prompt)
                .stream()
                .content()
                .map(chunk -> {
                    fullContent.append(chunk);
                    int id = counter.getAndIncrement();
                    return "data: " + id + ":" + chunk + "\n\n";
                });

        return Flux.concat(
                streamFlux,
                Mono.fromRunnable(() -> {
                    log.info("流式响应完成，保存助手消息，conversationId={}, contentLength={}",
                            finalConversationId, fullContent.length());
                    if (fullContent.length() > 0) {
                        saveAssistantMessage(finalConversationId, fullContent.toString());
                        log.info("助手消息保存成功，conversationId={}", finalConversationId);
                    }
                    updateConversationStats(finalConversationId);
                    log.info("对话统计更新完成，conversationId={}", finalConversationId);
                })
        ).doOnError(error -> {
            log.error("AI响应错误", error);
        });
    }

    private List<Message> getConversationHistory(Long conversationId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getConversationId, conversationId)
                .eq(ChatMessage::getStatus, 1)
                .orderByAsc(ChatMessage::getCreateTime);
        List<ChatMessage> messages = messageMapper.selectList(wrapper);

        return messages.stream()
                .filter(m -> !"system".equals(m.getRole()))
                .map(m -> {
                    if ("user".equals(m.getRole())) {
                        return new UserMessage(m.getContent());
                    } else {
                        return new AssistantMessage(m.getContent());
                    }
                })
                .collect(Collectors.toList());
    }

    private void saveUserMessage(Long conversationId, String content) {
        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setRole("user");
        message.setContent(content);
        message.setContentType("text");
        message.setStatus(1);
        messageMapper.insert(message);
    }

    private void saveAssistantMessage(Long conversationId, String content) {
        ChatMessage message = new ChatMessage();
        message.setConversationId(conversationId);
        message.setRole("assistant");
        message.setContent(content);
        message.setContentType("text");
        message.setStatus(1);
        messageMapper.insert(message);
    }

    private void updateConversationStats(Long conversationId) {
        LambdaQueryWrapper<ChatMessage> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ChatMessage::getConversationId, conversationId)
                .eq(ChatMessage::getStatus, 1);
        long count = messageMapper.selectCount(wrapper);

        ChatConversation conversation = conversationMapper.selectById(conversationId);
        if (conversation != null) {
            conversation.setMessageCount((int) count);
            conversation.setLastMessageTime(java.time.LocalDateTime.now());
            conversationMapper.updateById(conversation);
        }
    }

    private String truncateContent(String content, int maxLength) {
        if (content == null) return "";
        return content.length() > 0 ? content.substring(0, Math.min(content.length(), maxLength)) + "..." : content;
    }
}
