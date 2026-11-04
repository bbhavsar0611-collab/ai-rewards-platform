package com.rewards.ai_rewards_platform.dto;
import com.rewards.ai_rewards_platform.model.RewardIntentType;

public record RewardIntent(
        RewardIntentType intent,
        String rewardId
) {
}


