# TECNO POVA 5 Pro 5G (LH8n) - HyperOS Charging Bridge Module

KernelSU / Magisk module that bridges hardware charging state and speed tiers on the **TECNO POVA 5 Pro 5G (LH8n / MediaTek Dimensity 6080)** to **Xiaomi HyperOS SystemUI, HyperIsland, and Lockscreen Turbo View**.

---

## ⚡ Problem Solved

On stock HyperOS ports to non-Xiaomi MediaTek hardware:
- Xiaomi's proprietary `vendor.xiaomi.hardware.micharge` HAL is missing.
- Android's Health HAL reports `maxChargingCurrent = 0` and `maxChargingVoltage = 0`.
- SystemUI and HyperIsland receive no charging tier broadcasts, locking the device to slow/standard 5W/10W charging animations regardless of whether an 18W, 33W, or 68W fast charger is connected.

This module monitors the phone's physical MediaTek PMIC and charging algorithm nodes, detects the real-time charging protocol, and dispatches the exact sticky broadcasts HyperOS expects.

---

## 🚀 Features

- **68W Ultra Charge Tier:**
  - Detects MediaTek Pump Express 5.0 (`P5`) / USB-PD PPS dual-phase direct charging.
  - Automatically triggers lockscreen **"68W MAX"** Turbo wave/particle animation.
  - Triggers **HyperIsland (Dynamic Island)** green capsule showing **"68W MAX"**.
  - Streams decimal battery percentage increments (`ACTION_SOC_DECIMAL`).
- **33W Super Charge Tier:**
  - Detects 33W PPS / USB-PD contracts ($V_{\text{bus}} \ge 9\text{V}$).
  - Displays **"33W MAX"** on lockscreen and **"Quick charging"** on HyperIsland.
- **18W Fast Charge Tier:**
  - Displays double lightning bolt on AOD and lockscreen quick charge banner.
- **Standard 5V / 10W Tier:**
  - Safe fallback for standard USB and PC connections.
- **Zero-Touch & Non-Destructive:**
  - Operates purely in user-space via KernelSU / Magisk.
  - Does not modify `super.img`, `system`, or `vendor` partitions.
  - Automatically starts on system boot via `service.sh`.
- **Telemetry Action Button:**
  - Includes `action.sh` to inspect live hardware ADC voltage, current, and mode in KernelSU WebUI / terminal.

---

## 📁 Repository Structure

```text
├── META-INF/
│   └── com/google/android/
│       ├── update-binary          # Module installer binary
│       └── updater-script         # Magisk/KernelSU updater directive
├── module.prop                    # Module metadata
├── customize.sh                   # Install-time permissions and migration script
├── service.sh                     # Early-boot daemon launcher
├── charging_bridge.sh             # The main background monitoring daemon
├── action.sh                      # Live telemetry query script
├── build.py                       # Python script to package the module zip
├── .gitignore
└── README.md
```

---

## 🛠️ How to Build

Run the build script from this directory:
```bash
python build.py
```
This generates `LH8n_HyperOS_Charging_Bridge_Module_v1.0.0.zip`.

---

## 📲 Installation

### Via KernelSU / Magisk App:
1. Copy the generated `.zip` to your phone.
2. Open **KernelSU** or **Magisk** app $\to$ **Modules** $\to$ **Install from storage**.
3. Select the zip file and reboot.

### Via ADB CLI:
```bash
adb push LH8n_HyperOS_Charging_Bridge_Module_v1.0.0.zip /sdcard/Download/
adb shell "/data/adb/ksud module install /sdcard/Download/LH8n_HyperOS_Charging_Bridge_Module_v1.0.0.zip"
```

---

## 🔬 Hardware Nodes Monitored

- `/sys/devices/platform/charger/Charging_mode` (`P5`, `PE4`, `normal`)
- `/sys/devices/platform/charger/adapter_capacity` (advertised watts: `63`, `33`, `18`)
- `/sys/devices/platform/charger/ADC_Charger_Voltage` ($V_{\text{bus}}$ ADC in mV)
- `/sys/devices/platform/charger/ADC_Charging_Current` ($I_{\text{bat}}$ fuel gauge in mA)
- `/sys/devices/platform/charger/pd_type` (`PD with PPS`, `PD`, `normal`)
- `/sys/class/power_supply/mtk-slave-charger/online` (`1` = dual-phase charge pump)
- `/sys/class/power_supply/battery/status` (`Charging`, `Discharging`, `Full`)

---

## 👤 Credits & Authors
- **Kagiyama & Yakuzo**
- Ported for TECNO POVA 5 Pro 5G (LH8n) on HyperOS
