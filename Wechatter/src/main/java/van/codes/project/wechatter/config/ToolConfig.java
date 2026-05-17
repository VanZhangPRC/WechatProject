package van.codes.project.wechatter.config;

import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.memory.MessageWindowChatMemory;
import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import van.codes.project.wechatter.tool.ReminderTools;

@Configuration
public class ToolConfig {

    @Bean
    public ChatMemory chatMemory() {
        return MessageWindowChatMemory.builder()
                .maxMessages(10)
                .build();
    }

    @Bean
    public ToolCallbackProvider reminderToolCallbackProvider(ReminderTools reminderTools) {
        return MethodToolCallbackProvider.builder()
                .toolObjects(reminderTools)
                .build();
    }
}
