
package scoring;

public class ChallengeResult {
    private final double percentileScore;      // 0 to 100
    private final double difficultyScore;      // 0.0 to 1.0
    private final double verificationMultiplier; // 0.3 to 1.2

    public ChallengeResult(double percentileScore, double difficultyScore, double verificationMultiplier) {
        this.percentileScore = percentileScore;
        this.difficultyScore = difficultyScore;
        this.verificationMultiplier = verificationMultiplier;
    }

    public double getPercentileScore() { return percentileScore; }
    public double getDifficultyScore() { return difficultyScore; }
    public double getVerificationMultiplier() { return verificationMultiplier; }
}
