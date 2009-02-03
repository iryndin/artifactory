package org.artifactory.webapp.wicket.common.util;

import org.apache.wicket.RequestCycle;
import org.apache.wicket.protocol.http.WebRequest;
import org.apache.wicket.protocol.http.WebResponse;

import javax.servlet.http.Cookie;

/**
 * @author Yoav Aharoni
 */
public class CookieUtils {
    public static String getCookie(String name) {
        WebRequest request = (WebRequest) RequestCycle.get().getRequest();
        Cookie cookie = request.getCookie(name);
        if (cookie == null) {
            return null;
        }
        return cookie.getValue();
    }

    public static void setCookie(String name, Object value) {
        if (value == null) {
            clearCookie(name);
            return;
        }

        WebResponse response = (WebResponse) RequestCycle.get().getResponse();
        response.addCookie(new Cookie(name, value.toString()));
    }

    public static void clearCookie(String name) {
        WebResponse response = (WebResponse) RequestCycle.get().getResponse();
        response.clearCookie(new Cookie(name, null));
    }
}
