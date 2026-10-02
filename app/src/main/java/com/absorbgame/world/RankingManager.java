package com.absorbgame.world;

import com.absorbgame.entity.AIEntity;

/** Computes the player's rank by mass across all creatures. */
public class RankingManager {
    private final World world;

    public RankingManager(World world) { this.world = world; }

    /** Returns 1-based rank of the player by mass. */
    public int getPlayerRank() {
        if (world.player == null) return 0;
        int rank = 1;
        float pm = world.player.mass;
        for (int i = 0; i < world.aiList.size(); i++) {
            AIEntity ai = world.aiList.get(i);
            if (ai.alive && ai.mass > pm) rank++;
        }
        return rank;
    }

    public int getTotalAlive() {
        int c = world.player != null && world.player.alive ? 1 : 0;
        for (int i = 0; i < world.aiList.size(); i++) {
            if (world.aiList.get(i).alive) c++;
        }
        return c;
    }
}
