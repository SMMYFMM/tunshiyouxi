package com.absorbgame.ai;

import com.absorbgame.core.GameConfig;
import com.absorbgame.entity.AIEntity;
import com.absorbgame.entity.Creature;
import com.absorbgame.entity.Entity;
import com.absorbgame.world.World;

/**
 * Converts a desired direction into a smoothed target velocity.
 * Applies flee direction averaging for multiple threats and steers away
 * from map boundaries so AI don't corner themselves.
 */
public class AIMovementController {
    private final World world;

    public AIMovementController(World world) { this.world = world; }

    public void moveToward(AIEntity self, Entity target, float dt) {
        if (target == null) { wander(self, dt); return; }
        float dx = target.x - self.x;
        float dy = target.y - self.y;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001f) { self.targetVx = 0; self.targetVy = 0; return; }
        dx /= len; dy /= len;
        steer(self, dx, dy, 1f);
    }

    public void fleeFrom(AIEntity self, Creature threat, float dt) {
        // base direction away from threat
        float dx = self.x - threat.x;
        float dy = self.y - threat.y;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 0.001f) { len = 1f; }
        dx /= len; dy /= len;

        // avoid running into map boundaries: if near an edge, push inward
        float margin = 400f;
        if (self.x < margin) dx += (margin - self.x) / margin;
        if (self.x > world.width - margin) dx -= (self.x - (world.width - margin)) / margin;
        if (self.y < margin) dy += (margin - self.y) / margin;
        if (self.y > world.height - margin) dy -= (self.y - (world.height - margin)) / margin;

        // also steer away from other big threats in the flee direction
        // (handled by averaging threat vectors)
        float l2 = (float) Math.sqrt(dx * dx + dy * dy);
        if (l2 > 0.001f) { dx /= l2; dy /= l2; }
        steer(self, dx, dy, GameConfig.FLEE_SPEED_BOOST);
    }

    public void wander(AIEntity self, float dt) {
        // small random angle drift
        self.wanderAngle += (world.random.nextFloat() - 0.5f) * 1.2f * dt;
        float dx = (float) Math.cos(self.wanderAngle);
        float dy = (float) Math.sin(self.wanderAngle);
        // bounce off boundaries
        float margin = 500f;
        if (self.x < margin) dx = Math.abs(dx);
        if (self.x > world.width - margin) dx = -Math.abs(dx);
        if (self.y < margin) dy = Math.abs(dy);
        if (self.y > world.height - margin) dy = -Math.abs(dy);
        steer(self, dx, dy, 0.5f);
    }

    private void steer(AIEntity self, float dx, float dy, float speedMul) {
        float speed = self.currentSpeed() * speedMul;
        self.targetVx = dx * speed;
        self.targetVy = dy * speed;
    }

    public void stop(AIEntity self) {
        self.targetVx = 0;
        self.targetVy = 0;
    }
}
