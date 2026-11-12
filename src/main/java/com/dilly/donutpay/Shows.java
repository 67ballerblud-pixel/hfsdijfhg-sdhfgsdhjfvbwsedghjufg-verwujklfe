package com.dilly.donutpay;

import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.PlayerSkinDrawer;
import net.minecraft.entity.player.SkinTextures;
import net.minecraft.text.Text;
import org.joml.Matrix3x2fStack;

import java.util.Locale;
import java.util.Random;

/**
 * The per-tier full-screen shows, modelled on the DrDonut video.
 *   $1M coin rain | $5M deal-with-it shades | $10M breaking news | $50M wanted poster | $100M energy orb
 *   $500M legendary coin | $1B rocket | $5B jackpot wheel | $10B supernova -> black hole
 *   $50B god of donut (sunburst + laser eyes) | $100B "JK you're rich" gold flood | $1T legend (crown + fireworks)
 */
final class Shows {
    private Shows() {}

    private static final int GOLD = 0xFFD700, ORANGE = 0xFFAA00, GREEN = 0x55FF55, WHITE = 0xFFFFFF,
        PURPLE = 0xAA55FF, RED = 0xFF5555, GRAY = 0xAAAAAA, CREAM = 0xFFF4D6, PINK = 0xFF4D6D;
    private static final int[] GOLDS = {GOLD, ORANGE, 0xFFF2A0, 0xFFFFFF};
    private static final int[] RAINBOW = {0xFF5555, 0xFFAA00, 0xFFFF55, 0x55FF55, 0x55FFFF, 0x5555FF, 0xFF55FF};
    private static final String[] WORDS = {"CRACKED", "HUGE", "INSANE", "RICH", "MONSTER", "UNREAL", "LEGEND", "WHALE"};

    private static final class G {
        final DrawContext ctx; final TextRenderer tr; final PaymentHud.Note n; final Matrix3x2fStack ms;
        final int w, h, cx, dur; final long el; final double fade; final SkinTextures skin;
        G(DrawContext ctx, TextRenderer tr, PaymentHud.Note n, long now, int w, int h) {
            this.ctx = ctx; this.tr = tr; this.n = n; this.ms = ctx.getMatrices();
            this.w = w; this.h = h; this.cx = w / 2; this.dur = n.tier.ms; this.el = now - n.start;
            this.fade = Math.max(0, Math.min(1, Math.min(el / 250.0, (dur - el) / 500.0)));
            this.skin = PaymentHud.skin(n.name);
        }
    }

    // ================================================================ entry point
    static void draw(DrawContext ctx, TextRenderer tr, PaymentHud.Note n, long now, int w, int h) {
        G g = new G(ctx, tr, n, now, w, h);
        double amp = shakeAmp(g);
        g.ms.pushMatrix();
        if (amp > 0.2) {
            Random r = new Random((g.el / 35) * 31 + n.start);
            g.ms.translate((float) ((r.nextDouble() - 0.5) * 2 * amp), (float) ((r.nextDouble() - 0.5) * 2 * amp));
        }
        switch (n.tier) {
            case COIN_RAIN -> coinRain(g);
            case DEAL_WITH_IT -> dealWithIt(g);
            case BREAKING_NEWS -> breakingNews(g);
            case WANTED -> wanted(g);
            case BIG -> big(g);
            case LEGENDARY -> legendary(g);
            case MEGA -> mega(g);
            case JACKPOT -> jackpot(g);
            case SUPERNOVA -> supernova(g);
            case GOD -> god(g);
            case RICH -> rich(g);
            case LEGEND -> legend(g);
        }
        g.ms.popMatrix();
        if (n.preview) text(g, "[PREVIEW]", g.cx, 6, 1f, GRAY, g.fade);
    }

    private static double shakeAmp(G g) {
        long el = g.el;
        switch (g.n.tier) {
            case SUPERNOVA:
                if (el < 2000) return el / 2000.0 * 3;
                if (el < 3000) return 7 * (1 - (el - 2000) / 1000.0);
                return 0;
            case GOD: { long t = el - 1800; return t > 0 && t < 1500 ? 2.5 * (1 - t / 1500.0) : 0; }
            case LEGEND: return el < 500 ? 4 * (1 - el / 500.0) : 0;
            case WANTED: { long t = el - 1300; return t > 0 && t < 350 ? 3 * (1 - t / 350.0) : 0; }
            default: return 0;
        }
    }

    // ================================================================ $1M  COIN RAIN
    private static void coinRain(G g) {
        PaymentHud.cue(g.n, 0, g.el, 0, "minecraft:block.amethyst_block.chime", 0.9f, 1.6f);
        PaymentHud.cue(g.n, 1, g.el, 250, "minecraft:entity.experience_orb.pickup", 0.8f, 1.4f);
        PaymentHud.cue(g.n, 2, g.el, 500, "minecraft:entity.experience_orb.pickup", 0.8f, 1.8f);
        coins(g, 46, 0);
        double pop = PaymentHud.easeOutBack(clamp01(g.el / 350.0));
        int y = (int) (g.h * 0.22);
        text(g, "+" + Stats.fmt(g.n.amount), g.cx, y, (float) (2.0 * pop), GOLD, g.fade);
        text(g, "from " + g.n.name, g.cx, y + 22, 1.1f, WHITE, g.fade);
    }

    // ================================================================ $5M  DEAL WITH IT
    private static void dealWithIt(G g) {
        long el = g.el; int S = 44, cy = (int) (g.h * 0.34);
        double band = Math.min(PaymentHud.ease(clamp01(el / 300.0)), PaymentHud.ease(clamp01((g.dur - el) / 300.0)));
        fillA(g, 0, cy - S / 2 - 8, g.w, cy + S / 2 + 46, 0.55 * band, 0x000000);
        double p = bounce(clamp01(el / 800.0));
        int hx = g.cx - S / 2, hy = (int) (-S + (cy - S / 2 + S) * p);
        head(g, hx, hy, S);
        PaymentHud.cue(g.n, 0, el, 0, "minecraft:entity.player.levelup", 0.7f, 1.3f);
        if (el >= 900) {
            glasses(g, hx, hy, S, bounce(clamp01((el - 900) / 500.0)));
            PaymentHud.cue(g.n, 1, el, 1250, "minecraft:block.anvil.land", 0.5f, 1.7f);
        }
        if (el >= 1400) {
            double pop = PaymentHud.easeOutBack(clamp01((el - 1400) / 350.0));
            rainbowText(g, "DEAL WITH IT", g.cx, cy + S / 2 + 6, (float) (2.0 * pop), el);
            text(g, g.n.name + " donated " + Stats.fmt(g.n.amount), g.cx, cy + S / 2 + 30, 1.1f, WHITE, g.fade);
            PaymentHud.cue(g.n, 2, el, 1400, "minecraft:ui.toast.challenge_complete", 0.8f, 1.0f);
            confetti(g, 36, 1400, RAINBOW);
        }
    }

