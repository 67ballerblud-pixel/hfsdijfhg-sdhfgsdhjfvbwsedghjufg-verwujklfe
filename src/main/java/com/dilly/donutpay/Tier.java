package com.dilly.donutpay;

/** One distinct full-screen show per payment level. The highest tier whose minimum is reached plays. */
public enum Tier {
    COIN_RAIN(1_000_000L, 3400, "$1M Coins"),
    DEAL_WITH_IT(5_000_000L, 4300, "$5M Shades"),
    BREAKING_NEWS(10_000_000L, 5600, "$10M News"),
    WANTED(50_000_000L, 5200, "$50M Wanted"),
    BIG(100_000_000L, 5500, "$100M Energy"),
    LEGENDARY(500_000_000L, 6500, "$500M Legend"),
    MEGA(1_000_000_000L, 5500, "$1B Rocket"),
    JACKPOT(5_000_000_000L, 6500, "$5B Jackpot"),
    SUPERNOVA(10_000_000_000L, 8000, "$10B Nova"),
    GOD(50_000_000_000L, 7500, "$50B God"),
    RICH(100_000_000_000L, 6500, "$100B Rich"),
    LEGEND(1_000_000_000_000L, 8500, "$1T Legend");

    public final long min;
    public final int ms;
    public final String btn;

    Tier(long min, int ms, String btn) { this.min = min; this.ms = ms; this.btn = btn; }

    public static Tier of(long amount) {
        Tier best = null;
        for (Tier t : values()) if (amount >= t.min) best = t;
        return best;
    }
}
