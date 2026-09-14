-- 1. Users table with reputation-weighted calibration
CREATE TABLE users (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    username TEXT UNIQUE NOT NULL,
    reviewer_calibration NUMERIC DEFAULT 0.5, -- 0..1 scale, starts neutral
    account_tier INT DEFAULT 0,               -- gates vote weight
    created_at TIMESTAMPTZ DEFAULT now()
);
-- 2. Challenges table with pluggable evaluation types
CREATE TABLE challenges (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    title TEXT NOT NULL,
    eval_type TEXT NOT NULL CHECK (eval_type IN ('metric', 'boolean', 'subjective', 'rubric')),
    metric_direction TEXT CHECK (metric_direction IN ('higher_better', 'lower_better')),
    difficulty_score NUMERIC DEFAULT 0.5,     -- 0.0 to 1.0
    created_at TIMESTAMPTZ DEFAULT now()
);
-- 3. Submissions table with anti-farming unique constraint
CREATE TABLE submissions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    challenge_id UUID REFERENCES challenges(id) ON DELETE CASCADE,
    user_id UUID REFERENCES users(id) ON DELETE CASCADE,
    raw_metric NUMERIC,                       -- for eval_type='metric'
    boolean_result BOOLEAN,                   -- for eval_type='boolean'
    content_url TEXT,                         -- proof/media link
    verification_tier INT DEFAULT 0,          -- 0=self, 1=unverified, 2=peer, etc.
    elo_rating NUMERIC DEFAULT 1000.0,        -- for eval_type='subjective'
    percentile_score NUMERIC DEFAULT 0.0,     -- 0 to 100
    created_at TIMESTAMPTZ DEFAULT now(),
    UNIQUE(challenge_id, user_id)             -- only one active submission per user per challenge
);
-- 4. Pairwise comparisons for subjective Elo judging
CREATE TABLE comparisons (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    challenge_id UUID REFERENCES challenges(id) ON DELETE CASCADE,
    submission_a UUID REFERENCES submissions(id) ON DELETE CASCADE,
    submission_b UUID REFERENCES submissions(id) ON DELETE CASCADE,
    winner_id UUID REFERENCES submissions(id) ON DELETE CASCADE,
    reviewer_id UUID REFERENCES users(id) ON DELETE CASCADE,
    is_gold_standard BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMPTZ DEFAULT now()
);
