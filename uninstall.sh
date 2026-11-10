#!/system/bin/sh
# Kill running daemon instance
pkill -f "BypassIslandDaemon" 2>/dev/null || true

# Revert hardware bypass to normal charging
echo 0 > /sys/devices/platform/charger/bypass_charger 2>/dev/null || true
echo 0 > /sys/devices/platform/charger/tran_aichg_disable_charger 2>/dev/null || true

# Remove CLI symlink and temporary runtime logs
rm -f /data/adb/ksu/bin/bypass_charge 2>/dev/null
rm -f /data/local/tmp/bypass_island.log 2>/dev/null

