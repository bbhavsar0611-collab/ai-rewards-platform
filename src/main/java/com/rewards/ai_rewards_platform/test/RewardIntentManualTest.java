package com.rewards.ai_rewards_platform.test;

import com.rewards.ai_rewards_platform.ai.RewardIntentParser;
import com.rewards.ai_rewards_platform.dto.RewardIntent;
import com.rewards.ai_rewards_platform.validation.RewardIntentValidator;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class RewardIntentManualTest implements CommandLineRunner {

    private final RewardIntentParser parser;
    private final RewardIntentValidator validator;

    public RewardIntentManualTest(
            RewardIntentParser parser,
            RewardIntentValidator validator) {
        this.parser = parser;
        this.validator = validator;
    }

    @Override
    public void run(String... args) {

        String json = """
                {
                   "intent": "REDEEM_REWARD",
                   "rewardId": null
                 }
                """;

        try {
            // Step 1: Parse JSON
            RewardIntent rewardIntent = parser.parse(json);

            System.out.println("Parsed successfully: " + rewardIntent);

            // Step 2: Validate the object
            validator.validate(rewardIntent);

            System.out.println("Validation successful!");

        } catch (Exception exception) {
            System.out.println("Test failed: " + exception.getMessage());
        }
    }
}
