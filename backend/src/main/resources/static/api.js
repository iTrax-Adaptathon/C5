/**
 * Community Challenge Platform - Client-side API Engine
 * Pure JavaScript in-browser service with localStorage persistence
 */
(function(window) {
  const STORAGE_KEY = "c5_platform_submissions_v1";

  const VERIFICATION_TIERS = {
    0: { label: "Tier 0 (Self-reported)", multiplier: 0.3, badgeClass: "badge-self" },
    1: { label: "Tier 1 (Evidence Attached)", multiplier: 0.5, badgeClass: "badge-self" },
    2: { label: "Tier 2 (Peer-verified)", multiplier: 1.0, badgeClass: "badge-peer" },
    3: { label: "Tier 3 (Sensor/API Verified)", multiplier: 1.1, badgeClass: "badge-peer" },
    4: { label: "Tier 4 (Moderator Certified)", multiplier: 1.2, badgeClass: "badge-moderator" }
  };

  // Pure Math Scoring Engine
  const ScoringEngine = {
    K: 32,

    updateElo(ratingA, ratingB, aWon) {
      const expectedA = 1.0 / (1.0 + Math.pow(10.0, (ratingB - ratingA) / 400.0));
      const expectedB = 1.0 / (1.0 + Math.pow(10.0, (ratingA - ratingB) / 400.0));

      const actualA = aWon ? 1.0 : 0.0;
      const actualB = aWon ? 0.0 : 1.0;

      const newRatingA = Math.round(ratingA + this.K * (actualA - expectedA));
      const newRatingB = Math.round(ratingB + this.K * (actualB - expectedB));

      return { ratingA: newRatingA, ratingB: newRatingB };
    },

    percentileRank(value, allValues, higherIsBetter) {
      if (!allValues || allValues.length <= 1) return 100.0;

      const countBeaten = higherIsBetter
        ? allValues.filter(v => v < value).length
        : allValues.filter(v => v > value).length;

      return Math.round((countBeaten / (allValues.length - 1)) * 100.0);
    }
  };

  // Default Seed Data
  const DEFAULT_SEEDS = [
    {
      id: "run_1",
      challengeId: "5k-run",
      userId: "dev_satoshi",
      rawMetric: 17.8,
      contentUrl: "https://strava.com/activities/10001",
      verificationTier: 4,
      eloRating: 1000,
      percentileScore: 100.0
    },
    {
      id: "run_2",
      challengeId: "5k-run",
      userId: "quantum_coder",
      rawMetric: 19.5,
      contentUrl: "https://strava.com/activities/10002",
      verificationTier: 2,
      eloRating: 1000,
      percentileScore: 75.0
    },
    {
      id: "run_3",
      challengeId: "5k-run",
      userId: "pixel_hunter",
      rawMetric: 22.1,
      contentUrl: "https://strava.com/activities/10003",
      verificationTier: 2,
      eloRating: 1000,
      percentileScore: 50.0
    },
    {
      id: "run_4",
      challengeId: "5k-run",
      userId: "cyber_runner",
      rawMetric: 25.4,
      contentUrl: "https://strava.com/activities/10004",
      verificationTier: 3,
      eloRating: 1000,
      percentileScore: 25.0
    },
    {
      id: "run_5",
      challengeId: "5k-run",
      userId: "newbie_farmer",
      rawMetric: 31.2,
      contentUrl: "https://strava.com/activities/10005",
      verificationTier: 0,
      eloRating: 1000,
      percentileScore: 0.0
    },
    {
      id: "photo_1",
      challengeId: "photo-contest",
      userId: "creative_mind",
      desc: "Generative Canvas Model v2",
      rawMetric: null,
      contentUrl: "https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?w=500",
      verificationTier: 2,
      eloRating: 1150,
      percentileScore: 0.0
    },
    {
      id: "photo_2",
      challengeId: "photo-contest",
      userId: "prompt_master",
      desc: "Minimalist Shaded Architecture",
      rawMetric: null,
      contentUrl: "https://images.unsplash.com/photo-1634017839464-5c339ebe3cb4?w=500",
      verificationTier: 2,
      eloRating: 1080,
      percentileScore: 0.0
    },
    {
      id: "photo_3",
      challengeId: "photo-contest",
      userId: "neural_artist",
      desc: "Cybernetic Bloom",
      rawMetric: null,
      contentUrl: "https://images.unsplash.com/photo-1618005198919-d3d4b5a92ead?w=500",
      verificationTier: 1,
      eloRating: 1020,
      percentileScore: 0.0
    },
    {
      id: "photo_4",
      challengeId: "photo-contest",
      userId: "synth_wave",
      desc: "Neon Retro Grid",
      rawMetric: null,
      contentUrl: "https://images.unsplash.com/photo-1620641788421-7a1c342ea42e?w=500",
      verificationTier: 3,
      eloRating: 980,
      percentileScore: 0.0
    }
  ];

  function loadSubmissions() {
    try {
      const raw = localStorage.getItem(STORAGE_KEY);
      if (raw) return JSON.parse(raw);
    } catch (e) {
      console.warn("Storage read failed, initializing defaults:", e);
    }
    saveSubmissions(DEFAULT_SEEDS);
    return JSON.parse(JSON.stringify(DEFAULT_SEEDS));
  }

  function saveSubmissions(list) {
    try {
      localStorage.setItem(STORAGE_KEY, JSON.stringify(list));
    } catch (e) {
      console.error("Storage write failed:", e);
    }
  }

  const API = {
    ScoringEngine,

    // Initialize or reset database
    init() {
      return loadSubmissions();
    },

    reset() {
      saveSubmissions(DEFAULT_SEEDS);
      return JSON.parse(JSON.stringify(DEFAULT_SEEDS));
    },

    // 1. Submit Entry (index.html)
    async createSubmission({ userId, challengeId, rawMetric, contentUrl, verificationTier }) {
      await new Promise(r => setTimeout(r, 60));
      const list = loadSubmissions();

      let existing = list.find(s => s.challengeId === challengeId && s.userId === userId);
      if (!existing) {
        existing = {
          id: "sub_" + Date.now() + "_" + Math.random().toString(36).substring(2, 7),
          challengeId,
          userId,
          eloRating: 1000,
          percentileScore: 0.0,
          createdAt: new Date().toISOString()
        };
        list.push(existing);
      }

      existing.rawMetric = (rawMetric !== null && rawMetric !== undefined && rawMetric !== "" && !isNaN(rawMetric)) ? parseFloat(rawMetric) : null;
      existing.contentUrl = contentUrl;
      existing.verificationTier = parseInt(verificationTier, 10) || 0;

      // Normalize metric percentile ranks within this cohort
      if (existing.rawMetric !== null) {
        const cohort = list.filter(s => s.challengeId === challengeId && s.rawMetric !== null);
        const allMetrics = cohort.map(s => s.rawMetric);
        const higherIsBetter = !challengeId.toLowerCase().includes("run");

        cohort.forEach(entry => {
          entry.percentileScore = ScoringEngine.percentileRank(entry.rawMetric, allMetrics, higherIsBetter);
        });
      }

      saveSubmissions(list);
      return { status: "saved", entry: existing };
    },

    // 2. Filtered Leaderboard (leaderboard.html)
    async getLeaderboard(challengeId, minTier = 0) {
      await new Promise(r => setTimeout(r, 50));
      const list = loadSubmissions();

      const filtered = list.filter(s => 
        s.challengeId === challengeId && 
        (s.verificationTier ?? 0) >= minTier
      );

      // Sort by percentileScore descending (or Elo rating if photo contest)
      const isEloChallenge = challengeId === "photo-contest";
      filtered.sort((a, b) => {
        if (isEloChallenge) return (b.eloRating || 1000) - (a.eloRating || 1000);
        return (b.percentileScore || 0) - (a.percentileScore || 0);
      });

      return filtered.map(s => {
        const tierMeta = VERIFICATION_TIERS[s.verificationTier] || VERIFICATION_TIERS[0];
        return {
          user: s.userId,
          score: isEloChallenge ? `${s.eloRating} Elo` : `${(s.percentileScore || 0).toFixed(1)}%`,
          tier: s.verificationTier,
          label: tierMeta.label,
          badgeClass: tierMeta.badgeClass
        };
      });
    },

    // 3. Matchup Pair for Peer Arena (arena.html)
    async getMatchupPair(challengeId) {
      await new Promise(r => setTimeout(r, 50));
      const list = loadSubmissions();
      const cohort = list.filter(s => s.challengeId === challengeId);

      if (cohort.length < 2) {
        throw new Error("Requires at least 2 submissions in this challenge to generate a matchup");
      }

      // Pick 2 random distinct contenders
      const indexA = Math.floor(Math.random() * cohort.length);
      let indexB = Math.floor(Math.random() * (cohort.length - 1));
      if (indexB >= indexA) indexB++;

      const subA = cohort[indexA];
      const subB = cohort[indexB];

      return {
        submissionA: {
          id: subA.id,
          user: subA.userId,
          desc: subA.desc || "Verified Contender Entry",
          url: subA.contentUrl,
          elo: subA.eloRating || 1000
        },
        submissionB: {
          id: subB.id,
          user: subB.userId,
          desc: subB.desc || "Verified Contender Entry",
          url: subB.contentUrl,
          elo: subB.eloRating || 1000
        }
      };
    },

    // 4. Record Pairwise Vote (arena.html)
    async recordVote(challengeId, { winnerId, submissionAId, submissionBId }) {
      await new Promise(r => setTimeout(r, 60));
      const list = loadSubmissions();

      const subA = list.find(s => s.id === submissionAId);
      const subB = list.find(s => s.id === submissionBId);

      if (!subA || !subB) {
        throw new Error("One or both contenders could not be found");
      }

      const ratingA = subA.eloRating || 1000;
      const ratingB = subB.eloRating || 1000;
      const aWon = (winnerId === submissionAId);

      const result = ScoringEngine.updateElo(ratingA, ratingB, aWon);
      subA.eloRating = result.ratingA;
      subB.eloRating = result.ratingB;

      saveSubmissions(list);

      return {
        status: "elo_updated",
        ratingA: result.ratingA,
        ratingB: result.ratingB,
        deltaA: result.ratingA - ratingA,
        deltaB: result.ratingB - ratingB
      };
    }
  };

  // Self-initialize on script load
  API.init();

  window.API = API;
})(window);