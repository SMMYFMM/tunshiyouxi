package com.absorbgame.core;

/**
 * Deterministic pseudo-random generator (xorshift32/64 mix).
 * Seed is saved with the game so loaded worlds stay consistent.
 */
public final class GameRandom {
    private long state;

    public GameRandom(long seed) {
        this.state = (seed == 0L) ? 0x9E3779B97F4A7C15L : seed;
    }

    public long nextLong() {
        long x = state;
        x ^= x << 13;
        x ^= x >>> 7;
        x ^= x << 17;
        state = x;
        return x;
    }

    /** Uniform int in [0, bound). */
    public int nextInt(int bound) {
        if (bound <= 0) return 0;
        return (int) ((nextLong() & 0x7FFFFFFFFFFFFFFFL) % bound);
    }

    /** Uniform float in [0, 1). */
    public float nextFloat() {
        return (nextLong() & 0xFFFFFF) / (float) 0x1000000;
    }

    /** Uniform float in [min, max). */
    public float range(float min, float max) {
        return min + nextFloat() * (max - min);
    }

    public long getSeed() {
        return state;
    }

    public void setSeed(long seed) {
        this.state = seed;
    }
}
