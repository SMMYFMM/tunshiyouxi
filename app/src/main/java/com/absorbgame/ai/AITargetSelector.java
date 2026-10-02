package com.absorbgame.ai;

import com.absorbgame.core.GameConfig;
import com.absorbgame.entity.AIEntity;
import com.absorbgame.entity.Creature;
import com.absorbgame.entity.Entity;
import com.absorbgame.entity.Food;

import java.util.List;

/**
 * Scores candidate targets using a weighted utility formula:
 *   score = FoodValue + Distance + MassDifference + Safety + Opportunity
 * Returns the highest-scoring target (or null).
 */
public class AITargetSelector {

    public Entity selectFoodTarget(AIEntity self, List<Food> foods) {
        if (foods.isEmpty()) return null;
        Food best = null;
        float bestScore = -Float.MAX_VALUE;
        float maxDist = 1200f;
        for (int i = 0; i < foods.size(); i++) {
            Food f = foods.get(i);
            float dx = f.x - self.x, dy = f.y - self.y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            float distScore = 1f - Math.min(1f, dist / maxDist);
            float valueScore = f.value / GameConfig.FOOD_VALUE_MAX; // 0..1
            float score = valueScore * 0.4f + distScore * 0.6f;
            if (score > bestScore) { bestScore = score; best = f; }
        }
        return best;
    }

    public Creature selectPreyTarget(AIEntity self, List<Creature> prey) {
        if (prey.isEmpty()) return null;
        Creature best = null;
        float bestScore = -Float.MAX_VALUE;
        float maxDist = 1500f;
        for (int i = 0; i < prey.size(); i++) {
            Creature c = prey.get(i);
            if (!self.canEat(c)) continue; // only chase edible
            float dx = c.x - self.x, dy = c.y - self.y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            float distScore = 1f - Math.min(1f, dist / maxDist);
            // mass difference: bigger gap = easier catch = more reward
            float massRatio = c.mass / self.mass; // 0..~0.87
            float massScore = 1f - massRatio;     // smaller prey = higher
            // opportunity: how much mass we'd gain
            float gainScore = Math.min(1f, c.mass / 200f);
            // safety: fewer nearby threats = safer (approx via own caution handled in decision)
            float score = massScore * 0.35f + distScore * 0.35f + gainScore * 0.3f;
            score *= (0.5f + self.personality.aggression * 0.5f);
            if (score > bestScore) { bestScore = score; best = c; }
        }
        return best;
    }

    /** Pick the most dangerous nearby threat (biggest, closest). */
    public Creature selectPrimaryThreat(AIEntity self, List<Creature> threats) {
        if (threats.isEmpty()) return null;
        Creature worst = null;
        float worstScore = -Float.MAX_VALUE;
        for (int i = 0; i < threats.size(); i++) {
            Creature t = threats.get(i);
            if (t.mass <= self.mass * GameConfig.DANGER_RATIO) continue;
            float dx = t.x - self.x, dy = t.y - self.y;
            float dist = (float) Math.sqrt(dx * dx + dy * dy);
            float sizeScore = t.mass / self.mass; // bigger = more dangerous
            float closeness = 1f / (1f + dist * 0.002f);
            float score = sizeScore * 0.6f + closeness * 0.4f;
            if (score > worstScore) { worstScore = score; worst = t; }
        }
        return worst;
    }
}
