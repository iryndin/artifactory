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

package org.artifactory.common.wicket.application;

import org.apache.wicket.Page;
import org.apache.wicket.request.IRequestHandler;
import org.apache.wicket.request.component.IRequestablePage;
import org.apache.wicket.request.cycle.AbstractRequestCycleListener;
import org.apache.wicket.request.cycle.RequestCycle;
import org.apache.wicket.request.handler.IComponentRequestHandler;
import org.apache.wicket.request.handler.IPageRequestHandler;

/**
 * @author Yoav Aharoni
 */
public class ResponsePageSupport extends AbstractRequestCycleListener {
    private final static ThreadLocal<Page> PAGE_HOLDER = new ThreadLocal<Page>();

    @Override
    public void onRequestHandlerResolved(RequestCycle cycle, IRequestHandler handler) {
        if (getResponsePage() == null) {
            fetchPageFromHandler(handler);
        }
    }

    @Override
    public void onRequestHandlerScheduled(RequestCycle cycle, IRequestHandler handler) {
        fetchPageFromHandler(handler);
    }

    private void fetchPageFromHandler(IRequestHandler handler) {
        if (handler instanceof IComponentRequestHandler) {
            IRequestablePage page = ((IComponentRequestHandler) handler).getComponent().getPage();
            PAGE_HOLDER.set((Page) page);
        } else if (handler instanceof IPageRequestHandler) {
            IRequestablePage page = ((IPageRequestHandler) handler).getPage().getPage();
            PAGE_HOLDER.set((Page) page);
        }
    }

    @Override
    public void onEndRequest(RequestCycle cycle) {
        PAGE_HOLDER.remove();
    }

    public static Page getResponsePage() {
        return PAGE_HOLDER.get();
    }
}
