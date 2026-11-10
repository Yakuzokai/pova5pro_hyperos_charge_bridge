import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.lang.reflect.Method;
import android.content.Intent;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;

public class BypassIslandDaemon {
    private static final String NOTIFY_ID = "bypass_charging";
    private static final String PKG_APP = "com.pova5pro.bypass";
    private static final String PKG_FALLBACK = "miui.systemui.plugin";
    private static final String ACTION_NAME = "strong_toast_action";
    private static final String CATEGORY = "text_bitmap";
    private static final long DURATION_10S = 10000L;

    private static final String ACTION_QUICK_CHARGE_TYPE = "miui.intent.action.ACTION_QUICK_CHARGE_TYPE";
    private static final String EXTRA_QUICK_CHARGE_TYPE = "miui.intent.extra.quick_charge_type";
    private static final String EXTRA_POWER_MAX = "miui.intent.extra.POWER_MAX";
    private static final String EXTRA_CAR_CHARGE = "miui.intent.extra.CAR_CHARGE";
    private static final String ACTION_SOC_DECIMAL = "miui.intent.action.ACTION_SOC_DECIMAL";
    private static final String EXTRA_SOC_DECIMAL = "miui.intent.extra.soc_decimal";
    private static final String EXTRA_SOC_DECIMAL_RATE = "miui.intent.extra.soc_decimal_rate";

    private static final String NODE_BYPASS = "/sys/devices/platform/charger/bypass_charger";
    private static final String NODE_BATTERY_STATUS = "/sys/class/power_supply/battery/status";
    private static final String NODE_CHARGER_TYPE = "/sys/devices/platform/charger/Charger_Type";
    private static final String NODE_CHARGING_MODE = "/sys/devices/platform/charger/Charging_mode";
    private static final String NODE_ADAPTER_CAPACITY = "/sys/devices/platform/charger/adapter_capacity";
    private static final String NODE_PD_TYPE = "/sys/devices/platform/charger/pd_type";
    private static final String NODE_SLAVE_ONLINE = "/sys/class/power_supply/mtk-slave-charger/online";
    private static final String NODE_CAPACITY = "/sys/class/power_supply/battery/capacity";
    private static final String NODE_TEMP = "/sys/class/power_supply/battery/temp";
    private static final String NODE_VBUS = "/sys/devices/platform/charger/ADC_Charger_Voltage";

    private static final String SETTING_SECURE_SIDE_ROAD = "key_security_side_road_charge_state";

    // Electric purple (#A855F7): 0xFFA855F7 -> signed int -5745161
    private static final int COLOR_PURPLE = -5745161;

    private static Object sStatusBarService;
    private static Method sSetStatus;
    private static final IBinder sToken = new Binder();

    private static Object sActivityManager;
    private static Method sBroadcastWithFeature;
    private static Method sBroadcastLegacy;

    private static Object sPackageManager;
    private static Method sGetPackageInfo;
    private static boolean sGetPkgFlagsIsLong = true;
    private static long sLastPkgCheckMs = 0L;
    private static boolean sCachedPkgInstalled = false;

    private static Object sSettingsProvider;
    private static Method sProviderCall;
    private static Object sAttrSource;
    private static Method sSysPropSet;

    private static boolean sIslandRegistered = false;
    private static String sActiveIslandPkg = PKG_APP;
    private static int sLastBypassHardwareState = -1;
    private static int sLastGameTurboSettingState = -1;
    private static long sLastHeartbeatSec = 0L;

    private static int sLastChargeType = -1;
    private static int sLastChargePower = -1;
    private static int sDecimalAnimCount = 0;

    private static void updateHeartbeat() {
        long nowSec = System.currentTimeMillis() / 1000L;
        if (nowSec - sLastHeartbeatSec < 5L) return;
        sLastHeartbeatSec = nowSec;
        try {
            if (sSysPropSet == null) {
                Class<?> sp = Class.forName("android.os.SystemProperties");
                sSysPropSet = sp.getMethod("set", String.class, String.class);
            }
            sSysPropSet.invoke(null, "debug.pova.bypass_daemon", String.valueOf(nowSec));
        } catch (Throwable ignored) {}
    }

