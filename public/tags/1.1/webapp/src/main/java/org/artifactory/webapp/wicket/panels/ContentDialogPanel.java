package org.artifactory.webapp.wicket.panels;

import org.apache.log4j.Logger;
import wicket.ajax.AjaxRequestTarget;
import wicket.markup.html.basic.Label;
import wicket.markup.html.panel.Panel;
import wicket.model.Model;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ContentDialogPanel extends Panel {
    @SuppressWarnings({"UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ContentDialogPanel.class);

    private final Label contentText = new Label("contentText");

    public ContentDialogPanel(String id) {
        super(id);
        setOutputMarkupId(false);
        contentText.setOutputMarkupId(true);
        add(contentText);
    }

    public Label getContentText() {
        return contentText;
    }

    public String getContent() {
        return (String) contentText.getModelObject();
    }

    public void ajaxUpdate(String content, AjaxRequestTarget target) {
        contentText.setModel(new Model(content));
        target.addComponent(contentText);
        target.appendJavascript(
                "var currHeight=dojo.byId('content').style.height;" +
                "dojo.byId('content').style.height=dojo.html.getViewport().height*0.7+'px';");
        target.appendJavascript("dojo.widget.byId('contentDialog').show();");
    }
}
