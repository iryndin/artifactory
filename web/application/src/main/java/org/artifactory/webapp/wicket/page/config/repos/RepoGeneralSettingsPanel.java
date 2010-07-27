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

package org.artifactory.webapp.wicket.page.config.repos;

import org.apache.wicket.markup.html.form.RequiredTextField;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.panel.Panel;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;
import org.artifactory.webapp.wicket.util.validation.JcrNameValidator;
import org.artifactory.webapp.wicket.util.validation.UniqueXmlIdValidator;
import org.artifactory.webapp.wicket.util.validation.XsdNCNameValidator;

/**
 * @author Yoav Aharoni
 */
public class RepoGeneralSettingsPanel extends Panel {
    public RepoGeneralSettingsPanel(String id, boolean create, CachingDescriptorHelper cachingDescriptorHelper) {
        super(id);
        // Repository name
        RequiredTextField repoKeyField = new RequiredTextField("key");
        repoKeyField.setEnabled(create);// don't allow key update
        if (create) {
            repoKeyField.add(new JcrNameValidator("Invalid repository key '%s'."));
            repoKeyField.add(new XsdNCNameValidator("Invalid repository key '%s'."));
            repoKeyField.add(new UniqueXmlIdValidator(cachingDescriptorHelper.getModelMutableDescriptor()));
        }

        add(repoKeyField);
        add(new SchemaHelpBubble("key.help"));

        // Repository description
        add(new TextArea("description"));
        add(new SchemaHelpBubble("description.help"));

        add(new TextArea("includesPattern"));
        add(new SchemaHelpBubble("includesPattern.help"));

        add(new TextArea("excludesPattern"));
        add(new SchemaHelpBubble("excludesPattern.help"));
    }
}
