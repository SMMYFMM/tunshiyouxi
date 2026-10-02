package com.absorbgame.core;

/**
 * Central configuration. All tunable game parameters live here so they can be
 * adjusted without hunting through the codebase.
 */
public final class GameConfig {
    private GameConfig() {}

    // ---- World ----
    public static final float WORLD_WIDTH  = 10000f;
    public static final float WORLD_HEIGHT = 10000f;

    // ---- Player ----
    public static final float PLAYER_BASE_MASS   = 20f;
    public static final float PLAYER_BASE_RADIUS = 18f;
    public static final float PLAYER_BASE_SPEED  = 260f;     // world units / sec
    public static final float PLAYER_SPEED_FACTOR = 0.018f;  // bigger -> slower

    // ---- Food ----
    public static final int   TARGET_FOOD_COUNT = 500;
    public static final float FOOD_VALUE_MIN = 1f;
    public static final float FOOD_VALUE_MAX = 3f;
    public static final float FOOD_RADIUS    = 6f;
    public static final float FOOD_RESPAWN_DELAY = 0.05f;

    // ---- AI ----
    public static final int   AI_COUNT = 40;
    public static final float AI_BASE_MASS   = 15f;
    public static final float AI_BASE_RADIUS = 16f;
    public static final float AI_BASE_SPEED  = 260f;
    public static final float AI_SPEED_FACTOR = 0.018f;
    public static final float AI_MAX_MASS = 2500f;
    public static final float AI_SPAWN_SAFETY_RADIUS = 600f;

    // ---- Eating / growth ----
    // Radius = BASE_RADIUS * sqrt(mass / BASE_MASS)
    public static final float EAT_RATIO = 1.15f;  // self.mass >= target.mass * EAT_RATIO
    public static final float MASS_TRANSFER_RATIO = 0.85f; // how much of eaten mass transfers

    // ---- Camera ----
    public static final float CAMERA_MIN_ZOOM = 0.35f;
    public static final float CAMERA_MAX_ZOOM = 1.6f;
    public static final float CAMERA_ZOOM_SPEED = 2.5f;
    public static final float CAMERA_FOLLOW_SPEED = 6f;

    // ---- AI decision timing ----
    public static final float AI_DECISION_INTERVAL = 0.15f;   // ~6.6 Hz normal
    public static final float AI_DECISION_INTERVAL_CLOSE = 0.06f;
    public static final float AI_MAX_CHASE_TIME = 6f;

    // ---- AI danger / safety ----
    public static final float DANGER_RATIO = 1.25f; // threat.mass > self.mass * DANGER_RATIO -> flee
    public static final float FLEE_SPEED_BOOST = 1.18f;

    // ---- Save ----
    public static final float AUTOSAVE_INTERVAL = 45f; // seconds
    public static final int SAVE_VERSION = 1;

    // ---- Rendering ----
    public static final int MAX_BATCH_QUADS = 1500; // food + ai + player + effects

    // ---- Colors (RGBA packed) ----
    public static final float[] COLOR_PLAYER = {0.30f, 0.75f, 1.0f, 1f};
    public static final float[] COLOR_FOOD   = {1.0f, 0.85f, 0.30f, 1f};
    public static final float[] COLOR_BG_TOP = {0.05f, 0.07f, 0.12f, 1f};
    public static final float[] COLOR_BG_BOTTOM = {0.02f, 0.03f, 0.06f, 1f};
    public static final float[] COLOR_BORDER = {0.4f, 0.6f, 0.9f, 1f};

    /** Difficulty presets. */
    public enum Difficulty {
        EASY(   420f, 0.25f, 0.6f, 0.7f, 0.5f, 35),
        NORMAL( 560f, 0.15f, 1.0f, 1.0f, 1.0f, 40),
        HARD(   720f, 0.08f, 1.5f, 1.3f, 1.5f, 50);

        public final float senseRadius;
        public final float decisionInterval;
        public final float aggressiveness;  // chase willingness
        public final float foodPriority;
        public final float reactionSpeed;   // movement smoothing
        public final int aiCount;

        Difficulty(float senseRadius, float decisionInterval, float aggressiveness,
                   float foodPriority, float reactionSpeed, int aiCount) {
            this.senseRadius = senseRadius;
            this.decisionInterval = decisionInterval;
            this.aggressiveness = aggressiveness;
            this.foodPriority = foodPriority;
            this.reactionSpeed = reactionSpeed;
            this.aiCount = aiCount;
        }
    }
}
