package com.absorbgame.input;

/**
 * Virtual joystick. Tracks an active touch pointer and reports a normalized
 * direction vector (magnitude 0..1).
 */
public class Joystick {
    public float baseX, baseY;       // base position (screen px)
    public float knobX, knobY;       // current knob offset from base
    public float radius = 80f;       // max knob travel
    public boolean active = false;
    public int pointerId = -1;

    public void reset() {
        active = false;
        pointerId = -1;
        knobX = 0; knobY = 0;
    }

    public void onDown(float x, float y, int id) {
        if (active) return;
        active = true;
        pointerId = id;
        baseX = x; baseY = y;
        knobX = 0; knobY = 0;
    }

    public void onMove(float x, float y, int id) {
        if (!active || id != pointerId) return;
        float dx = x - baseX;
        float dy = y - baseY;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len > radius) {
            dx = dx / len * radius;
            dy = dy / len * radius;
        }
        knobX = dx;
        knobY = dy;
    }

    public void onUp(int id) {
        if (id != pointerId) return;
        reset();
    }

    /** Normalized output x in [-1, 1]. */
    public float getX() { return knobX / radius; }
    /** Normalized output y in [-1, 1]. Note: screen y is down; game y is up. */
    public float getY() { return -knobY / radius; }
    public float getMagnitude() {
        return Math.min(1f, (float) Math.sqrt(knobX * knobX + knobY * knobY) / radius);
    }
}
