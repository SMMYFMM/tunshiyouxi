package com.absorbgame.render;

import android.opengl.GLES20;
import android.opengl.GLUtils;

import com.absorbgame.core.GameConfig;
import com.absorbgame.entity.AIEntity;
import com.absorbgame.entity.Entity;
import com.absorbgame.entity.Food;
import com.absorbgame.entity.Player;
import com.absorbgame.core.GameEngine;
import com.absorbgame.world.World;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

/**
 * OpenGL ES 2.0 renderer. Draws all entities (food, AI, player) as textured
 * circles in a single batched draw call. Generates the circle texture
 * procedurally so no external assets are needed.
 */
public class GameRenderer {
    private final GameEngine engine;
    private World world; // current world (refreshed each frame)

    private int program;
    private int circleTexture;
    private int vboId;

    // vertex buffer: MAX_BATCH_QUADS * 6 verts * 8 floats (x,y,u,v,r,g,b,a)
    private FloatBuffer vertexBuffer;
    private final int maxVerts = GameConfig.MAX_BATCH_QUADS * 6;
    private final float[] vertexData = new float[maxVerts * 8];
    private int vertexCount = 0;

    // uniforms/attributes
    private int aPosition, aColor, aTexCoord;
    private int uViewProj, uTexture;

    // matrix
    private final float[] viewProj = new float[16];

    public GameRenderer(GameEngine engine) {
        this.engine = engine;
    }

    public void onSurfaceCreated() {
        GLES20.glClearColor(
                GameConfig.COLOR_BG_TOP[0],
                GameConfig.COLOR_BG_TOP[1],
                GameConfig.COLOR_BG_TOP[2], 1f);
        GLES20.glEnable(GLES20.GL_BLEND);
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA);

        program = buildProgram(VERTEX_SHADER, FRAGMENT_SHADER);
        aPosition = GLES20.glGetAttribLocation(program, "aPosition");
        aColor    = GLES20.glGetAttribLocation(program, "aColor");
        aTexCoord = GLES20.glGetAttribLocation(program, "aTexCoord");
        uViewProj  = GLES20.glGetUniformLocation(program, "uViewProj");
        uTexture   = GLES20.glGetUniformLocation(program, "uTexture");

        circleTexture = genCircleTexture(128);

