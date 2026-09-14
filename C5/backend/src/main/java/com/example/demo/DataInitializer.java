package com.example.demo;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class DataInitializer implements CommandLineRunner {

    private final SubmissionRepository submissionRepo;

    public DataInitializer(SubmissionRepository submissionRepo) {
        this.submissionRepo = submissionRepo;
    }

    @Override
    public void run(String... args) {
        if (submissionRepo.count() > 0) return;

        // Seed 5k-run submissions (Metric: lower time in minutes is better)
        createRunSubmission("dev_satoshi", 17.8, 4, "https://strava.com/activities/10001");
        createRunSubmission("quantum_coder", 19.5, 2, "https://strava.com/activities/10002");
        createRunSubmission("pixel_hunter", 22.1, 2, "https://strava.com/activities/10003");
        createRunSubmission("cyber_runner", 25.4, 3, "https://strava.com/activities/10004");
        createRunSubmission("newbie_farmer", 31.2, 0, "https://strava.com/activities/10005");

        // Calculate initial percentile ranks for 5k-run cohort
        List<Submission> runCohort = submissionRepo.findByChallengeId("5k-run");
        List<Double> runMetrics = runCohort.stream().map(Submission::getRawMetric).collect(Collectors.toList());
        for (Submission s : runCohort) {
            s.setPercentileScore(ScoringEngine.percentileRank(s.getRawMetric(), runMetrics, false));
            submissionRepo.save(s);
        }

        // Seed photo-contest submissions (Pairwise Elo)
        createPhotoSubmission("creative_mind", "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500", 1150, 2);
        createPhotoSubmission("prompt_master", "https://images.unsplash.com/photo-1634017839464-5c339ebe3cb4?w=500", 1080, 2);
        createPhotoSubmission("neural_artist", "https://images.unsplash.com/photo-1618005198919-d3d4b5a92ead?w=500", 1020, 1);
        createPhotoSubmission("synth_wave", "https://images.unsplash.com/photo-1620641788421-7a1c342ea42e?w=500", 980, 3);
    }

    private void createRunSubmission(String userId, double metric, int tier, String url) {
        Submission s = new Submission();
        s.setChallengeId("5k-run");
        s.setUserId(userId);
        s.setRawMetric(metric);
        s.setVerificationTier(tier);
        s.setContentUrl(url);
        submissionRepo.save(s);
    }

    private void createPhotoSubmission(String userId, String url, int elo, int tier) {
        Submission s = new Submission();
        s.setChallengeId("photo-contest");
        s.setUserId(userId);
        s.setContentUrl(url);
        s.setEloRating(elo);
        s.setVerificationTier(tier);
        submissionRepo.save(s);
    }
}
