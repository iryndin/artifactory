/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.webapp.wicket.application;

import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.protocol.http.BufferedWebResponse;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * @author yoavl
 */
public class EofAwareBufferedWebResponse extends BufferedWebResponse {
    private static final Logger log = LoggerFactory.getLogger(EofAwareBufferedWebResponse.class);

    public EofAwareBufferedWebResponse(HttpServletResponse httpServletResponse) {
        super(httpServletResponse);
    }

    @Override
    public void close() {
        try {
            super.close();
        } catch (Exception e) {
            Throwable throwable = e;
            while (throwable != null) {
                if (throwable instanceof IOException) {
                    String message = throwable.getMessage();
                    if (message != null && message.indexOf("Broken pipe") != -1) {
                        log.debug("Ignoring EOF exception when closing response.", e);
                    }
                    return;
                }
                throwable = throwable.getCause();
            }
            throw new WicketRuntimeException("Unable to write the response", e);
        }
    }
}