    // ================================================================ $10M  BREAKING NEWS
    private static void breakingNews(G g) {
        long el = g.el;
        PaymentHud.cue(g.n, 0, el, 0, "minecraft:block.note_block.pling", 0.9f, 1.0f);
        PaymentHud.cue(g.n, 1, el, 150, "minecraft:block.note_block.pling", 0.9f, 1.26f);
        PaymentHud.cue(g.n, 2, el, 300, "minecraft:block.note_block.pling", 0.9f, 1.5f);
        if (el < 200) fillA(g, 0, 0, g.w, g.h, (1 - el / 200.0) * 0.55, 0xFFFFFF);
        if (el >= 420 && el < 540) fillA(g, 0, 0, g.w, g.h, (1 - (el - 420) / 120.0) * 0.35, 0xFFFFFF);

        double p = Math.min(PaymentHud.ease(clamp01(el / 380.0)), PaymentHud.ease(clamp01((g.dur - el) / 380.0)));
        final int rowH = 15, tickH = 13, labelW = 100;
        int top = g.h - 32 - rowH - tickH + (int) ((1 - p) * (rowH + tickH + 50));
        boolean blink = (el / 250) % 2 == 0;

        // anchor head
        g.ctx.fill(4, top - 36, 40, top, 0xFFFFFFFF);
        head(g, 6, top - 34, 32);
        g.ctx.fill(4, top - 46, 28, top - 36, 0xFF000000 | (blink ? 0xD01010 : 0x801010));
        g.ctx.drawText(g.tr, "LIVE", 6, top - 44, 0xFFFFFFFF, false);

        g.ctx.fill(0, top, labelW, top + rowH, 0xFF000000 | (blink ? 0xD01010 : 0xFF3030));
        g.ctx.drawText(g.tr, "BREAKING NEWS", 6, top + 4, 0xFFFFFFFF, true);
        g.ctx.fill(labelW, top, g.w, top + rowH, 0xFFF2F2F2);
        String head = g.n.name.toUpperCase(Locale.ROOT) + " DONATES " + Stats.fmt(g.n.amount);
        g.ctx.drawText(g.tr, head, labelW + 6, top + 4, 0xFF111111, false);

        int ty = top + rowH;
        g.ctx.fill(0, ty, g.w, ty + tickH, 0xFF0A1F5C);
        g.ctx.fill(0, ty, g.w, ty + 1, 0xFF000000 | GOLD);
        String tick = "+++ ECONOMY IN SHOCK +++ " + g.n.name.toUpperCase(Locale.ROOT) + " JUST DONATED " + Stats.fmt(g.n.amount)
            + " +++ EXPERTS STUNNED +++ MARKETS GO WILD +++ ";
        int tw = Math.max(1, g.tr.getWidth(tick));
        int off = (int) ((el * 0.10) % tw);
        g.ctx.enableScissor(0, ty, g.w, ty + tickH);
        for (int x = -off; x < g.w; x += tw) g.ctx.drawText(g.tr, tick, x, ty + 3, 0xFFFFFFFF, false);
        g.ctx.disableScissor();

        double pop = PaymentHud.easeOutBack(clamp01(el / 400.0));
        text(g, Stats.fmt(g.n.amount), g.cx, (int) (g.h * 0.26), (float) (3.0 * pop), GOLD, g.fade);
        text(g, "from " + g.n.name, g.cx, (int) (g.h * 0.26) + 30, 1.2f, WHITE, g.fade);
    }

    // ================================================================ $50M  WANTED
    private static void wanted(G g) {
        long el = g.el;
        final int W = 100, H = 138;
        double slide = Math.min(PaymentHud.ease(clamp01(el / 450.0)), PaymentHud.ease(clamp01((g.dur - el) / 400.0)));
        int topY = (int) (-(H + 20) + (H + 20 + g.h * 0.08) * slide);
        double ang = 0.55 * Math.exp(-el / 520.0) * Math.cos(el / 120.0);
        PaymentHud.cue(g.n, 0, el, 0, "minecraft:entity.item_frame.place", 1.0f, 0.8f);

        g.ms.pushMatrix();
        g.ms.translate((float) g.cx, (float) topY);
        g.ms.rotate((float) ang);
        g.ctx.fill(-W / 2 + 4, 4, W / 2 + 4, H + 4, 0x55000000);
        g.ctx.fill(-W / 2, 0, W / 2, H, 0xFFE6CF9A);
        g.ctx.fill(-W / 2, 0, W / 2, 2, 0xFF8B5A2B);
        g.ctx.fill(-W / 2, H - 2, W / 2, H, 0xFF8B5A2B);
        g.ctx.fill(-W / 2, 0, -W / 2 + 2, H, 0xFF8B5A2B);
        g.ctx.fill(W / 2 - 2, 0, W / 2, H, 0xFF8B5A2B);
        g.ctx.fill(-3, -5, 3, 1, 0xFF444444);

        textNS(g, "WANTED", 0, 8, 1.9f, 0x3B2412, 1);
        g.ctx.fill(-25, 28, 25, 78, 0xFF5B3A1A);
        PlayerSkinDrawer.draw(g.ctx, g.skin, -22, 31, 44);
        textNS(g, "DEAD OR ALIVE", 0, 82, 0.8f, 0x3B2412, 1);
        textNS(g, g.n.name, 0, 94, fit(g, g.n.name, 1.1f, W - 12), 0x3B2412, 1);
        textNS(g, "REWARD", 0, 106, 0.9f, 0x3B2412, 1);
        textNS(g, Stats.fmt(g.n.amount), 0, 116, fit(g, Stats.fmt(g.n.amount), 1.8f, W - 12), 0x9B1C1C, 1);

        if (el >= 1300) {
            PaymentHud.cue(g.n, 1, el, 1300, "minecraft:block.anvil.land", 1.0f, 0.8f);
            double k = clamp01((el - 1300) / 220.0);
            float s = (float) (1 + 2.5 * (1 - PaymentHud.ease(k)));
            g.ms.pushMatrix();
            g.ms.translate(0f, 72f);
            g.ms.rotate(-0.35f);
            g.ms.scale(s * 2f, s * 2f);
            int col = targb(k, 0xCC1111);
            int rc = fillArgb(k, 0xCC1111);
            g.ctx.fill(-15, -7, 15, -6, rc);
            g.ctx.fill(-15, 6, 15, 7, rc);
            g.ctx.fill(-15, -7, -14, 7, rc);
            g.ctx.fill(14, -7, 15, 7, rc);
            g.ctx.drawText(g.tr, "PAID", -g.tr.getWidth("PAID") / 2, -4, col, false);
            g.ms.popMatrix();
        }
        g.ms.popMatrix();
        if (el > 1300) burst(g, g.cx, (int) (g.h * 0.08 + 72), 26, 1300, 90, GOLDS, 0x57A7L);
    }

