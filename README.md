# DonutPay Tracker (Fabric, Minecraft 1.21.11)

Client-side, passive payment tracker for DonutSMP, made for **DILLYY_MC**.

## Features
- Detects real `"<name> paid you $<amount>"` server messages (K / M / B / T suffixes supported)
- **Video-style feed**: stacked top-right cards with the sender's head, name and +$amount (green; gold from $100M)
- **12 payment tiers, each with its own full-screen animation** (all inspired by the video, all drawn in code, no assets):

| Amount | Animation |
|---|---|
| $1M | **Coin rain**: spinning gold coins fall over a gold `+$1M` |
| $5M | **Deal with it**: head drops in, pixel sunglasses slam onto it, rainbow `DEAL WITH IT` |
| $10M | **Breaking news**: camera flashes, red `BREAKING NEWS` bar, anchor head, scrolling ticker |
| $50M | **Wanted poster**: swings in on a nail, head + `REWARD $50M`, `PAID` stamp slams down with screen shake |
| $100M | **Energy orb**: gold title, head orb with spinning rings, scrambling `100 MILLION ENERGY` |
| $500M | **Legendary coin**: dark spotlight, spinning gold skull coin, `LEGENDARY DONATION` |
| $1B | **To the moon**: rocket carrying the head, head particles, bubbles, `TO THE MOON` / `YEET TO THE MOON` |
| $5B | **Jackpot wheel**: casino bulbs, rainbow dot-wheel that spins down, `X WINS $5B` + confetti |
| $10B | **Supernova**: `STAR GOING CRITICAL` warning, head-orb swells, flash, then a **black hole** swallowing head particles |
| $50B | **God of Donut SMP**: sunburst rays, giant head in a gold disc, **laser eyes**, floating hype words |
| $100B | **"JK. YOU'RE RICH."**: gold flood, coin storm, glasses drop onto the head |
| $1T | **Legend**: crown, rainbow sunburst, fireworks, `LARGEST DONATION IN DONUT HISTORY` |

  Every tier also plays its own timed sound cues (rocket launch + blast, thunder, sonic boom, anvil slam...).
  Settings: `Tier animations` on/off, `Shows from` (lowest amount that gets a show), `Show sounds`, plus a **tier picker + Play** button and **Demo all** to run every animation back to back.
- Scrolling payment-history HUD + session panel (session total, best payment, top payer)
- Stats screen: lifetime total, largest payment, top payers, scrollable history
- Settings GUI (sounds, volume, pitch, style, animation, HUD corner, threshold...)
- Stats saved to `.minecraft/config/donutpay/stats.json`, settings to `config.json`

## Controls
- `K` settings, `J` stats (rebindable), or `/donutpay`, `/donutpay stats`, `/donutpay resetsession`

## Build
Requires JDK 21 and Gradle 9.2+ (internet needed for the first build).

    gradle build

Jar: `build/libs/donutpay-tracker-1.5.0.jar` (not the `-sources` jar).
Install: put it plus **Fabric API 0.141.3+1.21.11** in `.minecraft/mods` with Fabric Loader >= 0.18.1.

No Gradle? Push this folder to GitHub: the included workflow builds the jar (Actions -> artifact).

## Safety / verification
- Only listens to **system messages from the server** (`ClientReceiveMessageEvents.GAME`), never signed player chat.
- The pattern is anchored to the start of the line, so chat like `<Bob> X paid you $1B` is ignored.
- Counts only when logged in as the configured owner and (by default) connected to a `donutsmp` server.
- "Preview" buttons are labelled `[PREVIEW]` and are never saved or counted.
- The mod never sends chat/commands/payments, reads no tokens, and makes no network connections.

## If your payments don't show up
DonutSMP's exact wording may differ slightly from the video. Edit `PAID_YOU` in `DonutPayClient.java`
(copy the exact line from your chat) and rebuild. Note: if the server ever relays normal chat as system
messages, a player could imitate a payment line, so treat the tracker as a convenience, not proof of funds.
