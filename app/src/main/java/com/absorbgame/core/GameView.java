package com.absorbgame.core;

import android.content.Context;
import android.opengl.GLSurfaceView;
import android.view.MotionEvent;

import com.absorbgame.input.Joystick;
import com.absorbgame.render.GameRenderer;

import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.opengles.GL10;

/**
 * GLSurfaceView that hosts the GL renderer and drives the game loop.
 * Touch input is captured here: left-half screen = joystick, top-right
 * corner = pause button.
 */
public class GameView extends GLSurfaceView implements GLSurfaceView.Renderer {
    private final GameEngine engine;
    private final GameRenderer renderer;
    private final Joystick joystick = new Joystick();

    private long lastFrameNanos = 0;
    private boolean pauseButtonDown = false;
    private float viewW = 1280, viewH = 720;

    public interface PauseListener { void onPauseRequested(); }
    private PauseListener pauseListener;

    public GameView(Context context, GameEngine engine) {
        super(context);
        this.engine = engine;
        setEGLContextClientVersion(2);
        setEGLConfigChooser(8, 8, 8, 8, 16, 0);
        this.renderer = new GameRenderer(engine);
        setRenderer(this);
        setRenderMode(GLSurfaceView.RENDERMODE_CONTINUOUSLY);
    }

    public void setPauseListener(PauseListener l) { this.pauseListener = l; }

    public Joystick getJoystick() { return joystick; }

    @Override
    public void onSurfaceCreated(GL10 gl, EGLConfig config) {
        renderer.onSurfaceCreated();
        lastFrameNanos = System.nanoTime();
    }

    @Override
    public void onSurfaceChanged(GL10 gl, int width, int height) {
        viewW = width;
        viewH = height;
        renderer.onSurfaceChanged(width, height);
    }

    @Override
    public void onDrawFrame(GL10 gl) {
        long now = System.nanoTime();
        float dt = Math.min(0.05f, (now - lastFrameNanos) / 1_000_000_000f);
        lastFrameNanos = now;

        // apply joystick input to player (thread-safe read)
        synchronized (joystick) {
            if (engine.world != null && engine.world.player != null
                    && engine.world.player.alive) {
                engine.world.player.setMoveInput(joystick.getX(), joystick.getY());
            }
        }

        engine.update(dt);
        renderer.onDrawFrame();
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        int action = event.getActionMasked();
        int idx = event.getActionIndex();
        int id = event.getPointerId(idx);
        float x = event.getX(idx);
        float y = event.getY(idx);

        switch (action) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN:
                // pause button in top-right corner
                if (x > viewW - 140 && y < 140) {
                    if (pauseListener != null) pauseListener.onPauseRequested();
                    return true;
                }
                // joystick only on left half
                if (x < viewW * 0.6f) {
                    synchronized (joystick) { joystick.onDown(x, y, id); }
                }
                break;
            case MotionEvent.ACTION_MOVE:
                for (int i = 0; i < event.getPointerCount(); i++) {
                    int pid = event.getPointerId(i);
                    synchronized (joystick) {
                        joystick.onMove(event.getX(i), event.getY(i), pid);
                    }
                }
                break;
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL:
                synchronized (joystick) { joystick.onUp(id); }
                break;
        }
        return true;
    }
}
