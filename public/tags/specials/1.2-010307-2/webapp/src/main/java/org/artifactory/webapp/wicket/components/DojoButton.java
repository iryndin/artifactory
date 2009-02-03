package org.artifactory.webapp.wicket.components;

import org.apache.log4j.Logger;
import wicket.ajax.AjaxRequestTarget;
import wicket.extensions.ajax.markup.html.IndicatingAjaxSubmitButton;
import wicket.markup.ComponentTag;
import wicket.markup.html.form.Form;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class DojoButton extends IndicatingAjaxSubmitButton {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(DojoButton.class);

    private String caption;

    public DojoButton(String id, Form form) {
        super(id, form);
    }

    public DojoButton(String id, Form form, String caption) {
        super(id, form);
        this.caption = caption;
        setOutputMarkupId(true);
    }

    public String getCaption() {
        return caption;
    }

    public void setCaption(String caption) {
        this.caption = caption;
    }

    protected void onSubmit(AjaxRequestTarget target, Form form) {
        String id = getId();
        String call="dojo.widget.manager.getWidgetById('" + id +"').postCreate()";
        target.appendJavascript(call);
    }

    @Override
    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);
        //tag.put("dojoType", "Button");
        tag.put("caption", caption);
        tag.put("value", caption);
        //tag.put("widgetId", getId());
    }
}
