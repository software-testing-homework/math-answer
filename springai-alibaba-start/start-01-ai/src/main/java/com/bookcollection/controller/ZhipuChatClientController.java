package com.bookcollection.controller;

import com.bookcollection.advisor.SGCallAdvisor1;
import com.bookcollection.advisor.SGCallAdvisor2;
import com.bookcollection.advisor.SimpleMessageChatMemoryAdvisor;
import com.bookcollection.entity.Book;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.zhipuai.ZhiPuAiChatOptions;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;
import java.util.function.Consumer;

@RestController
@RequestMapping("/chatclient")
public class ZhipuChatClientController {

    private final ChatClient chatClient;
    //构造器注入
    public ZhipuChatClientController(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @GetMapping("/simple")
    public String simple(@RequestParam(name="query") String query) {

        SystemMessage systemMessage =new SystemMessage("你是一个有用的ai助手。");
        UserMessage userMessage = new UserMessage(query);
        ZhiPuAiChatOptions options = new ZhiPuAiChatOptions();
        options.setModel("glm-4.5");
        options.setTemperature(0.0);
        options.setMaxTokens(15536);
        Prompt prompt = new Prompt(List.of(systemMessage, userMessage), options);

        return chatClient.prompt(prompt)
                .call()
                .content();
    }

    @GetMapping("/simple1")
    public String simple1(@RequestParam(name="query") String query) {
        ChatOptions options = ChatOptions.builder()
                .model("glm-4.5")
                .temperature(0.0)
                .maxTokens(15536)
                .build();
        return chatClient.prompt()
                .system("你是一个有用的ai助手。")
                .user(query)
                .options(options)
                .call()
                .content();
    }

    @GetMapping("/chatResponse")
    public ChatResponse chatResponse(@RequestParam(name="query") String query) {
        ChatOptions options = ChatOptions.builder()
                .model("glm-4.5")
                .temperature(0.0)
                .maxTokens(15536)
                .build();
        return chatClient.prompt()
                .system("你是一个有用的ai助手。")
                .user(query)
                .options(options)
                .call()
                .chatResponse();
    }

    @GetMapping("/entity")
    public Book response(){
        Book book = chatClient.prompt()
                    .user("你叫什么名字")
                    .call()
                    .entity(Book.class);
        return book;
    }

    @GetMapping("/advisor")
    public Book advisor(){
        Book book = chatClient.prompt()
                .user("你叫什么名字")
                .advisors(new SGCallAdvisor1(), new SGCallAdvisor2())
                .call()
                .entity(Book.class);
        return book;
    }

    @GetMapping("/stream")
    public Flux<String> stream(){
           Flux<String> stringFlux = chatClient.prompt()
                .user("你叫什么名字")
                .stream()
                .content();
        return stringFlux;
    }

    @GetMapping("/simpleMessageChatMemoryAdvisor")
    public String simpleMessageChatMemoryAdvisor ( @RequestParam(name="query") String query,
                                                   @RequestParam(name="conversationId") String conversationId ){
        return chatClient.prompt()
                .user(query)
                .advisors(new Consumer<ChatClient.AdvisorSpec>() {
                    @Override
                    public void accept(ChatClient.AdvisorSpec advisorSpec) {
                        advisorSpec.param("conversationId", conversationId);
                    }
                })
                .advisors(new SimpleMessageChatMemoryAdvisor())
                .call()
                .content();
    }
}
