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

package org.artifactory.common.wicket.panel.maven.settings;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.artifactory.common.wicket.WicketProperty;
import org.artifactory.common.wicket.behavior.defaultbutton.DefaultButtonBehavior;
import org.artifactory.common.wicket.component.border.titled.TitledBorder;
import org.artifactory.common.wicket.component.checkbox.styled.StyledCheckbox;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.modal.ModalHandler;
import org.artifactory.common.wicket.component.panel.titled.TitledPanel;
import org.artifactory.common.wicket.panel.maven.settings.modal.MavenSettingsModalPanel;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Enables the user to select several virtual repos that are configured in the system, and generate from his selection A
 * "repositories", and "mirrors" block that can be used with the maven settings.xml<br> Since this panel has to be
 * reusable, it requires a pre-created form and submit button to add to itself.
 *
 * @author Noam Tenne
 */
public class MavenSettingsPanel extends TitledPanel {
    private MavenSettingsGenerator generator;

    private List<VirutalRepoEntry> virtualRepoEntries;

    private Form form;

    @WicketProperty
    private VirutalRepoEntry releases;

    @WicketProperty
    private VirutalRepoEntry snapshots;

    @WicketProperty
    private VirutalRepoEntry pluginReleases;

    @WicketProperty
    private VirutalRepoEntry pluginSnapshots;

    @WicketProperty
    private boolean mirrorAny;

    @WicketProperty
    private VirutalRepoEntry mirrorAnySelection;

    /**
     * Default Constructor
     *
     * @param id                Panel ID
     * @param virtualRepoKeyMap Map of virtual repo keys and descriptions
     */
    public MavenSettingsPanel(String id, Map<String, String> virtualRepoKeyMap, MavenSettingsGenerator generator) {
        super(id);
        this.generator = generator;

        //Build a map of VirtualRepoEntry objects
        virtualRepoEntries = getVirtualRepoEntries(virtualRepoKeyMap);
        init();
    }

