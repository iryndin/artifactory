/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.common.wicket.util;

import org.apache.wicket.Component;
import org.apache.wicket.IRequestTarget;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.feedback.IFeedback;

/**
 * @author Yoav Aharoni
 */
public abstract class AjaxUtils {
    private AjaxUtils() {
        // utility class
    }

    public static void refreshFeedback() {
        refreshFeedback(getAjaxRequestTarget());
    }

    public static void refreshFeedback(final AjaxRequestTarget target) {
        Page page = WicketUtils.getPage();
        if (page == null || target == null) {
            return;
        }
        page.visitChildren(IFeedback.class, new Component.IVisitor<Component>() {
            public Object component(Component component) {
                if (component.getOutputMarkupId()) {
                    target.addComponent(component);
                }
                return CONTINUE_TRAVERSAL;
            }
        });
    }

    public static boolean isAjaxRequest() {
        return RequestCycle.get().getRequestTarget() instanceof AjaxRequestTarget;
    }

    public static AjaxRequestTarget getAjaxRequestTarget() {
        IRequestTarget target = RequestCycle.get().getRequestTarget();
        if (target instanceof AjaxRequestTarget) {
            return (AjaxRequestTarget) target;
        }
        return null;
    }

    public static void render(Component component, String markupId) {
        final String componentId = component.getMarkupId();
        AjaxRequestTarget.get().addComponent(component, markupId);
        component.setMarkupId(componentId);
    }

}
