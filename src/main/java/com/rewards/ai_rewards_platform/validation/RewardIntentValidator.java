package com.rewards.ai_rewards_platform.validation;

import com.rewards.ai_rewards_platform.dto.RewardIntent;
import com.rewards.ai_rewards_platform.model.RewardIntentType;
import org.springframework.stereotype.Component;

@Component
public class RewardIntentValidator {

    public void validate(RewardIntent rewardIntent) {

        if (rewardIntent == null) {
            throw new IllegalArgumentException(
                    "Reward intent cannot be null"
            );
        }

        if (rewardIntent.intent() == null) {
            throw new IllegalArgumentException(
                    "Intent is required"
            );
        }

        switch (rewardIntent.intent()) {

            case REDEEM_REWARD -> {
                if (rewardIntent.rewardId() == null
                        || rewardIntent.rewardId().isBlank()) {

                    throw new IllegalArgumentException(
                            "Reward ID is required for redemption"
                    );
                }
            }

            case CHECK_BALANCE, FIND_REWARDS, UNKNOWN -> {
                // No rewardId required for these intents.
            }
        }
    }
}

