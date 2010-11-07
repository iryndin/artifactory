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

package org.artifactory.common.wicket.ajax;

import org.apache.wicket.Component;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.ajax.AbstractDefaultAjaxBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.IAjaxCallDecorator;
import org.apache.wicket.markup.html.IHeaderResponse;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.version.undo.Change;

/**
 * An almost complete copy of Wickets org.apache.wicket.extensions.ajax.markup.html.AjaxLazyLoadPanel.
 * Overriding the original lazy load panel is needed since the nested item which is to be replaced of type
 * <code>div</code> causes the new-line break; This implementation uses a <code>span</code> tag instead.<br/>
 * Furthermore, long operations will also display the ajax screen blocking indicator, it is not possible to cancel
 * the blocker when using the original implementation. This implementation disables the blocking indicator.
 *
 * @author Noam Y. Tenne
 */
public abstract class AjaxLazyLoadSpanPanel extends Panel {

    private static final String LAZY_LOAD_COMPONENT_ID = "content";

    // state,
    // 0:add loading component
    // 1:loading component added, waiting for ajax replace
    // 2:ajax replacement completed
    private byte state = 0;

    public AjaxLazyLoadSpanPanel(final String id) {
        this(id, null);
    }

    public AjaxLazyLoadSpanPanel(final String id, final IModel<?> model) {
        super(id, model);

        setOutputMarkupId(true);

        add(new AbstractDefaultAjaxBehavior() {
            @Override
            protected IAjaxCallDecorator getAjaxCallDecorator() {
                return new NoAjaxIndicatorDecorator();
            }

            @Override
            protected void respond(AjaxRequestTarget target) {
                Component component = getLazyLoadComponent(LAZY_LOAD_COMPONENT_ID);
                AjaxLazyLoadSpanPanel.this.replace(component);
                target.addComponent(AjaxLazyLoadSpanPanel.this);
                setState((byte) 2);
            }

            @Override
            public void renderHead(IHeaderResponse response) {
                super.renderHead(response);
                handleCallbackScript(response, getCallbackScript().toString());
            }

            @Override
            public boolean isEnabled(Component component) {
                return state < 2;
            }

            /**
             * An ajax channel name used only by this ajax component which allow to display the default ajax
             * indicator when the user clicks on another ajax action while this one is still executing (otherwise
             * wicket will aggregate the other ajax events without the user knowing)
             */
            @Override
            protected String getChannelName() {
                return "LazyLoadChannel";
            }
        });
    }

    /**
     * Allows subclasses to change the callback script if needed.
     *
     * @param response
     * @param callbackScript
     */
    protected void handleCallbackScript(final IHeaderResponse response, final String callbackScript) {
        response.renderOnDomReadyJavascript(callbackScript);
    }

    /**
     * @see org.apache.wicket.Component#onBeforeRender()
     */
    @Override
    protected void onBeforeRender() {
        if (state == 0) {
            add(getLoadingComponent(LAZY_LOAD_COMPONENT_ID));
            setState((byte) 1);
        }
        super.onBeforeRender();
    }

    private void setState(byte state) {
        if (this.state != state) {
            addStateChange(new StateChange(this.state));
        }
        this.state = state;
    }

    /**
     * @param markupId The components markupid.
     * @return The component that must be lazy created. You may call setRenderBodyOnly(true) on this
     *         component if you need the body only.
     */
    public abstract Component getLazyLoadComponent(String markupId);

    /**
     * @param markupId The components markupid.
     * @return The component to show while the real component is being created.
     */
    public Component getLoadingComponent(final String markupId) {
        return new Label(markupId, "<img alt=\"Loading...\" src=\"" +
                RequestCycle.get().urlFor(AbstractDefaultAjaxBehavior.INDICATOR) + "\"/>").setEscapeModelStrings(false);
    }

    private final class StateChange extends Change {

        private final byte state;

        public StateChange(byte state) {
            this.state = state;
        }

        /**
         * @see org.apache.wicket.version.undo.Change#undo()
         */
        @Override
        public void undo() {
            AjaxLazyLoadSpanPanel.this.state = state;
        }
    }
}
