package com.dilly.donutpay;

import com.dilly.donutpay.gui.ConfigScreen;
import com.dilly.donutpay.gui.StatsScreen;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientLifecycleEvents;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * DonutPay Tracker - purely passive and client side.
 * It only READS system/game messages sent by the server. It never sends chat,
 * commands (other than local client commands), payments, or touches session tokens.
 */
public class DonutPayClient implements ClientModInitializer {
    public static final Logger LOG = LoggerFactory.getLogger("donutpay");

    /**
     * "<name> paid you $10M" / "Name paid you $ 1,500" (K/M/B/T suffixes).
     * Only symbols may come before the name, so player chat such as "<Bob> X paid you $1B"
     * or "Bob » X paid you $1B" does NOT match.
     */
    static final Pattern PAID_YOU = Pattern.compile(
        "^[^A-Za-z0-9_.]*([A-Za-z0-9_.]{3,17}) paid you [$\\uFF04]\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)\\s?([kKmMbBtT])?(?![A-Za-z0-9]).*$",
        Pattern.CASE_INSENSITIVE);

    /** Second format: "You received $1M from ThatMob." (trailing period optional). */
    static final Pattern RECEIVED = Pattern.compile(
        "^[^A-Za-z0-9_.]*You received [$\\uFF04]\\s*([0-9][0-9,]*(?:\\.[0-9]+)?)\\s?([kKmMbBtT])?(?![A-Za-z0-9]) from ([A-Za-z0-9_.]{3,17})\\s*$",
        Pattern.CASE_INSENSITIVE);

    static boolean debug = false;
    private static String lastKey = "";
    private static long lastAt = 0;

    private static KeyBinding openConfig, openStats;
    private static final KeyBinding.Category CATEGORY =
        KeyBinding.Category.create(Identifier.of("donutpay", "main"));