    /**
     * Init panel components
     */
    private void init() {
        form = new Form("form");
        TitledBorder border = new TitledBorder("mavenSettingsBorder");

        addChoice("releases", true, false, false, false);
        addChoice("snapshots", false, true, false, false);
        addChoice("pluginReleases", true, false, true, false);
        addChoice("pluginSnapshots", false, true, true, false);

        final DropDownChoice mirrorDropDownChoice =
                new DropDownChoice("mirrorAnySelection", new PropertyModel(this, "mirrorAnySelection"),
                        virtualRepoEntries);
        mirrorDropDownChoice.setOutputMarkupId(true);
        if (!virtualRepoEntries.isEmpty()) {
            mirrorDropDownChoice.setModelObject(getDefaultChoice(false, false, false, true));
        }
        mirrorDropDownChoice.setEnabled(false);
        form.add(mirrorDropDownChoice);
        form.add(new HelpBubble("mirrorAnySelection.help", new ResourceModel("mirrorAnySelection.help")));

        final StyledCheckbox mirrorAnyCheckbox =
                new StyledCheckbox("mirrorAny", new PropertyModel(this, "mirrorAny"));
        mirrorAnyCheckbox.setModelObject(Boolean.FALSE);
        mirrorAnyCheckbox.add(new AjaxFormComponentUpdatingBehavior("onclick") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                mirrorDropDownChoice.setEnabled(mirrorAnyCheckbox.isChecked());
                target.addComponent(mirrorDropDownChoice);
            }
        });
        form.add(mirrorAnyCheckbox);

        TitledAjaxSubmitLink generateButton = new TitledAjaxSubmitLink("generate", "Generate Settings", form) {
            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                ModalWindow modelWindow = ModalHandler.getInstanceFor(this);
                MavenSettingsModalPanel modalPanel = new MavenSettingsModalPanel(generator.generateSettings());
                modalPanel.setTitle("Generated Maven Settings");
                modelWindow.setContent(modalPanel);
                modelWindow.show(target);
            }
        };

        form.add(new DefaultButtonBehavior(generateButton));
        border.add(form);
        add(border);
        add(generateButton);
    }

    /**
     * Returns the selected release repo key
     *
     * @return String - Release repo key
     */
    public String getReleasesRepoKey() {
        return releases.getRepoKey();
    }

    /**
     * Returns the selected snapshot repo key
     *
     * @return String - Snapshot repo key
     */
    public String getSnapshotsRepoKey() {
        return snapshots.getRepoKey();
    }

    /**
     * Returns the selected plugin releases repo key
     *
     * @return String - Plugin releases repo key
     */
    public String getPluginReleasesRepoKey() {
        return pluginReleases.getRepoKey();
    }

    /**
     * Returns the selected plugin snapshots repo key
     *
     * @return String - Plugin snapshots repo key
     */
    public String getPluginSnapshotsRepoKey() {
        return pluginSnapshots.getRepoKey();
    }

    /**
     * Returns is mirror any selected
     *
     * @return boolean - Is mirror any selected
     */
    public boolean isMirrorAny() {
        return mirrorAny;
    }

    public void setMirrorAny(boolean mirrorAny) {
        this.mirrorAny = mirrorAny;
    }

    /**
     * Returns the selected mirror any repo key
     *
     * @return String - Mirror any repo key
     */
    public String getMirrorAnyKey() {
        return mirrorAnySelection.getRepoKey();
    }

    /**
     * Adds to the form a DropDownChoice. Sets the object a property model with the id as the expression and adds the
     * virtualRepoEntries list to it.
     *
     * @param id Object ID
     */
    private void addChoice(String id, boolean isRelease, boolean isSnapshot, boolean isPlugin,
            boolean isRemote) {
        DropDownChoice choice = new DropDownChoice(id, new PropertyModel(this, id), virtualRepoEntries);
        if (!virtualRepoEntries.isEmpty()) {
            choice.setModelObject(getDefaultChoice(isRelease, isSnapshot, isPlugin, isRemote));
        }
        form.add(choice);
        form.add(new HelpBubble(id + ".help", new ResourceModel(id + ".help")));
    }

    private VirutalRepoEntry getDefaultChoice(boolean isRelease, boolean isSnapshot, boolean isPlugin,
            boolean isRemote) {
        for (VirutalRepoEntry virtualEntry : virtualRepoEntries) {
            boolean canBeDefault = true;
            String key = virtualEntry.getRepoKey();
            if (isRelease && !repoKeyContains(key, "release")) {
                canBeDefault = false;
            }
            if (isSnapshot && !repoKeyContains(key, "snapshot")) {
                canBeDefault = false;
            }
            if (isPlugin && !repoKeyContains(key, "plugin")) {
                canBeDefault = false;
            }
            if (isRemote && !repoKeyContains(key, "remote")) {
                canBeDefault = false;
            }

            if (canBeDefault) {
                return virtualEntry;
            }
        }

        return virtualRepoEntries.get(0);
    }

    private boolean repoKeyContains(String repoKey, String idText) {
        return StringUtils.containsIgnoreCase(repoKey, idText);
    }

    /**
     * Builds a List of VirtualRepoEntry objects built out of the given Key-Description map
     *
     * @param virtualRepoKeyMap Virtual repository key-description map
     * @return List<VirutalRepoEntry> - VirtualRepoEntry list
     */
    private List<VirutalRepoEntry> getVirtualRepoEntries(Map<String, String> virtualRepoKeyMap) {
        Set<String> repoKeys = virtualRepoKeyMap.keySet();
        List<VirutalRepoEntry> entries = new ArrayList<VirutalRepoEntry>();
        for (String repoKey : repoKeys) {
            entries.add(new VirutalRepoEntry(repoKey, virtualRepoKeyMap.get(repoKey)));
        }
        Collections.sort(entries);
        return entries;
    }

    /**
     * A serializable object which represents a virtual repo. Used with the drop down choices on this panel.
     */
    private static class VirutalRepoEntry implements Serializable, Comparable {
        private String repoKey;
        private String repoDescription;

        /**
         * Default constructor
         *
         * @param repoKey         Virtual repo key
         * @param repoDescription Virtual repo description
         */
        public VirutalRepoEntry(String repoKey, String repoDescription) {
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
         * @param o Instance of VirutalRepoEntry
         * @return int - Comparison result
         */
        public int compareTo(Object o) {
            if (o instanceof VirutalRepoEntry) {
                String externalKey = ((VirutalRepoEntry) o).getRepoKey();
                return getRepoKey().compareTo(externalKey);
            }
            return 0;
        }
    }
}