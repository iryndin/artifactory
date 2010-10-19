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

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.MinimumValidator;
import org.apache.wicket.validation.validator.StringValidator;
import org.artifactory.common.wicket.WicketProperty;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.behavior.collapsible.CollapsibleBehavior;
import org.artifactory.common.wicket.component.CreateUpdateAction;
import org.artifactory.common.wicket.component.border.titled.TitledBorder;
import org.artifactory.common.wicket.component.checkbox.styled.StyledCheckbox;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.descriptor.repo.ChecksumPolicyType;
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.descriptor.repo.RepoType;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;
import org.artifactory.webapp.wicket.util.validation.UriValidator;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Remote repository configuration panel.
 *
 * @author Yossi Shaul
 */
public class HttpRepoPanel extends RepoConfigCreateUpdatePanel<HttpRepoDescriptor> {

    @WicketProperty
    private boolean isStoreArtifactsLocally = false;

    public HttpRepoPanel(CreateUpdateAction action, HttpRepoDescriptor repoDescriptor,
                         CachingDescriptorHelper cachingDescriptorHelper) {
        super(action, repoDescriptor, cachingDescriptorHelper);

        MutableCentralConfigDescriptor mutableCentralConfigDescriptor =
                cachingDescriptorHelper.getModelMutableDescriptor();

        addBasicSettings(action, mutableCentralConfigDescriptor, cachingDescriptorHelper);

        addAdvancedSettings();

        addPropertySetsSettings(cachingDescriptorHelper);
    }

    @Override
    public void addAndSaveDescriptor(HttpRepoDescriptor repoDescriptor) {
        repoDescriptor.setStoreArtifactsLocally(!isStoreArtifactsLocally);
        CachingDescriptorHelper helper = getCachingDescriptorHelper();
        MutableCentralConfigDescriptor mccd = helper.getModelMutableDescriptor();
        mccd.addRemoteRepository(repoDescriptor);
        helper.syncAndSaveRemoteRepositories();
    }

    @Override
    public void saveEditDescriptor(HttpRepoDescriptor repoDescriptor) {
        // flip back the logic again from the checkbox.
        repoDescriptor.setStoreArtifactsLocally(!isStoreArtifactsLocally);
        getCachingDescriptorHelper().syncAndSaveRemoteRepositories();
    }

    private void addBasicSettings(CreateUpdateAction action,
                                  MutableCentralConfigDescriptor mutableCentralConfigDescriptor,
                                  CachingDescriptorHelper cachingDescriptorHelper) {
        TitledBorder basicSettings = new TitledBorder("basicSettings");
        form.add(basicSettings);

        final HttpRepoGeneralSettingsPanel generalSettings =
                new HttpRepoGeneralSettingsPanel("generalSettings", isCreate(), cachingDescriptorHelper);
        basicSettings.add(generalSettings);

        // url
        TextField<String> urlField = new TextField<String>("url");
        urlField.add(new UriValidator("http", "https"));
        urlField.setRequired(true);
        generalSettings.add(urlField);
        generalSettings.add(new SchemaHelpBubble("url.help"));

        // type
        List<RepoType> repoTypeList = getRemoteRepoTypeList();
        basicSettings.add(new DropDownChoice<RepoType>("type", repoTypeList));
        basicSettings.add(new SchemaHelpBubble("type.help"));

        // proxy
        List<ProxyDescriptor> proxies = mutableCentralConfigDescriptor.getProxies();
        DropDownChoice<ProxyDescriptor> proxiesDropDown = new DropDownChoice<ProxyDescriptor>("proxy", proxies,
                new ChoiceRenderer<ProxyDescriptor>("key", "key"));
        ProxyDescriptor defaultProxyDescriptor = mutableCentralConfigDescriptor.getDefaultProxy();
        if (defaultProxyDescriptor != null && CreateUpdateAction.CREATE.equals(action)) {
            proxiesDropDown.setDefaultModel(new Model<ProxyDescriptor>(defaultProxyDescriptor));
        }
        proxiesDropDown.setNullValid(true);
        basicSettings.add(proxiesDropDown);
        basicSettings.add(new SchemaHelpBubble("proxy.help"));

        // checksumPolicyType
        ChecksumPolicyType[] checksumPolicies = ChecksumPolicyType.values();
        DropDownChoice<ChecksumPolicyType> checksumPoliciesDC = new DropDownChoice<ChecksumPolicyType>(
                "checksumPolicyType", Arrays.asList(checksumPolicies));
        checksumPoliciesDC.setChoiceRenderer(new ChecksumPolicyChoiceRenderer());
        basicSettings.add(checksumPoliciesDC);
        basicSettings.add(new SchemaHelpBubble("remoteRepoChecksumPolicyType.help", "checksumPolicyType"));

        // checkboxes
        basicSettings.add(new StyledCheckbox("handleReleases"));
        basicSettings.add(new SchemaHelpBubble("handleReleases.help"));

        basicSettings.add(new StyledCheckbox("handleSnapshots"));
        basicSettings.add(new SchemaHelpBubble("handleSnapshots.help"));

        basicSettings.add(new StyledCheckbox("blackedOut"));
        basicSettings.add(new SchemaHelpBubble("blackedOut.help"));

        basicSettings.add(new StyledCheckbox("offline"));
        basicSettings.add(new SchemaHelpBubble("offline.help"));

        basicSettings.add(new StyledCheckbox("shareConfiguration"));
        basicSettings.add(new SchemaHelpBubble("shareConfiguration.help"));
    }