    // ================================================================ $100M  ENERGY ORB
    private static void big(G g) {
        long el = g.el; double fade = g.fade;
        PaymentHud.cue(g.n, 0, el, 0, "minecraft:ui.toast.challenge_complete", 1.0f, 1.0f);
        glow(g, 0.30);
        int cx = g.cx, cy = (int) (g.h * 0.30);
        // head orb with a spinning dotted ring (video: the orb that later collapses)
        int oy = cy - 36;
        double pulse = 1 + 0.06 * Math.sin(el / 120.0);
        ellipse(g.ctx, cx, oy, 17 * pulse, 17 * pulse, argb(0.35 * fade, GOLD));
        ellipse(g.ctx, cx, oy, 14, 14, argb(0.55 * fade, 0xE0B000));
        head(g, cx - 10, oy - 10, 20);
        ring(g.ctx, cx, oy, 23, 16, el / 400.0, 2, argb(fade, 0xFFFFFF));
        ring(g.ctx, cx, oy, 30, 22, -el / 520.0, 1, argb(0.6 * fade, GOLD));

        double pop = PaymentHud.easeOutBack(clamp01(el / 350.0));
        int ta = PaymentHud.clampA(fade);
        PaymentHud.scaledCentered(g.ctx, g.tr, Stats.fmt(g.n.amount) + " from " + g.n.name, cx, cy, (float) (2.0 * pop), (ta << 24) | GOLD);
        PaymentHud.scaledCentered(g.ctx, g.tr, PaymentHud.scramble(PaymentHud.energy(g.n.amount), el, g.n.start + el), cx, cy + 22, 1.3f, (ta << 24) | GREEN);
        sparkles(g, cx, cy, 28);
    }

    // ================================================================ $500M  LEGENDARY COIN
    private static void legendary(G g) {
        long el = g.el; double f = g.fade; int cx = g.cx;
        PaymentHud.cue(g.n, 0, el, 0, "minecraft:block.end_portal.spawn", 0.5f, 1.2f);
        PaymentHud.cue(g.n, 1, el, 700, "minecraft:entity.player.levelup", 0.9f, 0.8f);
        fillA(g, 0, 0, g.w, g.h, 0.62 * f, 0x000000);
        stars(g, 70, f);
        int bw = (int) Math.min(g.w * 0.34, 150);
        fillA(g, cx - bw, 0, cx + bw, g.h, 0.10 * f, 0xFFFFFF);

        boolean fl = (el / 110) % 2 == 0;
        String title = "\u2620 LEGENDARY DONATION \u2620";
        text(g, title, cx, (int) (g.h * 0.08), fit(g, title, 1.6f, g.w - 20), fl ? GOLD : 0xFFF3A0, f);
        text(g, "1 IN 10,000 DROP", cx, (int) (g.h * 0.08) + 18, 0.9f, RED, f);

        double rise = PaymentHud.ease(clamp01(el / 700.0));
        int ry = (int) Math.min(34, g.h * 0.12), coinY = (int) (g.h * 0.40) + (int) ((1 - rise) * 90);
        double spin = el / 230.0;
        double c = Math.abs(Math.cos(spin));
        coinBody(g, cx, coinY, Math.max(2, 22 * c), ry, c);
        ring(g.ctx, cx, coinY, ry + 14 + 3 * Math.sin(el / 150.0), 20, el / 500.0, 2, argb(0.7 * f, GOLD));

        int hy = coinY + ry + 6;
        head(g, cx - 8, hy, 16);
        text(g, g.n.name, cx, hy + 20, 1.4f, WHITE, f);
        text(g, "JUST BROKE THE LEADERBOARD", cx, hy + 34, 0.9f, RED, f);
        double pop = PaymentHud.easeOutBack(clamp01((el - 500) / 400.0));
        text(g, Stats.fmt(g.n.amount), cx, hy + 48, (float) (2.2 * Math.max(0.01, pop)), GOLD, f);
        sparkles(g, cx, coinY, 40);
    }

    // ================================================================ $1B  ROCKET ("TO THE MOON")
    private static void mega(G g) {
        long el = g.el; double fade = g.fade;
        PaymentHud.cue(g.n, 0, el, 0, "minecraft:entity.firework_rocket.launch", 1.0f, 0.9f);
        PaymentHud.cue(g.n, 1, el, 900, "minecraft:entity.firework_rocket.blast", 1.0f, 0.8f);
        PaymentHud.cue(g.n, 2, el, 1400, "minecraft:ui.toast.challenge_complete", 0.9f, 1.0f);
        glow(g, 0.43);
        int cx = g.cx, cy = (int) (g.h * 0.34);
        boolean yeet = Math.floorMod(Long.hashCode(g.n.start), 2) == 1;
        String phrase = yeet ? "YEET TO THE MOON" : "TO THE MOON";
        int phraseCol = yeet ? GREEN : PURPLE;

        bubbles(g);
        rocket(g, cx, cy);
        headBurst(g, cx, cy);

        int ta = PaymentHud.clampA(fade);
        double pop = PaymentHud.easeOutBack(clamp01(el / 450.0));
        double pulse = 1 + 0.035 * Math.sin(el / 90.0);
        PaymentHud.scaledCentered(g.ctx, g.tr, "\u2605 " + phrase + " \u2605", cx, cy - 30, 1.5f, (ta << 24) | phraseCol);
        PaymentHud.scaledCentered(g.ctx, g.tr, Stats.fmt(g.n.amount), cx, cy - 4, (float) (4.0 * pop * pulse), (ta << 24) | GOLD);
        PaymentHud.scaledCentered(g.ctx, g.tr, "from " + g.n.name, cx, cy + 36, 1.6f, (ta << 24) | WHITE);
        PaymentHud.scaledCentered(g.ctx, g.tr, PaymentHud.energy(g.n.amount), cx, cy + 54, 1.1f, (ta << 24) | ORANGE);
        sparkles(g, cx, cy, 36);
    }

