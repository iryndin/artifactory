package org.artifactory.webapp.wicket.page.config;

import org.apache.wicket.model.IModel;
import org.artifactory.descriptor.Descriptor;
import org.artifactory.webapp.wicket.common.component.help.HelpBubble;

/**
 * Schema help bubble is a help bubble that uses schema help model which retrieves the help message from the artifactory
 * schema.
 *
 * @author Yossi Shaul
 */
public class SchemaHelpBubble extends HelpBubble {
    private String property;

    /**
     * This constructor will remove the .help from the id for the property name.
     *
     * @param id The wicket id (must end with '.help')
     */
    public SchemaHelpBubble(String id) {
        this(id, id.substring(0, id.indexOf(".help")));
    }

    public SchemaHelpBubble(String id, String property) {
        super(id);
        this.property = property;
    }

    public SchemaHelpBubble(String id, SchemaHelpModel model) {
        super(id, model);
    }

    @Override
    protected IModel initModel() {
        IModel iModel = getParent().getInnermostModel();
        Descriptor descriptor = (Descriptor) iModel.getObject();
        return new SchemaHelpModel(descriptor, property);
    }
}
