#!/system/bin/sh
SKIPUNZIP=0

ui_print "***********************************************"
ui_print "*   TECNO POVA 5 Pro 5G (LH8n) Charging Bridge *"
ui_print "*      Unified Binder Bridge + Bypass App      *"
ui_print "*             v1.3.2  (By Kagiyama)            *"
ui_print "***********************************************"

ui_print "- Setting executable permissions..."
set_perm_recursive "$MODPATH" 0 0 0755 0644
set_perm "$MODPATH/service.sh" 0 0 0755
[ -f "$MODPATH/action.sh" ] && set_perm "$MODPATH/action.sh" 0 0 0755
[ -f "$MODPATH/uninstall.sh" ] && set_perm "$MODPATH/uninstall.sh" 0 0 0755
[ -f "$MODPATH/system/bin/bypass_charge" ] && set_perm "$MODPATH/system/bin/bypass_charge" 0 0 0755

# Expose bypass_charge CLI via KernelSU PATH without requiring a metamodule
if [ -d "/data/adb/ksu/bin" ] && [ -f "$MODPATH/system/bin/bypass_charge" ]; then
    ln -sf "/data/adb/modules/pova5pro_hyperos_charge_bridge/system/bin/bypass_charge" /data/adb/ksu/bin/bypass_charge 2>/dev/null || \
        cp -f "$MODPATH/system/bin/bypass_charge" /data/adb/ksu/bin/bypass_charge 2>/dev/null || true
    chmod 0755 /data/adb/ksu/bin/bypass_charge 2>/dev/null || true
fi

# Clean up legacy standalone scripts if present
rm -f /data/adb/service.d/charging_bridge.sh 2>/dev/null

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

# Kill old daemon instances (including legacy charging_bridge.sh)
pkill -f "charging_bridge.sh" 2>/dev/null || true
pkill -f "BypassIslandDaemon" 2>/dev/null || true

# Start unified charging + bypass island daemon (cd / avoids NULL getcwd SIGSEGV)
if [ -f "$MODPATH/bypass_island.dex" ]; then
    cd /
    nohup /system/bin/app_process -Djava.class.path="$MODPATH/bypass_island.dex" /system/bin BypassIslandDaemon >/dev/null 2>&1 &
fi

ui_print "- Unified Charging Bridge & Bypass Island active!"
ui_print "***********************************************"
ui_print "*     Installation complete! By Kagiyama      *"
ui_print "***********************************************"
