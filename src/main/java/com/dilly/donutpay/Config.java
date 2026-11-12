package com.dilly.donutpay;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;

public class Config {
    public enum Style { FEED, MINIMAL, CARD, BANNER }
    public enum Anim { SLIDE, FADE, POP, NONE }
    public enum Corner { TOP_RIGHT, TOP_LEFT, BOTTOM_RIGHT, BOTTOM_LEFT }

    // identity / safety
    public String ownerName = "DILLYY_MC";
    public boolean onlyOnDonutSMP = true;
    /** Extra server addresses (substring match) where tracking is also allowed. Use /donutpay addserver. */
    public java.util.List<String> extraServers = new java.util.ArrayList<>();

    // notifications
    public boolean notifEnabled = true;
    public Style style = Style.FEED;
    public Anim animation = Anim.SLIDE;
    public int notifMs = 3500;
    /** FEED style: how many cards stack up, and how long each stays (ms). */
    public int feedMax = 5;
    public int feedMs = 4500;

    // HUD
    public boolean historyHud = true;
    public int historyLines = 6;
    public Corner hudCorner = Corner.TOP_RIGHT;
    public boolean sessionPanel = true;

    // sounds
    public boolean soundEnabled = true;
    public String soundId = "minecraft:entity.experience_orb.pickup";
    public float volume = 1.0f;
    public float pitch = 1.0f;

    // tier shows: every payment level from minShowAmount up gets its own animation (see Tier.java)
    public boolean celebrationEnabled = true;
    /** Payments below this only get the feed card. */
    public long minShowAmount = 1_000_000L;
    /** Gold accent on the feed card / history from this amount. */
    public long bigThreshold = 100_000_000L;
    public boolean showSounds = true;

    // ---- persistence ----
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static Config INSTANCE;

    static Path dir() {
        Path d = FabricLoader.getInstance().getConfigDir().resolve("donutpay");
        try { Files.createDirectories(d); } catch (Exception ignored) {}
        return d;
    }

    public static Config get() {
        if (INSTANCE == null) load();
        return INSTANCE;
    }

    public static void load() {
        Path f = dir().resolve("config.json");
        try (Reader r = Files.newBufferedReader(f)) {
            INSTANCE = GSON.fromJson(r, Config.class);
        } catch (Exception e) {
            INSTANCE = null;
        }
        if (INSTANCE == null) { INSTANCE = new Config(); save(); }
    }

    public static void save() {
        try (Writer w = Files.newBufferedWriter(dir().resolve("config.json"))) {
            GSON.toJson(get(), w);
        } catch (Exception e) {
            DonutPayClient.LOG.error("Could not save config", e);
        }
    }
}
