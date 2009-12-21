/*
 * This file is part of Artifactory.
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
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.Model;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.ValidationError;
import org.apache.wicket.validation.validator.NumberValidator;
import org.apache.wicket.validation.validator.StringValidator;
import org.artifactory.addon.wicket.PropertiesAddon;
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
    public HttpRepoPanel(CreateUpdateAction action, HttpRepoDescriptor repoDescriptor,
                         MutableCentralConfigDescriptor mutableCentralConfig) {
        super(action, repoDescriptor, mutableCentralConfig);

        TitledBorder localRepoFields = new TitledBorder("remoteRepoFields");
        form.add(localRepoFields);

        TextField urlField = new TextField("url");
        urlField.add(new UriValidator("http", "https"));
        urlField.setRequired(true);
        localRepoFields.add(urlField);
        localRepoFields.add(new SchemaHelpBubble("url.help"));

        List<RepoType> repoTypeList = getRemoteRepoTypeList();
        localRepoFields.add(new DropDownChoice("type", repoTypeList));
        localRepoFields.add(new SchemaHelpBubble("type.help"));

        localRepoFields.add(new TextField("username"));
        localRepoFields.add(new SchemaHelpBubble("username.help"));

        PasswordTextField passwordField = new PasswordTextField("password");
        passwordField.setRequired(false);
        passwordField.setResetPassword(false);
        localRepoFields.add(passwordField);
        localRepoFields.add(new SchemaHelpBubble("password.help"));

        TextField localAddressField = new TextField("localAddress");
        localAddressField.add(new LocalAddressValidator());
        localRepoFields.add(localAddressField);
        localRepoFields.add(new SchemaHelpBubble("localAddress.help"));

        List<ProxyDescriptor> proxies = mutableCentralConfig.getProxies();
        DropDownChoice proxiesDropDown = new DropDownChoice("proxy", proxies, new ChoiceRenderer("key", "key"));
        ProxyDescriptor defaultProxyDescriptor = mutableCentralConfig.getDefaultProxy();
        if (defaultProxyDescriptor != null && CreateUpdateAction.CREATE.equals(action)) {
            proxiesDropDown.setModel(new Model(defaultProxyDescriptor));
        }
        proxiesDropDown.setNullValid(true);
        localRepoFields.add(proxiesDropDown);
        localRepoFields.add(new SchemaHelpBubble("proxy.help"));

        localRepoFields.add(new StyledCheckbox("handleReleases"));
        localRepoFields.add(new SchemaHelpBubble("handleReleases.help"));

        localRepoFields.add(new StyledCheckbox("handleSnapshots"));
        localRepoFields.add(new SchemaHelpBubble("handleSnapshots.help"));

        localRepoFields.add(new StyledCheckbox("blackedOut"));
        localRepoFields.add(new SchemaHelpBubble("blackedOut.help"));

        localRepoFields.add(new StyledCheckbox("suppressPomConsistencyChecks"));
        localRepoFields.add(new SchemaHelpBubble("suppressPomConsistencyChecks.help"));

        TitledBorder advanced = new TitledBorder("advanced");
        advanced.add(new CollapsibleBehavior().setResizeModal(true));
        form.add(advanced);

        advanced.add(new StyledCheckbox("hardFail"));
        advanced.add(new SchemaHelpBubble("hardFail.help"));

        advanced.add(new StyledCheckbox("offline"));
        advanced.add(new SchemaHelpBubble("offline.help"));

        advanced.add(new StyledCheckbox("storeArtifactsLocally"));
        advanced.add(new SchemaHelpBubble("storeArtifactsLocally.help"));

        advanced.add(new StyledCheckbox("fetchJarsEagerly"));
        advanced.add(new SchemaHelpBubble("fetchJarsEagerly.help"));

        advanced.add(new StyledCheckbox("fetchSourcesEagerly"));
        advanced.add(new SchemaHelpBubble("fetchSourcesEagerly.help"));

        advanced.add(new TextField("maxUniqueSnapshots", Integer.class));
        advanced.add(new SchemaHelpBubble("maxUniqueSnapshots.help"));

        advanced.add(new TextArea("includesPattern"));
        advanced.add(new SchemaHelpBubble("includesPattern.help"));

        advanced.add(new TextArea("excludesPattern"));
        advanced.add(new SchemaHelpBubble("excludesPattern.help"));

        advanced.add(new TextField("retrievalCachePeriodSecs", Long.class));
        advanced.add(new SchemaHelpBubble("retrievalCachePeriodSecs.help"));

        advanced.add(new TextField("failedRetrievalCachePeriodSecs", Long.class));
        advanced.add(new SchemaHelpBubble("failedRetrievalCachePeriodSecs.help"));

        advanced.add(new TextField("missedRetrievalCachePeriodSecs", Long.class));
        advanced.add(new SchemaHelpBubble("missedRetrievalCachePeriodSecs.help"));

        advanced.add(new StyledCheckbox("shareConfiguration"));
        advanced.add(new SchemaHelpBubble("shareConfiguration.help"));

        ChecksumPolicyType[] checksumPolicies = ChecksumPolicyType.values();
        DropDownChoice checksumPoliciesDC = new DropDownChoice("checksumPolicyType", Arrays.asList(checksumPolicies));
        checksumPoliciesDC.setChoiceRenderer(new ChecksumPolicyChoiceRenderer());
        advanced.add(checksumPoliciesDC);
        advanced.add(new SchemaHelpBubble("checksumPolicyType.help"));

        advanced.add(new TextField("socketTimeoutMillis", Integer.class));
        advanced.add(new SchemaHelpBubble("socketTimeoutMillis.help"));

        final TextField unusedCleanupTextField = new TextField("unusedArtifactsCleanupPeriodHours", Integer.class);
        unusedCleanupTextField.add(new NumberValidator.MinimumValidator(1));
        unusedCleanupTextField.setOutputMarkupId(true);
        unusedCleanupTextField.setEnabled(entity.isUnusedArtifactsCleanupEnabled());
        advanced.add(unusedCleanupTextField);
        advanced.add(new SchemaHelpBubble("unusedArtifactsCleanupPeriodHours.help"));

        final StyledCheckbox unusedCleanupCheckBox = new StyledCheckbox("unusedArtifactsCleanupEnabled");
        unusedCleanupCheckBox.add(new AjaxFormComponentUpdatingBehavior("onclick") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                boolean unusedCleanupSelected = unusedCleanupCheckBox.isChecked();
                unusedCleanupTextField.setEnabled(unusedCleanupSelected);

                if (!unusedCleanupSelected) {
                    unusedCleanupTextField.setModelObject("0");
                }
                target.addComponent(unusedCleanupTextField);
            }
        });
        advanced.add(unusedCleanupCheckBox);

        PropertiesAddon propertiesAddon = getAddonsProvider().addonByType(PropertiesAddon.class);
        WebMarkupContainer propertySetsBorder =
                propertiesAddon.getPropertySetsBorder("setsBorder", "propertySets", entity,
                        mutableCentralConfig.getPropertySets());

        /**
         * Temporarily hide property sets border. This is done because currently, an enabled anonymous user has deploy
         * permissions on any remote repo cache, and having deploy permissions on a repo, means that you can add
         * properties to it. We do not want to enable this feature to anonymous users at this time.
         */
        propertySetsBorder.setVisible(false);
        form.add(propertySetsBorder);
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

    @Override
    public void addDescriptor(MutableCentralConfigDescriptor mccd, HttpRepoDescriptor repoDescriptor) {
        mccd.addRemoteRepository(repoDescriptor);
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
                        return "Ignore and return generated checksum";
                    case PASS_THRU:
                        return "Return the remote checksum";
                    default:
                        return policy;
                }
            } else {
                return super.getDisplayValue(object);
            }
        }
    }

    private class LocalAddressValidator extends StringValidator {
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
}