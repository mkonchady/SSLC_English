package org.mkonchady.sslcenglish.utils;

import org.mkonchady.sslcenglish.Constants;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import androidx.annotation.NonNull;

// Date utilities
public final class UtilsDate {

    final static Locale locale = Locale.getDefault();
    final static String dateFormat = "MMM dd, yyyy";
    final static String dateTimeFormat = "MMM dd, HH:mm";
    final static String dateTimeSecFormat = "MMM dd, yyyy HH:mm:ss";
    final static String timeFormat = "HH:mm:ss";
    final static String shortTimeFormat = "mm:ss";
    final private static SimpleDateFormat sdf_date_time = new SimpleDateFormat(dateTimeFormat, locale);
    final private static SimpleDateFormat sdf_date_time_sec = new SimpleDateFormat(dateTimeSecFormat, locale);
    final private static SimpleDateFormat sdf_date      = new SimpleDateFormat(dateFormat, locale);
    final private static SimpleDateFormat sdf_time      = new SimpleDateFormat(timeFormat, locale);
    final private static SimpleDateFormat sdf_short_time = new SimpleDateFormat(shortTimeFormat, locale);
    final private static SimpleDateFormat detailDateFormat =    new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ", locale);
    final private static SimpleDateFormat wahooDateFormat =     new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ssZ", locale);
    final private static SimpleDateFormat detailDateFormatGMT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", locale);

    /**
     * no default constructor
     */
    private UtilsDate() {
        throw new AssertionError();
    }

    public static boolean isDate(String dateToValidate) {
        if(dateToValidate == null || dateToValidate.isEmpty()) return false;
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat, Locale.getDefault());
        sdf.setLenient(false);
        try {
            sdf.parse(dateToValidate);
        } catch (ParseException e) {
            return false;
        }
        return true;
    }

    public static Date parseDate(@NonNull String date) {
        try {
            return new SimpleDateFormat(dateFormat, Locale.getDefault()).parse(date);
        } catch (ParseException e) {
            return null;
        }
    }


    public static long getDetailTimeStamp(String text) {
        try {
            Date date = detailDateFormat.parse(text);
            return date.getTime();
        } catch (ParseException pe1) {
            detailDateFormatGMT.setTimeZone(TimeZone.getTimeZone("GMT"));
            try {
                Date date = detailDateFormatGMT.parse(text);
                return date.getTime();
            } catch (ParseException pe2) {
                return System.currentTimeMillis();
            }
        }
    }

    public static String getDetailDateTimeSec(long millis) {
        detailDateFormat.setTimeZone(TimeZone.getDefault());
        return (detailDateFormat.format(millis));
    }

    public static String getDateTimeSec(long millis) {
        sdf_date_time_sec.setTimeZone(TimeZone.getDefault());
        return (sdf_date_time_sec.format(millis));
    }

    public static String getDateTime(long millis) {
        sdf_date_time.setTimeZone(TimeZone.getDefault());
        return (sdf_date_time.format(millis));
    }

    public static String getDate(long millis) {
        sdf_date.setTimeZone(TimeZone.getDefault());
        return (sdf_date.format(millis));
    }

    // pure time duration must be in GMT timezone
    public static String getTimeDurationHHMMSS(long milliseconds, boolean local) {
        // round up the milliseconds to the nearest second
        long millis = 1000 * ((milliseconds + 500) / 1000);

        // use the shorter format for less than an hour
        if (millis < Constants.MILLISECONDS_PER_MINUTE * 60) {
            if (local) sdf_short_time.setTimeZone(TimeZone.getDefault());
            else       sdf_short_time.setTimeZone(TimeZone.getTimeZone("GMT"));
            return (sdf_short_time.format(millis));
        }

        if (local) sdf_time.setTimeZone(TimeZone.getDefault());
        else       sdf_time.setTimeZone(TimeZone.getTimeZone("GMT"));
        return (sdf_time.format(millis));
    }

    // convert milliseconds to seconds
    public static int getTimeSeconds(long millis) {
        double x = millis / 1000.0;
        return (int) Math.round(x);
    }

}
