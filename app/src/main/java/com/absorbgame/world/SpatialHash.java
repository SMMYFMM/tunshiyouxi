package com.absorbgame.world;

import com.absorbgame.entity.Entity;
import java.util.ArrayList;
import java.util.List;

/**
 * Uniform-grid spatial hash for fast range queries.
 * Entities register their cell each frame. Query returns entities whose cell
 * overlaps the query circle.
 */
public class SpatialHash {
    private final float cellSize;
    private final int cols, rows;
    private final ArrayList<Entity>[] cells;
    private final ArrayList<Entity> queryBuffer = new ArrayList<>(256);

    @SuppressWarnings("unchecked")
    public SpatialHash(float worldW, float worldH, float cellSize) {
        this.cellSize = cellSize;
        this.cols = (int) Math.ceil(worldW / cellSize) + 1;
        this.rows = (int) Math.ceil(worldH / cellSize) + 1;
        cells = new ArrayList[cols * rows];
        for (int i = 0; i < cells.length; i++) cells[i] = new ArrayList<>();
    }

    public void clear() {
        for (ArrayList<Entity> c : cells) c.clear();
    }

    public void insert(Entity e) {
        if (!e.alive) return;
        int cx = clamp((int) (e.x / cellSize), 0, cols - 1);
        int cy = clamp((int) (e.y / cellSize), 0, rows - 1);
        cells[cy * cols + cx].add(e);
    }

    /**
     * Returns all entities whose grid cells overlap the circle (cx,cy,r).
     * Caller must not store the returned list (reused each call).
     */
    public List<Entity> query(float cx, float cy, float radius) {
        queryBuffer.clear();
        int minX = clamp((int) ((cx - radius) / cellSize), 0, cols - 1);
        int maxX = clamp((int) ((cx + radius) / cellSize), 0, cols - 1);
        int minY = clamp((int) ((cy - radius) / cellSize), 0, rows - 1);
        int maxY = clamp((int) ((cy + radius) / cellSize), 0, rows - 1);
        for (int y = minY; y <= maxY; y++) {
            for (int x = minX; x <= maxX; x++) {
                ArrayList<Entity> cell = cells[y * cols + x];
                for (int i = 0; i < cell.size(); i++) {
                    Entity e = cell.get(i);
                    float dx = e.x - cx, dy = e.y - cy;
                    if (dx * dx + dy * dy <= (radius + e.radius) * (radius + e.radius)) {
                        queryBuffer.add(e);
                    }
                }
            }
        }
        return queryBuffer;
    }

    private static int clamp(int v, int lo, int hi) {
        return v < lo ? lo : (v > hi ? hi : v);
    }
}
