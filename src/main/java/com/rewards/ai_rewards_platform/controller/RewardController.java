package com.rewards.ai_rewards_platform.controller;


import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rewards")
public class RewardController {

    @GetMapping
    public List<String> getRewards() {
        return List.of(
                "Amazon $100 Gift Card",
                "Apple $50 Gift Card",
                "Delta $200 Travel Credit"
        );
    }
}