    @Override
    public void onInitializeClient() {
        Config.load();
        Stats.load();

        openConfig = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.donutpay.config", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_K, CATEGORY));
        openStats = KeyBindingHelper.registerKeyBinding(new KeyBinding(
            "key.donutpay.stats", InputUtil.Type.KEYSYM, GLFW.GLFW_KEY_J, CATEGORY));

        // GAME = system messages from the server (chat-style AND action bar).
        // Signed player chat arrives on a different event (CHAT) which we do NOT listen to.
        ClientReceiveMessageEvents.GAME.register((message, overlay) ->
            handleServerMessage(message.getString(), overlay));

        // Fallback: some servers deliver plugin messages as "profileless" chat. A profileless
        // message has NO player sender (sender == null), so it is still server-generated and
        // cannot be typed by another player. Messages with a real sender are always ignored.
        ClientReceiveMessageEvents.CHAT.register((message, signed, sender, params, ts) -> {
            if (sender == null) handleServerMessage(message.getString(), false);
        });

        HudElementRegistry.addLast(Identifier.of("donutpay", "payment_hud"), (ctx, tick) -> PaymentHud.render(ctx));

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            while (openConfig.wasPressed()) mc.setScreen(new ConfigScreen(null));
            while (openStats.wasPressed()) mc.setScreen(new StatsScreen(null));
        });

        ClientLifecycleEvents.CLIENT_STOPPING.register(mc -> { Stats.get().save(); Config.save(); });

        ClientCommandRegistrationCallback.EVENT.register((dispatcher, access) -> {
            dispatcher.register(ClientCommandManager.literal("donutpay")
                .executes(c -> { MinecraftClient.getInstance().send(() ->
                    MinecraftClient.getInstance().setScreen(new ConfigScreen(null))); return 1; })
                .then(ClientCommandManager.literal("stats").executes(c -> {
                    MinecraftClient.getInstance().send(() ->
                        MinecraftClient.getInstance().setScreen(new StatsScreen(null))); return 1; }))
                .then(ClientCommandManager.literal("resetsession").executes(c -> {
                    Stats.get().resetSession();
                    c.getSource().sendFeedback(Text.literal("[DonutPay] Session reset.")); return 1; }))
                .then(ClientCommandManager.literal("debug").executes(c -> {
                    debug = !debug;
                    c.getSource().sendFeedback(Text.literal("[DonutPay] Debug " + (debug ? "ON - pay yourself / wait for a payment" : "OFF")));
                    return 1; }))
                .then(ClientCommandManager.literal("addserver").executes(c -> {
                    var e = MinecraftClient.getInstance().getCurrentServerEntry();
                    if (e == null) { c.getSource().sendFeedback(Text.literal("[DonutPay] Join a multiplayer server first.")); return 0; }
                    String a = e.address.toLowerCase(Locale.ROOT).trim();
                    if (!Config.get().extraServers.contains(a)) { Config.get().extraServers.add(a); Config.save(); }
                    c.getSource().sendFeedback(Text.literal("[DonutPay] Tracking now allowed on " + a));
                    return 1; }))
                .then(ClientCommandManager.literal("status").executes(c -> {
                    MinecraftClient mc = MinecraftClient.getInstance();
                    var e = mc.getCurrentServerEntry();
                    c.getSource().sendFeedback(Text.literal("[DonutPay] logged in as '" + mc.getSession().getUsername()
                        + "', configured owner '" + Config.get().ownerName
                        + "', server '" + (e == null ? "none" : e.address)
                        + "', DonutSMP-only " + Config.get().onlyOnDonutSMP));
                    return 1; })));
        });
    }

    private static void dbg(String s) {
        LOG.info("[debug] {}", s);
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player != null) mc.player.sendMessage(Text.literal("\u00A77[DonutPay debug] \u00A7f" + s), false);
    }

    static void handleServerMessage(String raw, boolean overlay) {
        String text = raw.replace('\u00A0', ' ').replaceAll("\\s+", " ").trim();
        boolean interesting = text.toLowerCase(Locale.ROOT).contains("paid") || text.contains("$");
        if (debug && interesting) dbg((overlay ? "[actionbar] " : "[chat] ") + "\"" + text + "\"");

        Config cfg = Config.get();
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.player == null) return;

        Matcher m = PAID_YOU.matcher(text);
        Matcher r = RECEIVED.matcher(text);
        boolean fmtA = m.matches();
        boolean fmtB = !fmtA && r.matches();
        if (!fmtA && !fmtB) {
            if (debug && interesting) dbg("-> ignored: text did not match 'Name paid you $Amount' or 'You received $Amount from Name'");
            return;
        }
        String numStr = fmtA ? m.group(2) : r.group(1);
        String sufStr = fmtA ? m.group(3) : r.group(2);
        String sender = (fmtA ? m.group(1) : r.group(3)).replaceAll("\\.+$", "");

        // Only count payments for the configured account.
        String me = mc.getSession().getUsername();
        if (!me.equalsIgnoreCase(cfg.ownerName)) {
            if (debug) dbg("-> ignored: logged in as " + me + " but configured owner is " + cfg.ownerName);
            return;
        }

        // Only on DonutSMP (unless disabled in settings).
        if (cfg.onlyOnDonutSMP) {
            var entry = mc.getCurrentServerEntry();
            String addr = entry == null ? "" : entry.address.toLowerCase(Locale.ROOT);
            boolean ok = addr.contains("donutsmp");
            for (String extra : cfg.extraServers)
                if (extra != null && !extra.isBlank() && addr.contains(extra.toLowerCase(Locale.ROOT).trim())) ok = true;
            if (!ok) {
                if (debug) dbg("-> ignored: server '" + (entry == null ? "none" : entry.address)
                    + "' is not allowed. Run /donutpay addserver while on it.");
                return;
            }
        }

        long amount;
        try { amount = parse(numStr, sufStr); } catch (NumberFormatException e) { return; }
        if (amount <= 0 || sender.equalsIgnoreCase(me)) return;

        // The same payment may arrive in chat AND the action bar: count it once.
        long now = System.currentTimeMillis();
        String key = sender.toLowerCase(Locale.ROOT) + ":" + amount;
        if (key.equals(lastKey) && now - lastAt < 1500) return;
        lastKey = key; lastAt = now;

        if (debug) dbg("-> COUNTED " + sender + " " + Stats.fmt(amount));
        mc.execute(() -> {
            Stats.get().record(sender, amount);
            PaymentHud.onPayment(sender, amount, false);
        });
    }

    static long parse(String num, String suffix) {
        double v = Double.parseDouble(num.replace(",", ""));
        double mult = 1;
        if (suffix != null) {
            switch (Character.toLowerCase(suffix.charAt(0))) {
                case 'k' -> mult = 1e3;
                case 'm' -> mult = 1e6;
                case 'b' -> mult = 1e9;
                case 't' -> mult = 1e12;
            }
        }
        return Math.round(v * mult);
    }
}
