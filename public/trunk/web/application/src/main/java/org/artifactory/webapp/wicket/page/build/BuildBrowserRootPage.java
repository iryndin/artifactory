package org.artifactory.webapp.wicket.page.build;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.protocol.http.servlet.AbortWithWebErrorCodeException;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.apache.wicket.util.string.StringValueConversionException;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.api.search.SearchService;
import org.artifactory.build.api.Build;
import org.artifactory.build.api.Module;
import org.artifactory.webapp.wicket.page.base.AuthenticatedPage;
import org.wicketstuff.annotation.mount.MountPath;
import org.wicketstuff.annotation.strategy.MountMixedParam;

import java.util.List;

import static org.artifactory.webapp.wicket.page.build.BuildBrowserConstants.*;

/**
 * The root page of the build browser
 *
 * @author Noam Y. Tenne
 */
@MountPath(path = BUILDS)
@MountMixedParam(parameterNames = {BUILD_NAME, BUILD_NUMBER, MODULE_ID})
public class BuildBrowserRootPage extends AuthenticatedPage {
    public static final String CHILD_PANEL_ID = "panel";

    @SpringBean
    private SearchService searchService;

    private PageParameters pageParameters;

    /**
     * Main constructor. Displays content according to the given page parameters
     *
     * @param pageParameters Build specification parameters
     */
    public BuildBrowserRootPage(PageParameters pageParameters) {
        setOutputMarkupId(true);
        this.pageParameters = pageParameters;
        Panel panelToAdd;

        if (pageParameters.containsKey(MODULE_ID)) {
            panelToAdd = getModuleSpecificTabbedPanel();
        } else if (pageParameters.containsKey(BUILD_NUMBER)) {
            panelToAdd = getTabbedPanel();
        } else if (pageParameters.containsKey(BUILD_NAME)) {
            panelToAdd = getBuildForNamePanel();
        } else {
            panelToAdd = new AllBuildsPanel(CHILD_PANEL_ID);
        }

        add(panelToAdd);
        add(new BuildBreadCrumbsPanel(pageParameters));
    }

    /**
     * Returns the module-specific tabbed panel to display
     *
     * @return Module specific tabbed panel
     */
    private Panel getModuleSpecificTabbedPanel() {
        String buildName = getBuildName();
        long buildNumber = getBuildNumber();
        String moduleId = getModuleId();

        Build build = getBuild(buildName, buildNumber);
        Module module = getModule(build, moduleId);
        return new BuildTabbedPanel(CHILD_PANEL_ID, build, module);
    }

    /**
     * Returns the general info tabbed panel to display
     *
     * @return General info tabbed panel
     */
    private Panel getTabbedPanel() {
        String buildName = getBuildName();
        long buildNumber = getBuildNumber();

        Build build = getBuild(buildName, buildNumber);
        return new BuildTabbedPanel(CHILD_PANEL_ID, build, null);
    }

    /**
     * Returns the builds-for-name panel to display
     *
     * @return Builds-for-name panel
     */
    private Panel getBuildForNamePanel() {
        Panel panelToAdd = null;
        String buildName = getBuildName();
        try {
            List<Build> buildsToDisplay = searchService.searchBuildsByName(buildName);

            if (buildsToDisplay == null || buildsToDisplay.isEmpty()) {
                String errorMessage = new StringBuilder().append("Could not find builds by name '").append(buildName).
                        append("'").toString();
                throwNotFoundError(errorMessage);
            }

            panelToAdd = new BuildsForNamePanel(CHILD_PANEL_ID, buildName, buildsToDisplay);
        } catch (RepositoryRuntimeException e) {
            String errorMessage = new StringBuilder().append("Error locating builds by '").append(buildName).
                    append("': ").append(e.getMessage()).toString();
            throwInternalError(errorMessage);
        }

        return panelToAdd;
    }

    @Override
    public String getPageName() {
        return "Build Browser";
    }

    /**
     * Returns the build name page parameter
     *
     * @return Build name
     */
    private String getBuildName() {
        return getStringParameter(BUILD_NAME);
    }

