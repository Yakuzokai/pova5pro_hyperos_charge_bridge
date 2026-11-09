#!/system/bin/sh
STATUS=$(cat /sys/class/power_supply/battery/status 2>/dev/null)
MODE=$(cat /sys/devices/platform/charger/Charging_mode 2>/dev/null)
ADAPTER=$(cat /sys/devices/platform/charger/adapter_capacity 2>/dev/null)
VBUS=$(cat /sys/devices/platform/charger/ADC_Charger_Voltage 2>/dev/null)
IBAT=$(cat /sys/devices/platform/charger/ADC_Charging_Current 2>/dev/null)
PD=$(cat /sys/devices/platform/charger/pd_type 2>/dev/null)
SLAVE=$(cat /sys/class/power_supply/mtk-slave-charger/online 2>/dev/null)
LEVEL=$(cat /sys/class/power_supply/battery/capacity 2>/dev/null)

echo "=========================================="
echo " TECNO POVA 5 Pro (LH8n) Charging Status "
echo "=========================================="
echo "Battery Level  : ${LEVEL}% (${STATUS})"
echo "Charging Mode  : ${MODE}"
echo "PD Protocol    : ${PD}"
echo "Adapter Rating : ${ADAPTER}W"
echo "VBUS Voltage   : ${VBUS} mV"
echo "Battery Current: ${IBAT} mA"
echo "Dual Charger   : $( [ "$SLAVE" = "1" ] && echo "Active (68W Dual Pump)" || echo "Single Stage" )"
echo "=========================================="
