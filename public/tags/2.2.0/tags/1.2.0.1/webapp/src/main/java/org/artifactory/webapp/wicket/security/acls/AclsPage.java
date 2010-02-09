package org.artifactory.webapp.wicket.security.acls;

import org.apache.log4j.Logger;
import org.artifactory.webapp.wicket.ArtifactoryPage;
import wicket.authorization.strategies.role.annotations.AuthorizeInstantiation;
import wicket.markup.html.WebMarkupContainer;

@AuthorizeInstantiation("ADMIN")
public class AclsPage extends ArtifactoryPage {

    @SuppressWarnings({"UnusedDeclaration", "UNUSED_SYMBOL"})
    private final static Logger LOGGER = Logger.getLogger(AclsPage.class);

    public AclsPage() {
        final GroupsManagementPanel groupsManagementPanel = new GroupsManagementPanel("groupsManagementPanel");
        add(groupsManagementPanel);
        final NewGroupPanel newGroupPanel = new NewGroupPanel("newGroupPanel", groupsManagementPanel.get("groups"), (WebMarkupContainer) (groupsManagementPanel.get("panel:recipients")));
        add(newGroupPanel);
    }

    protected String getPageName() {
        return "Users Management";
    }


}