/*
* Artifactory is a binaries repository manager.
* Copyright (C) 2012 JFrog Ltd.
*
* Artifactory is free software: you can redistribute it and/or modify
* it under the terms of the GNU Lesser General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Artifactory is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Lesser General Public License for more details.
*
* You should have received a copy of the GNU Lesser General Public License
* along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
*/

package org.artifactory.security.exceptions;

import org.artifactory.util.StringUtils;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.springframework.security.authentication.DisabledException;

/**
 * Thrown when user temporary blocked due to recurrent incorrect login attempts
 *
 * @author Michael Pasternak
 */
public class LoginDisabledException extends DisabledException {

    private static final int ONE_SECOND = 1000;
    private static final float SECONDS_IN_MINUTE = 60 * 1000;
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormat.forPattern("HH:mm:ss");

    /**
     * @param userName blocked user
     * @param retryAt when next login retry is allowed
     */
    public LoginDisabledException(String userName, long retryAt) {
        super(getUserErrorMessage(userName, retryAt));
    }

    /**
     * @param retryAt when next login retry is allowed
     */
    public LoginDisabledException(long retryAt) {
        super(getSessionErrorMessage(retryAt));
    }

    /**
     * @param retryAt when next login retry is allowed
     *
     * @return error message
     */
    private static String getSessionErrorMessage(long retryAt) {
        return String.format(
                "This request is blocked due to recurrent login failures, please try again in %s",
                calcNextLogin(retryAt, false)
        );
    }

    /**
     * @param userName blocked user
     * @param retryAt when next login retry is allowed
     *
     * @param userName
     * @param retryAt
     *
     * @return error message
     */
    private static String getUserErrorMessage(String userName, long retryAt) {
        return String.format(
                "'%s' is blocked due to recurrent login failures, " +
                "please try again in %s",
                userName,
                calcNextLogin(retryAt, false)
        );
    }

    /**
     * @param userName blocked user
     * @param retryAt when next login retry is allowed
     * @param showNextLogin if next login time should be shown
     */
    public LoginDisabledException(String userName, long retryAt, boolean showNextLogin) {
        super(
                String.format(
                        "'%s' is blocked due to recurrent login failures, " +
                                "please try again in %s",
                        userName,
                        calcNextLogin(retryAt, showNextLogin)
                )
        );
    }

    /**
     * Formats next login
     *
     * @param retryAt login available from
     * @param showNextLogin if next login time should be shown
     *
     * @return formatted string
     */
    private static String calcNextLogin(long retryAt, boolean showNextLogin) {
        DateTime delay = new DateTime(retryAt);
        DateTime now = DateTime.now();
        long diff = delay.getMillis() - now.getMillis();

        if (showNextLogin) {
            if (diff < SECONDS_IN_MINUTE) {
                long seconds = diff / ONE_SECOND;
                return String.format(
                        "%d seconds (%s)",
                        seconds == 0 ? 1 : seconds,
                        dateTimeFormatter.print(delay.toLocalTime())
                );
            } else {
                return String.format(
                        "%.1f minutes (%s)",
                        diff / SECONDS_IN_MINUTE,
                        dateTimeFormatter.print(delay.toLocalTime())
                );
            }
        } else {
            if (diff < SECONDS_IN_MINUTE) {
                long seconds = diff / ONE_SECOND;
                return String.format(
                        "%d seconds",
                        seconds == 0 ? 1 : seconds
                );
            } else {
                return String.format(
                        "%.1f minutes",
                        diff / SECONDS_IN_MINUTE
                );
            }
        }
    }

    /**
     * @param userName blocked user
     * @param retryAt login available from
     *
     * @return {@link LoginDisabledException}
     */
    public static LoginDisabledException userLocked(String userName, long retryAt) {
        return new LoginDisabledException(userName, retryAt);
    }

    /**
     * @param userName blocked user
     * @param retryAt login available from
     *
     * @return {@link LoginDisabledException}
     */
    public static LoginDisabledException sessionLocked(String userName, long retryAt) {
        return new LoginDisabledException(userName, retryAt);
    }
}