    private static boolean initStatusBarService() {
        try {
            Class<?> sm = Class.forName("android.os.ServiceManager");
            Method getService = sm.getMethod("getService", String.class);
            IBinder binder = (IBinder) getService.invoke(null, "statusbar");
            if (binder == null) return false;

            Class<?> stub = Class.forName("com.android.internal.statusbar.IStatusBarService$Stub");
            sStatusBarService = stub.getMethod("asInterface", IBinder.class).invoke(null, binder);
            for (Method m : sStatusBarService.getClass().getMethods()) {
                if (m.getName().equals("setStatus") && m.getParameterTypes().length == 4) {
                    sSetStatus = m;
                    break;
                }
            }
            return sSetStatus != null;
        } catch (Throwable t) {
            sStatusBarService = null;
            sSetStatus = null;
            return false;
        }
    }

    private static void initActivityManager() {
        if (sActivityManager != null) return;
        try {
            Class<?> amClass = Class.forName("android.app.ActivityManager");
            Object am = amClass.getMethod("getService").invoke(null);
            if (am == null) return;
            for (Method m : am.getClass().getMethods()) {
                if (m.getName().equals("broadcastIntentWithFeature") && m.getParameterTypes().length == 16) {
                    sBroadcastWithFeature = m;
                } else if (m.getName().equals("broadcastIntent") && m.getParameterTypes().length == 13) {
                    sBroadcastLegacy = m;
                }
            }
            sActivityManager = am;
        } catch (Throwable t) {
            sActivityManager = null;
            sBroadcastWithFeature = null;
            sBroadcastLegacy = null;
        }
    }

    private static void sendBroadcastDirect(Intent intent) {
        initActivityManager();
        if (sActivityManager != null) {
            try {
                if (sBroadcastWithFeature != null) {
                    int rc = (Integer) sBroadcastWithFeature.invoke(
                        sActivityManager,
                        null, null, intent, null,
                        null, -1, null, null,
                        null, null, null, -1,
                        null, false, false, -1
                    );
                    if (rc >= 0) return;
                }
                if (sBroadcastLegacy != null) {
                    int rc = (Integer) sBroadcastLegacy.invoke(
                        sActivityManager,
                        null, intent, null,
                        null, -1, null, null,
                        null, -1, null, false, false, -1
                    );
                    if (rc >= 0) return;
                }
            } catch (Throwable t) {
                Throwable c = t.getCause() != null ? t.getCause() : t;
                System.err.println("[BypassDaemon] broadcastIntent Binder error (" + c + "), falling back to am");
                sActivityManager = null;
                sBroadcastWithFeature = null;
                sBroadcastLegacy = null;
            }
        }
        // Fallback only if Binder invocation failed unexpectedly
        try {
            StringBuilder cmd = new StringBuilder("am broadcast -a ");
            cmd.append(intent.getAction());
            Bundle ex = intent.getExtras();
            if (ex != null) {
                for (String k : ex.keySet()) {
                    Object v = ex.get(k);
                    if (v instanceof Integer) {
                        cmd.append(" --ei ").append(k).append(' ').append(v);
                    }
                }
            }
            Runtime.getRuntime().exec(new String[]{"sh", "-c", cmd.toString()}).waitFor();
        } catch (Throwable ignored) {}
    }

    private static void sendQuickChargeBroadcast(int type, int watts) {
        Intent intent = new Intent(ACTION_QUICK_CHARGE_TYPE);
        intent.putExtra(EXTRA_QUICK_CHARGE_TYPE, type);
        intent.putExtra(EXTRA_POWER_MAX, watts);
        intent.putExtra(EXTRA_CAR_CHARGE, 0);
        sendBroadcastDirect(intent);
    }

    private static void sendSocDecimalBroadcast(int decimal, int rate) {
        Intent intent = new Intent(ACTION_SOC_DECIMAL);
        intent.putExtra(EXTRA_SOC_DECIMAL, decimal);
        intent.putExtra(EXTRA_SOC_DECIMAL_RATE, rate);
        sendBroadcastDirect(intent);
    }