    // ================================================================ $5B  JACKPOT WHEEL
    private static void jackpot(G g) {
        long el = g.el; double f = g.fade; int cx = g.cx;
        fillA(g, 0, 0, g.w, g.h, 0.66 * f, 0x000000);
        for (int i = 0; i < g.w / 10; i++) {
            int x = i * 10 + 4;
            int col = ((i + el / 140) % 2 == 0) ? 0xFFFFD700 : 0xFF553300;
            g.ctx.fill(x, 3, x + 3, 6, col);
            g.ctx.fill(x, g.h - 6, x + 3, g.h - 3, col);
        }
        int wy = (int) (g.h * 0.50), R = (int) Math.min(54, g.h * 0.20);
        double t = clamp01(el / 3600.0);
        double rot = 14 * (1 - Math.pow(1 - t, 3));
        if (el < 3500) PaymentHud.tick(g.n, el, 150, "minecraft:block.note_block.hat", 0.6f, 1.4f);
        for (int ring = 0; ring < 8; ring++) {
            double r = 6 + ring * (R - 6) / 7.0;
            int dots = Math.max(6, (int) (r * 0.9));
            for (int i = 0; i < dots; i++) {
                double ang = i * 2 * Math.PI / dots + rot;
                int x = (int) (cx + Math.cos(ang) * r), y = (int) (wy + Math.sin(ang) * r);
                int col = hsv(i / (double) dots + ring * 0.03, 0.9, 1.0);
                g.ctx.fill(x, y, x + 2, y + 2, argb(f, col));
            }
        }
        ellipse(g.ctx, cx, wy, 4, 4, argb(f, GOLD));
        for (int k = 0; k < 7; k++) g.ctx.fill(cx - (6 - k), wy - R - 10 + k, cx + (6 - k) + 1, wy - R - 9 + k, 0xFFFFD700);

        double pulse = 1 + 0.05 * Math.sin(el / 90.0);
        boolean alt = (el / 180) % 2 == 0;
        text(g, "$$$ JACKPOT $$$", cx, wy - R - 36, (float) (1.8 * pulse), alt ? GREEN : GOLD, f);
        if (el > 3600) {
            PaymentHud.cue(g.n, 0, el, 3600, "minecraft:ui.toast.challenge_complete", 1.0f, 1.0f);
            PaymentHud.cue(g.n, 1, el, 3650, "minecraft:entity.player.levelup", 1.0f, 1.2f);
            double pop = PaymentHud.easeOutBack(clamp01((el - 3600) / 350.0));
            text(g, g.n.name + " WINS " + Stats.fmt(g.n.amount), cx, wy + R + 12, (float) (1.6 * Math.max(0.01, pop)), alt ? GOLD : WHITE, f);
            confetti(g, 70, 3600, RAINBOW);
        } else {
            text(g, g.n.name + " spins the wheel...", cx, wy + R + 12, 1.1f, WHITE, f);
        }
    }

    // ================================================================ $10B  SUPERNOVA -> BLACK HOLE
    private static void supernova(G g) {
        long el = g.el; int cx = g.cx, cy = (int) (g.h * 0.42);
        double out = clamp01((g.dur - el) / 700.0);
        PaymentHud.cue(g.n, 0, el, 0, "minecraft:block.beacon.activate", 1.0f, 0.6f);
        if (el < 2000) PaymentHud.tick(g.n, el, 400, "minecraft:block.note_block.bit", 0.7f, (float) (0.6 + el / 1000.0));
        PaymentHud.cue(g.n, 1, el, 2000, "minecraft:entity.generic.explode", 1.0f, 0.5f);
        PaymentHud.cue(g.n, 2, el, 2000, "minecraft:entity.lightning_bolt.thunder", 0.8f, 0.7f);
        PaymentHud.cue(g.n, 3, el, 3000, "minecraft:block.end_portal.spawn", 0.6f, 0.6f);

        if (el < 2000) {                                   // phase 1: the star goes critical
            double k = el / 2000.0;
            fillA(g, 0, 0, g.w, g.h, 0.40 * k, 0x000000);
            stars(g, 50, k);
            double r = 14 + 26 * k * k;
            for (int i = 3; i >= 1; i--) ellipse(g.ctx, cx, cy, r + i * 5 + 2 * Math.sin(el / 60.0), r + i * 5 + 2 * Math.sin(el / 60.0), argb(0.10 + 0.04 * k, 0xFFC000));
            ellipse(g.ctx, cx, cy, r, r, argb(0.55, ORANGE));
            int hs = (int) (r * 1.3);
            head(g, cx - hs / 2, cy - hs / 2, hs);
            double ph = (el % 600) / 600.0;
            ring(g.ctx, cx, cy, r + ph * 70, 28, el / 300.0, 2, argb(1 - ph, 0xFFE080));
            boolean fl = (el / 130) % 2 == 0;
            int ty = (int) (g.h * 0.14);
            text(g, "\u26A0 STAR GOING CRITICAL \u26A0", cx, ty, 1.5f, fl ? GOLD : RED, 1);
            text(g, g.n.name + " JUST DROPPED " + Stats.fmt(g.n.amount), cx, ty + 17, 1.1f, WHITE, 1);
        } else if (el < 3000) {                            // phase 2: supernova flash
            double t = (el - 2000) / 1000.0;
            fillA(g, 0, 0, g.w, g.h, 0.4 + 0.4 * t, 0x000000);
            double rad = PaymentHud.ease(t) * Math.max(g.w, g.h) * 0.9;
            ellipse(g.ctx, cx, cy, rad, rad, argb(0.95, CREAM));
            double wash = t < 0.45 ? t / 0.45 : Math.max(0, 1 - (t - 0.45) / 0.55);
            fillA(g, 0, 0, g.w, g.h, 0.92 * wash, CREAM);
            ring(g.ctx, cx, cy, rad * 0.9, 90, 0, 2, argb(0.8 * (1 - t), 0xFFFFFF));
            ring(g.ctx, cx, cy, rad * 0.6, 60, 0.3, 2, argb(0.6 * (1 - t), 0xFFE080));
        } else {                                           // phase 3: black hole
            long t3 = el - 3000;
            fillA(g, 0, 0, g.w, g.h, 0.80 * out, 0x000000);
            stars(g, 90, out);
            Random r = new Random(g.n.start ^ 0xB1ACL);
            for (int i = 0; i < 46; i++) {
                double rs = 34 + r.nextDouble() * 110, sp = 0.8 + r.nextDouble() * 1.4, off = r.nextDouble() * 6.28;
                double life = 2400 + r.nextDouble() * 1200, ph = r.nextDouble() * life;
                double tt = ((t3 + ph) % life) / life;
                double rr = rs * (1 - tt), ang = off + tt * sp * 7;
                if (rr < 16) continue;
                int x = (int) (cx + Math.cos(ang) * rr), y = (int) (cy + Math.sin(ang) * rr);
                if (i % 3 == 0) {
                    int sz = Math.max(2, (int) (9 * (1 - tt)));
                    PlayerSkinDrawer.draw(g.ctx, g.skin, x - sz / 2, y - sz / 2, sz);
                } else {
                    g.ctx.fill(x, y, x + 2, y + 2, argb(out * (1 - tt * 0.5), WHITE));
                }
            }
            ellipse(g.ctx, cx, cy, 16, 16, 0xFFFFFFFF);
            ellipse(g.ctx, cx, cy, 15, 15, 0xFF000000);
            double ph = (t3 % 1500) / 1500.0;
            ring(g.ctx, cx, cy, 18 + ph * 30, 48, 0, 1, argb(out * (1 - ph) * 0.8, WHITE));
            if (t3 > 300) {
                double pop = PaymentHud.easeOutBack(clamp01((t3 - 300) / 400.0));
                text(g, Stats.fmt(g.n.amount) + " from " + g.n.name, cx, (int) (g.h * 0.14), (float) (1.9 * Math.max(0.01, pop)), GOLD, out);
                text(g, PaymentHud.energy(g.n.amount), cx, (int) (g.h * 0.14) + 24, 1.1f, WHITE, out);
            }
        }
    }

