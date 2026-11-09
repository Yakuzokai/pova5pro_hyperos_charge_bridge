#!/system/bin/sh
MODDIR=${0%/*}

# Wait for boot completion
while [ "$(getprop sys.boot_completed)" != "1" ]; do
    sleep 2
done

# Kill any existing instance to avoid duplicate daemons
pkill -f "charging_bridge.sh" 2>/dev/null || true
sleep 1

# Launch charging bridge daemon in background
if [ -f "$MODDIR/charging_bridge.sh" ]; then
    chmod 755 "$MODDIR/charging_bridge.sh"
    nohup /system/bin/sh "$MODDIR/charging_bridge.sh" >/dev/null 2>&1 &
fi
