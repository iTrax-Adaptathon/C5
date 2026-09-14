# C5
# 🛡️ Community Challenge Platform

A decentralized, anti-farming competition hub engineered to prevent bot manipulation, sybil attacks, and popularity bias. The platform combines multi-tiered verification weighting, cohort percentile normalization, and pairwise Elo matchmaking.

---

## ✨ Key Features

* **Anti-Farming Tier Multipliers**: Submissions are weighted by verification rigor—ranging from Tier 0 (Self-reported, 0.3x) to Tier 4 (Moderator Certified, 1.2x) to disincentivize bot spam.
* **Cohort Percentile Normalization**: Metric-based tracks (e.g., speed runs, reps) are dynamically scored against participant cohorts on a 0–100 percentile curve rather than raw arbitrary numbers.
* **Pairwise Elo Voting Arena**: Evaluates subjective and creative challenges through head-to-head comparisons, eliminating follower-count bias and traditional "like button" snowball effects.
* **Diminishing Returns Profile Roll-Up**: Employs logarithmic volume scaling ($\log(n+1)$) so spamming easy challenges cannot outrank high-tier, verified achievements.
* **Single Active Submission Guard**: Enforces database-level constraints allowing only one active submission per user per challenge.

---

## 🏗️ Architecture & Tech Stack

* **Frontend**: Vanilla HTML5, Modern CSS (Glassmorphism & Dark Neo-Tech UI), JavaScript Fetch API.
  * `index.html`: Entry submission portal with dynamic metric inputs.
  * `leaderboard.html`: Real-time standings filterable by verification trust tier.
  * `arena.html`: Pairwise head-to-head voting interface.
* **Backend**: Java 17+, Spring Boot 3.x REST API (`http://localhost:8080`).
  * `PlatformController.java`: Endpoints for submissions, dynamic leaderboard queries, and pairwise matchups.
  * `ScoringEngine.java`: Pure mathematical scoring logic for Elo updates, percentile distributions, and profile roll-ups.
* **Database**: In-Memory H2 Database with Spring Data JPA / Hibernate automatic schema generation.

---

## 🚀 Quick Start Guide

### Prerequisites
* Java JDK 17 or higher
* Git
* Modern Web Browser

### 1. Launch the Backend Server
Open a terminal in the `backend/` directory:

```bash
# Windows
.\mvnw.cmd spring-boot:run

# Mac / Linux
./mvnw spring-boot:run