    // ================================================================ $50B  GOD OF DONUT SMP
    private static void god(G g) {
        long el = g.el; double f = g.fade; int cx = g.cx, cy = (int) (g.h * 0.40);
        PaymentHud.cue(g.n, 0, el, 0, "minecraft:entity.ender_dragon.growl", 0.6f, 1.2f);
        PaymentHud.cue(g.n, 1, el, 1800, "minecraft:entity.warden.sonic_boom", 0.6f, 1.3f);
        fillA(g, 0, 0, g.w, g.h, 0.74 * f, 0x000000);
        stars(g, 70, f);
        int len = Math.max(g.w, g.h);
        rays(g, cx, cy, 0, len, 18, el / 5000.0, argb(0.45 * f, GOLD), 3);
        rays(g, cx, cy, 0, len, 18, el / 5000.0 + 0.04, argb(0.25 * f, GOLD), 2);

        double z = PaymentHud.easeOutBack(clamp01(el / 900.0));
        int HS = (int) ((26 + 70 * z) * (1 + 0.00003 * el) * (1 + 0.02 * Math.sin(el / 110.0)) * (0.5 + 0.5 * f));
        int R = (int) (HS * 0.72);
        ellipse(g.ctx, cx, cy, R + 6, R + 6, argb(0.22 * f, GOLD));
        ellipse(g.ctx, cx, cy, R, R, argb(0.55 * f, 0xE0B000));
        int hx = cx - HS / 2, hy = cy - HS / 2;
        head(g, hx, hy, HS);

        if (el > 1800) {                                    // laser eyes
            double a = 0.7 + 0.3 * Math.sin(el / 60.0);
            int ey = hy + (int) (HS * 0.47);
            laser(g, hx + (int) (HS * 0.28), ey, Math.atan2(1, -0.45), a);
            laser(g, hx + (int) (HS * 0.72), ey, Math.atan2(1, 0.45), a);
        }
        // floating hype words
        Random r = new Random(g.n.start ^ 0x60D5L);
        for (int i = 0; i < 10; i++) {
            double start = 500 + i * 450, life = 2200;
            double t = el - start;
            if (t < 0 || t > life) continue;
            double ang = r.nextDouble() * Math.PI * 2, dist = R + 22 + r.nextDouble() * 60;
            int x = (int) (cx + Math.cos(ang) * dist * 1.4), y = (int) (cy + Math.sin(ang) * dist - t / 40.0);
            double k = t / life;
            text(g, WORDS[i % WORDS.length], x, y, 0.9f, GOLD, Math.min(1, Math.min(k * 6, (1 - k) * 3)) * f);
        }
        int ty = cy + R + 10;
        text(g, "\u26A1 GOD OF DONUT SMP \u26A1", cx, ty, 1.4f, GOLD, f);
        text(g, g.n.name + " ASCENDED WITH " + Stats.fmt(g.n.amount), cx, ty + 17, 1.1f, WHITE, f);
        confetti(g, 40, 300, GOLDS);
    }

    // ================================================================ $100B  "JK. YOU'RE RICH."
    private static void rich(G g) {
        long el = g.el; double f = g.fade; int cx = g.cx;
        PaymentHud.cue(g.n, 0, el, 0, "minecraft:entity.player.levelup", 1.0f, 0.9f);
        PaymentHud.cue(g.n, 1, el, 600, "minecraft:entity.villager.celebrate", 1.0f, 1.0f);
        PaymentHud.cue(g.n, 2, el, 1150, "minecraft:block.anvil.land", 0.6f, 1.5f);
        double p = PaymentHud.ease(clamp01(el / 700.0));
        g.ctx.fillGradient(0, 0, g.w, g.h, argb(0.92 * p * f, 0xFFC81E), argb(0.92 * p * f, 0xB8860B));
        rays(g, cx, (int) (g.h * 0.40), 0, Math.max(g.w, g.h), 14, el / 3000.0, argb(0.18 * p * f, 0xFFFFFF), 6);
        coins(g, 80, 0);

        int S = 38, cy = (int) (g.h * 0.36);
        double dp = bounce(clamp01(el / 800.0));
        int hx = cx - S / 2, hy = (int) (-S + (cy - S / 2 + S) * dp);
        g.ctx.fill(hx - 2, hy - 2, hx + S + 2, hy + S + 2, 0xFF3B2412);
        head(g, hx, hy, S);
        if (el >= 900) glasses(g, hx, hy, S, bounce(clamp01((el - 900) / 500.0)));
        if (el >= 600) {
            double pop = PaymentHud.easeOutBack(clamp01((el - 600) / 400.0));
            outlined(g, "JK. YOU'RE RICH.", cx, cy + S / 2 + 8, (float) (2.4 * Math.max(0.01, pop)), WHITE, 0x3B2412, f);
            outlined(g, g.n.name + "  +" + Stats.fmt(g.n.amount), cx, cy + S / 2 + 34, 1.5f, WHITE, 0x3B2412, f);
        }
    }

