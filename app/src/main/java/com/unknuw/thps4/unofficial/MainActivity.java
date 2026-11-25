package com.unknuw.thps4.unofficial;

import android.Manifest;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;

import org.libsdl.app.SDLActivity;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

// THPS4's activity: SDLActivity under the game's own class. Beyond SDL it looks after
// the storage access the game in /storage/emulated/0/THPS4 needs - all files access on
// Android 11+ (asked again each time the app comes back without it, like the sister
// ports do), the legacy read/write permissions on Android 10 - and it feeds the
// overlay its readings: they are measured here, through Android's
// official APIs, and pushed into native, because the APIs answer on phones
// whose kernels keep the raw sysfs nodes from apps.
public class MainActivity extends SDLActivity {

    private static native void nativeSetStats(float cpuTemp, float batTemp, int cpuLoad, int ramUsed,
                                              int batLevel, float gpuTemp, int gpuLoad);

    // one line per boot into the game's own log: what this phone lets
    // the app read, so a missing HUD figure can be told apart from a
    // phone that keeps the reading to itself (section 197)
    private static native void nativeLogStats(String line);

    // the orientation follows the phone between the two landscape sides while this is
    // on (the panel's Sensor landscape setting; native hands the saved value in at
    // boot and on every save). Off, the game sits in the one fixed landscape.
    private volatile boolean sensorLandscape = true;

    // called from native when the settings land and when they change
    public void applySensorLandscape(boolean on) {
        sensorLandscape = on;
        runOnUiThread(() -> setRequestedOrientation(on
                ? ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                : ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE));
    }

