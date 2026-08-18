# On-device verification — Flipper AI

Everything on the app's side is covered by tests (153 green) and the minified
release is boot-verified. The one thing no emulator can stand in for is the
physical Flipper Zero executing a command over BLE. This checklist closes that
last link in a few minutes and, if anything fails, tells you exactly what to send
back so the fix is precise rather than guessed.

## Setup (once)

1. Install `FlipperAI-1.0.0.apk`. If Samsung Auto Blocker warns, allow this one
   install (Settings → search "Auto Blocker").
2. Open the app → drawer (☰) → **Settings**.
3. Paste your OpenRouter key. Confirm **AI Model** shows `Claude Sonnet 5`
   (`anthropic/claude-sonnet-5`). If it shows Gemini, tap and switch — Google's
   models refuse a lot of Flipper tasks.
4. Drawer → **Device** → **Scan for Flipper** → connect. Wait until the badge
   reads **Connected** (aqua) and "Device Info" shows your firmware.

## The three capability tests

Type each into the chat and watch the approval dialog + the result.

| # | Type in chat | Expected |
|---|---|---|
| 1 — write a script | `создай badusb скрипт, который открывает блокнот` | A `.txt` written to `/ext/badusb/`. Then drawer → Files → `/ext/badusb` → the file exists with DuckyScript (REM / DELAY / GUI r / STRING). |
| 2 — manage an app | `запусти приложение NFC` | HIGH-risk dialog → hold to confirm → the **NFC app opens on the Flipper's screen**. |
| 3 — transmit a signal | `передай сигнал из /ext/subghz/<твой файл>.sub` | Confirm → the Sub-GHz app transmits; the Flipper shows the TX. |

**Success = all three do the physical thing on the Flipper.** That is the goal met.

## If something fails — send me exactly this

The errors now carry their cause, so one line usually pinpoints it:

- **Chat gives an error bubble** → send the full text. It names the model and, for
  refusals, the `finish_reason`; for hardware, the Flipper's own error suffix.
- **The dialog never appears / nothing happens** → tell me which of the three, and
  whether the spinner showed.
- **"RPC app start failed …" / "No RPC action mapping …"** → send the exact string;
  it tells me whether the app name didn't resolve or the transport dropped.

### Capturing a log (optional, most precise)

With the phone connected by USB and USB debugging on:

```bash
adb logcat -c
# reproduce the failing command in the app, then:
adb logcat -d -s FlipperProtocol:* FlipperBleService:* VesperAgent:* OpenRouterClient:* > flipper-log.txt
```

Send `flipper-log.txt`. That shows the exact RPC request/response and I can fix
the precise failure point.
