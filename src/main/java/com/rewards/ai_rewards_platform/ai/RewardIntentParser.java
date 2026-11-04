
package com.rewards.ai_rewards_platform.ai;

import com.rewards.ai_rewards_platform.dto.RewardIntent;
import org.springframework.stereotype.Component;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

@Component
public class RewardIntentParser {

    private final ObjectMapper objectMapper;

    public RewardIntentParser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public RewardIntent parse(String jsonResponse) {

        try {
            return objectMapper.readValue(
                    jsonResponse,
                    RewardIntent.class
            );

        } catch (JacksonException exception) {

            throw new IllegalArgumentException(
                    "Unable to parse LLM response into RewardIntent",
                    exception
            );
        }
    }
}
