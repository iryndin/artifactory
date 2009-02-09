package org.artifactory.webapp.wicket.common.component.panel.shortcutkey;

import org.apache.commons.lang.ArrayUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.artifactory.webapp.wicket.common.component.template.HtmlTemplate;

import java.util.HashMap;
import java.util.Map;

/**
 * ShortcutKeyHandler model holds the clicked key.
 *
 * @author Yoav Aharoni
 */
public class KeyEventHandler extends Panel {
    private Map<Integer, KeyListener> listenerMap = new HashMap<Integer, KeyListener>();

    public KeyEventHandler(String id) {
        super(id, new Model());

        HiddenField keyCodeField = new HiddenField("keyCodeField", getModel(), Integer.class);
        keyCodeField.setOutputMarkupId(true);
        keyCodeField.add(new AjaxFormComponentUpdatingBehavior("onkeyup") {
            protected void onUpdate(AjaxRequestTarget target) {
                onKeyUp(getKeyCode(), target);
            }
        });
        add(keyCodeField);

        HtmlTemplate template = new HtmlTemplate("initScript");
        template.setParameter("keyCodeField", new PropertyModel(keyCodeField, "markupId"));
        template.setParameter("keys", new KeysArrayModel());
        add(template);
    }

    protected void onKeyUp(Integer keyCode, AjaxRequestTarget target) {
        KeyListener keyListener = listenerMap.get(keyCode);
        if (keyListener != null) {
            KeyReleasedEvent event = new KeyReleasedEvent(keyCode, target);
            keyListener.keyReleased(event);
        }

    }

    public Integer getKeyCode() {
        return (Integer) getModelObject();
    }

    public void addKeyListener(KeyListener listener, Integer... keyCodes) {
        if (ArrayUtils.isEmpty(keyCodes)) {
            throw new IllegalArgumentException("got empty array of keyCodes");
        }

        for (Integer keyCode : keyCodes) {
            listenerMap.put(keyCode, listener);
        }
    }

    private class KeysArrayModel extends AbstractReadOnlyModel {
        public Object getObject() {
            StringBuilder buf = new StringBuilder();
            for (Integer keyCode : listenerMap.keySet()) {
                buf.append(',').append(keyCode);
            }
            return buf.substring(1);
        }
    }
}
