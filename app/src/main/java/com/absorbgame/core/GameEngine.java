package com.absorbgame.core;

import com.absorbgame.save.SaveManager;
import com.absorbgame.world.AIManager;
import com.absorbgame.world.CollisionManager;
import com.absorbgame.world.FoodManager;
import com.absorbgame.world.RankingManager;
import com.absorbgame.world.World;

/**
 * Central game state machine. Owns the World and its managers, runs the
 * simulation update, and exposes lifecycle hooks (new game, pause, resume,
 * save, load).
 *
 * GameState: MENU, PLAYING, PAUSED, DEAD.
 */
public class GameEngine {
    public enum GameState { MENU, PLAYING, PAUSED, DEAD }

    public GameState state = GameState.MENU;
    public World world;
    public GameConfig.Difficulty difficulty = GameConfig.Difficulty.NORMAL;

    public AIManager aiManager;
    public FoodManager foodManager;
    public CollisionManager collisionManager;
    public RankingManager rankingManager;

    /** Guards all world mutation & serialization. Held by GL thread during
     *  simulation and by any thread during save/load. */
    public final Object worldLock = new Object();

    private SaveManager saveManager;
    private float autosaveTimer = 0f;

    public GameEngine() {}

    public void setSaveManager(SaveManager sm) { this.saveManager = sm; }

    /** Start a brand-new game with the given difficulty and seed. */
    public void newGame(GameConfig.Difficulty diff, long seed, float viewW, float viewH) {
        synchronized (worldLock) {
            this.difficulty = diff;
            world = new World(seed, viewW, viewH);
            aiManager = new AIManager(world);
            foodManager = new FoodManager(world);
            collisionManager = new CollisionManager(world);
            rankingManager = new RankingManager(world);

            aiManager.setDifficulty(diff);
            world.spawnPlayer();
            foodManager.ensureFoodCount();
            aiManager.spawnInitial();
            world.rebuildSpatial();
        }
        autosaveTimer = 0f;
        state = GameState.PLAYING;
    }

    /** Advance the simulation by dt seconds. Frozen when not PLAYING. */
    public void update(float dt) {
        if (state != GameState.PLAYING) return;
        if (world == null) return;
        synchronized (worldLock) {
            world.gameTime += dt;

            if (world.player != null && world.player.alive) {
                world.player.update(dt, world.width, world.height);
            }

            aiManager.update(dt);
            foodManager.compact();

            world.rebuildSpatial();
            collisionManager.resolve();

            foodManager.compact();
            foodManager.ensureFoodCount();

            if (world.player != null) {
                world.camera.update(dt, world.player.x, world.player.y, world.player.radius);
            }

            if (world.player != null && !world.player.alive) {
                state = GameState.DEAD;
            }

            autosaveTimer += dt;
            if (autosaveTimer >= GameConfig.AUTOSAVE_INTERVAL && saveManager != null) {
                autosaveTimer = 0f;
                saveManager.save(this, SaveManager.SLOT_AUTO);
            }
        }
    }

    /** Synchronized save (safe to call from any thread). */
    public boolean saveGame(String slot) {
        if (saveManager == null) return false;
        synchronized (worldLock) {
            return saveManager.save(this, slot);
        }
    }

    /** Synchronized load (safe to call from any thread). */
    public boolean loadGame(String slot, float viewW, float viewH) {
        if (saveManager == null) return false;
        synchronized (worldLock) {
            return saveManager.load(this, slot, viewW, viewH);
        }
    }

    public void pause() {
        if (state == GameState.PLAYING) state = GameState.PAUSED;
    }

    public void resume() {
        if (state == GameState.PAUSED) state = GameState.PLAYING;
    }

    public int getPlayerRank() {
        if (rankingManager == null) return 0;
        synchronized (worldLock) { return rankingManager.getPlayerRank(); }
    }

    public int getTotalAlive() {
        if (rankingManager == null) return 0;
        synchronized (worldLock) { return rankingManager.getTotalAlive(); }
    }

    public long getSeed() { return world != null ? world.random.getSeed() : 0L; }
}
