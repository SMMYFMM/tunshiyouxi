package com.absorbgame;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import com.absorbgame.audio.GameAudio;
import com.absorbgame.core.GameConfig;
import com.absorbgame.core.GameEngine;
import com.absorbgame.core.GameView;
import com.absorbgame.save.SaveManager;

/**
 * Single Activity hosting the GLSurfaceView game and all overlay UI
 * (main menu, HUD, pause, death, settings). Drives game-state transitions
 * and lifecycle (auto-pause + autosave on background).
 */
public class MainActivity extends Activity {

    private GameEngine engine;
    private GameView gameView;
    private SaveManager saveManager;
    private GameAudio audio;
    private final Handler uiHandler = new Handler(Looper.getMainLooper());

    private FrameLayout root;
    private View mainMenu, pauseMenu, deathScreen, settingsPanel, difficultyPanel;
    private TextView hudMass, hudRank, hudTime, hudFps;
    private GameConfig.Difficulty selectedDifficulty = GameConfig.Difficulty.NORMAL;

    private boolean musicOn = true, sfxOn = true, vibrateOn = true, showFps = false;
    private long lastFpsTime = 0;
    private int fpsFrames = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setFullscreen();

        engine = new GameEngine();
        saveManager = new SaveManager(getFilesDir());
        engine.setSaveManager(saveManager);
        audio = new GameAudio();

        root = new FrameLayout(this);
        setContentView(root);

        buildGameView();
        buildHud();
        buildMainMenu();
        buildDifficultyPanel();
        buildPauseMenu();
        buildDeathScreen();
        buildSettingsPanel();