    // ================================================================ $1T  LEGEND (crown, rainbow sunburst, fireworks)
    private static void legend(G g) {
        long el = g.el; double f = g.fade; int cx = g.cx, cy = (int) (g.h * 0.36);
        PaymentHud.cue(g.n, 0, el, 0, "minecraft:ui.toast.challenge_complete", 1.0f, 0.8f);
        PaymentHud.cue(g.n, 1, el, 0, "minecraft:entity.ender_dragon.death", 0.4f, 1.4f);
        if (el < 350) fillA(g, 0, 0, g.w, g.h, (1 - el / 350.0) * 0.8, 0xFFFFFF);
        fillA(g, 0, 0, g.w, g.h, 0.62 * f, 0x000000);
        stars(g, 60, f);
        int len = Math.max(g.w, g.h);
        for (int i = 0; i < 16; i++)
            rays(g, cx, cy, 0, len, 1, el / 4500.0 + i * 2 * Math.PI / 16, argb(0.42 * f, hsv(i / 16.0 + el / 4000.0, 0.7, 1.0)), 3);

        int S = 60;
        double z = PaymentHud.easeOutBack(clamp01(el / 800.0));
        int HS = (int) (S * Math.max(0.05, z));
        int R = (int) (HS * 0.78);
        ellipse(g.ctx, cx, cy, R + 5, R + 5, argb(0.25 * f, GOLD));
        ellipse(g.ctx, cx, cy, R, R, argb(0.6 * f, 0xE0B000));
        head(g, cx - HS / 2, cy - HS / 2, HS);
        crown(g, cx, cy - HS / 2, HS);

        int ty = (int) (g.h * 0.08);
        text(g, "\u2605\u2605\u2605 LEGENDARY \u2605\u2605\u2605", cx, ty, 1.5f, hsv(el / 600.0, 0.6, 1.0), f);
        int by = cy + R + 8;
        double pop = PaymentHud.easeOutBack(clamp01((el - 400) / 500.0));
        text(g, Stats.fmt(g.n.amount), cx, by, (float) (3.6 * Math.max(0.01, pop)), GOLD, f);
        text(g, g.n.name + " IS A LEGEND", cx, by + 34, 1.5f, WHITE, f);
        text(g, "LARGEST DONATION IN DONUT HISTORY", cx, by + 50, 0.9f, GOLD, f);

        Random r = new Random(g.n.start ^ 0xF1AEL);
        for (int i = 0; i < 7; i++) {
            long at = 500 + i * 900L;
            int fx = (int) (g.w * (0.12 + r.nextDouble() * 0.76)), fy = (int) (g.h * (0.15 + r.nextDouble() * 0.40));
            int col = hsv(r.nextDouble(), 0.8, 1.0);
            PaymentHud.cue(g.n, 2 + i, el, at, "minecraft:entity.firework_rocket.blast", 0.6f, 0.8f + (float) r.nextDouble() * 0.5f);
            burst(g, fx, fy, 30, at, 85, new int[]{col, WHITE, GOLD}, 0xF00L + i);
        }
        confetti(g, 70, 600, RAINBOW);
    }

    // ================================================================ shared effects
    private static void rocket(G g, int cx, int cy) {
        final int u = 2; double t = g.el;
        int ry = (int) (cy + 50 - (0.00002 * t * t + 0.03 * t));
        int rx = cx + (int) Math.round(Math.sin(t / 120.0) * 1.5);
        if (ry < -40) return;
        int bw = 8 * u, bx = rx - bw / 2;
        DrawContext c = g.ctx;
        c.fill(rx - u, ry - 3 * u, rx + u, ry - 2 * u, 0xFFE8E8E8);
        c.fill(rx - 2 * u, ry - 2 * u, rx + 2 * u, ry - u, 0xFFEFEFEF);
        c.fill(rx - 3 * u, ry - u, rx + 3 * u, ry, 0xFFF4F4F4);
        c.fill(bx, ry, bx + bw, ry + 12 * u, 0xFFF4F4F4);
        c.fill(bx + bw - u, ry, bx + bw, ry + 12 * u, 0xFFCFCFCF);
        c.fill(bx - 3 * u, ry + 8 * u, bx, ry + 12 * u, 0xFFD83A32);
        c.fill(bx - 2 * u, ry + 6 * u, bx, ry + 8 * u, 0xFFD83A32);
        c.fill(bx + bw, ry + 8 * u, bx + bw + 3 * u, ry + 12 * u, 0xFFD83A32);
        c.fill(bx + bw, ry + 6 * u, bx + bw + 2 * u, ry + 8 * u, 0xFFD83A32);
        c.fill(bx + u, ry + u, bx + bw - u, ry + 7 * u, 0xFF7A7A7A);
        PlayerSkinDrawer.draw(c, g.skin, bx + 2 * u, ry + 2 * u, 8);
        boolean flick = (g.el / 70) % 2 == 0;
        c.fill(rx - 2 * u, ry + 12 * u, rx + 2 * u, ry + 14 * u, 0xFFFFC83A);
        c.fill(rx - u, ry + 14 * u, rx + u, ry + (flick ? 17 : 16) * u, 0xFFFF8A1F);
    }

    private static void headBurst(G g, int cx, int cy) {
        Random r = new Random(g.n.start ^ 0x5DEECE66DL);
        for (int i = 0; i < 22; i++) {
            double ang = -Math.PI / 2 + (r.nextDouble() - 0.5) * Math.PI * 1.6;
            double spd = 70 + r.nextDouble() * 150, life = 1200 + r.nextDouble() * 1100, delay = r.nextDouble() * 350;
            double t = g.el - delay;
            if (t < 0 || t > life) continue;
            double s = t / 1000.0;
            int px = (int) (cx + Math.cos(ang) * spd * s), py = (int) (cy + Math.sin(ang) * spd * s + 170 * s * s);
            double k = t / life;
            int size = Math.max(2, (int) Math.round(8 * (1 - Math.max(0, (k - 0.75) / 0.25))));
            PlayerSkinDrawer.draw(g.ctx, g.skin, px - size / 2, py - size / 2, size);
        }
    }

    private static void bubbles(G g) {
        Random r = new Random(g.n.start ^ 0xB0B0L);
        for (int i = 0; i < 16; i++) {
            int bx = (int) (r.nextDouble() * g.w), rad = 4 + r.nextInt(9);
            double spd = 28 + r.nextDouble() * 45, delay = r.nextDouble() * 1500, life = 2200 + r.nextDouble() * 1400;
            double t = g.el - delay;
            if (t < 0 || t > life) continue;
            double k = t / life;
            int by = (int) (g.h * 0.95 - spd * (t / 1000.0));
            int bxw = bx + (int) Math.round(Math.sin(t / 380.0 + i) * 4);
            int a = (int) (0x66 * g.fade * (1 - k * k));
            ellipse(g.ctx, bxw, by, rad, rad, (a << 24) | 0xFFFFFF);
            ellipse(g.ctx, bxw - rad / 3, by - rad / 3, Math.max(1, rad / 3), Math.max(1, rad / 3), (Math.min(255, a + 0x30) << 24) | 0xFFFFFF);
        }
    }

    private static void sparkles(G g, int cx, int cy, int count) {
        Random r = new Random(g.n.start);
        for (int i = 0; i < count; i++) {
            double ang = r.nextDouble() * Math.PI * 2, spd = 40 + r.nextDouble() * 140;
            double life = 900 + r.nextDouble() * 1800, delay = r.nextDouble() * 500;
            double t = g.el - delay;
            int size = 1 + r.nextInt(3);
            boolean gold = r.nextBoolean();
            if (t < 0 || t > life) continue;
            double k = t / life;
            int px = (int) (cx + Math.cos(ang) * spd * (t / 1000.0) * 1.4);
            int py = (int) (cy + Math.sin(ang) * spd * (t / 1000.0) * 1.4 + 60 * k * k);
            g.ctx.fill(px, py, px + size, py + size, argb((1 - k) * g.fade, gold ? GOLD : WHITE));
        }
    }