        // VBO
        int[] vbos = new int[1];
        GLES20.glGenBuffers(1, vbos, 0);
        vboId = vbos[0];
        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);
        GLES20.glBufferData(GLES20.GL_ARRAY_BUFFER,
                vertexData.length * 4, null, GLES20.GL_DYNAMIC_DRAW);

        vertexBuffer = ByteBuffer.allocateDirect(vertexData.length * 4)
                .order(ByteOrder.nativeOrder()).asFloatBuffer();
    }

    public void onSurfaceChanged(int width, int height) {
        GLES20.glViewport(0, 0, width, height);
        world = engine.world;
        if (world != null && world.camera != null) world.camera.setViewport(width, height);
    }

    public void onDrawFrame() {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT);
        world = engine.world;
        if (world == null || world.player == null) return;

        // build view-proj (orthographic, centered on camera, scaled by zoom)
        float zoom = world.camera.zoom;
        float hw = (world.camera.viewW * 0.5f) / zoom;
        float hh = (world.camera.viewH * 0.5f) / zoom;
        float cx = world.camera.x, cy = world.camera.y;
        ortho(viewProj, cx - hw, cx + hw, cy - hh, cy + hh, -1f, 1f);

        GLES20.glUseProgram(program);
        GLES20.glUniformMatrix4fv(uViewProj, 1, false, viewProj, 0);
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, circleTexture);
        GLES20.glUniform1i(uTexture, 0);

        GLES20.glBindBuffer(GLES20.GL_ARRAY_BUFFER, vboId);
        vertexCount = 0;

        // draw order: food, then AI (smaller first so big ones on top), then player
        // food
        for (int i = 0; i < world.foodList.size(); i++) {
            Food f = world.foodList.get(i);
            if (!f.alive) continue;
            if (!world.camera.isVisible(f.x, f.y, f.radius)) continue;
            addCircle(f.x, f.y, f.radius, f.r, f.g, f.b, f.a);
        }
        // AI
        for (int i = 0; i < world.aiList.size(); i++) {
            AIEntity ai = world.aiList.get(i);
            if (!ai.alive) continue;
            if (!world.camera.isVisible(ai.x, ai.y, ai.radius)) continue;
            addCircle(ai.x, ai.y, ai.radius, ai.r, ai.g, ai.b, ai.a);
        }
        // player
        Player p = world.player;
        if (p.alive) {
            addCircle(p.x, p.y, p.radius, p.r, p.g, p.b, p.a);
        }

        // world border
        addBorder();

        flush();
    }

    private void addCircle(float x, float y, float r, float cr, float cg, float cb, float ca) {
        if (vertexCount >= maxVerts) flush();
        int idx = vertexCount * 8;
        // two triangles: (0,1,2) and (0,2,3)
        float x0 = x - r, y0 = y - r;
        float x1 = x + r, y1 = y + r;
        // TL, BL, BR, TR
        // tri1: TL BL BR
        put(idx,     x0, y1, 0, 1, cr, cg, cb, ca);
        put(idx + 8, x0, y0, 0, 0, cr, cg, cb, ca);
        put(idx + 16, x1, y0, 1, 0, cr, cg, cb, ca);
        // tri2: TL BR TR
        put(idx + 24, x0, y1, 0, 1, cr, cg, cb, ca);
        put(idx + 32, x1, y0, 1, 0, cr, cg, cb, ca);
        put(idx + 40, x1, y1, 1, 1, cr, cg, cb, ca);
        vertexCount += 6;
    }

    private void put(int idx, float x, float y, float u, float v,
                     float r, float g, float b, float a) {
        vertexData[idx] = x;
        vertexData[idx + 1] = y;
        vertexData[idx + 2] = u;
        vertexData[idx + 3] = v;
        vertexData[idx + 4] = r;
        vertexData[idx + 5] = g;
        vertexData[idx + 6] = b;
        vertexData[idx + 7] = a;
    }

    private void addBorder() {
        // 4 thick-ish lines around world bounds using thin circles won't do;
        // instead draw 4 rectangles manually. Keep simple: draw corner circles.
        // For simplicity we draw border as 4 stretched quads.
        float t = 15f; // border thickness
        float[] c = GameConfig.COLOR_BORDER;
        addRect(0, 0, world.width, t, c);
        addRect(0, world.height - t, world.width, t, c);
        addRect(0, 0, t, world.height, c);
        addRect(world.width - t, 0, t, world.height, c);
    }

    private void addRect(float x, float y, float w, float h, float[] c) {
        if (vertexCount >= maxVerts) flush();
        int idx = vertexCount * 8;
        float x1 = x + w, y1 = y + h;
        // sample texture center (opaque white) for solid color fill
        float u = 0.5f, v = 0.5f;
        put(idx,      x, y, u, v, c[0], c[1], c[2], c[3]);
        put(idx + 8,  x, y1, u, v, c[0], c[1], c[2], c[3]);
        put(idx + 16, x1, y, u, v, c[0], c[1], c[2], c[3]);
        put(idx + 24, x, y1, u, v, c[0], c[1], c[2], c[3]);
        put(idx + 32, x1, y1, u, v, c[0], c[1], c[2], c[3]);
        put(idx + 40, x1, y, u, v, c[0], c[1], c[2], c[3]);
        vertexCount += 6;
    }

    private void flush() {
        if (vertexCount == 0) return;
        vertexBuffer.clear();
        vertexBuffer.put(vertexData, 0, vertexCount * 8);
        vertexBuffer.position(0);

        GLES20.glBufferSubData(GLES20.GL_ARRAY_BUFFER, 0,
                vertexCount * 8 * 4, vertexBuffer);

        int stride = 8 * 4;
        GLES20.glEnableVertexAttribArray(aPosition);
        GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_FLOAT, false, stride, 0);
        GLES20.glEnableVertexAttribArray(aColor);
        GLES20.glVertexAttribPointer(aColor, 4, GLES20.GL_FLOAT, false, stride, 2 * 4);
        GLES20.glEnableVertexAttribArray(aTexCoord);
        GLES20.glVertexAttribPointer(aTexCoord, 2, GLES20.GL_FLOAT, false, stride, 6 * 4);

        GLES20.glDrawArrays(GLES20.GL_TRIANGLES, 0, vertexCount);
        vertexCount = 0;
    }

    /** Generate a radial-gradient circle texture. */
    private int genCircleTexture(int size) {
        int[] pixels = new int[size * size];
        float cx = size * 0.5f, cy = size * 0.5f;
        float maxR = size * 0.5f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = x + 0.5f - cx;
                float dy = y + 0.5f - cy;
                float d = (float) Math.sqrt(dx * dx + dy * dy);
                int alpha;
                int rgb;
                if (d > maxR) {
                    alpha = 0;
                    rgb = 0;
                } else {
                    float t = d / maxR; // 0 center .. 1 edge
                    alpha = (int) (255 * Math.max(0, 1f - (float) Math.pow(t, 2.5)));
                    // bright center, darker edge (lighting)
                    float light = 1f - t * 0.55f;
                    int l = (int) (255 * light);
                    rgb = (l << 16) | (l << 8) | l;
                }
                pixels[y * size + x] = (alpha << 24) | (rgb & 0x00FFFFFF);
            }
        }
        int[] tex = new int[1];
        GLES20.glGenTextures(1, tex, 0);
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, tex[0]);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE);
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE);
        android.graphics.Bitmap bmp = android.graphics.Bitmap.createBitmap(pixels, size, size,
                android.graphics.Bitmap.Config.ARGB_8888);
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0);
        bmp.recycle();
        return tex[0];
    }

    private static int buildProgram(String vs, String fs) {
        int v = compile(GLES20.GL_VERTEX_SHADER, vs);
        int f = compile(GLES20.GL_FRAGMENT_SHADER, fs);
        int p = GLES20.glCreateProgram();
        GLES20.glAttachShader(p, v);
        GLES20.glAttachShader(p, f);
        GLES20.glLinkProgram(p);
        int[] linked = new int[1];
        GLES20.glGetProgramiv(p, GLES20.GL_LINK_STATUS, linked, 0);
        if (linked[0] == 0) {
            throw new RuntimeException("Program link failed: " + GLES20.glGetProgramInfoLog(p));
        }
        return p;
    }

    private static int compile(int type, String src) {
        int s = GLES20.glCreateShader(type);
        GLES20.glShaderSource(s, src);
        GLES20.glCompileShader(s);
        int[] ok = new int[1];
        GLES20.glGetShaderiv(s, GLES20.GL_COMPILE_STATUS, ok, 0);
        if (ok[0] == 0) {
            throw new RuntimeException("Shader compile failed: " + GLES20.glGetShaderInfoLog(s));
        }
        return s;
    }

    private static final String VERTEX_SHADER =
            "uniform mat4 uViewProj;\n" +
            "attribute vec2 aPosition;\n" +
            "attribute vec4 aColor;\n" +
            "attribute vec2 aTexCoord;\n" +
            "varying vec4 vColor;\n" +
            "varying vec2 vTexCoord;\n" +
            "void main() {\n" +
            "  gl_Position = uViewProj * vec4(aPosition, 0.0, 1.0);\n" +
            "  vColor = aColor;\n" +
            "  vTexCoord = aTexCoord;\n" +
            "}\n";

    private static final String FRAGMENT_SHADER =
            "precision mediump float;\n" +
            "uniform sampler2D uTexture;\n" +
            "varying vec4 vColor;\n" +
            "varying vec2 vTexCoord;\n" +
            "void main() {\n" +
            "  vec4 t = texture2D(uTexture, vTexCoord);\n" +
            "  gl_FragColor = t * vColor;\n" +
            "}\n";

    /** Simple column-major ortho matrix (matches OpenGL convention). */
    private static void ortho(float[] m, float l, float r, float b, float t, float n, float f) {
        // identity
        for (int i = 0; i < 16; i++) m[i] = 0;
        m[0] = 2f / (r - l);
        m[5] = 2f / (t - b);
        m[10] = -2f / (f - n);
        m[12] = -(r + l) / (r - l);
        m[13] = -(t + b) / (t - b);
        m[14] = -(f + n) / (f - n);
        m[15] = 1f;
    }
}