    /**
     * Returns the build number page parameter
     *
     * @return Build number
     */
    private long getBuildNumber() {
        return getLongParameter(BUILD_NUMBER);
    }

    /**
     * Returns the module ID page parameter
     *
     * @return Module ID
     */
    private String getModuleId() {
        return getStringParameter(MODULE_ID);
    }

    /**
     * Returns the string value for the given key
     *
     * @param key Key of parameter to find
     * @return String value
     */
    private String getStringParameter(String key) {
        validateKey(key);

        String value = pageParameters.getString(key);

        if (StringUtils.isBlank(value)) {
            String errorMessage = new StringBuilder().append("Blank value found for parameter '").append(key).
                    append("'").toString();
            throwNotFoundError(errorMessage);
        }

        return value;
    }

    /**
     * Returns the long value for the given key
     *
     * @param key Key of parameter to find
     * @return Long value
     */
    private long getLongParameter(String key) {
        validateKey(key);

        try {
            return pageParameters.getLong(key);
        } catch (StringValueConversionException e) {
            throwNotFoundError("Invalid value for build number parameter: " + e.getMessage());
        }

        //Shouldn't get here
        return 0;
    }

    /**
     * Validates that the given key exists as a parameter key
     *
     * @param key Key to validate
     * @throws AbortWithWebErrorCodeException If the key was not found
     */
    private void validateKey(String key) {
        if (!pageParameters.containsKey(key)) {
            String errorMessage = new StringBuilder().append("Could not find parameter '").append(key).append("'").
                    toString();
            throwNotFoundError(errorMessage);
        }
    }

    /**
     * Returns the latest built build object for the given name and number
     *
     * @param buildName   Name of build to locate
     * @param buildNumber Number of build to locate
     * @return Build object if found.
     * @throws AbortWithWebErrorCodeException If the build was not found
     */
    private Build getBuild(String buildName, long buildNumber) {
        try {
            Build build = searchService.getLatestBuildByNameAndNumber(buildName, buildNumber);
            if (build == null) {
                String errorMessage =
                        new StringBuilder().append("Could not find build '").append(buildName).append("' #")
                                .append(buildNumber).toString();
                throwNotFoundError(errorMessage);
            }
            return build;
        } catch (RepositoryRuntimeException e) {
            String errorMessage = new StringBuilder().append("Error locating latest build for '").append(buildName).
                    append("' #").append(buildNumber).append(": ").append(e.getMessage()).toString();
            throwInternalError(errorMessage);
        }

        //Should not happen
        return null;
    }

    /**
     * Returns the module object of the given ID
     *
     * @param build    Build to search within
     * @param moduleId Module ID to locate
     * @return Module object if found.
     * @throws AbortWithWebErrorCodeException If the module was not found
     */
    private Module getModule(Build build, String moduleId) {
        Module module = build.getModule(moduleId);

        if (module == null) {
            String errorMessage = new StringBuilder().append("Could not find module '").append(moduleId).
                    append("' within build '").append(build.getName()).append("' #").append(build.getNumber()).
                    toString();
            throwNotFoundError(errorMessage);
        }

        return module;
    }

    /**
     * Throws a 404 AbortWithWebErrorCodeException with the given message
     *
     * @param errorMessage Message to display in the error
     */
    private void throwNotFoundError(String errorMessage) throws AbortWithWebErrorCodeException {
        throwError(HttpStatus.SC_NOT_FOUND, errorMessage);
    }

    /**
     * Throws a 500 AbortWithWebErrorCodeException with the given message
     *
     * @param errorMessage Message to display in the error
     */
    private void throwInternalError(String errorMessage) throws AbortWithWebErrorCodeException {
        throwError(HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage);
    }

    /**
     * Throws an AbortWithWebErrorCodeException with the given status and message
     *
     * @param status       Status to set for error
     * @param errorMessage Message to display in the error
     */
    private void throwError(int status, String errorMessage) {
        throw new AbortWithWebErrorCodeException(status, errorMessage);
    }
}