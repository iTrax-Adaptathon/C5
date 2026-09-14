
package com.example.demo;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
public class ScoringEngine {
    private static final int K = 32;
    public record EloResult(int ratingA, int ratingB) {}
    // 1. Elo pairwise rating for subjective competitions (Section 2.3)
    public static EloResult updateElo(int ratingA, int ratingB, boolean aWon) {
        double expectedA = 1.0 / (1.0 + Math.pow(10.0, (ratingB - ratingA) / 400.0));
        double expectedB = 1.0 / (1.0 + Math.pow(10.0, (ratingA - ratingB) / 400.0));
        double actualA = aWon ? 1.0 : 0.0;
        double actualB = aWon ? 0.0 : 1.0;
        int newRatingA = (int) Math.round(ratingA + K * (actualA - expectedA));
        int newRatingB = (int) Math.round(ratingB + K * (actualB - expectedB));
        return new EloResult(newRatingA, newRatingB);
    }
    // 2. Percentile rank within challenge cohort (Section 2.2)
    public static double percentileRank(double value, List<Double> allValues, boolean higherIsBetter) {
        if (allValues == null || allValues.size() <= 1) return 100.0;
        List<Double> sorted = new ArrayList<>(allValues);
        if (higherIsBetter) {
            Collections.sort(sorted);
        } else {
            sorted.sort(Collections.reverseOrder());
        }
        long countBelow = sorted.stream().filter(v -> v < value).count();
        return Math.round(((double) countBelow / (sorted.size() - 1)) * 100.0);
    }
    // 3. Reviewer weight calibration against gold standards (Section 2.4)
    public static double updateCalibration(double currentCalibration, boolean agreedWithGoldStandard) {
        double step = 0.05;
        double updated = agreedWithGoldStandard ? currentCalibration + step : currentCalibration - step;
        return Math.max(0.1, Math.min(1.0, Math.round(updated * 100.0) / 100.0));
    }
    // 4. Profile roll-up score with log diminishing returns (Section 2.7)
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
} Scoring rate last