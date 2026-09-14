package com.example.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface SubmissionRepository extends JpaRepository<Submission, Long> {
    List<Submission> findByChallengeIdAndVerificationTierGreaterThanEqualOrderByPercentileScoreDesc(String challengeId, Integer minTier);
    List<Submission> findByChallengeId(String challengeId);
    Optional<Submission> findByChallengeIdAndUserId(String challengeId, String userId);
}
