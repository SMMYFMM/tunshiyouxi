package com.absorbgame.entity;

import com.absorbgame.core.GameConfig;

/**
 * A "blob" creature: player or AI. Has mass, velocity, speed.
 * Radius and speed are derived from mass.
 */
public abstract class Creature extends Entity {
    public float mass;
    public float vx, vy;
    public float targetVx, targetVy; // desired velocity set by controller
    public float baseSpeed;
    public float speedFactor;
    public float baseRadius;
    public float baseMass;

    // stats
    public float survivalTime = 0f;
    public int killCount = 0;
    public float maxMass;

    public Creature(float x, float y, float baseMass, float baseRadius,
                    float baseSpeed, float speedFactor) {
        super(x, y, baseRadius);
        this.baseMass = baseMass;
        this.baseRadius = baseRadius;
        this.baseSpeed = baseSpeed;
        this.speedFactor = speedFactor;
        setMass(baseMass);
        maxMass = baseMass;
    }

    public void setMass(float m) {
        this.mass = m;
        this.radius = baseRadius * (float) Math.sqrt(m / baseMass);
        if (m > maxMass) maxMass = m;
    }

    /** Current max speed based on mass. Larger = slower. */
    public float currentSpeed() {
        return baseSpeed / (1f + speedFactor * (float) Math.sqrt(mass));
    }

    /**
     * Update physics: smoothly approach target velocity, integrate position.
     * dt in seconds.
     */
    public void update(float dt, float worldW, float worldH) {
        // smooth velocity toward target
        float lerp = Math.min(1f, 10f * dt);
        vx += (targetVx - vx) * lerp;
        vy += (targetVy - vy) * lerp;

        x += vx * dt;
        y += vy * dt;

        // clamp to world bounds
        if (x < radius) { x = radius; vx = 0; targetVx = 0; }
        if (y < radius) { y = radius; vy = 0; targetVy = 0; }
        if (x > worldW - radius) { x = worldW - radius; vx = 0; targetVx = 0; }
        if (y > worldH - radius) { y = worldH - radius; vy = 0; targetVy = 0; }

        survivalTime += dt;
    }

    /** Returns true if this creature can eat the target. */
    public boolean canEat(Creature target) {
        return mass >= target.mass * GameConfig.EAT_RATIO;
    }

    public boolean canEat(Creature target, float ratio) {
        return mass >= target.mass * ratio;
    }
}
