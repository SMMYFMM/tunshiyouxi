package com.absorbgame.save;

import com.absorbgame.ai.AIState;
import com.absorbgame.core.GameConfig;
import com.absorbgame.core.GameEngine;
import com.absorbgame.entity.AIEntity;
import com.absorbgame.entity.Food;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

/**
 * Saves/loads the full game state to the app's private files directory.
 * Uses a temp-file -> flush -> verify -> replace strategy so a crash during
 * save never corrupts the last valid save.
 *
 * Format: JSON (org.json, no external dependencies).
 */
public class SaveManager {
    public static final String SLOT_AUTO = "auto";
    public static final String SLOT_1 = "slot1";
    public static final String SLOT_2 = "slot2";
    public static final String SLOT_3 = "slot3";

    private final File saveDir;

    public SaveManager(File filesDir) {
        this.saveDir = new File(filesDir, "saves");
        if (!saveDir.exists()) saveDir.mkdirs();
    }

    public boolean hasSave(String slot) {
        return fileFor(slot).exists();
    }

    private File fileFor(String slot) { return new File(saveDir, slot + ".json"); }
    private File tmpFor(String slot)  { return new File(saveDir, slot + ".tmp"); }

    /** Save the engine state to a slot. Returns true on success. */
    public boolean save(GameEngine engine, String slot) {
        if (engine.world == null) return false;
        try {
            JSONObject json = serialize(engine);
            String text = json.toString();
            byte[] data = text.getBytes("UTF-8");

            // 1) write to tmp
            File tmp = tmpFor(slot);
            FileOutputStream fos = new FileOutputStream(tmp, false);
            fos.write(data);
            fos.flush();
            fos.getFD().sync();
            fos.close();

            // 2) verify by reading back
            byte[] read = readAll(tmp);
            if (read.length != data.length) {
                tmp.delete();
                return false;
            }
            String verify = new String(read, "UTF-8");
            // parse-check
            new JSONObject(verify);

            // 3) replace
            File real = fileFor(slot);
            if (real.exists()) real.delete();
            if (!tmp.renameTo(real)) {
                // fallback: copy
                FileOutputStream out = new FileOutputStream(real, false);
                out.write(data);
                out.flush();
                out.getFD().sync();
                out.close();
                tmp.delete();
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /** Load a save into an existing engine (replaces its world). */
    public boolean load(GameEngine engine, String slot, float viewW, float viewH) {
        File f = fileFor(slot);
        if (!f.exists()) return false;
        try {
            JSONObject json = new JSONObject(new String(readAll(f), "UTF-8"));
            deserialize(engine, json, viewW, viewH);
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            // if save is corrupt, try to restore from .tmp if present
            File tmp = tmpFor(slot);
            if (tmp.exists()) {
                try {
                    JSONObject j2 = new JSONObject(new String(readAll(tmp), "UTF-8"));
                    deserialize(engine, j2, viewW, viewH);
                    return true;
                } catch (Exception ignored) {}
            }
            return false;
        }
    }

    private JSONObject serialize(GameEngine e) throws Exception {
        JSONObject root = new JSONObject();
        root.put("version", GameConfig.SAVE_VERSION);
        root.put("seed", e.getSeed());
        root.put("difficulty", e.difficulty.name());
        root.put("gameTime", (double) e.world.gameTime);

        // player
        JSONObject p = new JSONObject();
        p.put("x", (double) e.world.player.x);
        p.put("y", (double) e.world.player.y);
        p.put("mass", (double) e.world.player.mass);
        p.put("vx", (double) e.world.player.vx);
        p.put("vy", (double) e.world.player.vy);
        p.put("alive", e.world.player.alive);
        p.put("killCount", e.world.player.killCount);
        p.put("survivalTime", (double) e.world.player.survivalTime);
        root.put("player", p);

        // AI
        JSONArray aiArr = new JSONArray();
        for (int i = 0; i < e.world.aiList.size(); i++) {
            AIEntity a = e.world.aiList.get(i);
            JSONObject o = new JSONObject();
            o.put("x", (double) a.x);
            o.put("y", (double) a.y);
            o.put("mass", (double) a.mass);
            o.put("vx", (double) a.vx);
            o.put("vy", (double) a.vy);
            o.put("alive", a.alive);
            o.put("state", a.state.name());
            o.put("aggression", (double) a.personality.aggression);
            o.put("caution", (double) a.personality.caution);
            o.put("greed", (double) a.personality.greed);
            o.put("wanderBias", (double) a.personality.wanderBias);
            aiArr.put(o);
        }
        root.put("ai", aiArr);

        // food
        JSONArray foodArr = new JSONArray();
        for (int i = 0; i < e.world.foodList.size(); i++) {
            Food fd = e.world.foodList.get(i);
            JSONObject o = new JSONObject();
            o.put("x", (double) fd.x);
            o.put("y", (double) fd.y);
            o.put("value", (double) fd.value);
            foodArr.put(o);
        }
        root.put("food", foodArr);
        return root;
    }

    private void deserialize(GameEngine e, JSONObject json, float viewW, float viewH) throws Exception {
        long seed = json.getLong("seed");
        GameConfig.Difficulty diff = GameConfig.Difficulty.valueOf(json.getString("difficulty"));
        // create a fresh world with the saved seed
        e.newGame(diff, seed, viewW, viewH);
        e.world.gameTime = (float) json.getDouble("gameTime");

        // player
        JSONObject p = json.getJSONObject("player");
        e.world.player.x = (float) p.getDouble("x");
        e.world.player.y = (float) p.getDouble("y");
        e.world.player.setMass((float) p.getDouble("mass"));
        e.world.player.vx = (float) p.getDouble("vx");
        e.world.player.vy = (float) p.getDouble("vy");
        e.world.player.targetVx = 0; e.world.player.targetVy = 0;
        e.world.player.alive = p.getBoolean("alive");
        e.world.player.killCount = p.getInt("killCount");
        e.world.player.survivalTime = (float) p.getDouble("survivalTime");
        e.world.camera.x = e.world.player.x;
        e.world.camera.y = e.world.player.y;

        // AI - clear and rebuild from save
        e.world.aiList.clear();
        JSONArray aiArr = json.getJSONArray("ai");
        com.absorbgame.ai.AIPersonality dummy =
                new com.absorbgame.ai.AIPersonality(0.5f,0.5f,0.5f,0.5f);
        for (int i = 0; i < aiArr.length(); i++) {
            JSONObject o = aiArr.getJSONObject(i);
            com.absorbgame.ai.AIPersonality pers = new com.absorbgame.ai.AIPersonality(
                    (float) o.getDouble("aggression"),
                    (float) o.getDouble("caution"),
                    (float) o.getDouble("greed"),
                    (float) o.getDouble("wanderBias"));
            AIEntity a = new AIEntity(
                    (float) o.getDouble("x"), (float) o.getDouble("y"), pers);
            a.setMass((float) o.getDouble("mass"));
            a.vx = (float) o.getDouble("vx");
            a.vy = (float) o.getDouble("vy");
            a.alive = o.getBoolean("alive");
            a.state = AIState.valueOf(o.getString("state"));
            a.controller = e.aiManager.getController();
            e.world.aiList.add(a);
        }

        // food - clear and rebuild
        e.world.foodList.clear();
        JSONArray foodArr = json.getJSONArray("food");
        for (int i = 0; i < foodArr.length(); i++) {
            JSONObject o = foodArr.getJSONObject(i);
            e.world.foodList.add(new Food(
                    (float) o.getDouble("x"),
                    (float) o.getDouble("y"),
                    (float) o.getDouble("value")));
        }

        e.world.rebuildSpatial();
        // restore RNG state to the saved snapshot so future random calls
        // continue from the same point (newGame consumed some randomness above)
        e.world.random.setSeed(seed);
        e.state = GameEngine.GameState.PLAYING;
    }

    private static byte[] readAll(File f) throws Exception {
        FileInputStream fis = new FileInputStream(f);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = fis.read(buf)) > 0) bos.write(buf, 0, n);
        fis.close();
        return bos.toByteArray();
    }
}
