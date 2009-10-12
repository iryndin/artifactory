package org.artifactory.webapp.wicket.page.config;

import org.apache.wicket.model.AbstractReadOnlyModel;
import org.artifactory.descriptor.DescriptionExtractor;
import org.artifactory.descriptor.Descriptor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This model extracts the help message from the artifactory schema using the descriptor
 * and property.
 *
 * @author Yossi Shaul
 */
public class SchemaHelpModel extends AbstractReadOnlyModel {
    private final static Logger log = LoggerFactory.getLogger(SchemaHelpModel.class);

    private Descriptor descriptor;
    private String propertyName;

    public SchemaHelpModel(Descriptor descriptor, String propertyName) {
        this.descriptor = descriptor;
        this.propertyName = propertyName;
    }

    @Override
    public Object getObject() {
        DescriptionExtractor extractor = DescriptionExtractor.getInstance();
        String helpMessage;
        try {
            helpMessage = extractor.getDescription(descriptor, propertyName);
        } catch (Exception e) {
            // don't fail, just log and return an empty string
            log.error(String.format("Failed to extract help message for descriptor %s with property %s", 
                    descriptor, propertyName), e);
            helpMessage = "";
        }
        return helpMessage;
    }
}
