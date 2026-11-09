#!/system/bin/sh
SKIPUNZIP=0

ui_print "***********************************************"
ui_print "*   TECNO POVA 5 Pro 5G (LH8n) Charging Bridge *"
ui_print "*       Hardware Bridge for HyperOS UI         *"
ui_print "***********************************************"

ui_print "- Setting executable permissions..."
set_perm_recursive "$MODPATH" 0 0 0755 0644
set_perm "$MODPATH/service.sh" 0 0 0755
set_perm "$MODPATH/charging_bridge.sh" 0 0 0755
[ -f "$MODPATH/action.sh" ] && set_perm "$MODPATH/action.sh" 0 0 0755

# Clean up legacy standalone script if present in service.d
if [ -f "/data/adb/service.d/charging_bridge.sh" ]; then
    ui_print "- Migrating from standalone service.d to Module..."
    rm -f /data/adb/service.d/charging_bridge.sh
fi

# Kill old daemon instance and start fresh from module directory
pkill -f "charging_bridge.sh" 2>/dev/null || true
nohup /system/bin/sh "$MODPATH/charging_bridge.sh" >/dev/null 2>&1 &

ui_print "- Charging Bridge Daemon active!"
ui_print "***********************************************"
ui_print "*  Installation complete! Plug in to test!    *"
ui_print "***********************************************"
