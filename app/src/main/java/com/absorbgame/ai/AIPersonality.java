package com.absorbgame.ai;

/**
 * Per-AI personality traits. Randomized at spawn so AI feel different and
 * not all behave identically.
 */
public class AIPersonality {
    public float aggression;   // 0..1 willingness to chase
    public float caution;      // 0..1 tendency to flee
    public float greed;        // 0..1 priority on food
    public float wanderBias;   // 0..1 how much it wanders aimlessly

    public AIPersonality(float aggression, float caution, float greed, float wanderBias) {
        this.aggression = aggression;
        this.caution = caution;
        this.greed = greed;
        this.wanderBias = wanderBias;
    }

    public static AIPersonality random(java.util.Random r) {
        return new AIPersonality(
                0.3f + r.nextFloat() * 0.6f,
                0.3f + r.nextFloat() * 0.6f,
                0.4f + r.nextFloat() * 0.5f,
                0.2f + r.nextFloat() * 0.5f);
    }
}
