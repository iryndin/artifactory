package org.artifactory.webapp.wicket.page.config.repos;

import org.apache.wicket.markup.html.form.*;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.repo.ChecksumPolicyType;
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoType;
import org.artifactory.webapp.wicket.common.component.CreateUpdateAction;
import org.artifactory.webapp.wicket.common.component.border.titled.TitledBorder;
import org.artifactory.webapp.wicket.common.component.checkbox.styled.StyledCheckbox;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;
import org.artifactory.webapp.wicket.utils.validation.UriValidator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Remote repository configuration panel.
 *
 * @author Yossi Shaul
 */
public class HttpRepoPanel extends RepoConfigCreateUpdatePanel<HttpRepoDescriptor> {
    public HttpRepoPanel(CreateUpdateAction action, HttpRepoDescriptor repoDescriptor) {
        super(action, repoDescriptor);

        TitledBorder localRepoFields = new TitledBorder("remoteRepoFields");
        form.add(localRepoFields);

        TextField urlField = new TextField("url");
        urlField.add(new UriValidator("http", "https"));
        urlField.setRequired(true);
        localRepoFields.add(urlField);
        localRepoFields.add(new SchemaHelpBubble("url.help"));

        List<RemoteRepoType> repoTypeList = getRemoteRepoTypeList();
        localRepoFields.add(new DropDownChoice("type", repoTypeList));
        localRepoFields.add(new SchemaHelpBubble("type.help"));

        localRepoFields.add(new TextField("username"));
        localRepoFields.add(new SchemaHelpBubble("username.help"));

        PasswordTextField passwordField = new PasswordTextField("password");
        passwordField.setRequired(false);
        passwordField.setResetPassword(false);
        localRepoFields.add(passwordField);
        localRepoFields.add(new SchemaHelpBubble("password.help"));

        localRepoFields.add(new TextField("localAddress"));
        localRepoFields.add(new SchemaHelpBubble("localAddress.help"));

        List<ProxyDescriptor> proxies = getEditingDescriptor().getProxies();
        DropDownChoice proxiesDropDown = new DropDownChoice("proxy", proxies,
                new ChoiceRenderer("key", "key"));
        proxiesDropDown.setNullValid(true);
        localRepoFields.add(proxiesDropDown);
        localRepoFields.add(new SchemaHelpBubble("proxy.help"));

        localRepoFields.add(new StyledCheckbox("handleReleases"));
        localRepoFields.add(new SchemaHelpBubble("handleReleases.help"));

        localRepoFields.add(new StyledCheckbox("handleSnapshots"));
        localRepoFields.add(new SchemaHelpBubble("handleSnapshots.help"));

        localRepoFields.add(new StyledCheckbox("blackedOut"));
        localRepoFields.add(new SchemaHelpBubble("blackedOut.help"));

        TitledBorder advanced = new TitledBorder("advanced");
        form.add(advanced);


        advanced.add(new StyledCheckbox("hardFail"));
        advanced.add(new SchemaHelpBubble("hardFail.help"));

        advanced.add(new StyledCheckbox("offline"));
        advanced.add(new SchemaHelpBubble("offline.help"));

        advanced.add(new StyledCheckbox("storeArtifactsLocally"));
        advanced.add(new SchemaHelpBubble("storeArtifactsLocally.help"));

        advanced.add(new TextField("maxUniqueSnapshots", Integer.class));
        advanced.add(new SchemaHelpBubble("maxUniqueSnapshots.help"));

        advanced.add(new TextArea("includesPattern"));
        advanced.add(new SchemaHelpBubble("includesPattern.help"));

        advanced.add(new TextArea("excludesPattern"));
        advanced.add(new SchemaHelpBubble("excludesPattern.help"));

        advanced.add(new TextField("failedRetrievalCachePeriodSecs", Long.class));
        advanced.add(new SchemaHelpBubble("failedRetrievalCachePeriodSecs.help"));

        advanced.add(new TextField("missedRetrievalCachePeriodSecs", Long.class));
        advanced.add(new SchemaHelpBubble("missedRetrievalCachePeriodSecs.help"));

        ChecksumPolicyType[] checksumPolicies = ChecksumPolicyType.values();
        DropDownChoice checksumPoliciesDC = new DropDownChoice("checksumPolicyType", Arrays.asList(checksumPolicies));
        checksumPoliciesDC.setChoiceRenderer(new ChecksumPolicyChoiceRenderer());
        advanced.add(checksumPoliciesDC);
        advanced.add(new SchemaHelpBubble("checksumPolicyType.help"));

        advanced.add(new TextField("socketTimeoutMillis", Integer.class));
        advanced.add(new SchemaHelpBubble("socketTimeoutMillis.help"));
    }

    private List<RemoteRepoType> getRemoteRepoTypeList() {
        List<RemoteRepoType> repoTypeList = new ArrayList<RemoteRepoType>();
        for (RemoteRepoType remoteRepoType : RemoteRepoType.values()) {
            // don;t include the obr type (not supported yet)
            if (!remoteRepoType.equals(RemoteRepoType.obr)) {
                repoTypeList.add(remoteRepoType);
            }
        }
        return repoTypeList;
    }

    @Override
    public void handleCreate(CentralConfigDescriptor descriptor) {
        HttpRepoDescriptor remoteRepo = getRepoDescriptor();
        getEditingDescriptor().addRemoteRepository(remoteRepo);
    }

    private class ChecksumPolicyChoiceRenderer extends ChoiceRenderer {
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
}