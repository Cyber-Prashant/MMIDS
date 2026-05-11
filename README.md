# 🛡️ MMIDS — Jetpack Compose Edition

Pure Kotlin + Jetpack Compose. No Flutter needed.

---

## 📦 Project Structure

```
mmids_compose/
├── app/src/main/
│   ├── AndroidManifest.xml
│   ├── java/com/mmids/
│   │   ├── MMIDSApp.kt              ← Application class
│   │   ├── MainActivity.kt          ← Entry point
│   │   ├── ui/
│   │   │   ├── theme/Theme.kt       ← Dark theme + colors
│   │   │   ├── navigation/          ← Nav graph
│   │   │   ├── components/          ← Reusable Compose components
│   │   │   └── screens/
│   │   │       ├── DashboardScreen.kt
│   │   │       ├── LogViewerScreen.kt
│   │   │       ├── SettingsScreen.kt
│   │   │       └── IntruderAlertActivity.kt
│   │   ├── services/
│   │   │   ├── MonitoringService.kt
│   │   │   └── AppUsageTracker.kt
│   │   ├── receivers/Receivers.kt
│   │   └── admin/MMIDSDeviceAdmin.kt
│   └── res/
│       ├── xml/device_admin_policies.xml
│       ├── raw/shutter.mp3          ← ADD THIS
│       └── values/themes.xml
├── build.gradle
└── settings.gradle
```

---

## ⚙️ Setup on Kali Linux

### 1. Install Java 17
```bash
sudo apt install -y openjdk-17-jdk wget unzip
java -version
```

### 2. Install Android SDK
```bash
mkdir -p ~/Android/cmdline-tools && cd ~/Android/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-11076708_latest.zip
mv cmdline-tools latest

echo 'export ANDROID_HOME="$HOME/Android"' >> ~/.bashrc
echo 'export PATH="$ANDROID_HOME/cmdline-tools/latest/bin:$PATH"' >> ~/.bashrc
echo 'export PATH="$ANDROID_HOME/platform-tools:$PATH"' >> ~/.bashrc
source ~/.bashrc

sdkmanager --install "platform-tools" "platforms;android-34" "build-tools;34.0.0"
yes | sdkmanager --licenses
```

### 3. Add Shutter Sound
Place any .mp3 file named `shutter.mp3` into:
```
app/src/main/res/raw/shutter.mp3
```

### 4. Build APK
```bash
cd mmids_compose

# Make gradlew executable
chmod +x gradlew

# Build release APK
./gradlew assembleRelease
```

APK output:
```
app/build/outputs/apk/release/app-release.apk
```

### 5. Install on Phone
```bash
adb install app/build/outputs/apk/release/app-release.apk
```

---

## 🔓 Access Your Hidden App

| Method | How |
|--------|-----|
| Dialer Code | Open phone dialer → type `*#66437#` |
| ADB (dev) | `adb shell am start -n com.mmids/.MainActivity` |

---

## 🔘 Volume Triggers (Screen LOCKED only)

| Button | Action |
|--------|--------|
| Volume UP | Monitoring ON 🟢 |
| Volume DOWN | Monitoring OFF 🔴 |

---

## ⚙️ First Launch Permissions

```
1. Usage Access   → Settings > Apps > Special Access > Usage Access > System Service → ON
2. Display over apps → Granted on first launch
3. Notifications  → Granted on first launch
4. Device Admin   → Settings screen → Enable
```

---

## 🔌 Shutdown Behavior
- Monitoring auto-disabled on power off
- Logs saved and preserved with DEVICE_SHUTDOWN marker

---

## ✅ Ethical Use Only
Install only on your own device. This app is for personal security awareness and lab/educational use.
