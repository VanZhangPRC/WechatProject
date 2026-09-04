package van.project.wechatter.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import van.project.wechatter.aitool.CommonTools;
import van.project.wechatter.aitool.ManageFinancesTools;
import van.project.wechatter.aitool.ReminderTools;
import van.project.wechatter.aitool.SearchTool;

@Configuration
public class AIConfig {

    @Bean
    public ChatClient chatClient(ChatModel chatModel, ToolCallbackProvider toolCallbackProvider) {
        return ChatClient.builder(chatModel)
                .defaultAdvisors(MessageChatMemoryAdvisor.builder(MessageWindowChatMemory.builder().build()).build())
                .defaultToolCallbacks(toolCallbackProvider)
                .defaultSystem("""
                        你是一名优秀的个人助理，协助用户完成一些工作，如回答用户的问题，提醒用户待办事项等。
                        面对用户的需求，根据工具描述判断能否满足用户要求，如果无法满足礼貌回复无法完成要求。
                        """)
                .build();
    }

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
    }

    @Bean
    public ToolCallbackProvider toolCallbackProvider(ReminderTools reminderTools, CommonTools commonTools,
                                                     ManageFinancesTools manageFinancesTools, SearchTool searchTool) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(reminderTools, commonTools, manageFinancesTools, searchTool)
                .build();
    }
}