    // Whatever fixed landscape SDL asks for becomes the sensor's landscape
    // while the setting is on, so the game flips with the phone
    // instead of sitting locked to one side.
    @Override
    public void setRequestedOrientation(int orientation) {
        if (sensorLandscape) {
            switch (orientation) {
                case ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE:
                case ActivityInfo.SCREEN_ORIENTATION_REVERSE_LANDSCAPE:
                case ActivityInfo.SCREEN_ORIENTATION_USER_LANDSCAPE:
                    orientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE;
                    break;
                default:
                    break;
            }
        }
        super.setRequestedOrientation(orientation);
    }

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        startStats();
        askStorageAccess();
    }

    @Override
    protected void onPause() {
        stopStats();
        super.onPause();
    }

    private void askStorageAccess() {
        if (Build.VERSION.SDK_INT >= 30) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    startActivity(new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                            Uri.parse("package:" + getPackageName())));
                } catch (Exception e) {
                    startActivity(new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION));
                }
            }
        } else if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
        }
    }

    // ---- the overlay's readings ----
    // Every two seconds while the game is in front: the battery from its sticky
    // broadcast, the memory from ActivityManager, the CPU temperature from the
    // thermal zones ranked by how CPU-like their names are, its load from
    // /proc/stat or, where newer Android keeps that from apps, from the clocks
    // against their maximums. The GPU's temperature comes from the thermal
    // zones that name the GPU, its load from whichever busy counter the vendor
    // exposes (Adreno's kgsl, MediaTek's GED, a devfreq load node) - Android
    // has no official API for either. What the phone does not give goes in as
    // -1 and the overlay leaves it out.

    private Thread statsThread;
    private volatile boolean statsRun;
    private List<ThermalZone> cpuZones;
    private List<ThermalZone> gpuZones;
    private File gpuLoadFile;
    private boolean gpuLoadPair;  // the node reports busy/total counters, not a percent
    private long lastCpuTotal = -1;
    private long lastCpuIdle = -1;
    private String cpuSource = "none";  // where the last CPU load came from: proc, clocks or none

    private static final class ThermalZone {
        final int rank;  // lower is more likely the CPU itself
        final File temp;

        ThermalZone(int rank, File temp) {
            this.rank = rank;
            this.temp = temp;
        }
    }

    private void startStats() {
        if (statsThread != null) return;
        statsRun = true;
        statsThread = new Thread(() -> {
            // the probe goes out on the first three cycles, not just
            // once: the very first can land before the game's log is
            // up and be dropped, and the native end prints only the
            // first copy it receives (section 199)
            int cycles = 0;
            while (statsRun) {
                if (cycles++ < 3) {
                    try {
                        nativeLogStats(statsProbe());
                    } catch (Throwable t) {
                        // the probe is a diagnostic; its failure changes nothing
                    }
                }
                try {
                    nativeSetStats(readCpuTemp(), readBatteryTemp(), readCpuLoad(), readRamUsed(),
                            readBatteryLevel(), readGpuTemp(), readGpuLoad());
                } catch (Throwable t) {
                    statsRun = false;
                }
                try {
                    Thread.sleep(2000);
                } catch (InterruptedException e) {
                    return;
                }
            }
        }, "overlay-stats");
        statsThread.setDaemon(true);
        statsThread.start();
    }

    private void stopStats() {
        statsRun = false;
        if (statsThread != null) statsThread.interrupt();
        statsThread = null;
    }

    private float readBatteryTemp() {
        try {
            Intent i = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (i == null) return -1f;
            int tenths = i.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1);
            return tenths < 0 ? -1f : tenths / 10f;
        } catch (Exception e) {
            return -1f;
        }
    }

    private int readBatteryLevel() {
        try {
            Intent i = registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
            if (i == null) return -1;
            int level = i.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = i.getIntExtra(BatteryManager.EXTRA_SCALE, -1);
            if (level < 0 || scale <= 0) return -1;
            return clampPercent(level * 100 / scale);
        } catch (Exception e) {
            return -1;
        }
    }

    private int readRamUsed() {
        try {
            ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo info = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(info);
            if (info.totalMem <= 0) return -1;
            return clampPercent((int) ((info.totalMem - info.availMem) * 100 / info.totalMem));
        } catch (Exception e) {
            return -1;
        }
    }

    private static int clampPercent(int v) {
        return Math.max(0, Math.min(100, v));
    }

    private static String readLine(File f) {
        try (BufferedReader r = new BufferedReader(new FileReader(f))) {
            return r.readLine();
        } catch (Exception e) {
            return null;
        }
    }

    private static long readLong(String path) {
        try {
            String line = readLine(new File(path));
            return line == null ? -1 : Long.parseLong(line.trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private int readCpuLoad() {
        try (BufferedReader r = new BufferedReader(new FileReader("/proc/stat"))) {
            String[] p = r.readLine().trim().split("\\s+");
            long total = 0, idle = 0;
            for (int i = 1; i < p.length && i <= 8; i++) {
                long v = Long.parseLong(p[i]);
                total += v;
                if (i == 4 || i == 5) idle += v;
            }
            boolean had = lastCpuTotal >= 0;
            long dTotal = total - lastCpuTotal;
            long dIdle = idle - lastCpuIdle;
            lastCpuTotal = total;
            lastCpuIdle = idle;
            if (had && dTotal > 0) {
                cpuSource = "proc";
                return clampPercent((int) ((dTotal - dIdle) * 100 / dTotal));
            }
        } catch (Exception e) {
            // kept from apps on newer Android: the clocks below
        }
        long cur = 0, max = 0;
        int cores = Runtime.getRuntime().availableProcessors();
        for (int i = 0; i < cores; i++) {
            String base = "/sys/devices/system/cpu/cpu" + i + "/cpufreq/";
            long c = readLong(base + "scaling_cur_freq");
            long m = readLong(base + "cpuinfo_max_freq");
            if (c > 0 && m > 0) {
                cur += c;
                max += m;
            }
        }
        if (max > 0) cpuSource = "clocks";
        return max > 0 ? clampPercent((int) (cur * 100 / max)) : -1;
    }

    // what this phone exposes, in one line for the game's log: every
    // thermal zone's type and whether its temperature may be read,
    // the devfreq devices by name, the GPU busy counters that exist,
    // and where the CPU load came from. Runs once per boot.
    private String statsProbe() {
        StringBuilder sb = new StringBuilder("phone exposes:");
        String[] roots = {"/sys/class/thermal", "/sys/devices/virtual/thermal"};
        for (String root : roots) {
            File[] zones = new File(root).listFiles((dir, name) -> name.startsWith("thermal_zone"));
            if (zones == null) continue;
            for (File zone : zones) {
                String type = readLine(new File(zone, "type"));
                if (type == null) type = "?";
                sb.append(" zone[").append(type.trim()).append(' ')
                        .append(new File(zone, "temp").canRead() ? "readable" : "locked").append(']');
            }
        }
        File[] devs = new File("/sys/class/devfreq").listFiles();
        if (devs != null) {
            for (File dev : devs) sb.append(" devfreq[").append(dev.getName()).append(']');
        }
        String[] gpuPaths = {"/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage",
                "/sys/kernel/ged/hal/gpu_utilization", "/sys/kernel/ged/hal/current_utilization",
                "/sys/kernel/gpu/gpu_busy"};
        for (String path : gpuPaths) {
            File f = new File(path);
            if (f.exists()) sb.append(" gpu[").append(path).append(' ').append(f.canRead() ? "readable" : "locked").append(']');
        }
        readCpuLoad();  // prime the counters: the first read has no previous sample
        readCpuLoad();  // and the second one settles which source answers
        sb.append(" cpu-load via ").append(cpuSource);
        return sb.toString();
    }

    private static int zoneRank(String type) {
        if (type.contains("gpu") || type.contains("battery") || type.contains("charger")) return -1;
        if (type.contains("cpu-silicon")) return 0;
        if (type.contains("cpu-0")) return 1;
        if (type.contains("cpu")) return 2;  // cpuss, mtktscpu, cputop
        if (type.contains("tmu")) return 3;  // Exynos
        if (type.contains("soc")) return 4;
        if (type.contains("tsens")) return 5;  // Qualcomm
        if (type.contains("cluster")) return 6;
        if (type.contains("big") || type.contains("mid") || type.contains("little")) return 7;
        return -1;
    }

    // the zones that name a CPU and that the app may read, best first, found once
    private List<ThermalZone> findCpuZones() {
        List<ThermalZone> out = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        String[] roots = {"/sys/class/thermal", "/sys/devices/virtual/thermal"};
        for (String root : roots) {
            File[] zones = new File(root).listFiles((dir, name) -> name.startsWith("thermal_zone"));
            if (zones == null) continue;
            for (File zone : zones) {
                String type = readLine(new File(zone, "type"));
                if (type == null) continue;
                int rank = zoneRank(type.trim().toLowerCase(Locale.US));
                File temp = new File(zone, "temp");
                if (rank < 0 || !temp.canRead()) continue;
                String path = temp.getAbsolutePath();
                if (seen.contains(path)) continue;
                seen.add(path);
                out.add(new ThermalZone(rank, temp));
            }
        }
        Collections.sort(out, (a, b) -> a.rank - b.rank);
        return out;
    }

    private float readCpuTemp() {
        if (cpuZones == null) cpuZones = findCpuZones();
        for (ThermalZone zone : cpuZones) {
            String line = readLine(zone.temp);
            if (line == null) continue;
            try {
                float v = Float.parseFloat(line.trim());
                if (v > 1000f) v /= 1000f;  // most phones give thousandths of a degree
                if (v > 10f && v < 120f) return v;
            } catch (NumberFormatException e) {
                // the next zone
            }
        }
        return -1f;
    }

    private static int gpuZoneRank(String type) {
        if (!type.contains("gpu")) return -1;
        if (type.startsWith("gpu")) return 0;  // gpu, gpu-thermal, gpu_thermal
        if (type.contains("gpuss") || type.contains("mtktsgpu")) return 1;
        return 2;
    }

    // the zones that name the GPU and that the app may read, best first, found once
    private List<ThermalZone> findGpuZones() {
        List<ThermalZone> out = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        String[] roots = {"/sys/class/thermal", "/sys/devices/virtual/thermal"};
        for (String root : roots) {
            File[] zones = new File(root).listFiles((dir, name) -> name.startsWith("thermal_zone"));
            if (zones == null) continue;
            for (File zone : zones) {
                String type = readLine(new File(zone, "type"));
                if (type == null) continue;
                int rank = gpuZoneRank(type.trim().toLowerCase(Locale.US));
                File temp = new File(zone, "temp");
                if (rank < 0 || !temp.canRead()) continue;
                String path = temp.getAbsolutePath();
                if (seen.contains(path)) continue;
                seen.add(path);
                out.add(new ThermalZone(rank, temp));
            }
        }
        Collections.sort(out, (a, b) -> a.rank - b.rank);
        return out;
    }

    private float readGpuTemp() {
        if (gpuZones == null) gpuZones = findGpuZones();
        for (ThermalZone zone : gpuZones) {
            String line = readLine(zone.temp);
            if (line == null) continue;
            try {
                float v = Float.parseFloat(line.trim());
                if (v > 1000f) v /= 1000f;  // most phones give thousandths of a degree
                if (v > 10f && v < 120f) return v;
            } catch (NumberFormatException e) {
                // the next zone
            }
        }
        return -1f;
    }

    // every integer in a sysfs line, in order ("42", "120 240", "25@800000000")
    private static long[] parseInts(String line) {
        if (line == null) return new long[0];
        String[] parts = line.trim().split("[^0-9]+");
        List<Long> vals = new ArrayList<>();
        for (String p : parts) {
            if (p.isEmpty()) continue;
            try {
                vals.add(Long.parseLong(p));
            } catch (NumberFormatException e) {
                // not a number after all
            }
        }
        long[] out = new long[vals.size()];
        for (int i = 0; i < out.length; i++) out[i] = vals.get(i);
        return out;
    }

    // the GPU's busy share. No official API exists, so this tries the vendor
    // counters in order and keeps the first one that answers; the winner is
    // cached and dropped again if it stops answering.
    private int readGpuLoad() {
        if (gpuLoadFile == null) {
            gpuLoadFile = resolveGpuLoadFile();
            if (gpuLoadFile == null) return -1;
        }
        long[] v = parseInts(readLine(gpuLoadFile));
        if (gpuLoadPair && v.length >= 2 && v[1] > 0) return clampPercent((int) (v[0] * 100 / v[1]));
        if (v.length >= 1 && v[0] >= 0 && v[0] <= 100) return (int) v[0];
        gpuLoadFile = null;  // stopped answering in the expected shape: resolve again
        return -1;
    }

    private File resolveGpuLoadFile() {
        // Qualcomm Adreno: a plain busy percentage
        File kgsl = new File("/sys/class/kgsl/kgsl-3d0/gpu_busy_percentage");
        if (kgsl.canRead()) {
            gpuLoadPair = false;
            return kgsl;
        }
        // MediaTek GED: busy/total counters (a single percent on some kernels,
        // which readGpuLoad's fall-through handles)
        File ged = new File("/sys/kernel/ged/hal/gpu_utilization");
        if (ged.canRead()) {
            gpuLoadPair = true;
            return ged;
        }
        // MediaTek GED on other kernels: a plain utilization percent
        File gedNow = new File("/sys/kernel/ged/hal/current_utilization");
        if (gedNow.canRead()) {
            gpuLoadPair = false;
            return gedNow;
        }
        File busy = new File("/sys/kernel/gpu/gpu_busy");
        if (busy.canRead()) {
            gpuLoadPair = false;
            return busy;
        }
        // generic devfreq: the GPU's node carries a load figure
        File[] devs = new File("/sys/class/devfreq").listFiles();
        if (devs != null) {
            for (File dev : devs) {
                String name = dev.getName().toLowerCase(Locale.US);
                if (!name.contains("gpu") && !name.contains("mali") && !name.contains("kgsl")
                        && !name.contains("ged")) continue;
                File load = new File(dev, "load");
                if (load.canRead()) {
                    gpuLoadPair = false;
                    return load;
                }
                File gpuLoad = new File(dev, "gpu_load");
                if (gpuLoad.canRead()) {
                    gpuLoadPair = false;
                    return gpuLoad;
                }
            }
        }
        return null;
    }

}
