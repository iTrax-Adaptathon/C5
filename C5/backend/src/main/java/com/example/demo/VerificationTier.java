
package com.example.demo;
import java.util.Map;
public class VerificationTier {
    public static final Map<Integer, Double> MULTIPLIERS = Map.of(
        0, 0.3,  // self-reported
        1, 0.5,  // evidence attached, unverified
        2, 1.0,  // peer-verified
        3, 1.1,  // sensor/API-verified
        4, 1.2   // moderator-verified
    );
    public static double getMultiplier(int tier) {
        return MULTIPLIERS.getOrDefault(tier, 0.3);
    }
}
