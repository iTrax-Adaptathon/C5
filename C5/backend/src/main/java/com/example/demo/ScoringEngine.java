package com.example.demo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ScoringEngine {
    private static final int K = 32;

    public record EloResult(int ratingA, int ratingB) {}

    // 1. Elo pairwise rating for subjective competitions (replaces raw likes)
    public static EloResult updateElo(int ratingA, int ratingB, boolean aWon) {
        double expectedA = 1.0 / (1.0 + Math.pow(10.0, (ratingB - ratingA) / 400.0));
        double expectedB = 1.0 / (1.0 + Math.pow(10.0, (ratingA - ratingB) / 400.0));

        double actualA = aWon ? 1.0 : 0.0;
        double actualB = aWon ? 0.0 : 1.0;

        int newRatingA = (int) Math.round(ratingA + K * (actualA - expectedA));
        int newRatingB = (int) Math.round(ratingB + K * (actualB - expectedB));

        return new EloResult(newRatingA, newRatingB);
    }

    // 2. Percentile rank normalization (0-100 scale within cohort)
    public static double percentileRank(double value, List<Double> allValues, boolean higherIsBetter) {
        if (allValues == null || allValues.size() <= 1) return 100.0;

        long countBeaten = higherIsBetter
            ? allValues.stream().filter(v -> v < value).count()
            : allValues.stream().filter(v -> v > value).count();
        return Math.round(((double) countBeaten / (allValues.size() - 1)) * 100.0);
    }

    // 3. Reviewer weight calibration against gold standards
    public static double updateCalibration(double currentCalibration, boolean agreedWithGoldStandard) {
        double step = 0.05;
        double updated = agreedWithGoldStandard ? currentCalibration + step : currentCalibration - step;
        return Math.max(0.1, Math.min(1.0, Math.round(updated * 100.0) / 100.0));
    }

    // 4. Anti-farming profile roll-up score with log diminishing returns
    public static double computeProfileScore(List<ChallengeResult> results) {
        if (results == null || results.isEmpty()) return 0.0;

        double weightedSum = 0.0;
        for (ChallengeResult res : results) {
            weightedSum += res.getPercentileScore() * res.getDifficultyScore() * res.getVerificationMultiplier();
        }

        double averageScore = weightedSum / results.size();
        double volumeMultiplier = Math.log(1.0 + results.size());

        return Math.round((averageScore * volumeMultiplier) * 100.0) / 100.0;
    }
}
