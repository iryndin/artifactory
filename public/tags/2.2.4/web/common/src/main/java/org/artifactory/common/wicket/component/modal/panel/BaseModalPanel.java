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

package org.artifactory.common.wicket.component.modal.panel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.artifactory.common.wicket.behavior.JavascriptEvent;
import org.artifactory.common.wicket.component.modal.HasModalHandler;
import org.artifactory.common.wicket.component.modal.ModalHandler;
import org.artifactory.common.wicket.component.panel.feedback.aggregated.AggregateFeedbackPanel;
import org.artifactory.common.wicket.model.Titled;

import java.io.Serializable;

/**
 * @author Yoav Aharoni
 */
public class BaseModalPanel<E extends Serializable> extends Panel implements Titled, HasModalHandler {
    public static final String MODAL_ID = ModalHandler.CONTENT_ID;
    protected static final String TITLE_KEY = "panel.title";

    private int minimalWidth = 100;
    private int minimalHeight = 50;
    private int initialWidth = 600;
    private int initialHeight = 0;

    private String title;

    public BaseModalPanel() {
        super(MODAL_ID);
    }

    public BaseModalPanel(IModel model) {
        super(MODAL_ID, model);
    }

    public BaseModalPanel(E entity) {
        super(MODAL_ID, new CompoundPropertyModel(entity));
    }

    private ModalHandler modalHandler;

    {
        setOutputMarkupId(true);

        // add modalHandler
        modalHandler = new ModalHandler("modalHandler");
        add(modalHandler);

        AggregateFeedbackPanel feedback = new AggregateFeedbackPanel("feedback");
        feedback.add(new JavascriptEvent("onshow", "ModalHandler.onError();"));
        feedback.add(new JavascriptEvent("ondestroy", "ModalHandler.onError();"));
        feedback.addMessagesSource(this);
        add(feedback);

        ModalHandler.getInstanceFor(this);
    }

    public ModalHandler getModalHandler() {
        return modalHandler;
    }

    public boolean isResizable() {
        return true;
    }

    @SuppressWarnings({"unchecked"})
    public E getPanelModelObject() {
        return (E) getModelObject();
    }

    public void setPanelModelObject(E object) {
        setModelObject(object);
    }

    public StringResourceModel getResourceModel(String key) {
        return new StringResourceModel(key, this, null, "??" + key + "??");
    }

    public String getResourceString(String key) {
        return getString(key, null, "??" + key + "??");
    }

    public String getTitle() {
        return title == null ? getResourceString(TITLE_KEY) : title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void onShow(AjaxRequestTarget target) {
    }

    public int getMinimalWidth() {
        return minimalWidth;
    }

    public void setMinimalWidth(int minimalWidth) {
        this.minimalWidth = minimalWidth;
    }

    public int getMinimalHeight() {
        return minimalHeight;
    }

    public void setMinimalHeight(int minimalHeight) {
        this.minimalHeight = minimalHeight;
    }

    public int getInitialWidth() {
        return initialWidth;
    }

    public void setInitialWidth(int initialWidth) {
        this.initialWidth = initialWidth;
    }

    public int getInitialHeight() {
        return initialHeight;
    }

    public void setInitialHeight(int initialHeight) {
        this.initialHeight = initialHeight;
    }

    public void setWidth(int width) {
        setMinimalWidth(width);
        setInitialWidth(width);
    }

    /**
     * Sets the height of the content (not including the caption)
     *
     * @param height Height of the content in pixels
     */
    public void setHeight(int height) {
        setMinimalHeight(height);
        setInitialHeight(height);
    }

    public String getCookieName() {
        return getClass().getSimpleName();
    }

    public void close(AjaxRequestTarget target) {
        ModalHandler modalHandler = ModalHandler.getInstanceFor(this);
        modalHandler.close(target);
        modalHandler.setContent(new WebMarkupContainer(MODAL_ID));
    }

    /**
     * onClose event hanlder. Override onClose to run your code uppon closing the modal panel.
     *
     * @param target AjaxRequestTarget
     */
    @SuppressWarnings({"UnusedDeclaration"})
    public void onClose(AjaxRequestTarget target) {
    }

    public void onCloseButtonClicked(AjaxRequestTarget target) {
    }
}
