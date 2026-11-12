package com.dilly.donutpay;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.client.util.DefaultSkinHelper;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * HUD modelled on the DrDonut video:
 *  - FEED style: stacked cards in the top-right (player head, name, +$amount) that slide in from the edge.
 *  - every payment from Config.minShowAmount up plays the full-screen show of its Tier (see Tier.java / Shows.java).
 */
public final class PaymentHud {
    /** A queued full-screen show. tier == null means a plain legacy toast. */
    static final class Note {
        final String name; final long amount; final long start; final Tier tier; final boolean preview;
        int cues; long tick = -1;
        Note(String name, long amount, long start, Tier tier, boolean preview) {
            this.name = name; this.amount = amount; this.start = start; this.tier = tier; this.preview = preview;
        }
    }

    private static final class Card {
        final String name; final long amount; final long start; final boolean preview; final SkinTextures skin;
        double y = -1;
        Card(String name, long amount, long start, boolean preview, SkinTextures skin) {
            this.name = name; this.amount = amount; this.start = start; this.preview = preview; this.skin = skin;
        }
    }

    private static final ArrayDeque<Note> queue = new ArrayDeque<>();
    private static final List<Card> cards = new ArrayList<>(); // newest first
    private static Note current;
    private static long lastPaymentAt = 0;

    private static final int GOLD = 0xFFD700, ORANGE = 0xFFAA00, GREEN = 0x55FF55, GRAY = 0xAAAAAA, WHITE = 0xFFFFFF,
        PURPLE = 0xAA55FF;
    private static final String SCRAMBLE = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789#$%&*?@";

    /** preview=true is used ONLY by the settings GUI and is never saved or counted. */
    public static void onPayment(String name, long amount, boolean preview) {
        Config cfg = Config.get();
        Tier tier = null;
        if (preview) tier = Tier.of(amount);
        else if (cfg.celebrationEnabled && amount >= cfg.minShowAmount) tier = Tier.of(amount);
        long now = System.currentTimeMillis();
        if (!preview) lastPaymentAt = now;

        // instant "ding" for every payment; tiers add their own sounds on top when they start playing
        if (cfg.soundEnabled) Sounds.play(cfg.soundId, tier != null && cfg.showSounds ? cfg.volume * 0.6f : cfg.volume, cfg.pitch);

        if (cfg.notifEnabled && cfg.style == Config.Style.FEED) {
            cards.add(0, new Card(name, amount, now, preview, skin(name)));
            while (cards.size() > Math.max(1, cfg.feedMax)) cards.remove(cards.size() - 1);
        } else if (cfg.notifEnabled && tier == null) {
            queue.add(new Note(name, amount, 0, null, preview));
        }
        if (tier != null) queue.add(new Note(name, amount, 0, tier, preview));
    }

    /** Plays a sound once when a show reaches the given time (bit = unique per show). */
    static void cue(Note n, int bit, long el, long at, String id, float vol, float pitch) {
        int m = 1 << bit;
        if ((n.cues & m) != 0 || el < at) return;
        n.cues |= m;
        if (el - at > 1500) return;
        Config c = Config.get();
        if (c.soundEnabled && c.showSounds) Sounds.play(id, Math.min(1f, c.volume * vol), pitch);
    }

    /** Repeating tick sound every `every` ms. */
    static void tick(Note n, long el, long every, String id, float vol, float pitch) {
        long k = el / every;
        if (k <= n.tick) return;
        n.tick = k;
        Config c = Config.get();
        if (c.soundEnabled && c.showSounds) Sounds.play(id, Math.min(1f, c.volume * vol), pitch);
    }

