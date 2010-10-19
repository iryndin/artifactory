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

package org.artifactory.webapp.wicket.page.home.settings;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.maven.MavenService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.wicket.component.label.highlighter.Syntax;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.modal.ModalHandler;
import org.artifactory.common.wicket.component.panel.titled.TitledPanel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * A base implementation of a settings generator panel
 *
 * @author Noam Y. Tenne
 */
public abstract class BaseSettingsGeneratorPanel extends TitledPanel implements SettingsGenerator {

    @SpringBean
    protected AuthorizationService authorizationService;

    @SpringBean
    protected MavenService mavenService;

    protected List<VirtualRepoEntry> virtualRepoEntries;
    protected Form form;
    protected String servletContextUrl;

    /**
     * Main constructor
     *
     * @param id                ID to assign to the panel
     * @param servletContextUrl Running context URL
     * @param virtualRepoKeyMap Virtual repo key association map
     */
    protected BaseSettingsGeneratorPanel(String id, String servletContextUrl, Map<String, String> virtualRepoKeyMap) {
        super(id);
        this.servletContextUrl = servletContextUrl;
        virtualRepoEntries = getVirtualRepoEntries(virtualRepoKeyMap);
        form = new Form("form");
    }

    /**
     * Builds a List of VirtualRepoEntry objects built out of the given Key-Description map
     *
     * @param virtualRepoKeyMap Virtual repository key-description map
     * @return VirtualRepoEntry list
     */
    protected List<VirtualRepoEntry> getVirtualRepoEntries(Map<String, String> virtualRepoKeyMap) {
        Set<String> repoKeys = virtualRepoKeyMap.keySet();
        List<VirtualRepoEntry> entries = new ArrayList<VirtualRepoEntry>();
        for (String repoKey : repoKeys) {
            entries.add(new VirtualRepoEntry(repoKey, virtualRepoKeyMap.get(repoKey)));
        }
        Collections.sort(entries);
        return entries;
    }

    /**
     * Returns the settings generation button
     *
     * @param servletContextUrl Running context URL
     * @return Generate Settings submit button
     */
    protected TitledAjaxSubmitLink getGenerateButton(final String servletContextUrl) {
        return new TitledAjaxSubmitLink("generate", getGenerateButtonTitle(), form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                ModalWindow modelWindow = ModalHandler.getInstanceFor(this);
                SettingsModalPanel modalPanel = new SettingsModalPanel(generateSettings(servletContextUrl),
                        getSettingsSyntax());
                modalPanel.setTitle(getSettingsWindowTitle());
                modelWindow.setContent(modalPanel);
                modelWindow.show(target);
            }
        };
    }

    /**
     * Returns the title of the setting generation button
     *
     * @return Generator button label
     */
    protected abstract String getGenerateButtonTitle();

    /**
     * Returns the title of the setting modal window
     *
     * @return Settings modal window title
     */
    protected abstract String getSettingsWindowTitle();

    /**
     * Returns the syntax type of the settings content
     *
     * @return Settings syntax type
     */
    protected abstract Syntax getSettingsSyntax();

    /**
     * Returns the mimetype of the settings content
     *
     * @return Settings mimetype
     */
    protected abstract String getSettingsMimeType();

    /**
     * Returns the default name to give to the file should the user want to download the settings
     *
     * @return Downloadable settings file name
     */
    protected abstract String getSaveToFileName();

    /**
     * A serializable object which represents a virtual repo. Used with the drop down choices on this panel.
     */
    protected static class VirtualRepoEntry implements Serializable, Comparable<VirtualRepoEntry> {
        private String repoKey;
        private String repoDescription;

        /**
         * Default constructor
         *
         * @param repoKey         Virtual repo key
         * @param repoDescription Virtual repo description
         */
        public VirtualRepoEntry(String repoKey, String repoDescription) {
            this.repoKey = repoKey;
            this.repoDescription = repoDescription;
        }

        /**
         * Returns the virtual repo key
         *
         * @return String - Virtual repo key
         */
        public String getRepoKey() {
            return repoKey;
        }

        /**
         * Returns the virtual repo description
         *
         * @return String - Virtual repo description
         */
        public String getRepoDescription() {
            return repoDescription;
        }

        /**
         * Make the repo key the default string representation
         *
         * @return String - repoKey
         */
        @Override
        public String toString() {
            return repoKey;
        }

        /**
         * Compare repo keys
         *
         * @param o Instance of VirtualRepoEntry
         * @return int - Comparison result
         */
        public int compareTo(VirtualRepoEntry o) {
            return getRepoKey().compareTo(o.getRepoKey());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (!(o instanceof VirtualRepoEntry)) {
                return false;
            }

            VirtualRepoEntry that = (VirtualRepoEntry) o;

            if (repoKey != null ? !repoKey.equals(that.repoKey) : that.repoKey != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return repoKey != null ? repoKey.hashCode() : 0;
        }
    }
}