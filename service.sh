#!/system/bin/sh
MODDIR=${0%/*}

# Wait for boot completion
while [ "$(getprop sys.boot_completed)" != "1" ]; do
    sleep 2
done

# Ensure Game Turbo & Performance Mode properties and Control Center tile remain enabled
setprop persist.vendor.smart.bypass.plus true 2>/dev/null || true
setprop persist.sys.power_mode_support true 2>/dev/null || true
pm enable com.miui.securitycenter/com.miui.powercenter.powersaver.PerformanceWildModeTileService >/dev/null 2>&1 || true
pm grant com.pova5pro.bypass android.permission.WRITE_SECURE_SETTINGS >/dev/null 2>&1 || true

# Ensure bypass_charge CLI stays accessible on KernelSU PATH (metamodule-less compatible)
if [ -d "/data/adb/ksu/bin" ] && [ -f "$MODDIR/system/bin/bypass_charge" ]; then
    chmod 0755 "$MODDIR/system/bin/bypass_charge" 2>/dev/null || true
    ln -sf "$MODDIR/system/bin/bypass_charge" /data/adb/ksu/bin/bypass_charge 2>/dev/null || \
        cp -f "$MODDIR/system/bin/bypass_charge" /data/adb/ksu/bin/bypass_charge 2>/dev/null || true
    chmod 0755 /data/adb/ksu/bin/bypass_charge 2>/dev/null || true
fi

# Kill any existing daemon instances (including legacy charging_bridge.sh)
pkill -f "charging_bridge.sh" 2>/dev/null || true
pkill -f "BypassIslandDaemon" 2>/dev/null || true
sleep 1

# Launch unified charging + bypass island daemon (cd / avoids NULL getcwd SIGSEGV)
if [ -f "$MODDIR/bypass_island.dex" ]; then
    cd /
    nohup /system/bin/app_process -Djava.class.path="$MODDIR/bypass_island.dex" /system/bin BypassIslandDaemon >/dev/null 2>&1 &
fi
