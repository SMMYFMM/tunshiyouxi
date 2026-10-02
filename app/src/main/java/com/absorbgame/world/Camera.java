package com.absorbgame.world;

import com.absorbgame.core.GameConfig;

/** Camera that follows the target and zooms out as it grows. */
public class Camera {
    public float x, y;          // world position (center)
    public float zoom = 1f;     // 1 = default, smaller = zoomed out
    public float viewW, viewH;  // screen size in pixels

    public Camera(float viewW, float viewH) {
        this.viewW = viewW;
        this.viewH = viewH;
    }

    public void setViewport(float w, float h) {
        this.viewW = w;
        this.viewH = h;
    }

    public void update(float dt, float targetX, float targetY, float targetRadius) {
        // follow target smoothly
        float lerp = Math.min(1f, GameConfig.CAMERA_FOLLOW_SPEED * dt);
        x += (targetX - x) * lerp;
        y += (targetY - y) * lerp;

        // desired zoom: bigger blob -> zoom out. Clamp.
        float desired = GameConfig.CAMERA_MAX_ZOOM
                / (1f + 0.0006f * targetRadius * targetRadius);
        desired = Math.max(GameConfig.CAMERA_MIN_ZOOM, Math.min(GameConfig.CAMERA_MAX_ZOOM, desired));
        zoom += (desired - zoom) * Math.min(1f, GameConfig.CAMERA_ZOOM_SPEED * dt);
    }

    /** World half-width visible on screen. */
    public float halfViewW() { return (viewW * 0.5f) / zoom; }
    public float halfViewH() { return (viewH * 0.5f) / zoom; }

    public boolean isVisible(float ex, float ey, float er) {
        return ex + er > x - halfViewW() && ex - er < x + halfViewW()
                && ey + er > y - halfViewH() && ey - er < y + halfViewH();
    }
}
