package org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.build.actionable;

import org.apache.wicket.markup.html.panel.Panel;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.build.api.Build;
import org.artifactory.build.api.BuildService;
import org.artifactory.common.wicket.component.modal.ModalHandler;
import org.artifactory.webapp.actionable.ActionableItemBase;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.build.action.GoToBuildAction;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.build.action.ShowInCiServerAction;
import org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.build.action.ViewBuildXmlAction;
import org.artifactory.webapp.wicket.util.ItemCssClass;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Artifact associated build actionable item
 *
 * @author Noam Y. Tenne
 */
public class BuildTabActionableItem extends ActionableItemBase {

    private Build build;
    private String moduleId;

    /**
     * Main constructor
     *
     * @param textContentViewer Modal handler for displaying the build XML
     * @param build             Build object to handle
     * @param moduleId          ID of module association to specifiy
     */
    public BuildTabActionableItem(ModalHandler textContentViewer, Build build, String moduleId) {
        this.build = build;
        this.moduleId = moduleId;

        getActions().add(new GoToBuildAction(build.getName(), build.getNumber()));

        BuildService buildService = ContextHelper.get().beanForType(BuildService.class);
        String buildXml = buildService.getXmlFromBuild(build);
        getActions().add(new ViewBuildXmlAction(textContentViewer, buildXml));

        getActions().add(new ShowInCiServerAction(build.getUrl()));
    }

    public Panel newItemDetailsPanel(String id) {
        return null;
    }

    public String getDisplayName() {
        return build.getName();
    }

    public String getCssClass() {
        return ItemCssClass.doc.getCssClass();
    }

    public void filterActions(AuthorizationService authService) {
    }

    /**
     * Returns the build
     *
     * @return Build
     */
    public Build getBuild() {
        return build;
    }

    /**
     * Returns the ID of the module to specify association with
     *
     * @return Module ID association to specify
     */
    public String getModuleId() {
        return moduleId;
    }

    /**
     * Returns the date that the build has started at
     *
     * @return Build started date
     * @throws ParseException Any exceptions that might occur while parsing the date
     */
    public Date getBuildStartedDate() throws ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat(Build.STARTED_FORMAT);
        return dateFormat.parse(build.getStarted());
    }
}