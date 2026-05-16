package com.bookcollection.controller;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.zhipuai.ZhiPuAiChatOptions;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequestMapping("/api/zhipuai")
public class ZhipuAIController {
    private final ChatModel chatModel;

    public ZhipuAIController(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @PostMapping("/chat")
    public String chat() {
        return "hello world";
    }

    @GetMapping("/simple")
    public String simple(@RequestParam(name="query") String query) {
        // 简单调用
        return chatModel.call(query);
    }

    @GetMapping("/message")
    public String message(@RequestParam(name="query") String query) {
        SystemMessage systemMessage =new SystemMessage("你是一个有用的ai助手。");
        UserMessage userMessage = new UserMessage(query);
        // 调用
        return chatModel.call(query);
    }
    @GetMapping("/chatOptions")
    public ChatResponse chatOptions(@RequestParam(name="query") String query) {
        SystemMessage systemMessage =new SystemMessage("你是一个有用的ai助手。");
        UserMessage userMessage = new UserMessage(query);
        ZhiPuAiChatOptions options = new ZhiPuAiChatOptions();
        options.setModel("glm-4.5");
        options.setTemperature(0.0);
        options.setMaxTokens(15536);

/*        ZhiPuAiChatOptions options = ZhiPuAiChatOptions.builder()
                .model("glm-4.5")
                .temperature(0.0)
                .maxTokens(15536)
                .build();*/
        // 调用
        return chatModel.call(new Prompt(List.of(systemMessage, userMessage), options));
    }

    @GetMapping("/chatResponse")
    public String chatResponse(@RequestParam(name="query") String query) {
        SystemMessage systemMessage =new SystemMessage("你是一个有用的ai助手。");
        UserMessage userMessage = new UserMessage(query);
        ZhiPuAiChatOptions options = new ZhiPuAiChatOptions();
        options.setModel("glm-4.5");
        options.setTemperature(0.0);
        options.setMaxTokens(15536);
        ChatResponse chatResponse = chatModel.call(new Prompt(List.of(systemMessage, userMessage), options));
        return chatResponse.getResult().getOutput().getText();
    }
    
    // 流式响应
    @GetMapping("/stream")
    public Flux<String> stream(@RequestParam(name="query") String query) {
        return chatModel.stream(query);
    }
}
