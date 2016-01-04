package org.artifactory.util.dateUtils;

import org.jfrog.build.api.Build;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * @author Chen Keinan
 */
public class DateUtils {

    /**
     * format Build date
     *
     * @param time - long date
     * @return
     * @throws ParseException
     */
    public static String formatBuildDate(long time) throws ParseException {
        Date date = new Date(time);
        SimpleDateFormat df2 = new SimpleDateFormat(Build.STARTED_FORMAT);
        String dateText = df2.format(date);
        return dateText;
    }
}
