package com.absorbgame.world;

import com.absorbgame.core.GameConfig;
import com.absorbgame.entity.AIEntity;
import com.absorbgame.entity.Creature;
import com.absorbgame.entity.Entity;
import com.absorbgame.entity.Food;
import com.absorbgame.entity.Player;

import java.util.List;

/**
 * Handles eating collisions: creature vs food and creature vs creature.
 * Uses the spatial hash for broad-phase, then precise overlap test.
 */
public class CollisionManager {
    private final World world;

    public CollisionManager(World world) { this.world = world; }

    public void resolve() {
        // food eating: only check around each creature
        List<Entity> nearby = null;
        for (int i = 0; i < world.aiList.size(); i++) {
            AIEntity ai = world.aiList.get(i);
            if (!ai.alive) continue;
            eatFoodNear(ai);
        }
        if (world.player != null && world.player.alive) {
            eatFoodNear(world.player);
        }

        // creature vs creature (player + AI)
        // Iterate AI and check against player + other AI via spatial query
        resolveCreatureCollisions();
    }

    private void eatFoodNear(Creature c) {
        List<Entity> near = world.spatial.query(c.x, c.y, c.radius + GameConfig.FOOD_RADIUS + 2f);
        for (int i = 0; i < near.size(); i++) {
            Entity e = near.get(i);
            if (!e.alive || !(e instanceof Food)) continue;
            Food f = (Food) e;
            float r = c.radius + f.radius;
            float dx = c.x - f.x, dy = c.y - f.y;
            if (dx * dx + dy * dy <= r * r) {
                c.setMass(c.mass + f.value);
                f.alive = false;
            }
        }
    }

    private void resolveCreatureCollisions() {
        // Build a combined list of all creatures (player + AI) for pair checks
        // Using spatial hash per creature is efficient.
        // Player vs AI
        if (world.player != null && world.player.alive) {
            checkCreatureEat(world.player);
        }
        // AI vs others
        for (int i = 0; i < world.aiList.size(); i++) {
            AIEntity ai = world.aiList.get(i);
            if (!ai.alive) continue;
            checkCreatureEat(ai);
        }
    }

    private void checkCreatureEat(Creature self) {
        List<Entity> near = world.spatial.query(self.x, self.y,
                self.radius * 1.5f + 40f);
        for (int i = 0; i < near.size(); i++) {
            Entity e = near.get(i);
            if (e == self || !e.alive) continue;
            if (!(e instanceof Creature)) continue;
            Creature other = (Creature) e;
            // eating requires the target's center to be well inside the eater
            float r = self.radius;
            float dx = self.x - other.x, dy = self.y - other.y;
            float d2 = dx * dx + dy * dy;
            if (d2 > r * r) continue; // target center must be within self's radius

            if (self.canEat(other)) {
                // self eats other
                float gained = other.mass * GameConfig.MASS_TRANSFER_RATIO;
                self.setMass(self.mass + gained);
                self.killCount++;
                if (other instanceof Player) {
                    ((Player) other).die();
                } else if (other instanceof AIEntity) {
                    ((AIEntity) other).die();
                }
                other.alive = false;
            }
        }
    }
}
