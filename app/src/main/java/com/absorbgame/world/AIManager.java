package com.absorbgame.world;

import com.absorbgame.ai.AIController;
import com.absorbgame.ai.AIPersonality;
import com.absorbgame.core.GameConfig;
import com.absorbgame.entity.AIEntity;

import java.util.Random;

/** Manages AI spawning, respawning, and per-AI decision updates. */
public class AIManager {
    private final World world;
    private final AIController controller;
    private final Random personalityRng = new Random(42);
    private GameConfig.Difficulty difficulty = GameConfig.Difficulty.NORMAL;

    public AIManager(World world) {
        this.world = world;
        this.controller = new AIController(world);
    }

    public void setDifficulty(GameConfig.Difficulty d) { this.difficulty = d; }

    public AIController getController() { return controller; }

    /** Spawn the initial AI population for the difficulty. */
    public void spawnInitial() {
        world.aiList.clear();
        for (int i = 0; i < difficulty.aiCount; i++) {
            spawnAI();
        }
    }

    public AIEntity spawnAI() {
        float[] pos = world.safeSpawnPos();
        // start with some mass variance so the ecosystem has variety
        float mass = GameConfig.AI_BASE_MASS
                + world.random.range(0f, GameConfig.AI_BASE_MASS * 1.5f);
        AIEntity ai = new AIEntity(pos[0], pos[1], AIPersonality.random(personalityRng));
        ai.setMass(mass);
        // link controller
        ai.controller = controller;
        world.aiList.add(ai);
        return ai;
    }

    /** Update all AI decisions + physics. Dead AI are respawned. */
    public void update(float dt) {
        for (int i = 0; i < world.aiList.size(); i++) {
            AIEntity ai = world.aiList.get(i);
            if (!ai.alive) continue;
            // cap AI mass to prevent runaway giants
            if (ai.mass > GameConfig.AI_MAX_MASS) ai.setMass(GameConfig.AI_MAX_MASS);
            controller.update(ai, dt, difficulty);
            ai.update(dt, world.width, world.height);
        }
        respawnDead();
    }

    private void respawnDead() {
        int alive = 0;
        for (int i = 0; i < world.aiList.size(); i++) {
            if (world.aiList.get(i).alive) alive++;
        }
        int need = difficulty.aiCount - alive;
        for (int i = 0; i < need; i++) {
            // remove one dead slot if present
            boolean replaced = false;
            for (int j = 0; j < world.aiList.size(); j++) {
                if (!world.aiList.get(j).alive) {
                    float[] pos = world.safeSpawnPos();
                    AIEntity ai = new AIEntity(pos[0], pos[1], AIPersonality.random(personalityRng));
                    ai.controller = controller;
                    world.aiList.set(j, ai);
                    replaced = true;
                    break;
                }
            }
            if (!replaced) spawnAI();
        }
    }
}
