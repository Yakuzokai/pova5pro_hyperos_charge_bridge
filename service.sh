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

# Kill any existing instance to avoid duplicate daemons
pkill -f "charging_bridge.sh" 2>/dev/null || true
pkill -f "BypassIslandDaemon" 2>/dev/null || true
sleep 1

# Launch charging bridge daemon in background
if [ -f "$MODDIR/charging_bridge.sh" ]; then
    chmod 755 "$MODDIR/charging_bridge.sh"
    cd /
    nohup /system/bin/sh "$MODDIR/charging_bridge.sh" >/dev/null 2>&1 &
fi

# Launch bypass island daemon in background (cd / avoids NULL getcwd SIGSEGV)
if [ -f "$MODDIR/bypass_island.dex" ]; then
    cd /
    nohup /system/bin/app_process -Djava.class.path="$MODDIR/bypass_island.dex" /system/bin BypassIslandDaemon >/dev/null 2>&1 &
fi
