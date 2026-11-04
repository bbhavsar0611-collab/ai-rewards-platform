
package com.rewards.ai_rewards_platform.ai;

import org.springframework.stereotype.Component;

@Component
public class RewardPromptBuilder {

    public String buildPrompt(String customerMessage) {

        return """
                You are an AI assistant for a Rewards Redemption Platform.

                Your task is to identify the customer's intent.

                Supported intents:
                - CHECK_BALANCE
                - REDEEM_REWARD
                - FIND_REWARDS
                - UNKNOWN

                Rules:
                1. Return only valid JSON.
                2. Do not include explanations or Markdown.
                3. Never invent a reward ID.
                4. If the customer wants to redeem a reward but has not
                   specified which reward, return null for rewardId.
                5. If the request does not match a supported intent,
                   return UNKNOWN.
                6. Do not execute any business operation.

                Expected JSON structure:
                {
                  "intent": "REDEEM_REWARD",
                  "rewardId": null
                }

                Customer message:
                %s
                """.formatted(customerMessage);
    }
}
