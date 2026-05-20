package com.example.noexcuse;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * AllowedAppsConfig
 * ─────────────────
 * Defines which apps are considered "good" during Focus Mode.
 * The FocusMonitorService will NOT trigger the distraction timer
 * when the user is inside one of these apps.
 *
 * HOW TO CUSTOMIZE:
 * • Add any app's package name to ALLOWED_PACKAGES.
 * • You can find an app's package name on the Play Store URL:
 * https://play.google.com/store/apps/details?id=<PACKAGE_NAME>
 *
 * CURRENTLY WHITELISTED:
 * • Your own app              (com.example.noexcuse)
 * • System UI / Launcher      (navigating home is fine)
 * • Duolingo                  (language learning)
 * • Khan Academy              (courses)
 * • Coursera                  (online learning)
 * • Anki                      (flashcards)
 * • Notion                    (note-taking)
 * • Google Docs               (writing)
 * • Google Calendar           (planning)
 * • Google Drive              (documents)
 */
public final class AllowedAppsConfig {

    private AllowedAppsConfig() {}

    private static final Set<String> ALLOWED_PACKAGES = new HashSet<>(Arrays.asList(

            // ── Your own app ─────────────────────────────────────────────────
            "com.example.noexcuse",

            // ── Android system (allow navigating, adjusting volume, etc.) ────
            "com.android.systemui",
            "com.android.launcher",
            "com.android.launcher3",
            "com.google.android.apps.nexuslauncher",    // Pixel launcher
            "com.miui.home",                             // MIUI launcher
            "com.samsung.android.app.launcher",          // Samsung launcher
            "com.huawei.android.launcher",               // Huawei launcher

            // ── Educational apps ─────────────────────────────────────────────
            "com.duolingo",                              // Duolingo
            "org.khanacademy.android",                   // Khan Academy
            "org.coursera.android",                      // Coursera
            "com.ankidroid.anki",                        // AnkiDroid flashcards
            "com.sololearn",                             // SoloLearn (coding)
            "com.grasshopper.android",                   // Grasshopper (coding)
            "com.photomath",                             // Photomath
            "ai.edsby.android",                          // Edsby
            "com.quizlet.quizletandroid",                // Quizlet

            // ── Productivity & note-taking ───────────────────────────────────
            "notion.id",                                 // Notion
            "com.google.android.apps.docs.editors.docs", // Google Docs
            "com.google.android.apps.docs",              // Google Drive
            "com.google.android.calendar",               // Google Calendar
            "com.microsoft.office.word",                 // Microsoft Word
            "com.microsoft.onenote",                     // OneNote
            "com.evernote",                              // Evernote
            "com.obsidian.app",                          // Obsidian

            // ── Focus / timer apps ───────────────────────────────────────────
            "com.stuudyminder",                          // Forest
            "com.joaomgcd.autotools",
            "cc.forestapp"                               // Forest (focus timer)
    ));

    /**
     * Returns true if the given package is allowed during Focus Mode.
     * Uses prefix matching so sub-packages of system apps also pass.
     */
    public static boolean isAllowed(String packageName) {
        // FIX: Changed from 'return true' to 'return false'.
        // If the package is null (detection lag), we do NOT treat it as an allowed app.
        if (packageName == null) return false;

        // Exact match
        if (ALLOWED_PACKAGES.contains(packageName)) return true;

        // Prefix match for system UI edge cases (e.g. com.android.systemui:screenshot)
        for (String allowed : ALLOWED_PACKAGES) {
            if (packageName.startsWith(allowed)) return true;
        }

        return false;
    }

    /**
     * Call this at runtime to add an app to the whitelist dynamically.
     * (e.g. from a settings screen where the user picks trusted apps)
     */
    public static void addAllowed(String packageName) {
        ALLOWED_PACKAGES.add(packageName);
    }

    /**
     * Call this to remove an app from the whitelist.
     */
    public static void removeAllowed(String packageName) {
        ALLOWED_PACKAGES.remove(packageName);
    }
}