    private static String readFileTrim(String path) {
        File file = new File(path);
        if (!file.exists()) return "";
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line = br.readLine();
            return line != null ? line.trim() : "";
        } catch (Throwable t) {
            return "";
        }
    }

    private static int parseIntOr(String val, int fallback) {
        if (val == null || val.isEmpty()) return fallback;
        try {
            return Integer.parseInt(val);
        } catch (Exception e) {
            return fallback;
        }
    }

    private static boolean writeFile(String path, String value) {
        try (FileWriter fw = new FileWriter(path)) {
            fw.write(value);
            return true;
        } catch (Throwable t) {
            System.err.println("[BypassDaemon] Failed to write " + value + " to " + path + ": " + t.getMessage());
            return false;
        }
    }

    private static void initSettingsProvider() {
        if (sSettingsProvider != null && sProviderCall != null) return;
        try {
            Class<?> amClass = Class.forName("android.app.ActivityManager");
            Object am = amClass.getMethod("getService").invoke(null);
            if (am == null) return;
            Method getCp = am.getClass().getMethod("getContentProviderExternal", String.class, int.class, IBinder.class, String.class);
            Object holder = getCp.invoke(am, "settings", 0, sToken, null);
            if (holder == null) return;
            Object provider = holder.getClass().getField("provider").get(holder);
            if (provider == null) return;
            Class<?> attrCls = Class.forName("android.content.AttributionSource");
            sAttrSource = attrCls.getMethod("myAttributionSource").invoke(null);
            sProviderCall = provider.getClass().getMethod("call", attrCls, String.class, String.class, String.class, Bundle.class);
            sSettingsProvider = provider;
        } catch (Throwable t) {
            sSettingsProvider = null;
            sProviderCall = null;
        }
    }

    private static String getSetting(String table, String key) {
        initSettingsProvider();
        if (sSettingsProvider != null && sProviderCall != null && sAttrSource != null) {
            try {
                Bundle args = new Bundle();
                args.putInt("_user", 0);
                Bundle res = (Bundle) sProviderCall.invoke(sSettingsProvider, sAttrSource, "settings", "GET_" + table, key, args);
                if (res != null) {
                    String val = res.getString("value");
                    return val != null ? val.trim() : "";
                }
                return "";
            } catch (Throwable t) {
                sSettingsProvider = null;
                sProviderCall = null;
            }
        }
        return "";
    }

    private static void putSetting(String table, String key, String val) {
        initSettingsProvider();
        if (sSettingsProvider != null && sProviderCall != null && sAttrSource != null) {
            try {
                Bundle args = new Bundle();
                args.putString("value", val);
                args.putInt("_user", 0);
                sProviderCall.invoke(sSettingsProvider, sAttrSource, "settings", "PUT_" + table, key, args);
            } catch (Throwable t) {
                sSettingsProvider = null;
                sProviderCall = null;
            }
        }
    }

    private static boolean isPackageInstalled(String pkg) {
        long now = System.currentTimeMillis();
        if (sCachedPkgInstalled && (now - sLastPkgCheckMs) < 60000L) {
            return true;
        }
        sLastPkgCheckMs = now;
        try {
            if (sPackageManager == null || sGetPackageInfo == null) {
                Class<?> sm = Class.forName("android.os.ServiceManager");
                IBinder binder = (IBinder) sm.getMethod("getService", String.class).invoke(null, "package");
                if (binder != null) {
                    Class<?> stub = Class.forName("android.content.pm.IPackageManager$Stub");
                    sPackageManager = stub.getMethod("asInterface", IBinder.class).invoke(null, binder);
                    try {
                        sGetPackageInfo = sPackageManager.getClass().getMethod("getPackageInfo", String.class, long.class, int.class);
                        sGetPkgFlagsIsLong = true;
                    } catch (NoSuchMethodException e) {
                        sGetPackageInfo = sPackageManager.getClass().getMethod("getPackageInfo", String.class, int.class, int.class);
                        sGetPkgFlagsIsLong = false;
                    }
                }
            }
            if (sPackageManager != null && sGetPackageInfo != null) {
                Object pi = sGetPkgFlagsIsLong
                    ? sGetPackageInfo.invoke(sPackageManager, pkg, 0L, 0)
                    : sGetPackageInfo.invoke(sPackageManager, pkg, 0, 0);
                boolean ok = (pi != null);
                if (PKG_APP.equals(pkg)) sCachedPkgInstalled = ok;
                return ok;
            }
        } catch (Throwable t) {
            sPackageManager = null;
            sGetPackageInfo = null;
        }
        return sCachedPkgInstalled;
    }

    private static boolean isPluggedIn(String status, int vbus) {
        if ("Discharging".equalsIgnoreCase(status)) {
            return false;
        }
        int chgType = parseIntOr(readFileTrim(NODE_CHARGER_TYPE), -1);
        if (chgType == 0 && vbus < 4000 && !"Charging".equalsIgnoreCase(status) && !"Not charging".equalsIgnoreCase(status)) {
            return false;
        }
        return true;
    }

    private static void updateChargingBridge(boolean plugged, String status, int vbus) {
        if (!plugged || !"Charging".equalsIgnoreCase(status)) {
            if (sLastChargeType != 0) {
                sendQuickChargeBroadcast(0, 0);
                sLastChargeType = 0;
                sLastChargePower = 0;
                sDecimalAnimCount = 0;
            }
            return;
        }

        String mode = readFileTrim(NODE_CHARGING_MODE);
        int adapter = parseIntOr(readFileTrim(NODE_ADAPTER_CAPACITY), 0);
        String pd = readFileTrim(NODE_PD_TYPE);
        int slave = parseIntOr(readFileTrim(NODE_SLAVE_ONLINE), 0);

        int currType;
        int currPower;

        // Verified live telemetry:
        // 68W Ultra Charge: Dual-phase direct charging (slave charger online) or PE5 mode ("P5"),
        // or verified high-voltage contract (vbus >= 8500 mV) with 60W+ adapter.
        if (slave == 1 || "P5".equals(mode) || (adapter >= 60 && vbus >= 8500)) {
            currType = 4;
            currPower = 68;
        // 33W Super Charge: Verified >= 8500 mV bus contract with 30W+ adapter or PPS PD contract
        } else if (vbus >= 8500 && (adapter >= 30 || (!pd.isEmpty() && !"normal".equals(pd)))) {
            currType = 4;
            currPower = 33;
        // 18W Fast Charge: Verified >= 7500 mV bus contract or verified PD negotiation >= 4500 mV
        } else if (vbus >= 7500 || ("PD".equals(pd) && vbus >= 4500)) {
            currType = 1;
            currPower = 18;
        // 10W Standard Charge: Verified baseline 5V charging rail
        } else if (vbus >= 4000) {
            currType = 0;
            currPower = 10;
        } else {
            currType = 0;
            currPower = 0;
        }

        if (currType != sLastChargeType || currPower != sLastChargePower) {
            sendQuickChargeBroadcast(currType, currPower);
            sLastChargeType = currType;
            sLastChargePower = currPower;
            sDecimalAnimCount = 16;
        }

        if (currType == 4 && sDecimalAnimCount > 0) {
            int decimal = Math.min(98, (17 - sDecimalAnimCount) * 6);
            sendSocDecimalBroadcast(decimal, 10);
            sDecimalAnimCount--;
        }
    }

    private static void showIsland(int capacity) throws Exception {
        boolean useAppIcon = isPackageInstalled(PKG_APP);
        sActiveIslandPkg = useAppIcon ? PKG_APP : PKG_FALLBACK;

        String iconParams = useAppIcon
            ? "\"iconParams\":{\"iconResName\":\"ic_island_battery\",\"iconType\":1,\"iconFormat\":\"svg\",\"category\":\"drawable\"}"
            : "\"iconParams\":{\"iconResName\":\"quick_charger_mode\",\"iconType\":0,\"iconFormat\":\"shader\",\"category\":\"raw\"}";

        String json = "{"
            + "\"left\":{\"textParams\":{\"text\":\"Bypass Charging\",\"turnAnim\":true}},"
            + "\"right\":{\"textParams\":{\"text\":\"" + capacity + "%\",\"textColor\":" + COLOR_PURPLE + "},"
            + iconParams + "},"
            + "\"glowEffect\":{\"enable\":true}"
            + "}";

        Bundle bundle = new Bundle();
        bundle.putString("package_name", sActiveIslandPkg);
        bundle.putString("notifyId", NOTIFY_ID);
        bundle.putString("strong_toast_category", CATEGORY);
        bundle.putLong("duration", DURATION_10S);
        bundle.putString("island_param", json);

        sSetStatus.invoke(sStatusBarService, 1, sToken, ACTION_NAME, bundle);
        sIslandRegistered = true;
    }

    private static void hideIsland() throws Exception {
        Bundle bundle = new Bundle();
        bundle.putString("package_name", sActiveIslandPkg);
        bundle.putString("notifyId", NOTIFY_ID);

        sSetStatus.invoke(sStatusBarService, 0, sToken, ACTION_NAME, bundle);
        sIslandRegistered = false;
    }

    public static void main(String[] args) {
        System.out.println("[BypassDaemon] Unified Charging Bridge + Bypass Island started.");

        while (true) {
            long sleepMs = 2500L;
            try {
                updateHeartbeat();
                if (sStatusBarService == null || sSetStatus == null) {
                    if (!initStatusBarService()) {
                        Thread.sleep(2000L);
                        continue;
                    }
                    System.out.println("[BypassDaemon] Connected to StatusBarService.");
                }

                String status = readFileTrim(NODE_BATTERY_STATUS);
                int vbus = parseIntOr(readFileTrim(NODE_VBUS), 0);
                boolean plugged = isPluggedIn(status, vbus);

                // 1. Unified zero-fork HyperOS Fast Charging & SOC Decimal Bridge
                updateChargingBridge(plugged, status, vbus);

                int rawBypass = parseIntOr(readFileTrim(NODE_BYPASS), 0);
                int capacity = plugged ? parseIntOr(readFileTrim(NODE_CAPACITY), 100) : 100;
                int temp = plugged ? parseIntOr(readFileTrim(NODE_TEMP), 300) : 300;

                // Safety checks: disable bypass if unplugged, < 10% battery, or > 45.0 C
                if ((!plugged || capacity < 10 || temp > 450) && rawBypass > 0) {
                    System.out.println("[BypassDaemon] Safety trip (plugged=" + plugged + ", cap=" + capacity + ", temp=" + temp + "). Disabling bypass.");
                    writeFile(NODE_BYPASS, "0");
                    rawBypass = 0;
                }

                // 2. Synchronize Game Turbo toggle <-> hardware node (zero idle Binder calls when unplugged & reset)
                if (plugged || sLastGameTurboSettingState != 0 || sLastBypassHardwareState != 0) {
                    int gameTurboVal = parseIntOr(getSetting("secure", SETTING_SECURE_SIDE_ROAD), 0);

                    if (gameTurboVal != sLastGameTurboSettingState) {
                        if (gameTurboVal == 1 && rawBypass <= 0) {
                            if (plugged && capacity >= 10 && temp <= 450) {
                                System.out.println("[BypassDaemon] Game Turbo toggled ON. Enabling hardware bypass.");
                                writeFile(NODE_BYPASS, "110");
                                rawBypass = 110;
                            } else {
                                putSetting("secure", SETTING_SECURE_SIDE_ROAD, "0");
                                gameTurboVal = 0;
                            }
                        } else if (gameTurboVal == 0 && rawBypass > 0) {
                            System.out.println("[BypassDaemon] Game Turbo toggled OFF. Disabling hardware bypass.");
                            writeFile(NODE_BYPASS, "0");
                            rawBypass = 0;
                        }
                        sLastGameTurboSettingState = gameTurboVal;
                    }

                    int currentHwState = (rawBypass > 0 && plugged) ? 1 : 0;
                    if (currentHwState != sLastBypassHardwareState) {
                        if (gameTurboVal != currentHwState) {
                            putSetting("secure", SETTING_SECURE_SIDE_ROAD, String.valueOf(currentHwState));
                            sLastGameTurboSettingState = currentHwState;
                        }
                        sLastBypassHardwareState = currentHwState;
                    }
                }

                // 3. Manage HyperIsland lifecycle (10s purple auto-hide capsule)
                boolean bypassActive = (rawBypass > 0 && plugged);
                if (bypassActive) {
                    if (!sIslandRegistered) {
                        System.out.println("[BypassDaemon] Bypass active. Showing HyperIsland (" + capacity + "%).");
                        showIsland(capacity);
                    }
                } else if (sIslandRegistered) {
                    System.out.println("[BypassDaemon] Bypass inactive. Dismissing HyperIsland.");
                    hideIsland();
                }

                if (sDecimalAnimCount > 0) {
                    sleepMs = 500L;
                } else if (plugged || bypassActive) {
                    sleepMs = 1200L;
                }
            } catch (Throwable t) {
                System.err.println("[BypassDaemon] Exception: " + t.getMessage());
                sStatusBarService = null;
                sSetStatus = null;
                sIslandRegistered = false;
            }

            try {
                Thread.sleep(sleepMs);
            } catch (InterruptedException e) {
                break;
            }
        }
    }
}