    private static void glow(G g, double strength) {
        int a = (int) (255 * strength * g.fade);
        g.ctx.fillGradient(0, 0, g.w, g.h / 3, (a << 24) | 0xFFB000, 0x00FFB000);
        g.ctx.fillGradient(0, g.h - g.h / 3, g.w, g.h, 0x00FFB000, (a << 24) | 0xFFB000);
    }

    private static void coins(G g, int count, long startMs) {
        Random r = new Random(g.n.start ^ 0xC01DL);
        for (int i = 0; i < count; i++) {
            double x0 = r.nextDouble() * g.w, spd = 70 + r.nextDouble() * 90, delay = startMs + r.nextDouble() * 1100;
            double rad = 3 + r.nextInt(3), ph = r.nextDouble() * 6.28;
            double t = g.el - delay;
            if (t < 0) continue;
            int y = (int) (-8 + spd * t / 1000.0);
            if (y > g.h + 8) continue;
            int x = (int) (x0 + Math.sin(t / 400.0 + ph) * 6);
            double c = Math.abs(Math.cos(t / 140.0 + ph));
            double rx = Math.max(1, rad * c);
            ellipse(g.ctx, x, y, rx + 1, rad + 1, 0xFFB8860B);
            ellipse(g.ctx, x, y, rx, rad, 0xFFFFD700);
            if (c > 0.45) ellipse(g.ctx, x, y, rx * 0.55, rad * 0.55, 0xFFFFE680);
        }
    }

    private static void coinBody(G g, int cx, int cy, double rx, double ry, double facing) {
        int r = (int) Math.ceil(ry);
        for (int dy = -r; dy <= r; dy++) {
            double t = dy / ry;
            if (Math.abs(t) > 1) continue;
            int hw = (int) Math.round(rx * Math.sqrt(1 - t * t));
            if (hw <= 0) continue;
            int col = t < -0.33 ? 0xFFFFE680 : (t > 0.45 ? 0xFFB88A00 : 0xFFFFD21A);
            g.ctx.fill(cx - hw - 1, cy + dy, cx + hw + 1, cy + dy + 1, 0xFF8A6200);
            g.ctx.fill(cx - hw, cy + dy, cx + hw, cy + dy + 1, col);
        }
        if (facing > 0.5) {                                 // little embossed skull
            int ex = (int) Math.round(7 * facing), e = Math.max(1, (int) Math.round(3 * facing));
            g.ctx.fill(cx - ex - e, cy - 9, cx - ex + e, cy - 5, 0xFF1A1A1A);
            g.ctx.fill(cx + ex - e, cy - 9, cx + ex + e, cy - 5, 0xFF1A1A1A);
            int mw = (int) Math.round(8 * facing);
            g.ctx.fill(cx - mw, cy + 1, cx + mw, cy + 3, 0xFF1A1A1A);
            g.ctx.fill(cx - mw, cy + 6, cx + mw, cy + 8, 0xFF1A1A1A);
        }
    }

    private static void glasses(G g, int hx, int hy, int S, double drop) {
        int ey = hy + (int) (S * 0.42) - (int) ((1 - drop) * S * 1.4);
        int lh = Math.max(3, (int) (S * 0.2)), lw = (int) (S * 0.44);
        int lx1 = hx + (int) (S * 0.04), lx2 = hx + (int) (S * 0.52);
        int k = 0xFF000000, arm = Math.max(1, lh / 4);
        g.ctx.fill(lx1, ey, lx1 + lw, ey + lh, k);
        g.ctx.fill(lx2, ey, lx2 + lw, ey + lh, k);
        g.ctx.fill(lx1 + lw, ey, lx2, ey + Math.max(1, lh / 3), k);
        g.ctx.fill(hx - 2, ey, lx1, ey + arm, k);
        g.ctx.fill(lx2 + lw, ey, hx + S + 2, ey + arm, k);
        g.ctx.fill(lx1 + 2, ey + 1, lx1 + 4, ey + 2, 0xAAFFFFFF);
        g.ctx.fill(lx2 + 2, ey + 1, lx2 + 4, ey + 2, 0xAAFFFFFF);
    }

    private static void crown(G g, int cx, int headTop, int HS) {
        int bw = (int) (HS * 0.7), bx = cx - bw / 2, by = headTop - (int) (HS * 0.10), ph = (int) (HS * 0.22);
        g.ctx.fill(bx, by - 4, bx + bw, by + 3, 0xFFFFD700);
        g.ctx.fill(bx, by - 4, bx + bw, by - 3, 0xFFFFF2A0);
        int pw = Math.max(3, bw / 5);
        for (int i = 0; i < 3; i++) {
            int px = bx + i * (bw - pw) / 2;
            g.ctx.fill(px, by - 4 - ph, px + pw, by - 4, 0xFFFFD700);
            g.ctx.fill(px + pw / 2, by - 6 - ph, px + pw / 2 + 2, by - 4 - ph, 0xFFFFF2A0);
        }
        g.ctx.fill(cx - 2, by - 2, cx + 2, by + 1, 0xFFD83A32);
    }

    private static void laser(G g, int ex, int ey, double ang, double a) {
        g.ms.pushMatrix();
        g.ms.translate((float) ex, (float) ey);
        g.ms.rotate((float) ang);
        int len = Math.max(g.w, g.h) + 100;
        g.ctx.fill(0, -3, len, 3, argb(0.25 * a, PINK));
        g.ctx.fill(0, -1, len, 1, argb(a, PINK));
        g.ctx.fill(0, 0, len, 1, argb(a, WHITE));
        g.ms.popMatrix();
    }

    private static void rays(G g, int cx, int cy, double r0, double r1, int count, double rot, int argb, int thick) {
        for (int i = 0; i < count; i++) {
            g.ms.pushMatrix();
            g.ms.translate((float) cx, (float) cy);
            g.ms.rotate((float) (rot + i * 2 * Math.PI / count));
            g.ctx.fill((int) r0, 0, (int) r1, Math.max(1, thick), argb);
            g.ms.popMatrix();
        }
    }

    private static void ring(DrawContext ctx, int cx, int cy, double r, int dots, double rot, int size, int argb) {
        for (int i = 0; i < dots; i++) {
            double a = rot + i * 2 * Math.PI / dots;
            int x = (int) Math.round(cx + Math.cos(a) * r), y = (int) Math.round(cy + Math.sin(a) * r);
            ctx.fill(x, y, x + size, y + size, argb);
        }
    }

    private static void ellipse(DrawContext ctx, int cx, int cy, double rx, double ry, int argb) {
        int r = (int) Math.ceil(ry);
        for (int dy = -r; dy <= r; dy++) {
            double t = dy / ry;
            if (Math.abs(t) > 1) continue;
            int hw = (int) Math.round(rx * Math.sqrt(1 - t * t));
            if (hw <= 0) continue;
            ctx.fill(cx - hw, cy + dy, cx + hw, cy + dy + 1, argb);
        }
    }

