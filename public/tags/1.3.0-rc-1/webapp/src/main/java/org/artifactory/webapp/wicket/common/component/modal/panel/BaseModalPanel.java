package org.artifactory.webapp.wicket.common.component.modal.panel;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.StringResourceModel;
import org.artifactory.webapp.wicket.common.Titled;
import org.artifactory.webapp.wicket.common.behavior.JavascriptEvent;
import org.artifactory.webapp.wicket.common.component.modal.HasModalHandler;
import org.artifactory.webapp.wicket.common.component.modal.ModalHandler;
import org.artifactory.webapp.wicket.common.component.panel.feedback.aggregated.AggregateFeedbackPanel;
import org.artifactory.webapp.wicket.page.base.BasePage;

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
        feedback.addMessagesSource(this);
        add(feedback);

        ModalHandler.getInstanceFor(this);
    }

    public ModalHandler getModalHandler() {
        return modalHandler;
    }

    @SuppressWarnings({"unchecked"})
    public E getPanelModelObject() {
        return (E) getModelObject();
    }

    public void setPanelModelObject(E object) {
        setModelObject(object);
    }

    public BasePage getBasePage() {
        return (BasePage) getPage();
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

    @SuppressWarnings({"UnusedDeclaration"})
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

    public MarkupContainer addWithId(final Component child) {
        add(child);
        child.setOutputMarkupId(true);
        return this;
    }

    public void close(AjaxRequestTarget target) {
        ModalHandler modalHandler = ModalHandler.getInstanceFor(this);
        modalHandler.close(target);
        modalHandler.setContent(new WebMarkupContainer(MODAL_ID));
    }

    /**
     * onClose event hanlder.
     * Override onClose to run your code uppon closing the modal panel.
     *
     * @param target AjaxRequestTarget
     */
    public void onClose(AjaxRequestTarget target) {
    }

    public void onCloseButtonClicked(AjaxRequestTarget target) {
    }
}
