# TECNO POVA 5 Pro 5G (LH8n) - HyperOS Charging Bridge & Bypass Module

KernelSU / Magisk module that bridges hardware charging state, speed tiers, **NVDC Bypass Charging**, **Performance / Ultimate Mode**, and the bundled **Bypass Control** companion app on the **TECNO POVA 5 Pro 5G (LH8n / MediaTek Dimensity 6080)** to **Xiaomi HyperOS SystemUI, HyperIsland, Control Center, Battery Settings, and GameSpace / Game Turbo**.

---

## ⚡ Problem Solved

On stock HyperOS ports to non-Xiaomi MediaTek hardware:
- Xiaomi's proprietary `vendor.xiaomi.hardware.micharge` HAL is missing.
- Android's Health HAL reports `maxChargingCurrent = 0` and `maxChargingVoltage = 0`.
- SystemUI and HyperIsland receive no charging tier broadcasts, locking the device to slow/standard 5W/10W animations regardless of whether an 18W, 33W, or 68W fast charger is connected.
- Xiaomi's native Bypass Charging checks `persist.vendor.smart.bypass.plus`, ignoring Transsion MT6360/MT6833 NVDC hardware bypass (`/sys/devices/platform/charger/bypass_charger`).
- Native HyperOS **Performance / Ultimate Mode** is hidden in Battery Settings & Control Center when `persist.sys.power_mode_support` is unset.

This module monitors physical MediaTek PMIC and charging algorithm nodes, dispatches HyperOS charging tier broadcasts, bridges GameSpace/Game Turbo + Control Center + HyperIsland with purple 10s auto-hide styling, unlocks native HyperOS Performance Mode, and includes the standalone **Bypass Control** app.

---

## 🚀 Features (v1.3.2)

### 1. Dynamic Fast Charging Tiers
- **68W Ultra Charge Tier:** Detects MediaTek Pump Express 5.0 (`P5`) / USB-PD PPS dual-phase direct charging; triggers **"68W MAX"** Lockscreen & HyperIsland animations plus `ACTION_SOC_DECIMAL`.
- **33W Super Charge Tier:** Detects 33W PPS / USB-PD contracts ($V_{\text{bus}} \ge 9\text{V}$).
- **18W Fast Charge & 10W Standard Tiers:** Accurate AOD and Lockscreen quick-charge state.

### 2. GameSpace / Game Turbo In-Game Toggle
- Exposes the native **Bypass Charging** toggle inside the GameSpace / Game Turbo in-game sidebar via `persist.vendor.smart.bypass.plus=true`.
- Bidirectional sync between `Settings.Secure.key_security_side_road_charge_state` and `/sys/devices/platform/charger/bypass_charger`.

### 3. HyperIsland Purple Battery Logo & 10s Auto-Hide
- Displays **"Bypass Charging"** with vivid electric purple (`#A855F7`) percentage text and a custom `1:1` purple battery + lightning vector icon (`ic_island_battery`).
- Auto-hides into the camera punch hole cutout after **10 seconds** while staying alive for swipe-to-reveal / swipe-to-hide gestures; dismisses cleanly on disable or unplug.

### 4. Bypass Control App & Control Center Shortcuts (NEW in v1.3.0)
- **Bundled Companion App (`BypassControl.apk` / `com.pova5pro.bypass`):**
  - **Bypass Verification Card:** Verifies whether NVDC bypass is genuinely isolating battery cells (`|IBAT| <= 80 mA` while `VBUS >= 4000 mV` and `tran_aichg_disable_charger == 0`).
  - **Toggleable Debug Mode:** 500ms live telemetry stream, 9-point assertion checklist, 4-second live **Calibration Check**, full sysfs/property dump, and clipboard report export.
  - **Performance / Ultimate Mode Toggle:** Switch HyperOS Performance Mode ON/OFF directly from the app via `PowerSaverProvider`, alongside quick buttons for Control Center tiles and HyperOS Battery Settings.
- **HyperOS Control Center Shortcuts:**
  - **`Bypass Charge` (`BypassTileService`)**: Custom monochrome battery-bolt quick settings tile with live isolated mA subtitle (`Verified · +3.6 mA`); long-press opens Bypass Control.
  - **`Ultimate mode` (`PerformanceWildModeTileService`)**: Native HyperOS Performance Mode tile unlocked and enabled automatically.
- **HyperOS Battery Settings Unlock:** `persist.sys.power_mode_support=true` exposes **Current mode** (`Ultimate mode` / `Balanced` / `Battery saver`) directly inside **Settings $\to$ Battery**.

