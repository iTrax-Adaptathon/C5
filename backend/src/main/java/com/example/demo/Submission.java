package com.example.demo;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "submissions", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"challengeId", "userId"})
})
public class Submission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String challengeId;
    private String userId;
    private Double rawMetric;
    private String contentUrl;
    private Integer verificationTier = 0;
    private Integer eloRating = 1000;
    private Double percentileScore = 0.0;
    private Instant createdAt = Instant.now();

    public Submission() {}

    public Long getId() { return id; }
    public String getChallengeId() { return challengeId; }
    public void setChallengeId(String challengeId) { this.challengeId = challengeId; }

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public Double getRawMetric() { return rawMetric; }
    public void setRawMetric(Double rawMetric) { this.rawMetric = rawMetric; }

    public String getContentUrl() { return contentUrl; }
    public void setContentUrl(String contentUrl) { this.contentUrl = contentUrl; }

    public Integer getVerificationTier() { return verificationTier; }
    public void setVerificationTier(Integer verificationTier) { this.verificationTier = verificationTier; }

    public Integer getEloRating() { return eloRating; }
    public void setEloRating(Integer eloRating) { this.eloRating = eloRating; }

    public Double getPercentileScore() { return percentileScore; }
    public void setPercentileScore(Double percentileScore) { this.percentileScore = percentileScore; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