    private void addAdvancedSettings() {
        TitledBorder advancedSettings = new TitledBorder("advancedSettings");
        advancedSettings.add(new CssClass("http-repo-panell-advanced-settings"));
        advancedSettings.add(new CollapsibleBehavior().setResizeModal(true));
        form.add(advancedSettings);

        // maxUniqueSnapshots
        advancedSettings.add(new TextField<Integer>("maxUniqueSnapshots", Integer.class));
        advancedSettings.add(new SchemaHelpBubble("maxUniqueSnapshots.help"));

        // unusedArtifactsCleanupPeriodHours
        final TextField<Integer> unusedCleanupTextField = new TextField<Integer>("unusedArtifactsCleanupPeriodHours",
                Integer.class);
        unusedCleanupTextField.add(new MinimumValidator<Integer>(1));
        unusedCleanupTextField.setOutputMarkupId(true);
        unusedCleanupTextField.setEnabled(entity.isUnusedArtifactsCleanupEnabled());
        advancedSettings.add(unusedCleanupTextField);
        advancedSettings.add(new SchemaHelpBubble("unusedArtifactsCleanupPeriodHours.help"));

        final StyledCheckbox unusedCleanupCheckBox = new StyledCheckbox("unusedArtifactsCleanupEnabled");
        unusedCleanupCheckBox.add(new AjaxFormComponentUpdatingBehavior("onclick") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                boolean unusedCleanupSelected = unusedCleanupCheckBox.isChecked();
                unusedCleanupTextField.setEnabled(unusedCleanupSelected);

                if (!unusedCleanupSelected) {
                    unusedCleanupTextField.setDefaultModelObject("0");
                }
                target.addComponent(unusedCleanupTextField);
            }
        });
        advancedSettings.add(unusedCleanupCheckBox);

        // checkboxes
        advancedSettings.add(new StyledCheckbox("fetchJarsEagerly"));
        advancedSettings.add(new SchemaHelpBubble("fetchJarsEagerly.help"));

        advancedSettings.add(new StyledCheckbox("fetchSourcesEagerly"));
        advancedSettings.add(new SchemaHelpBubble("fetchSourcesEagerly.help"));

        advancedSettings.add(new StyledCheckbox("hardFail"));
        advancedSettings.add(new SchemaHelpBubble("hardFail.help"));
        flipLogic();
        StyledCheckbox checkbox = new StyledCheckbox("storeArtifactsLocally");
        checkbox.setDefaultModel(new PropertyModel(this, "isStoreArtifactsLocally"));
        advancedSettings.add(checkbox);
        advancedSettings.add(new SchemaHelpBubble("storeArtifactsLocally.help"));

        advancedSettings.add(new StyledCheckbox("suppressPomConsistencyChecks"));
        advancedSettings.add(new SchemaHelpBubble("suppressPomConsistencyChecks.help"));

        // username
        advancedSettings.add(new TextField("username"));
        advancedSettings.add(new SchemaHelpBubble("username.help"));

        // password
        PasswordTextField passwordField = new PasswordTextField("password");
        passwordField.setRequired(false);
        passwordField.setResetPassword(false);
        advancedSettings.add(passwordField);
        advancedSettings.add(new SchemaHelpBubble("password.help"));

        // localAddress
        TextField<String> localAddressField = new TextField<String>("localAddress");
        localAddressField.add(new LocalAddressValidator());
        advancedSettings.add(localAddressField);
        advancedSettings.add(new SchemaHelpBubble("localAddress.help"));

        advancedSettings.add(new TextField<Integer>("socketTimeoutMillis", Integer.class));
        advancedSettings.add(new SchemaHelpBubble("socketTimeoutMillis.help"));

        advancedSettings.add(new TextField<Long>("retrievalCachePeriodSecs", Long.class));
        advancedSettings.add(new SchemaHelpBubble("retrievalCachePeriodSecs.help"));

        advancedSettings.add(new TextField<Long>("failedRetrievalCachePeriodSecs", Long.class));
        advancedSettings.add(new SchemaHelpBubble("failedRetrievalCachePeriodSecs.help"));

        advancedSettings.add(new TextField<Long>("missedRetrievalCachePeriodSecs", Long.class));
        advancedSettings.add(new SchemaHelpBubble("missedRetrievalCachePeriodSecs.help"));

        advancedSettings.add(new StyledCheckbox("synchronizeProperties"));
        advancedSettings.add(new SchemaHelpBubble("synchronizeProperties.help"));

        advancedSettings.add(new TextArea("notes"));
        advancedSettings.add(new SchemaHelpBubble("notes.help"));
    }

    private void flipLogic() {
        if (!isCreate()) {
            isStoreArtifactsLocally = !getRepoDescriptor().isStoreArtifactsLocally();
        }
    }

    private List<RepoType> getRemoteRepoTypeList() {
        List<RepoType> repoTypeList = new ArrayList<RepoType>();
        for (RepoType remoteRepoType : RepoType.values()) {
            // don;t include the obr type (not supported yet)
            if (!remoteRepoType.equals(RepoType.obr)) {
                repoTypeList.add(remoteRepoType);
            }
        }
        return repoTypeList;
    }

    private static class LocalAddressValidator extends StringValidator {
        @Override
        protected void onValidate(IValidatable validatable) {
            String localAddress = (String) validatable.getValue();
            try {
                InetAddress.getByName(localAddress);
            } catch (UnknownHostException e) {
                ValidationError error = new ValidationError();
                error.setMessage("Invalid local address: " + e.getMessage());
                validatable.error(error);
            }
        }

    }

    private static class ChecksumPolicyChoiceRenderer extends ChoiceRenderer {
        @Override
        public Object getDisplayValue(Object object) {
            if (object instanceof ChecksumPolicyType) {
                ChecksumPolicyType policy = (ChecksumPolicyType) object;
                switch (policy) {
                    case FAIL:
                        return "Fail";
                    case GEN_IF_ABSENT:
                        return "Generate if absent";
                    case IGNORE_AND_GEN:
                        return "Ignore and generated";
                    case PASS_THRU:
                        return "Ignore and pass-through";
                    default:
                        return policy;
                }
            } else {
                return super.getDisplayValue(object);
            }
        }
    }

    private static class HttpRepoGeneralSettingsPanel extends RepoGeneralSettingsPanel {
        public HttpRepoGeneralSettingsPanel(String id, boolean create,
                                            CachingDescriptorHelper cachingDescriptorHelper) {
            super(id, create, cachingDescriptorHelper);
        }
    }

}