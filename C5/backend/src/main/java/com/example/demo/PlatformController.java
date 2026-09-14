package com.example.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
public class PlatformController {

    @Autowired
    private SubmissionRepository submissionRepo;

    // 1. Submit Entry (index.html)
    @PostMapping("/submissions")
    public ResponseEntity<?> createSubmission(@RequestBody Submission body) {
        Submission sub = submissionRepo.findByChallengeIdAndUserId(body.getChallengeId(), body.getUserId())
            .orElse(body);

        sub.setRawMetric(body.getRawMetric());
        sub.setContentUrl(body.getContentUrl());
        sub.setVerificationTier(body.getVerificationTier());
        submissionRepo.save(sub);

        // Normalize metric scores using percentile rank within this cohort
        if (sub.getRawMetric() != null) {
            List<Submission> cohort = submissionRepo.findByChallengeId(sub.getChallengeId());
            List<Double> allMetrics = cohort.stream()
                .map(Submission::getRawMetric)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

            boolean higherIsBetter = !sub.getChallengeId().toLowerCase().contains("run");
            for (Submission entry : cohort) {
                if (entry.getRawMetric() != null) {
                    double rank = ScoringEngine.percentileRank(entry.getRawMetric(), allMetrics, higherIsBetter);
                    entry.setPercentileScore(rank);
                    submissionRepo.save(entry);
                }
            }
        }

        return ResponseEntity.ok(Map.of("status", "saved"));
    }

    // 2. Filtered Leaderboard (leaderboard.html)
    @GetMapping("/challenges/{id}/leaderboard")
    public List<Map<String, Object>> getLeaderboard(
            @PathVariable String id, 
            @RequestParam(defaultValue = "0") Integer min_tier) {

        List<Submission> subs = submissionRepo.findByChallengeIdAndVerificationTierGreaterThanEqualOrderByPercentileScoreDesc(id, min_tier);

        return subs.stream().map(s -> {
            Map<String, Object> item = new HashMap<>();
            item.put("user", s.getUserId());
            item.put("score", s.getPercentileScore() + "%");
            item.put("tier", s.getVerificationTier());
            item.put("label", "Tier " + s.getVerificationTier());
            return item;
        }).collect(Collectors.toList());
    }

    // 3. Random Matchup for Head-to-Head Arena (arena.html)
    @GetMapping("/challenges/{id}/pair")
    public ResponseEntity<?> getMatchupPair(@PathVariable String id) {
        List<Submission> all = submissionRepo.findByChallengeId(id);
        if (all.size() < 2) {
            return ResponseEntity.badRequest().body("Requires at least 2 submissions to generate a matchup");
        }

        Collections.shuffle(all);
        Map<String, Object> response = new HashMap<>();
        response.put("submissionA", Map.of("id", all.get(0).getId(), "user", all.get(0).getUserId(), "url", all.get(0).getContentUrl()));
        response.put("submissionB", Map.of("id", all.get(1).getId(), "user", all.get(1).getUserId(), "url", all.get(1).getContentUrl()));
        return ResponseEntity.ok(response);
    }

    // 4. Record Pairwise Vote (arena.html)
    @PostMapping("/challenges/{id}/vote")
    public ResponseEntity<?> recordVote(@RequestBody Map<String, Object> body) {
        if (!body.containsKey("winnerId")) {
            return ResponseEntity.badRequest().body(Map.of("error", "winnerId is required"));
        }
        Long winnerId = Long.valueOf(body.get("winnerId").toString());
        Long subAId = body.get("submissionAId") != null ? Long.valueOf(body.get("submissionAId").toString()) : null;
        Long subBId = body.get("submissionBId") != null ? Long.valueOf(body.get("submissionBId").toString()) : null;

        if (subAId == null || subBId == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "submissionAId and submissionBId are required"));
        }

        Submission subA = submissionRepo.findById(subAId).orElseThrow();
        Submission subB = submissionRepo.findById(subBId).orElseThrow();

        // Calculate and apply Elo adjustments
        ScoringEngine.EloResult result = ScoringEngine.updateElo(subA.getEloRating(), subB.getEloRating(), winnerId.equals(subA.getId()));
        subA.setEloRating(result.ratingA());
        subB.setEloRating(result.ratingB());

        submissionRepo.save(subA);
        submissionRepo.save(subB);

        return ResponseEntity.ok(Map.of("status", "elo_updated", "ratingA", result.ratingA(), "ratingB", result.ratingB()));
    }
}
