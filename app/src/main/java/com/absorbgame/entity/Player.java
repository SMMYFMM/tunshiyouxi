package com.absorbgame.entity;

import com.absorbgame.core.GameConfig;

/** The player-controlled blob. */
public class Player extends Creature {

    public Player(float x, float y) {
        super(x, y,
                GameConfig.PLAYER_BASE_MASS,
                GameConfig.PLAYER_BASE_RADIUS,
                GameConfig.PLAYER_BASE_SPEED,
                GameConfig.PLAYER_SPEED_FACTOR);
        setColor(GameConfig.COLOR_PLAYER[0], GameConfig.COLOR_PLAYER[1],
                GameConfig.COLOR_PLAYER[2], GameConfig.COLOR_PLAYER[3]);
    }

    /**
     * Set desired movement direction (normalized vector) from joystick input.
     */
    public void setMoveInput(float dx, float dy) {
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        float speed = currentSpeed();
        if (len > 1f) { dx /= len; dy /= len; }
        targetVx = dx * speed * Math.min(1f, len);
        targetVy = dy * speed * Math.min(1f, len);
    }

    /** Called when player dies (eaten). */
    public void die() {
        alive = false;
        targetVx = 0; targetVy = 0; vx = 0; vy = 0;
    }
}
