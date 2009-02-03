package org.artifactory.utils;

import org.apache.log4j.Logger;
import org.artifactory.repo.CentralConfig;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class DateUtil {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(DateUtil.class);

    public static String format(Date date, CentralConfig cc) {
        return new SimpleDateFormat(cc.getDateFormat()).format(date);
    }

    public static String format(long date, CentralConfig cc) {
        return new SimpleDateFormat(cc.getDateFormat()).format(new Date(date));
    }
}