### 5. Hardware Safety Protections
- Automatically trips bypass off if USB charger is unplugged, battery capacity drops below `10%`, or battery temperature exceeds `45.0°C`.

---

## 📁 Repository Structure

```text
├── META-INF/
│   └── com/google/android/
│       ├── update-binary          # Module installer binary
│       └── updater-script         # Magisk/KernelSU updater directive
├── BypassControl.apk              # Bundled companion app (Health Check, Debug Mode & QS Tile)
├── module.prop                    # Module metadata (v1.3.2, author=Yakuzokai)
├── system.prop                    # Enables GameTurbo bypass & HyperOS Performance Mode
├── customize.sh                   # Install-time permissions, APK/CLI install & daemon launch
├── service.sh                     # Early-boot property/tile/CLI ensure & daemon launcher
├── uninstall.sh                   # Removes /data/adb/ksu/bin/bypass_charge symlink on uninstall
├── bypass_island.dex              # Unified zero-fork DEX daemon (Charging tiers + HyperIsland + GameTurbo sync)
├── src/BypassIslandDaemon.java    # Source for bypass_island.dex
├── action.sh                      # KernelSU / Magisk 1-tap Action button (Toggle & telemetry)
├── system/
│   └── bin/
│       └── bypass_charge          # CLI command (linked to /data/adb/ksu/bin/bypass_charge)
├── build.py                       # Python script to package the flashable module zip
├── .gitignore
└── README.md
```

---

## 🛠️ How to Build

Run the build script from this directory:
```bash
python build.py
```
This generates `LH8n_HyperOS_Charging_Bridge_Module_v1.3.2.zip`.

---

## 📲 Installation

### Via KernelSU / Magisk App:
1. Copy `LH8n_HyperOS_Charging_Bridge_Module_v1.3.2.zip` to your phone.
2. Open **KernelSU** or **Magisk** app $\to$ **Modules** $\to$ **Install from storage**.
3. Select the zip file and reboot (`Bypass Control` app is installed automatically!).

### Via ADB CLI:
```bash
adb push LH8n_HyperOS_Charging_Bridge_Module_v1.3.2.zip /sdcard/Download/
adb shell "su -c '/data/adb/ksud module install /sdcard/Download/LH8n_HyperOS_Charging_Bridge_Module_v1.3.2.zip'"
```

---

## 🎮 How to Use

1. **HyperOS Control Center:** Tap **Bypass Charge** or **Ultimate mode** quick tiles directly; long-press **Bypass Charge** to launch Bypass Control.
2. **Bypass Control App:** View real-time **BYPASS VERIFIED** status, toggle **Bypass Charging** / **Performance Mode**, pin Control Center shortcuts, or toggle **DEBUG** for deep hardware diagnostics.
3. **In-Game via Game Turbo Sidebar:** Swipe open the GameSpace sidebar and tap **Bypass Charging**.
4. **HyperOS Settings $\to$ Battery:** Switch **Current mode** between **Ultimate mode**, **Balanced**, and **Battery saver**.
5. **Manager Action / Terminal:** Tap **Action** in KernelSU/Magisk or run `su -c bypass_charge {on|off|toggle|status}`.

---

## 🔬 Hardware Nodes Monitored

- `/sys/devices/platform/charger/bypass_charger` (`110`/`1` = NVDC Bypass active, `0` = normal)
- `/sys/devices/platform/charger/tran_aichg_disable_charger` (`0` = power path intact)
- `/sys/devices/platform/charger/Charging_mode` (`P5`, `PE4`, `normal`)
- `/sys/devices/platform/charger/adapter_capacity` (advertised watts: `68`, `33`, `18`)
- `/sys/devices/platform/charger/ADC_Charger_Voltage` ($V_{\text{bus}}$ ADC in mV)
- `/sys/devices/platform/charger/ADC_Charging_Current` ($I_{\text{bat}}$ ADC in mA)
- `/sys/class/power_supply/battery/current_now` ($I_{\text{bat}}$ fuel gauge in µA)
- `/sys/devices/platform/charger/pd_type` (`PD with PPS`, `PD`, `normal`)
- `/sys/class/power_supply/mtk-slave-charger/online` (`1` = dual-phase charge pump)
- `/sys/class/power_supply/battery/status` (`Charging`, `Discharging`, `Full`)

---

## 👤 Credits & Author
- **Yakuzokai**
