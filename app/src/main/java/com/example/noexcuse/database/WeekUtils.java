package com.example.noexcuse.database;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/**
 * Helper — calcule weekStartDate (lundi dyal semana courante)
 * u timestamps dyal bداية / نهاية nhar ou semana
 */
public class WeekUtils {

    private static final SimpleDateFormat DATE_FORMAT =
            new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

    // ─── Week Start Date ───────────────────────────────────────────────────

    /**
     * Jib weekStartDate dyal daba — "2025-05-05"
     * dima lundi dyal had semana
     */
    public static String getCurrentWeekStart() {
        return getWeekStart(Calendar.getInstance());
    }

    /**
     * Jib weekStartDate dyal semana li fazat — "2025-04-28"
     */
    public static String getPreviousWeekStart() {
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.WEEK_OF_YEAR, -1);
        return getWeekStart(cal);
    }

    /**
     * Jib weekStartDate dyal ayyi Calendar
     */
    public static String getWeekStart(Calendar cal) {
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return DATE_FORMAT.format(cal.getTime());
    }

    // ─── Day Of Week ───────────────────────────────────────────────────────

    /**
     * Jib dayOfWeek dyal daba — "MONDAY", "TUESDAY", etc.
     */
    public static String getTodayDayOfWeek() {
        Calendar cal = Calendar.getInstance();
        int day = cal.get(Calendar.DAY_OF_WEEK);
        switch (day) {
            case Calendar.MONDAY:    return "MONDAY";
            case Calendar.TUESDAY:   return "TUESDAY";
            case Calendar.WEDNESDAY: return "WEDNESDAY";
            case Calendar.THURSDAY:  return "THURSDAY";
            case Calendar.FRIDAY:    return "FRIDAY";
            case Calendar.SATURDAY:  return "SATURDAY";
            case Calendar.SUNDAY:    return "SUNDAY";
            default:                 return "MONDAY";
        }
    }

    // ─── Timestamps dyal nhar ─────────────────────────────────────────────

    /**
     * Bداية nhar dyal daba (00:00:00) f milliseconds
     */
    public static long getStartOfToday() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal.getTimeInMillis();
    }

    /**
     * نهاية nhar dyal daba (23:59:59) f milliseconds
     */
    public static long getEndOfToday() {
        Calendar cal = Calendar.getInstance();
        cal.set(Calendar.HOUR_OF_DAY, 23);
        cal.set(Calendar.MINUTE, 59);
        cal.set(Calendar.SECOND, 59);
        cal.set(Calendar.MILLISECOND, 999);
        return cal.getTimeInMillis();
    }

    // ─── Timestamps dyal semana ───────────────────────────────────────────

    /**
     * Jib timestamp dyal بداية semana (lundi 00:00:00)
     * weekStart = "2025-05-05"
     */
    public static long getWeekStartTimestamp(String weekStart) {
        try {
            Date date = DATE_FORMAT.parse(weekStart);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal.getTimeInMillis();
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * Jib timestamp dyal نهاية semana (ahad 23:59:59)
     * weekStart = "2025-05-05"
     */
    public static long getWeekEndTimestamp(String weekStart) {
        try {
            Date date = DATE_FORMAT.parse(weekStart);
            Calendar cal = Calendar.getInstance();
            cal.setTime(date);
            cal.add(Calendar.DAY_OF_WEEK, 6); // lundi → ahad
            cal.set(Calendar.HOUR_OF_DAY, 23);
            cal.set(Calendar.MINUTE, 59);
            cal.set(Calendar.SECOND, 59);
            cal.set(Calendar.MILLISECOND, 999);
            return cal.getTimeInMillis();
        } catch (Exception e) {
            return 0;
        }
    }
}