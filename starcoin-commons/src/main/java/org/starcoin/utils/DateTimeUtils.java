package org.starcoin.utils;

import java.util.Calendar;
import java.util.Date;

public class DateTimeUtils {
    public  static long getTimeStamp(int day) {
        Calendar calendar = Calendar.getInstance();
        calendar.set(calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH) + day, 0, 0, 0);
        return calendar.getTimeInMillis();
    }

    public static long getAnHourAgo() {
        Calendar calendar = Calendar.getInstance();
        return calendar.getTimeInMillis() - 3600 * 1000;
    }

    public static long getWholeDatTime(long date) {
        long now = date / 1000l;
        long daySecond = 60 * 60 * 24;
        return (now - now % daySecond) * 1000l;
    }
}