    // ---------------------------------------------------------------- render
    public static void render(DrawContext ctx) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.options.hudHidden || mc.player == null) return;
        Config cfg = Config.get();
        TextRenderer tr = mc.textRenderer;
        int w = ctx.getScaledWindowWidth(), h = ctx.getScaledWindowHeight();
        long now = System.currentTimeMillis();

        int panelBottom = 0;
        if (cfg.historyHud || cfg.sessionPanel) panelBottom = drawPanel(ctx, tr, cfg, w, h, now);

        if (!cards.isEmpty()) drawFeed(ctx, tr, cfg, w, panelBottom > 0 ? panelBottom + 6 : 6, now);

        // advance notification queue
        if (current != null) {
            int dur = current.tier != null ? current.tier.ms : cfg.notifMs;
            if (queue.size() > 3 && !current.preview) dur = Math.min(dur, 1200);
            if (now - current.start > dur) current = null;
        }
        if (current == null && !queue.isEmpty()) {
            Note n = queue.poll();
            current = new Note(n.name, n.amount, now, n.tier, n.preview);
        }
        if (current == null) return;

        if (current.tier != null) Shows.draw(ctx, tr, current, now, w, h);
        else drawToast(ctx, tr, cfg, current, now, w);
    }

    // ---------------------------------------------------------------- feed cards (top-right)
    private static void drawFeed(DrawContext ctx, TextRenderer tr, Config cfg, int w, int topY, long now) {
        final int CH = 26, GAP = 3;
        final long SLIDE = 260;
        cards.removeIf(c -> now - c.start > cfg.feedMs);
        for (int i = 0; i < cards.size(); i++) {
            Card c = cards.get(i);
            long age = now - c.start;
            String amt = "+" + Stats.fmt(c.amount);
            String nm = c.preview ? c.name + " [PREVIEW]" : c.name;
            int cw = Math.max(84, 28 + Math.max(tr.getWidth(nm), tr.getWidth(amt)) + 10);

            double target = topY + i * (CH + GAP);
            c.y = c.y < 0 ? target : c.y + (target - c.y) * 0.25;

            double pin = ease(Math.min(1.0, age / (double) SLIDE));
            double pout = ease(Math.min(1.0, (cfg.feedMs - age) / (double) SLIDE));
            double p = Math.max(0, Math.min(pin, pout));
            int x = w - cw + (int) Math.round((1 - p) * (cw + 6));
            int y = (int) Math.round(c.y);

            int accent = 0xFF000000 | (c.amount >= cfg.bigThreshold ? ORANGE : GREEN);
            ctx.fill(x, y, x + cw + 2, y + CH, 0xD0101418);
            ctx.fill(x, y, x + cw + 2, y + 1, accent);              // top line
            ctx.fill(x, y + CH - 1, x + cw + 2, y + CH, accent);    // bottom line
            ctx.fill(x, y, x + 2, y + CH, accent);                  // left bar

            PlayerSkinDrawer.draw(ctx, c.skin, x + 6, y + 4, 18);
            ctx.drawText(tr, nm, x + 28, y + 5, 0xFFFFFFFF, true);
            ctx.drawText(tr, amt, x + 28, y + 15, accent, true);
        }
    }

    // ---------------------------------------------------------------- history + session panel
    private static int drawPanel(DrawContext ctx, TextRenderer tr, Config cfg, int w, int h, long now) {
        Stats st = Stats.get();
        final int LH = 12, PAD = 4, WIDTH = 150;
        int lines = Math.max(1, cfg.historyLines);
        int rows = (cfg.historyHud ? lines : 0);
        int header = cfg.sessionPanel ? 3 : 0;
        int height = (header + rows) * LH + PAD * 2;
        boolean right = cfg.hudCorner == Config.Corner.TOP_RIGHT || cfg.hudCorner == Config.Corner.BOTTOM_RIGHT;
        boolean top = cfg.hudCorner == Config.Corner.TOP_RIGHT || cfg.hudCorner == Config.Corner.TOP_LEFT;
        int x = right ? w - WIDTH - 6 : 6;
        int y = top ? 6 : h - height - 30;

        ctx.fill(x, y, x + WIDTH, y + height, 0x88000000);
        ctx.fill(x, y, x + 2, y + height, 0xFF000000 | GOLD);

        int cy = y + PAD;
        if (cfg.sessionPanel) {
            ctx.drawText(tr, "Session: " + Stats.fmt(st.sessionTotal) + " (" + st.sessionCount + ")", x + 6, cy, 0xFF000000 | GREEN, true);
            cy += LH;
            String best = st.sessionLargest > 0 ? Stats.fmt(st.sessionLargest) + " from " + st.sessionLargestFrom : "-";
            ctx.drawText(tr, "Best: " + tr.trimToWidth(best, WIDTH - 40), x + 6, cy, 0xFF000000 | GOLD, true);
            cy += LH;
            var top1 = st.topPayers(1);
            String tp = top1.isEmpty() ? "-" : top1.get(0).getKey() + " " + Stats.fmt(top1.get(0).getValue());
            ctx.drawText(tr, "Top payer: " + tr.trimToWidth(tp, WIDTH - 62), x + 6, cy, 0xFF000000 | WHITE, true);
            cy += LH;
        }
        int bottom = (right && top) ? y + height : 0;
        if (!cfg.historyHud) return bottom;

        // scrolling list: every entry slides down one slot when a new payment arrives
        double e = ease(Math.min(1.0, (now - lastPaymentAt) / 250.0));
        List<Stats.Payment> rec = st.recent;
        for (int i = 0; i < Math.min(rec.size(), lines + 1); i++) {
            Stats.Payment p = rec.get(i);
            double slot = i - (1 - e);
            if (lastPaymentAt == 0 || now - lastPaymentAt > 250) slot = i;
            int ey = cy + (int) Math.round(slot * LH);
            if (ey < cy - 1 || ey > cy + (lines - 1) * LH + 1) continue;
            int alpha = 255;
            if (i == 0 && now - lastPaymentAt < 250) alpha = clampA(e);
            int col = (alpha << 24) | (p.amount >= cfg.bigThreshold ? GOLD : GREEN);
            String text = "+" + Stats.fmt(p.amount);
            ctx.drawText(tr, text, x + 6, ey, col, true);
            ctx.drawText(tr, tr.trimToWidth(p.from, WIDTH - 12 - tr.getWidth(text) - 8), x + 6 + tr.getWidth(text) + 6, ey,
                (alpha << 24) | WHITE, true);
        }
        return bottom;
    }

    // ---------------------------------------------------------------- legacy centered toast (MINIMAL / CARD / BANNER)
    private static void drawToast(DrawContext ctx, TextRenderer tr, Config cfg, Note n, long now, int w) {
        long el = now - n.start;
        int dur = cfg.notifMs;
        double p = Math.min(Math.min(1, el / 300.0), Math.min(1, (dur - el) / 300.0));
        p = Math.max(0, p);
        double e = ease(p);
        int a = clampA(cfg.animation == Config.Anim.NONE ? (p > 0 ? 1 : 0) : e);

        Matrix3x2fStack ms = ctx.getMatrices();
        ms.pushMatrix();
        double ty = 0, sc = 1;
        switch (cfg.animation) {
            case SLIDE -> ty = -(1 - e) * 40;
            case POP -> sc = 0.6 + 0.4 * easeOutBack(p);
            default -> {}
        }
        int cx = w / 2;
        int baseY = 22;
        ms.translate((float) (cx), (float) (baseY + ty));
        ms.scale((float) sc, (float) sc);

        String amt = "+" + Stats.fmt(n.amount);
        String from = "from " + n.name + (n.preview ? "  [PREVIEW]" : "");
        int accent = n.preview ? 0x55AAFF : GREEN;

        switch (cfg.style) {
            case MINIMAL -> {
                String line = amt + "  " + from;
                ctx.drawCenteredTextWithShadow(tr, Text.literal(line), 0, 0, (a << 24) | accent);
            }
            case BANNER -> {
                int bw = 200, bh = 22;
                ctx.fillGradient(-bw / 2, 0, bw / 2, bh, ((a * 170 / 255) << 24) | 0x103010, ((a * 170 / 255) << 24) | 0x000000);
                ctx.fill(-bw / 2, bh - 2, bw / 2, bh, (a << 24) | accent);
                ctx.drawCenteredTextWithShadow(tr, Text.literal(amt + "  " + from), 0, 7, (a << 24) | WHITE);
            }
            default -> { // CARD
                int bw = 176, bh = 38;
                ctx.fill(-bw / 2, 0, bw / 2, bh, ((a * 200 / 255) << 24) | 0x101010);
                ctx.fill(-bw / 2, 0, -bw / 2 + 3, bh, (a << 24) | accent);
                ms.pushMatrix();
                ms.translate((float) (-bw / 2 + 10), (float) (5));
                ms.scale(1.6f, 1.6f);
                ctx.drawText(tr, amt, 0, 0, (a << 24) | accent, true);
                ms.popMatrix();
                ctx.drawText(tr, tr.trimToWidth(from, bw - 16), -bw / 2 + 10, 22, (a << 24) | WHITE, true);
                ctx.drawText(tr, Stats.exact(n.amount), -bw / 2 + 10, 29, (a << 24) | GRAY, true);
            }
        }
        ms.popMatrix();
    }

    // ---------------------------------------------------------------- helpers
    static SkinTextures skin(String name) {
        var nh = MinecraftClient.getInstance().getNetworkHandler();
        if (nh != null) {
            PlayerListEntry e = nh.getPlayerListEntry(name);
            if (e != null) return e.getSkinTextures();
        }
        return DefaultSkinHelper.getSteve();
    }

    static void scaledCentered(DrawContext ctx, TextRenderer tr, String s, int cx, int y, float sc, int color) {
        Matrix3x2fStack ms = ctx.getMatrices();
        ms.pushMatrix();
        ms.translate((float) cx, (float) y);
        ms.scale(sc, sc);
        ctx.drawCenteredTextWithShadow(tr, Text.literal(s), 0, 0, color);
        ms.popMatrix();
    }

    /** "2 BILLION ENERGY!!!" / "100 MILLION ENERGY" */
    static String energy(long v) {
        double x; String unit;
        if (v >= 1_000_000_000_000L) { x = v / 1e12; unit = "TRILLION"; }
        else if (v >= 1_000_000_000L) { x = v / 1e9; unit = "BILLION"; }
        else if (v >= 1_000_000L) { x = v / 1e6; unit = "MILLION"; }
        else if (v >= 1_000L) { x = v / 1e3; unit = "THOUSAND"; }
        else return v + " ENERGY";
        String num = x == Math.floor(x) ? String.valueOf((long) x) : String.format(Locale.ROOT, "%.1f", x);
        return num + " " + unit + " ENERGY" + (v >= 1_000_000_000L ? "!!!" : "");
    }

    /** Obfuscated text that resolves left-to-right. */
    static String scramble(String target, long el, long now) {
        int solved = (int) Math.min(target.length(), el / 45);
        Random r = new Random(now / 70);
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < target.length(); i++) {
            char c = target.charAt(i);
            sb.append(i < solved || c == ' ' ? c : SCRAMBLE.charAt(r.nextInt(SCRAMBLE.length())));
        }
        return sb.toString();
    }

    static double fade(long el, int dur, double inMs, double outMs) {
        return Math.max(0, Math.min(1, Math.min(el / inMs, (dur - el) / outMs)));
    }
    static int clampA(double v) { return Math.max(5, Math.min(255, (int) (v * 255))); }
    static double ease(double t) { return 1 - Math.pow(1 - t, 3); }
    static double easeOutBack(double t) {
        double c1 = 1.70158, c3 = c1 + 1;
        return 1 + c3 * Math.pow(t - 1, 3) + c1 * Math.pow(t - 1, 2);
    }
}
