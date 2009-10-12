package org.artifactory.webapp.wicket.common.behavior.template;

import org.apache.wicket.Component;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.behavior.AbstractBehavior;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.template.PackagedTextTemplate;
import org.apache.wicket.util.template.TextTemplateHeaderContributor;

import java.io.Serializable;
import java.util.Map;

/**
 * @author Yoav Aharoni
 */
public class TemplateBehavior extends AbstractBehavior {
    private String beforeRenderString;
    private String afterRenderString;

    @Override
    public void bind(Component component) {
        super.bind(component);

        if (hasJavascriptContribution()) {
            component.add(newHeaderContributor());
        }

        loadTemplate();
    }

    protected boolean hasJavascriptContribution() {
        return false;
    }

    protected Map<String, String> getParameters() {
        return null;
    }

    private void loadTemplate() {
        String template =
                new PackagedTextTemplate(getClass(), getClass().getSimpleName() + ".html")
                        .asString(getParameters());
        String[] strings = template.split("<wicket:child/>", 2);
        beforeRenderString = strings[0];
        afterRenderString = strings[1];
    }

    protected TextTemplateHeaderContributor newHeaderContributor() {
        return TextTemplateHeaderContributor
                .forJavaScript(getClass(), getClass().getSimpleName() + ".js",
                        new Model((Serializable) getParameters()));
    }

    @Override
    public void beforeRender(Component component) {
        super.beforeRender(component);
        write(beforeRenderString);
    }

    @Override
    public void onRendered(Component component) {
        super.onRendered(component);
        write(afterRenderString);
    }

    private static void write(String render) {
        RequestCycle.get().getResponse().write(render);
    }
}
