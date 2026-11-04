
package com.rewards.ai_rewards_platform.service;

import com.rewards.ai_rewards_platform.ai.RewardIntentParser;
import com.rewards.ai_rewards_platform.ai.RewardPromptBuilder;
import com.rewards.ai_rewards_platform.dto.RewardIntent;
import com.rewards.ai_rewards_platform.validation.RewardIntentValidator;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class RewardIntentService {

    private final ChatClient chatClient;
    private final RewardPromptBuilder promptBuilder;
    private final RewardIntentParser parser;
    private final RewardIntentValidator validator;

    public RewardIntentService(
            ChatClient.Builder builder,
            RewardPromptBuilder promptBuilder,
            RewardIntentParser parser,
            RewardIntentValidator validator) {

        this.chatClient = builder.build();
        this.promptBuilder = promptBuilder;
        this.parser = parser;
        this.validator = validator;
    }

    public RewardIntent processMessage(String customerMessage) {

        // 1. Build prompt
        String prompt = promptBuilder.buildPrompt(customerMessage);

        // 2. Send prompt to actual LLM
        String llmResponse = chatClient.prompt()
                .user(prompt)
                .call()
                .content();

        System.out.println("Actual LLM response: " + llmResponse);

        // 3. Convert JSON into Java object
        RewardIntent rewardIntent = parser.parse(llmResponse);

        // 4. Validate intent
        validator.validate(rewardIntent);

        return rewardIntent;
    }
}
