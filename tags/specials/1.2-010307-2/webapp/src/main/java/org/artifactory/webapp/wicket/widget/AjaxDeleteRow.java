package org.artifactory.webapp.wicket.widget;

import org.apache.log4j.Logger;
import wicket.Component;
import wicket.ajax.AjaxEventBehavior;
import wicket.ajax.AjaxRequestTarget;
import wicket.markup.ComponentTag;
import wicket.markup.MarkupStream;
import wicket.markup.html.WebMarkupContainer;
import wicket.model.IModel;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public abstract class AjaxDeleteRow<T> extends WebMarkupContainer {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(AjaxDeleteRow.class);

    private T toBeDeletedObject;

    @SuppressWarnings({"unchecked"})
    public AjaxDeleteRow(String id, IModel model, final Component listener) {
        super(id);
        toBeDeletedObject = (T) model.getObject(this);
        add(new AjaxEventBehavior("onClick") {
            protected void onEvent(final AjaxRequestTarget target) {
                doDelete();
                onDeleted(target, listener);
            }

            @SuppressWarnings({"UnnecessaryLocalVariable"})
            @Override
            protected CharSequence getCallbackScript() {
                CharSequence orig = super.getCallbackScript();
                String callbackScript =
                        "if (confirm('" + getConfirmationQuestion() + "')) {" +
                                orig + "} else { return false; }";
                return callbackScript;
            }
        });
    }

    public T getToBeDeletedObject() {
        return toBeDeletedObject;
    }

    protected void onComponentTag(ComponentTag tag) {
        super.onComponentTag(tag);
        tag.setName("img");
        tag.put("src", "../images/delete.png");
        tag.put("alt", "Delete");
        tag.put("style", "cursor:pointer;");
    }

    @Override
    protected void onComponentTagBody(
            final MarkupStream markupStream, final ComponentTag openTag) {
        replaceComponentTagBody(markupStream, openTag, "");
    }

    protected abstract void doDelete();

    protected abstract void onDeleted(AjaxRequestTarget target, Component listener);

    protected abstract String getConfirmationQuestion();
}
