#!/system/bin/sh
NODE="/sys/devices/platform/charger/bypass_charger"
CURRENT=$(cat "$NODE" 2>/dev/null)

if [ "$CURRENT" = "0" ] || [ -z "$CURRENT" ]; then
    echo 110 > "$NODE"
    cmd settings put secure key_security_side_road_charge_state 1 2>/dev/null
    ACTION_MSG=">>> TOGGLED: Bypass Charging ENABLED <<<"
else
    echo 0 > "$NODE"
    cmd settings put secure key_security_side_road_charge_state 0 2>/dev/null
    ACTION_MSG=">>> TOGGLED: Bypass Charging DISABLED <<<"
fi

STATUS=$(cat /sys/class/power_supply/battery/status 2>/dev/null)
MODE=$(cat /sys/devices/platform/charger/Charging_mode 2>/dev/null)
ADAPTER=$(cat /sys/devices/platform/charger/adapter_capacity 2>/dev/null)
VBUS=$(cat /sys/devices/platform/charger/ADC_Charger_Voltage 2>/dev/null)
IBAT=$(cat /sys/devices/platform/charger/ADC_Charging_Current 2>/dev/null)
CURR_UA=$(cat /sys/class/power_supply/battery/current_now 2>/dev/null)
PD=$(cat /sys/devices/platform/charger/pd_type 2>/dev/null)
SLAVE=$(cat /sys/class/power_supply/mtk-slave-charger/online 2>/dev/null)
LEVEL=$(cat /sys/class/power_supply/battery/capacity 2>/dev/null)
BYPASS=$(cat "$NODE" 2>/dev/null)
PERF=$(cmd settings get system POWER_PERFORMANCE_MODE_OPEN 2>/dev/null)

echo "=========================================="
echo "   POVA 5 Pro (LH8n) Charging Bridge      "
echo "              By Kagiyama                 "
echo "=========================================="
echo "$ACTION_MSG"
echo "------------------------------------------"
if [ "$BYPASS" = "1" ] || [ "$BYPASS" = "110" ]; then
    echo "Bypass Status  : ACTIVE (10s HyperIsland)"
else
    echo "Bypass Status  : OFF (Normal Charging)"
fi
echo "Perf Mode      : $( [ "$PERF" = "1" ] && echo "ON (Ultimate Mode)" || echo "OFF (Balanced)" )"
echo "Battery Level  : ${LEVEL}% (${STATUS})"
echo "Charging Mode  : ${MODE}"
echo "PD Protocol    : ${PD}"
echo "Adapter Rating : ${ADAPTER}W"
echo "VBUS Voltage   : ${VBUS} mV"
echo "ADC Current    : ${IBAT} mA (Gauge: ${CURR_UA} uA)"
echo "Dual Charger   : $( [ "$SLAVE" = "1" ] && echo "Active (68W Dual Pump)" || echo "Single Stage" )"
echo "=========================================="
