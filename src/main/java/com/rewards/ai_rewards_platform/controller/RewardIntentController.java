
package com.rewards.ai_rewards_platform.reward.controller;

import com.rewards.ai_rewards_platform.dto.RewardIntent;
import com.rewards.ai_rewards_platform.service.RewardIntentService;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/rewards/intent")
public class RewardIntentController {

    private final RewardIntentService rewardIntentService;

    public RewardIntentController(
            RewardIntentService rewardIntentService) {
        this.rewardIntentService = rewardIntentService;
    }

    @PostMapping("/interpret")
    public RewardIntent interpret(
            @RequestBody String customerMessage) {

        return rewardIntentService.processMessage(customerMessage);
    }
}
