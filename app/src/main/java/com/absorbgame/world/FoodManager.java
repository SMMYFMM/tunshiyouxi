package com.absorbgame.world;

import com.absorbgame.core.GameConfig;
import com.absorbgame.entity.Food;

/** Manages food spawning so the world keeps a target food count. */
public class FoodManager {
    private final World world;

    public FoodManager(World world) { this.world = world; }

    public void ensureFoodCount() {
        int need = GameConfig.TARGET_FOOD_COUNT - world.foodList.size();
        for (int i = 0; i < need; i++) {
            spawnFood();
        }
    }

    public void spawnFood() {
        float x = world.random.range(20f, world.width - 20f);
        float y = world.random.range(20f, world.height - 20f);
        float value = world.random.range(GameConfig.FOOD_VALUE_MIN, GameConfig.FOOD_VALUE_MAX);
        world.foodList.add(new Food(x, y, value));
    }

    /** Compacts the food list by removing dead entries (avoids per-frame remove). */
    public void compact() {
        int w = 0;
        for (int i = 0; i < world.foodList.size(); i++) {
            Food f = world.foodList.get(i);
            if (f.alive) world.foodList.set(w++, f);
        }
        // trim
        while (world.foodList.size() > w) world.foodList.remove(world.foodList.size() - 1);
    }
}
