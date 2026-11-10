#!/system/bin/sh
# ==============================================================================
# TECNO POVA 5 Pro 5G (LH8n) - HyperOS Automated Charging Bridge Daemon
# Bridges Transsion MTK hardware charging nodes to HyperOS SystemUI / Island / AOD
# ==============================================================================

LAST_TYPE=-1
LAST_POWER=-1
ANIM_COUNT=0

# Give Android system services time to start up if running at boot
sleep 5

while true; do
    STATUS=""
    [ -r /sys/class/power_supply/battery/status ] && IFS= read -r STATUS < /sys/class/power_supply/battery/status 2>/dev/null
    
    if [ "$STATUS" != "Charging" ]; then
        if [ "$LAST_TYPE" -ne 0 ]; then
            am broadcast -a miui.intent.action.ACTION_QUICK_CHARGE_TYPE \
                --ei miui.intent.extra.quick_charge_type 0 \
                --ei miui.intent.extra.POWER_MAX 0 \
                --ei miui.intent.extra.CAR_CHARGE 0 >/dev/null 2>&1
            LAST_TYPE=0
            LAST_POWER=0
            ANIM_COUNT=0
        fi
        sleep 2.5
        continue
    fi

    # Read physical hardware nodes cleanly via shell builtin (zero fork overhead)
    MODE=""; ADAPTER=""; VBUS=""; PD=""; SLAVE=""
    [ -r /sys/devices/platform/charger/Charging_mode ] && IFS= read -r MODE < /sys/devices/platform/charger/Charging_mode 2>/dev/null
    [ -r /sys/devices/platform/charger/adapter_capacity ] && IFS= read -r ADAPTER < /sys/devices/platform/charger/adapter_capacity 2>/dev/null
    [ -r /sys/devices/platform/charger/ADC_Charger_Voltage ] && IFS= read -r VBUS < /sys/devices/platform/charger/ADC_Charger_Voltage 2>/dev/null
    [ -r /sys/devices/platform/charger/pd_type ] && IFS= read -r PD < /sys/devices/platform/charger/pd_type 2>/dev/null
    [ -r /sys/class/power_supply/mtk-slave-charger/online ] && IFS= read -r SLAVE < /sys/class/power_supply/mtk-slave-charger/online 2>/dev/null

    # Sanitize inputs
    [ -z "$ADAPTER" ] && ADAPTER=0
    [ -z "$VBUS" ] && VBUS=0
    [ -z "$SLAVE" ] && SLAVE=0

    # Determine Charging Tier
    if [ "$MODE" = "P5" ] || [ "$ADAPTER" -ge 60 ] || [ "$SLAVE" -eq 1 ]; then
        # 68W Ultra Charge (Dual Charge Pump / PPS direct charge)
        CURR_TYPE=4
        CURR_POWER=68
    elif [ "$ADAPTER" -ge 30 ] || { [ "$PD" != "normal" ] && [ "$VBUS" -ge 9000 ]; }; then
        # 33W Super Charge
        CURR_TYPE=4
        CURR_POWER=33
    elif [ "$VBUS" -ge 8000 ] || [ "$PD" = "PD" ]; then
        # 18W Fast Charge
        CURR_TYPE=1
        CURR_POWER=18
    else
        # Standard 5V / 10W
        CURR_TYPE=0
        CURR_POWER=10
    fi

    # Dispatch broadcast if charging tier has transitioned
    if [ "$CURR_TYPE" -ne "$LAST_TYPE" ] || [ "$CURR_POWER" -ne "$LAST_POWER" ]; then
        am broadcast -a miui.intent.action.ACTION_QUICK_CHARGE_TYPE \
            --ei miui.intent.extra.quick_charge_type "$CURR_TYPE" \
            --ei miui.intent.extra.POWER_MAX "$CURR_POWER" \
            --ei miui.intent.extra.CAR_CHARGE 0 >/dev/null 2>&1
        LAST_TYPE=$CURR_TYPE
        LAST_POWER=$CURR_POWER
        # Trigger smooth decimal counter animation for ~8 seconds
        ANIM_COUNT=16
    fi

    # Stream decimal broadcast for Turbo tiers during initial animation
    if [ "$CURR_TYPE" -eq 4 ] && [ "$ANIM_COUNT" -gt 0 ]; then
        # Generate smooth decimal progression (e.g. 15, 30, 45, 60, 75, 90)
        DECIMAL=$(( (17 - ANIM_COUNT) * 6 ))
        [ "$DECIMAL" -gt 98 ] && DECIMAL=98
        am broadcast -a miui.intent.action.ACTION_SOC_DECIMAL \
            --ei miui.intent.extra.soc_decimal "$DECIMAL" \
            --ei miui.intent.extra.soc_decimal_rate 10 >/dev/null 2>&1
        ANIM_COUNT=$(( ANIM_COUNT - 1 ))
        sleep 0.5
    else
        sleep 1.5
    fi
done

