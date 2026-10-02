package com.absorbgame.ai;

import com.absorbgame.entity.AIEntity;
import com.absorbgame.entity.Creature;
import com.absorbgame.entity.Entity;
import com.absorbgame.entity.Food;
import com.absorbgame.world.World;

import java.util.ArrayList;
import java.util.List;

/**
 * AI perception: queries the spatial hash around the AI and categorizes
 * nearby entities into food, prey (smaller), and threats (bigger).
 */
public class AISensor {
    private final World world;

    // reusable buffers (avoid GC)
    private final ArrayList<Food> foods = new ArrayList<>(32);
    private final ArrayList<Creature> prey = new ArrayList<>(16);
    private final ArrayList<Creature> threats = new ArrayList<>(16);

    public AISensor(World world) { this.world = world; }

    public void sense(AIEntity self, float senseRadius) {
        foods.clear();
        prey.clear();
        threats.clear();

        List<Entity> near = world.spatial.query(self.x, self.y, senseRadius);
        for (int i = 0; i < near.size(); i++) {
            Entity e = near.get(i);
            if (e == self || !e.alive) continue;
            if (e instanceof Food) {
                foods.add((Food) e);
            } else if (e instanceof Creature) {
                Creature c = (Creature) e;
                if (c.mass < self.mass) prey.add(c);
                else if (c.mass > self.mass) threats.add(c);
            }
        }
    }

    public ArrayList<Food> getFoods() { return foods; }
    public ArrayList<Creature> getPrey() { return prey; }
    public ArrayList<Creature> getThreats() { return threats; }
}
