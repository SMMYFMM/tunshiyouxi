package com.absorbgame.world;

import com.absorbgame.core.GameConfig;
import com.absorbgame.core.GameRandom;
import com.absorbgame.entity.AIEntity;
import com.absorbgame.entity.Food;
import com.absorbgame.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 * Central world state: holds player, AI list, food list, spatial hash,
 * camera, and the seeded RNG. Managers operate on this container.
 */
public class World {
    public final float width = GameConfig.WORLD_WIDTH;
    public final float height = GameConfig.WORLD_HEIGHT;

    public Player player;
    public final ArrayList<AIEntity> aiList = new ArrayList<>(64);
    public final ArrayList<Food> foodList = new ArrayList<>(GameConfig.TARGET_FOOD_COUNT + 64);

    public final SpatialHash spatial;
    public final Camera camera;
    public final GameRandom random;

    public float gameTime = 0f;
    public boolean paused = false;

    public World(long seed, float viewW, float viewH) {
        this.random = new GameRandom(seed);
        this.camera = new Camera(viewW, viewH);
        this.spatial = new SpatialHash(width, height, 250f);
    }

    public void spawnPlayer() {
        player = new Player(width * 0.5f, height * 0.5f);
        camera.x = player.x;
        camera.y = player.y;
    }

    /** Find a safe spawn position (far from player and big AI). */
    public float[] safeSpawnPos() {
        float px = player != null ? player.x : width * 0.5f;
        float py = player != null ? player.y : height * 0.5f;
        for (int attempt = 0; attempt < 30; attempt++) {
            float x = random.range(GameConfig.AI_SPAWN_SAFETY_RADIUS,
                    width - GameConfig.AI_SPAWN_SAFETY_RADIUS);
            float y = random.range(GameConfig.AI_SPAWN_SAFETY_RADIUS,
                    height - GameConfig.AI_SPAWN_SAFETY_RADIUS);
            float dx = x - px, dy = y - py;
            if (dx * dx + dy * dy > GameConfig.AI_SPAWN_SAFETY_RADIUS * GameConfig.AI_SPAWN_SAFETY_RADIUS) {
                return new float[]{x, y};
            }
        }
        // fallback: edge
        return new float[]{width * 0.1f, height * 0.1f};
    }

    /** Rebuild spatial hash for current frame (only alive entities). */
    public void rebuildSpatial() {
        spatial.clear();
        if (player != null && player.alive) spatial.insert(player);
        for (int i = 0; i < aiList.size(); i++) {
            AIEntity a = aiList.get(i);
            if (a.alive) spatial.insert(a);
        }
        for (int i = 0; i < foodList.size(); i++) {
            spatial.insert(foodList.get(i));
        }
    }

    public List<Food> getFoodList() { return foodList; }
    public List<AIEntity> getAIList() { return aiList; }
}