    private static void stars(G g, int count, double alpha) {
        Random r = new Random(g.n.start ^ 0x57A5L);
        for (int i = 0; i < count; i++) {
            int x = r.nextInt(Math.max(1, g.w)), y = r.nextInt(Math.max(1, g.h));
            double ph = r.nextDouble() * 6.28, tw = 0.5 + 0.5 * Math.sin(g.el / 220.0 + ph);
            int sz = r.nextInt(4) == 0 ? 2 : 1;
            g.ctx.fill(x, y, x + sz, y + sz, argb(alpha * tw, WHITE));
        }
    }

    private static void confetti(G g, int count, long startMs, int[] cols) {
        Random r = new Random(g.n.start ^ 0xC0FFEEL);
        for (int i = 0; i < count; i++) {
            double x0 = r.nextDouble() * g.w, vy = 60 + r.nextDouble() * 120;
            double life = 2500 + r.nextDouble() * 1500, delay = startMs + r.nextDouble() * 1200;
            int sz = 2 + r.nextInt(2), col = cols[i % cols.length];
            double t = g.el - delay;
            if (t < 0 || t > life) continue;
            int y = (int) (-10 + vy * t / 1000.0), x = (int) (x0 + Math.sin(t / 300.0 + i) * 14);
            g.ctx.fill(x, y, x + sz, y + sz, argb(Math.min(1, (1 - t / life) * 2) * g.fade, col));
        }
    }

    private static void burst(G g, int cx, int cy, int count, long delay, double spd, int[] cols, long seed) {
        Random r = new Random(seed ^ g.n.start);
        double t = g.el - delay;
        if (t < 0 || t > 1500) return;
        double s = t / 1000.0, k = t / 1500.0;
        for (int i = 0; i < count; i++) {
            double ang = i * 2 * Math.PI / count + r.nextDouble() * 0.3, v = spd * (0.6 + r.nextDouble() * 0.6);
            int x = (int) (cx + Math.cos(ang) * v * s), y = (int) (cy + Math.sin(ang) * v * s + 70 * s * s);
            g.ctx.fill(x, y, x + 2, y + 2, argb(1 - k, cols[i % cols.length]));
        }
    }

    // ================================================================ small helpers
    private static void head(G g, int x, int y, int size) { PlayerSkinDrawer.draw(g.ctx, g.skin, x, y, size); }

    private static void fillA(G g, int x1, int y1, int x2, int y2, double alpha, int rgb) {
        g.ctx.fill(x1, y1, x2, y2, argb(alpha, rgb));
    }

    private static void text(G g, String s, int cx, int y, float sc, int rgb, double alpha) {
        g.ms.pushMatrix();
        g.ms.translate((float) cx, (float) y);
        g.ms.scale(sc, sc);
        g.ctx.drawCenteredTextWithShadow(g.tr, Text.literal(s), 0, 0, targb(alpha, rgb));
        g.ms.popMatrix();
    }

    private static void textNS(G g, String s, int cx, int y, float sc, int rgb, double alpha) {
        g.ms.pushMatrix();
        g.ms.translate((float) cx, (float) y);
        g.ms.scale(sc, sc);
        g.ctx.drawText(g.tr, s, -g.tr.getWidth(s) / 2, 0, targb(alpha, rgb), false);
        g.ms.popMatrix();
    }

    private static void outlined(G g, String s, int cx, int y, float sc, int rgb, int outRgb, double alpha) {
        g.ms.pushMatrix();
        g.ms.translate((float) cx, (float) y);
        g.ms.scale(sc, sc);
        int x = -g.tr.getWidth(s) / 2, oc = targb(alpha, outRgb);
        int[][] d = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}, {-1, -1}, {1, 1}, {-1, 1}, {1, -1}};
        for (int[] o : d) g.ctx.drawText(g.tr, s, x + o[0], o[1], oc, false);
        g.ctx.drawText(g.tr, s, x, 0, targb(alpha, rgb), false);
        g.ms.popMatrix();
    }

    private static void rainbowText(G g, String s, int cx, int y, float sc, long el) {
        g.ms.pushMatrix();
        g.ms.translate((float) cx, (float) y);
        g.ms.scale(sc, sc);
        int x = -g.tr.getWidth(s) / 2;
        for (int i = 0; i < s.length(); i++) {
            String ch = String.valueOf(s.charAt(i));
            g.ctx.drawText(g.tr, ch, x, 0, targb(g.fade, hsv(el / 700.0 + i * 0.08, 0.85, 1.0)), true);
            x += g.tr.getWidth(ch);
        }
        g.ms.popMatrix();
    }

    private static float fit(G g, String s, float sc, int maxW) {
        int tw = Math.max(1, g.tr.getWidth(s));
        return tw * sc > maxW ? maxW / (float) tw : sc;
    }

    private static double clamp01(double v) { return Math.max(0, Math.min(1, v)); }

    private static double bounce(double t) {
        double n1 = 7.5625, d1 = 2.75;
        if (t < 1 / d1) return n1 * t * t;
        if (t < 2 / d1) { t -= 1.5 / d1; return n1 * t * t + 0.75; }
        if (t < 2.5 / d1) { t -= 2.25 / d1; return n1 * t * t + 0.9375; }
        t -= 2.625 / d1;
        return n1 * t * t + 0.984375;
    }

    /** alpha for text (never 0, vanilla treats 0 as opaque). */
    private static int targb(double alpha, int rgb) { return (PaymentHud.clampA(alpha) << 24) | (rgb & 0xFFFFFF); }

    /** alpha for fills (0 allowed). */
    private static int argb(double alpha, int rgb) { return fillArgb(alpha, rgb); }

    private static int fillArgb(double alpha, int rgb) {
        return (Math.max(0, Math.min(255, (int) (alpha * 255))) << 24) | (rgb & 0xFFFFFF);
    }

    private static int hsv(double h, double s, double v) {
        h = ((h % 1) + 1) % 1;
        double c = v * s, x = c * (1 - Math.abs((h * 6) % 2 - 1)), m = v - c, r, gg, b;
        int k = (int) (h * 6) % 6;
        if (k == 0) { r = c; gg = x; b = 0; }
        else if (k == 1) { r = x; gg = c; b = 0; }
        else if (k == 2) { r = 0; gg = c; b = x; }
        else if (k == 3) { r = 0; gg = x; b = c; }
        else if (k == 4) { r = x; gg = 0; b = c; }
        else { r = c; gg = 0; b = x; }
        return (((int) ((r + m) * 255)) << 16) | (((int) ((gg + m) * 255)) << 8) | ((int) ((b + m) * 255));
    }
}
