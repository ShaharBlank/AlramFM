# AlarmFM 📻

An Android alarm clock app that wakes you up to live Israeli radio streams instead of a plain ringtone — with an automatic fallback to any system alarm sound if the stream is unavailable.

Built with a UI/UX identical to the stock Android Clock app.

---

## Features

- ⏰ **Full alarm clock** — set, edit, delete, enable/disable alarms
- 📻 **11 Israeli radio stations** — streams live audio when the alarm fires
- 🔇 **Offline fallback** — if the stream fails, automatically switches to any system ringtone you choose (or stays silent)
- 🔁 **Repeat days** — set alarms to repeat on selected days of the week
- 😴 **Snooze** — configurable snooze from the full-screen ring screen or notification
- 📳 **Vibration** — toggle per alarm
- 🔒 **Full-screen over lock screen** — alarm activity shows on top of the lock screen, identical to the stock clock
- 🔋 **Reliable in background** — uses `setExactAndAllowWhileIdle()` and survives Doze mode
- 🔁 **Survives reboot** — all enabled alarms are rescheduled automatically on boot

---

## Radio Stations

| Station | Hebrew | Frequency |
|---|---|---|
| Galgalatz | גלגלץ | 91.8 / 96.6 FM |
| Galei Tzahal | גלי צה"ל | 98.5 / 106.0 FM |
| Kan Bet | כאן ב | 94.5 / 95.0 FM |
| Kan Gimel | כאן גימל | 89.7 / 97.5 FM |
| Kan 88 | כאן 88 | 88.0 FM |
| 103FM | 103FM | 103.0 FM |
| Radio Tel Aviv 102FM | רדיו תל אביב | 102.0 FM |
| Eco 99FM | אקו 99FM | 99.0 FM |
| Radio Haifa | רדיו חיפה | 107.5 FM |
| Galey Israel | גלי ישראל | 94.0 FM |
| Radius 100FM | רדיוס | 100.0 FM |

---

## Tech Stack

| Layer | Library |
|---|---|
| Language | Kotlin |
| UI | Material3, ViewBinding, ConstraintLayout |
| Architecture | MVVM — ViewModel + Flow |
| Database | Room (SQLite) |
| Radio streaming | ExoPlayer (Media3) — supports MP3, AAC, HLS |
| Alarms | AlarmManager `setExactAndAllowWhileIdle` |
| Fallback sounds | `RingtoneManager` (all system alarm tones) |
| Async | Kotlin Coroutines |

---

## Requirements

- **Android 8.0 (API 26)** or higher
- **Internet connection** for radio streaming (fallback sound works offline)
- Permissions requested on first launch:
    - `SCHEDULE_EXACT_ALARM` — required to fire alarms on time
    - `POST_NOTIFICATIONS` — required on Android 13+ for the ringing notification
    - Battery optimization exemption — recommended for reliability in background

---

## Project Structure

```
app/src/main/java/com/example/alarmfm/
├── AlarmFMApp.kt                  # Application class
├── data/
│   ├── Alarm.kt                   # Room entity
│   ├── AlarmDao.kt                # Database queries
│   └── AlarmDatabase.kt           # Singleton Room database
├── model/
│   ├── RadioStation.kt            # Station list + stream URLs
│   └── SoundOption.kt             # Loads system ringtones for fallback picker
├── receiver/
│   └── AlarmReceiver.kt           # BroadcastReceiver — fires alarm + handles reboot
├── service/
│   └── AlarmService.kt            # Foreground service — streams radio, auto-fallback
├── ui/
│   ├── MainActivity.kt            # Alarm list screen
│   ├── AlarmViewModel.kt          # MVVM ViewModel
│   ├── AlarmAdapter.kt            # RecyclerView adapter
│   ├── AddEditAlarmActivity.kt    # Add / edit alarm screen
│   └── AlarmRingActivity.kt       # Full-screen ring screen (lock screen overlay)
└── util/
    └── AlarmScheduler.kt          # Schedules / cancels / snoozes via AlarmManager
```

---

## How It Works

### Alarm firing flow

```
AlarmManager (exact, RTC_WAKEUP)
    └─▶ AlarmReceiver.onReceive()
            └─▶ AlarmService.onStartCommand()   [foreground service]
                    ├─▶ ExoPlayer streams radio station
                    ├─▶ Notification with full-screen intent
                    │       └─▶ AlarmRingActivity (shown over lock screen)
                    └─▶ If stream fails after 10s → activateFallback()
                                └─▶ RingtoneManager plays system alarm tone
```

### Fallback logic

1. ExoPlayer fires `onPlayerError()` → fallback activates immediately
2. If the buffer is still empty after 10 seconds → fallback activates as a safety net
3. Fallback options: **None** (silent), **Default alarm sound**, or any system ringtone installed on the device

---

## Getting Started

### 1. Clone

```bash
git clone https://github.com/ShaharBlank/AlarmFM.git
cd AlarmFM
```

### 2. Open in Android Studio

File → Open → select the `AlarmFM` folder. Android Studio will detect the Gradle project automatically.

### 3. Sync

Click **Sync Now** when prompted, or go to **File → Sync Project with Gradle Files**. This downloads all dependencies.

### 4. Add the launcher icon

Right-click `res/` → **New → Image Asset** → set your logo as the foreground layer.

### 5. Run

Connect a device or start an emulator, then press ▶ **Run**.

> **Note:** The exact alarm permission (`Alarms & reminders`) will be requested on first launch via a dialog that opens the system Settings page directly.

---

## Building a Release APK

1. **Generate a keystore** (one time):
   ```
   Build → Generate Signed Bundle / APK → APK → Create new keystore
   ```

2. **Add signing config** to `app/build.gradle.kts`:
   ```kotlin
   android {
       signingConfigs {
           create("release") {
               storeFile = file("release/my-keystore.jks")
               storePassword = System.getenv("KEYSTORE_PASSWORD")
               keyAlias = System.getenv("KEY_ALIAS")
               keyPassword = System.getenv("KEY_PASSWORD")
           }
       }
       buildTypes {
           release {
               isMinifyEnabled = true
               proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
               signingConfig = signingConfigs.getByName("release")
           }
       }
   }
   ```

3. **Never commit** your `.jks` file or passwords — they are covered by `.gitignore`.

---

## Updating Stream URLs

Stream URLs occasionally change when radio stations migrate CDNs. All URLs are defined in one place:

```
app/src/main/java/com/example/alarmfm/model/RadioStation.kt
```

The current URLs use these official CDNs:
- **Kan stations** (Bet, Gimel, 88) → `live.streamtheworld.com`
- **Galei Tzahal / Galgalatz** → `glzwizzlv.bynetcdn.com`
- **103FM / Radius** → `cdn.cybercdn.live`
- **Radio Haifa** → `radiohaifa.co.il`
- **102FM** → `102.livecdn.biz`

---

## License

MIT License — see [LICENSE](LICENSE) for details.