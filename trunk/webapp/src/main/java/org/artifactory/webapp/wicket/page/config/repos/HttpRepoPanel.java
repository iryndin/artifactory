package org.artifactory.webapp.wicket.page.config.repos;

import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.artifactory.descriptor.config.CentralConfigDescriptor;
import org.artifactory.descriptor.repo.HttpRepoDescriptor;
import org.artifactory.descriptor.repo.ProxyDescriptor;
import org.artifactory.descriptor.repo.RemoteRepoType;
import org.artifactory.webapp.wicket.common.component.CreateUpdateAction;
import org.artifactory.webapp.wicket.common.component.border.titled.TitledBorder;
import org.artifactory.webapp.wicket.common.component.checkbox.styled.StyledCheckbox;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;
import org.artifactory.webapp.wicket.utils.validation.UriValidator;

import java.util.ArrayList;
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

        localRepoFields.add(new StyledCheckbox("handleReleases"));
        localRepoFields.add(new StyledCheckbox("handleSnapshots"));
        localRepoFields.add(new StyledCheckbox("blackedOut"));

        localRepoFields.add(new StyledCheckbox("hardFail"));
        localRepoFields.add(new StyledCheckbox("offline"));
        localRepoFields.add(new StyledCheckbox("storeArtifactsLocally"));

        localRepoFields.add(new TextField("maxUniqueSnapshots", Integer.class));
        localRepoFields.add(new TextField("includesPattern"));
        localRepoFields.add(new TextField("excludesPattern"));

        TextField urlField = new TextField("url");
        urlField.add(new UriValidator("http", "https"));
        urlField.setRequired(true);
        localRepoFields.add(urlField);
        localRepoFields.add(new TextField("failedRetrievalCachePeriodSecs", Long.class));
        localRepoFields.add(new TextField("missedRetrievalCachePeriodSecs", Long.class));

        List<RemoteRepoType> repoTypeList = getRemoteRepoTypeList();
        localRepoFields.add(new DropDownChoice("type", repoTypeList));


        localRepoFields.add(new TextField("username"));
        PasswordTextField passwordField = new PasswordTextField("password");
        passwordField.setRequired(false);
        passwordField.setResetPassword(false);
        localRepoFields.add(passwordField);

        localRepoFields.add(new TextField("socketTimeoutMillis", Integer.class));
        localRepoFields.add(new TextField("localAddress"));

        List<ProxyDescriptor> proxies = getEditingDescriptor().getProxies();
        DropDownChoice proxiesDropDown = new DropDownChoice("proxy", proxies,
                new ChoiceRenderer("key", "key"));
        proxiesDropDown.setNullValid(true);
        localRepoFields.add(proxiesDropDown);

        localRepoFields.add(new SchemaHelpBubble("maxUniqueSnapshots.help"));
        localRepoFields.add(new SchemaHelpBubble("includesPattern.help"));
        localRepoFields.add(new SchemaHelpBubble("excludesPattern.help"));
        localRepoFields.add(new SchemaHelpBubble("url.help"));
        localRepoFields.add(new SchemaHelpBubble("failedRetrievalCachePeriodSecs.help"));
        localRepoFields.add(new SchemaHelpBubble("missedRetrievalCachePeriodSecs.help"));
        localRepoFields.add(new SchemaHelpBubble("type.help"));
        localRepoFields.add(new SchemaHelpBubble("username.help"));
        localRepoFields.add(new SchemaHelpBubble("password.help"));
        localRepoFields.add(new SchemaHelpBubble("socketTimeoutMillis.help"));
        localRepoFields.add(new SchemaHelpBubble("localAddress.help"));
        localRepoFields.add(new SchemaHelpBubble("proxy.help"));
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

    public void handleCreate(CentralConfigDescriptor descriptor) {
        HttpRepoDescriptor remoteRepo = getRepoDescriptor();
        getEditingDescriptor().addRemoteRepository(remoteRepo);
    }

}