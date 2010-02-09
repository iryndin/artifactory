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

import org.apache.wicket.IRequestTarget;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.protocol.http.WebRequestCycleProcessor;
import org.apache.wicket.request.RequestParameters;
import org.artifactory.log.LoggerFactory;
import org.slf4j.Logger;

/**
 * @author Tomer Cohen
 */
public class IgnoreAjaxUnfoundComponentRequestCycleProcessor extends WebRequestCycleProcessor {
    private static final Logger log = LoggerFactory.getLogger(IgnoreAjaxUnfoundComponentRequestCycleProcessor.class);
    private static final String IGNORE_MESSAGE = "Ignoring %s while resolving listener interface {%s}.";

    @Override
    protected IRequestTarget resolveListenerInterfaceTarget(RequestCycle requestCycle, Page page, String componentPath,
            String interfaceName,
            RequestParameters requestParameters) {
        try {
            return super.resolveListenerInterfaceTarget(requestCycle, page, componentPath, interfaceName,
                    requestParameters);
        } catch (WicketRuntimeException e) {
            // if e subclass of WicketRuntimeException re-throw exception
            if (!e.getClass().equals(WicketRuntimeException.class)) {
                throw e;
            }

            // hide exception and log it
            String message = String.format(IGNORE_MESSAGE, e.getClass().getSimpleName(), e.getMessage());
            if (ArtifactoryApplication.get().isDevelopmentMode()) {
                log.error(message, e);
            } else {
                log.warn(message);
            }
            return new NopRequestTarget();
        }
    }

    private static class NopRequestTarget implements IRequestTarget {
        public void detach(RequestCycle requestCycle) {
        }

        public void respond(RequestCycle requestCycle) {
        }
    }
}
