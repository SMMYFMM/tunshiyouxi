package com.absorbgame.entity;

import com.absorbgame.ai.AIController;
import com.absorbgame.ai.AIPersonality;
import com.absorbgame.ai.AIState;
import com.absorbgame.core.GameConfig;

/** An AI-controlled blob. */
public class AIEntity extends Creature {
    public AIPersonality personality;
    public AIState state = AIState.WANDER;
    public AIController controller;

    // decision bookkeeping
    public float decisionTimer = 0f;
    public float stateTimer = 0f;
    public Entity currentTarget;   // food or creature being chased
    public Creature fleeTarget;    // threat being fled from
    public float wanderAngle;

    public AIEntity(float x, float y, AIPersonality personality) {
        super(x, y,
                GameConfig.AI_BASE_MASS,
                GameConfig.AI_BASE_RADIUS,
                GameConfig.AI_BASE_SPEED,
                GameConfig.AI_SPEED_FACTOR);
        this.personality = personality;
        this.wanderAngle = (float) (Math.random() * Math.PI * 2);
        // distinct color
        float hue = (float) Math.random();
        float[] c = hsvToRgb(hue, 0.65f, 0.95f);
        setColor(c[0], c[1], c[2], 1f);
    }

    public void setController(AIController c) { this.controller = c; }

    public void die() {
        alive = false;
        targetVx = 0; targetVy = 0; vx = 0; vy = 0;
    }

    private static float[] hsvToRgb(float h, float s, float v) {
        float c = v * s;
        float hp = h * 6f;
        float x = c * (1f - Math.abs(hp % 2f - 1f));
        float r = 0, g = 0, b = 0;
        if (hp < 1) { r = c; g = x; }
        else if (hp < 2) { r = x; g = c; }
        else if (hp < 3) { g = c; b = x; }
        else if (hp < 4) { g = x; b = c; }
        else if (hp < 5) { r = x; b = c; }
        else { r = c; b = x; }
        float m = v - c;
        return new float[]{r + m, g + m, b + m};
    }
}
