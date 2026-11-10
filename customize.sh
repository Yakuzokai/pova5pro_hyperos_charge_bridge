#!/system/bin/sh
SKIPUNZIP=0

ui_print "***********************************************"
ui_print "*   TECNO POVA 5 Pro 5G (LH8n) Charging Bridge *"
ui_print "*        HyperIsland + GameTurbo + App         *"
ui_print "*             v1.3.1  (By Kagiyama)            *"
ui_print "***********************************************"

ui_print "- Setting executable permissions..."
set_perm_recursive "$MODPATH" 0 0 0755 0644
set_perm "$MODPATH/service.sh" 0 0 0755
set_perm "$MODPATH/charging_bridge.sh" 0 0 0755
[ -f "$MODPATH/action.sh" ] && set_perm "$MODPATH/action.sh" 0 0 0755
[ -f "$MODPATH/system/bin/bypass_charge" ] && set_perm "$MODPATH/system/bin/bypass_charge" 0 0 0755

# Clean up legacy standalone script if present in service.d
if [ -f "/data/adb/service.d/charging_bridge.sh" ]; then
    ui_print "- Migrating from standalone service.d to Module..."
    rm -f /data/adb/service.d/charging_bridge.sh
fi

# Set properties for Game Turbo Bypass & HyperOS Performance / Ultimate Mode
setprop persist.vendor.smart.bypass.plus true
setprop persist.sys.power_mode_support true
pm enable com.miui.securitycenter/com.miui.powercenter.powersaver.PerformanceWildModeTileService >/dev/null 2>&1 || true

# Install / update bundled Bypass Control companion app
if [ -f "$MODPATH/BypassControl.apk" ]; then
    ui_print "- Installing Bypass Control companion app..."
    pm install -r "$MODPATH/BypassControl.apk" >/dev/null 2>&1 || true
    pm grant com.pova5pro.bypass android.permission.WRITE_SECURE_SETTINGS >/dev/null 2>&1 || true
fi

# Kill old daemon instances
pkill -f "charging_bridge.sh" 2>/dev/null || true
pkill -f "BypassIslandDaemon" 2>/dev/null || true

# Start charging bridge daemon
if [ -f "$MODPATH/charging_bridge.sh" ]; then
    cd /
    nohup /system/bin/sh "$MODPATH/charging_bridge.sh" >/dev/null 2>&1 &
fi

# Start bypass island daemon (cd / prevents getcwd NULL SIGSEGV in System_specialProperties)
if [ -f "$MODPATH/bypass_island.dex" ]; then
    cd /
    nohup /system/bin/app_process -Djava.class.path="$MODPATH/bypass_island.dex" /system/bin BypassIslandDaemon >/dev/null 2>&1 &
fi

ui_print "- Charging Bridge, Bypass Island & App active!"
ui_print "***********************************************"
ui_print "*     Installation complete! By Kagiyama      *"
ui_print "***********************************************"