        showScreen(mainMenu);

    }

    // ---------------- Game view ----------------
    private void buildGameView() {
        gameView = new GameView(this, engine);
        gameView.setPauseListener(() -> togglePause());
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        root.addView(gameView, lp);
    }

    // ---------------- HUD ----------------
    private void buildHud() {
        LinearLayout hud = new LinearLayout(this);
        hud.setOrientation(LinearLayout.VERTICAL);
        hud.setPadding(30, 30, 30, 30);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.TOP | Gravity.LEFT;
        hud.setLayoutParams(lp);

        hudMass = makeHudText("质量: 0", 28);
        hudRank = makeHudText("排名: #1", 24);
        hudTime = makeHudText("时间: 00:00", 24);
        hudFps = makeHudText("FPS: 0", 20);
        hudFps.setVisibility(showFps ? View.VISIBLE : View.GONE);
        hud.addView(hudMass);
        hud.addView(hudRank);
        hud.addView(hudTime);
        hud.addView(hudFps);
        root.addView(hud);
        hud.setVisibility(View.GONE);
        hudHolder = hud;
    }
    private View hudHolder;

    private TextView makeHudText(String text, float size) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextSize(size);
        t.setTextColor(Color.WHITE);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setShadowLayer(4, 2, 2, Color.BLACK);
        return t;
    }

    // ---------------- Main menu ----------------
    private void buildMainMenu() {
        LinearLayout menu = new LinearLayout(this);
        menu.setOrientation(LinearLayout.VERTICAL);
        menu.setGravity(Gravity.CENTER);
        menu.setBackgroundColor(0xEE000000);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT);
        menu.setLayoutParams(lp);

        TextView title = new TextView(this);
        title.setText("ABSORB");
        title.setTextSize(72);
        title.setTextColor(0xFF4FC3F7);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        title.setGravity(Gravity.CENTER);
        title.setPadding(0, 0, 0, 60);
        menu.addView(title);

        Button newGame = menuButton("开始游戏");
        Button cont = menuButton("继续游戏");
        Button saves = menuButton("存档管理");
        Button settings = menuButton("设置");
        Button exit = menuButton("退出");

        cont.setEnabled(saveManager.hasSave(SaveManager.SLOT_AUTO));
        cont.setTextColor(cont.isEnabled() ? Color.WHITE : Color.GRAY);

        newGame.setOnClickListener(v -> { audio.playSfx(GameAudio.SFX_UI); showScreen(difficultyPanel); });
        cont.setOnClickListener(v -> { audio.playSfx(GameAudio.SFX_UI); startFromSave(SaveManager.SLOT_AUTO); });
        saves.setOnClickListener(v -> { audio.playSfx(GameAudio.SFX_UI); showSaveManagement(); });
        settings.setOnClickListener(v -> { audio.playSfx(GameAudio.SFX_UI); showScreen(settingsPanel); });
        exit.setOnClickListener(v -> { audio.playSfx(GameAudio.SFX_UI); finishAffinity(); });

        menu.addView(newGame);
        menu.addView(cont);
        menu.addView(saves);
        menu.addView(settings);
        menu.addView(exit);
        root.addView(menu);
        mainMenu = menu;
    }

    // ---------------- Difficulty ----------------
    private void buildDifficultyPanel() {
        LinearLayout panel = centeredPanel("选择难度");
        RadioGroup group = new RadioGroup(this);
        group.setOrientation(RadioGroup.VERTICAL);
        group.setGravity(Gravity.CENTER);
        RadioButton easy = radio("简单", GameConfig.Difficulty.EASY == selectedDifficulty);
        RadioButton normal = radio("普通", true);
        RadioButton hard = radio("困难", false);
        group.addView(easy); group.addView(normal); group.addView(hard);
        group.setOnCheckedChangeListener((g, id) -> {
            if (id == easy.getId()) selectedDifficulty = GameConfig.Difficulty.EASY;
            else if (id == hard.getId()) selectedDifficulty = GameConfig.Difficulty.HARD;
            else selectedDifficulty = GameConfig.Difficulty.NORMAL;
        });
        panel.addView(group);

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        Button start = menuButton("开始");
        Button back = menuButton("返回");
        row.addView(start); row.addView(back);
        panel.addView(row);

        start.setOnClickListener(v -> { audio.playSfx(GameAudio.SFX_UI); startNewGame(); });
        back.setOnClickListener(v -> { audio.playSfx(GameAudio.SFX_UI); showScreen(mainMenu); });

        root.addView(panel);
        difficultyPanel = panel;
    }

    // ---------------- Pause menu ----------------
    private void buildPauseMenu() {
        LinearLayout panel = centeredPanel("已暂停");
        Button resume = menuButton("继续游戏");
        Button save = menuButton("保存游戏");
        Button save1 = menuButton("保存到 存档1");
        Button save2 = menuButton("保存到 存档2");
        Button save3 = menuButton("保存到 存档3");
        Button toMenu = menuButton("返回主菜单");

        resume.setOnClickListener(v -> { audio.playSfx(GameAudio.SFX_PAUSE); engine.resume(); hideAllOverlays(); startHudLoop(); });
        save.setOnClickListener(v -> { audio.playSfx(GameAudio.SFX_UI);
            boolean ok = engine.saveGame(SaveManager.SLOT_AUTO);
            toast(ok ? "已保存" : "保存失败"); });
        save1.setOnClickListener(v -> saveTo(SaveManager.SLOT_1));
        save2.setOnClickListener(v -> saveTo(SaveManager.SLOT_2));
        save3.setOnClickListener(v -> saveTo(SaveManager.SLOT_3));
        toMenu.setOnClickListener(v -> {
            audio.playSfx(GameAudio.SFX_UI);
            engine.saveGame(SaveManager.SLOT_AUTO);
            engine.state = GameEngine.GameState.MENU;
            showScreen(mainMenu);
            stopHudLoop();
        });

        panel.addView(resume); panel.addView(save);
        panel.addView(save1); panel.addView(save2); panel.addView(save3);
        panel.addView(toMenu);
        root.addView(panel);
        pauseMenu = panel;
    }

    private void saveTo(String slot) {
        audio.playSfx(GameAudio.SFX_UI);
        boolean ok = engine.saveGame(slot);
        toast(ok ? "已保存到 " + slot : "保存失败");
    }

    // ---------------- Death screen ----------------
    private void buildDeathScreen() {
        LinearLayout panel = centeredPanel("你被吞噬了");
        TextView stats = new TextView(this);
        stats.setTextColor(Color.WHITE);
        stats.setTextSize(28);
        stats.setGravity(Gravity.CENTER);
        stats.setPadding(0, 30, 0, 40);
        panel.addView(stats);
        deathStats = stats;

        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER);
        Button replay = menuButton("再来一局");
        Button menu = menuButton("返回主菜单");
        row.addView(replay); row.addView(menu);
        panel.addView(row);

        replay.setOnClickListener(v -> { audio.playSfx(GameAudio.SFX_UI); startNewGame(); });
        menu.setOnClickListener(v -> { audio.playSfx(GameAudio.SFX_UI); engine.state = GameEngine.GameState.MENU; showScreen(mainMenu); stopHudLoop(); });

        root.addView(panel);
        deathScreen = panel;
    }
    private TextView deathStats;

    // ---------------- Settings ----------------
    private void buildSettingsPanel() {
        LinearLayout panel = centeredPanel("设置");
        Switch music = switchRow("音乐", musicOn, panel);
        Switch sfx = switchRow("音效", sfxOn, panel);
        Switch vib = switchRow("震动", vibrateOn, panel);
        Switch fps = switchRow("FPS 显示", showFps, panel);

        music.setOnCheckedChangeListener((b, c) -> { musicOn = c; audio.setMusicEnabled(c); if (c) audio.startBgm(); else audio.stopBgm(); });
        sfx.setOnCheckedChangeListener((b, c) -> { sfxOn = c; audio.setSfxEnabled(c); });
        vib.setOnCheckedChangeListener((b, c) -> vibrateOn = c);
        fps.setOnCheckedChangeListener((b, c) -> { showFps = c; hudFps.setVisibility(c ? View.VISIBLE : View.GONE); });

        Button back = menuButton("返回");
        back.setOnClickListener(v -> { audio.playSfx(GameAudio.SFX_UI);
            showScreen(engine.state == GameEngine.GameState.MENU ? mainMenu : pauseMenu); });
        panel.addView(back);
        root.addView(panel);
        settingsPanel = panel;
    }

    // ---------------- Save management ----------------
    private void showSaveManagement() {
        LinearLayout panel = centeredPanel("存档管理");
        String[] slots = {SaveManager.SLOT_AUTO, SaveManager.SLOT_1, SaveManager.SLOT_2, SaveManager.SLOT_3};
        for (String slot : slots) {
            LinearLayout row = new LinearLayout(this);
            row.setGravity(Gravity.CENTER);
            boolean has = saveManager.hasSave(slot);
            TextView label = makeHudText(slot + (has ? " (存在)" : " (空)"), 22);
            label.setPadding(0, 10, 30, 10);
            row.addView(label);
            Button load = menuButton("读取");
            load.setEnabled(has);
            load.setOnClickListener(v -> startFromSave(slot));
            Button del = menuButton("删除");
            del.setEnabled(has);
            del.setOnClickListener(v -> {
                java.io.File f = new java.io.File(getFilesDir(), "saves/" + slot + ".json");
                if (f.delete()) toast("已删除 " + slot);
                showSaveManagement();
            });
            row.addView(load); row.addView(del);
            panel.addView(row);
        }
        Button back = menuButton("返回");
        back.setOnClickListener(v -> showScreen(mainMenu));
        panel.addView(back);
        if (saveMgmtPanel != null) root.removeView(saveMgmtPanel);
        root.addView(panel);
        saveMgmtPanel = panel;
        showScreen(panel);
    }
    private View saveMgmtPanel;

    // ---------------- Game flow ----------------
    private void startNewGame() {
        hideAllOverlays();
        long seed = System.nanoTime();
        engine.newGame(selectedDifficulty, seed, 1280, 720);
        startHudLoop();
        if (musicOn) audio.startBgm();
    }

    private void startFromSave(String slot) {
        hideAllOverlays();
        boolean ok = engine.loadGame(slot, 1280, 720);
        if (ok) {
            startHudLoop();
            if (musicOn) audio.startBgm();
            toast("已读取存档");
        } else {
            toast("读取失败");
            showScreen(mainMenu);
        }
    }

    private void togglePause() {
        if (engine.state == GameEngine.GameState.PLAYING) {
            engine.pause();
            audio.playSfx(GameAudio.SFX_PAUSE);
            engine.saveGame(SaveManager.SLOT_AUTO); // save on pause
            showScreen(pauseMenu);
            stopHudLoop();
        } else if (engine.state == GameEngine.GameState.PAUSED) {
            engine.resume();
            audio.playSfx(GameAudio.SFX_PAUSE);
            hideAllOverlays();
            startHudLoop();
        }
    }

    // ---------------- HUD loop ----------------
    private final Runnable hudRunnable = new Runnable() {
        @Override
        public void run() {
            if (engine.state == GameEngine.GameState.PLAYING && engine.world != null) {
                float mass = engine.world.player.mass;
                hudMass.setText(String.format("质量: %.0f", mass));
                hudRank.setText("排名: #" + engine.getPlayerRank() + " / " + engine.getTotalAlive());
                int sec = (int) engine.world.gameTime;
                hudTime.setText(String.format("时间: %02d:%02d", sec / 60, sec % 60));
            }
            if (engine.state == GameEngine.GameState.DEAD) {
                showDeathStats();
                stopHudLoop();
                return;
            }
            if (showFps) {
                long now = System.currentTimeMillis();
                if (lastFpsTime == 0) lastFpsTime = now;
                fpsFrames++;
                if (now - lastFpsTime >= 500) {
                    int fps = (int) (fpsFrames * 1000f / (now - lastFpsTime));
                    hudFps.setText("FPS: " + fps);
                    fpsFrames = 0;
                    lastFpsTime = now;
                }
            }
            uiHandler.postDelayed(this, 100);
        }
    };

    private void startHudLoop() {
        hudHolder.setVisibility(View.VISIBLE);
        lastFpsTime = 0; fpsFrames = 0;
        uiHandler.removeCallbacks(hudRunnable);
        uiHandler.post(hudRunnable);
    }
    private void stopHudLoop() {
        uiHandler.removeCallbacks(hudRunnable);
    }

    private void showDeathStats() {
        if (engine.world == null || engine.world.player == null) return;
        int sec = (int) engine.world.player.survivalTime;
        deathStats.setText(String.format(
                "生存时间: %02d:%02d\n最大质量: %.0f\n吞噬数量: %d\n最终排名: #%d",
                sec / 60, sec % 60,
                engine.world.player.maxMass,
                engine.world.player.killCount,
                engine.getPlayerRank()));
        audio.playSfx(GameAudio.SFX_DEATH);
        showScreen(deathScreen);
        hudHolder.setVisibility(View.GONE);
    }

    // ---------------- Lifecycle ----------------
    @Override
    protected void onPause() {
        super.onPause();
        // auto-pause + autosave when app goes to background
        if (engine.state == GameEngine.GameState.PLAYING) {
            engine.pause();
            engine.saveGame(SaveManager.SLOT_AUTO);
        }
        audio.stopBgm();
    }

    @Override
    protected void onResume() {
        super.onResume();
        setFullscreen();
        // stay paused; player must tap resume
        if (engine.state == GameEngine.GameState.PAUSED) {
            showScreen(pauseMenu);
        }
        if (musicOn && engine.state == GameEngine.GameState.PLAYING) audio.startBgm();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (engine.state == GameEngine.GameState.PLAYING || engine.state == GameEngine.GameState.PAUSED) {
            engine.saveGame(SaveManager.SLOT_AUTO);
        }
        audio.stopBgm();
    }

    @Override
    public void onBackPressed() {
        if (engine.state == GameEngine.GameState.PLAYING) togglePause();
        else if (engine.state == GameEngine.GameState.PAUSED) togglePause();
        else super.onBackPressed();
    }

    // ---------------- UI helpers ----------------
    private void showScreen(View screen) {
        hideAllOverlays();
        if (screen != null) screen.setVisibility(View.VISIBLE);
    }
    private void hideAllOverlays() {
        for (int i = 0; i < root.getChildCount(); i++) {
            View v = root.getChildAt(i);
            if (v != gameView && v != hudHolder) v.setVisibility(View.GONE);
        }
        if (engine.state == GameEngine.GameState.PLAYING) hudHolder.setVisibility(View.VISIBLE);
        else hudHolder.setVisibility(View.GONE);
    }

    private Button menuButton(String text) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(24);
        b.setTextColor(Color.WHITE);
        b.setTypeface(Typeface.DEFAULT_BOLD);
        b.setBackgroundColor(0x33333333);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                420, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 12, 0, 12);
        b.setLayoutParams(lp);
        b.setPadding(20, 18, 20, 18);
        return b;
    }

    private LinearLayout centeredPanel(String title) {
        LinearLayout p = new LinearLayout(this);
        p.setOrientation(LinearLayout.VERTICAL);
        p.setGravity(Gravity.CENTER);
        p.setBackgroundColor(0xEE000000);
        p.setLayoutParams(new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        TextView t = new TextView(this);
        t.setText(title);
        t.setTextSize(44);
        t.setTextColor(Color.WHITE);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, 0, 0, 30);
        p.addView(t);
        p.setVisibility(View.GONE);
        return p;
    }

    private RadioButton radio(String text, boolean checked) {
        RadioButton r = new RadioButton(this);
        r.setText(text);
        r.setTextSize(28);
        r.setTextColor(Color.WHITE);
        r.setChecked(checked);
        return r;
    }

    private Switch switchRow(String label, boolean checked, LinearLayout parent) {
        LinearLayout row = new LinearLayout(this);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(40, 10, 40, 10);
        TextView t = new TextView(this);
        t.setText(label);
        t.setTextSize(26);
        t.setTextColor(Color.WHITE);
        t.setPadding(0, 0, 40, 0);
        row.addView(t);
        Switch s = new Switch(this);
        s.setChecked(checked);
        s.setTextColor(Color.WHITE);
        s.setTextSize(22);
        row.addView(s);
        parent.addView(row);
        return s;
    }

    private void toast(String msg) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
    }

    private void setFullscreen() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            getWindow().getAttributes().layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
        }
    }
}
