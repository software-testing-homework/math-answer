package com.bookcollection.advisor;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.AdvisorChain;
import org.springframework.ai.chat.client.advisor.api.BaseAdvisor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.*;

@Slf4j
public class SimpleMessageChatMemoryAdvisor implements BaseAdvisor {

    private static Map<String, List<Message>> chatMemory = new HashMap<>();
    @Override
    public ChatClientRequest before(ChatClientRequest chatClientRequest, AdvisorChain advisorChain) {
        //
        String conversationId = chatClientRequest.context().get("conversationId").toString();
        List<Message> messages = chatMemory.get(conversationId);
        if (messages == null) {
            messages = new ArrayList<>();
        }
        //
         List<Message> requestMessageList = chatClientRequest.prompt()
                .getInstructions();
         messages.addAll( requestMessageList);
         chatMemory.put(conversationId, messages);
         //
         Prompt oldprompt = chatClientRequest.prompt();
         Prompt newprompt = oldprompt.mutate()
                 .messages(messages)
                 .build();
         ChatClientRequest request = chatClientRequest.mutate()
                 .prompt( newprompt)
                 .build();
        return request;
    }

    @Override
    public ChatClientResponse after(ChatClientResponse chatClientResponse, AdvisorChain advisorChain) {
        //
        String conversationId = chatClientResponse.context().get("conversationId").toString();
        List<Message> hisMessages = chatMemory.get(conversationId);
        if(hisMessages == null){
            hisMessages = new ArrayList<>();
        }
        //
        if(Objects.isNull(chatClientResponse)){
            return chatClientResponse;
        }
        AssistantMessage assistantMessage = chatClientResponse.chatResponse()
                .getResult().getOutput();
        hisMessages.add(assistantMessage);
        chatMemory.put(conversationId, hisMessages);
        return chatClientResponse;
    }

    @Override
    public int getOrder() {
        return 0;
    }
}
