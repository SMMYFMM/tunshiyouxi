package com.absorbgame.entity;

/**
 * Base class for all world entities (food, player, AI).
 * Holds position, radius, color, and alive flag.
 */
public abstract class Entity {
    public float x, y;
    public float radius;
    public float r, g, b, a = 1f;
    public boolean alive = true;

    public Entity(float x, float y, float radius) {
        this.x = x;
        this.y = y;
        this.radius = radius;
    }

    public void setColor(float r, float g, float b, float a) {
        this.r = r; this.g = g; this.b = b; this.a = a;
    }

    /** Squared distance to another entity. */
    public float distSq(Entity other) {
        float dx = x - other.x;
        float dy = y - other.y;
        return dx * dx + dy * dy;
    }

    public float dist(Entity other) {
        return (float) Math.sqrt(distSq(other));
    }
}
