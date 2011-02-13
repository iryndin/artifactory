/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2011 JFrog Ltd.
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

package org.artifactory.webapp.wicket.page.build.page;

import org.apache.commons.httpclient.HttpStatus;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.PageParameters;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.protocol.http.servlet.AbortWithWebErrorCodeException;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.build.BasicBuildInfo;
import org.artifactory.api.build.BuildService;
import org.artifactory.api.repo.exception.RepositoryRuntimeException;
import org.artifactory.log.LoggerFactory;
import org.artifactory.webapp.wicket.page.base.AuthenticatedPage;
import org.artifactory.webapp.wicket.page.browse.home.RememberPageBehavior;
import org.artifactory.webapp.wicket.page.build.panel.AllBuildsPanel;
import org.artifactory.webapp.wicket.page.build.panel.BuildBreadCrumbsPanel;
import org.artifactory.webapp.wicket.page.build.panel.BuildTabbedPanel;
import org.artifactory.webapp.wicket.page.build.panel.BuildsForNamePanel;
import org.jfrog.build.api.Build;
import org.jfrog.build.api.Module;
import org.slf4j.Logger;
import org.wicketstuff.annotation.mount.MountPath;
import org.wicketstuff.annotation.strategy.MountMixedParam;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Set;

import static org.artifactory.webapp.wicket.page.build.BuildBrowserConstants.*;

/**
 * The root page of the build browser
 *
 * @author Noam Y. Tenne
 */
@MountPath(path = BUILDS)
@MountMixedParam(parameterNames = {BUILD_NAME, BUILD_NUMBER, BUILD_STARTED, MODULE_ID})
public class BuildBrowserRootPage extends AuthenticatedPage {

    private static final Logger log = LoggerFactory.getLogger(BuildBrowserRootPage.class);

    public static final String CHILD_PANEL_ID = "panel";

    @SpringBean
    private BuildService buildService;
    private PageParameters pageParameters;

    /**
     * Main constructor. Displays content according to the given page parameters
     *
     * @param pageParameters Page parameters include with request
     */
    public BuildBrowserRootPage(PageParameters pageParameters) {
        add(new RememberPageBehavior());

        this.pageParameters = pageParameters;
        setOutputMarkupId(true);
        Panel panelToAdd;
        if (pageParameters.containsKey(MODULE_ID)) {
            panelToAdd = getModuleSpecificTabbedPanel(null);
        } else if (pageParameters.containsKey(BUILD_STARTED) || pageParameters.containsKey(BUILD_NUMBER)) {
            /**
             * If the URL was sent from Artifactory, it will include the build started param; but if it was sent by a
             * user, it could contain only the build number
             */
            panelToAdd = getTabbedPanel();
        } else if (pageParameters.containsKey(BUILD_NAME)) {
            panelToAdd = getBuildForNamePanel();
        } else {
            panelToAdd = new AllBuildsPanel(CHILD_PANEL_ID);
        }

        add(panelToAdd);
        BuildBreadCrumbsPanel breadCrumbsPanel = new BuildBreadCrumbsPanel();
        add(breadCrumbsPanel);
        breadCrumbsPanel.addCrumbs(pageParameters);
    }


    /**
     * Returns the module-specific tabbed panel to display
     *
     * @param forcedModule ID module to display instead of a one that might be specified in the parameters
     * @return Module specific tabbed panel
     */
    private Panel getModuleSpecificTabbedPanel(String forcedModule) {
        String buildName = getBuildName();
        String buildNumber = getBuildNumber();
        String buildStarted = null;
        String moduleId;

        /**
         * If the forced module is specified, it means that the user entered a request with a module id, but no build
         * started parameter
         */
        if (StringUtils.isNotBlank(forcedModule)) {
            moduleId = forcedModule;
        } else {
            //Normal request from artifactory, containing all needed parameters
            buildStarted = getStringParameter(BUILD_STARTED);
            moduleId = getModuleId();
        }
        Build build = getBuild(buildName, buildNumber, buildStarted);
        Module module = getModule(build, moduleId);
        pageParameters.put(BUILD_STARTED, buildStarted);
        pageParameters.put(MODULE_ID, moduleId);
        return new BuildTabbedPanel(CHILD_PANEL_ID, build, module);
    }

    /**
     * Returns the general info tabbed panel to display
     *
     * @return General info tabbed panel
     */
    private Panel getTabbedPanel() {
        String buildName = getBuildName();
        String buildNumber = getBuildNumber();

        String buildStarted = null;
        /**
         * If the build started wasn't specified, the URL contains only a build number, which means we select to display
         * the latest build of the specified number
         */
        if (!pageParameters.containsKey(BUILD_STARTED)) {
            Build build = getBuild(buildName, buildNumber, buildStarted);
            pageParameters.put(BUILD_STARTED, build.getStarted());
            return new BuildTabbedPanel(CHILD_PANEL_ID, build, null);
        }

        buildStarted = getStringParameter(BUILD_STARTED);
        try {
            new SimpleDateFormat(Build.STARTED_FORMAT).parse(buildStarted);
            Build build = getBuild(buildName, buildNumber, buildStarted);
            return new BuildTabbedPanel(CHILD_PANEL_ID, build, null);
        } catch (ParseException e) {
            /**
             * If the build started param was specified, but didn't parse properly, then the request contains a build
             * number And build module id, which means we select to display the specified module of the latest build of
             * the specified number
             */
            return getModuleSpecificTabbedPanel(buildStarted);
        }
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
            Set<BasicBuildInfo> buildsByName = buildService.searchBuildsByName(buildName);

            if (buildsByName == null || buildsByName.isEmpty()) {
                String errorMessage = new StringBuilder().append("Could not find builds by name '").append(buildName).
                        append("'").toString();
                throwNotFoundError(errorMessage);
            }

            panelToAdd = new BuildsForNamePanel(CHILD_PANEL_ID, buildName, buildsByName);
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
     * Returns the module ID page parameter
     *
     * @return Module ID
     */
    private String getModuleId() {
        return getStringParameter(MODULE_ID);
    }

    /**
     * Returns the latest built build object for the given name and number
     *
     * @param buildName    Name of build to locate
     * @param buildNumber  Number of build to locate
     * @param buildStarted Started time of build to locate
     * @return Build object if found.
     * @throws AbortWithWebErrorCodeException If the build was not found
     */
    private Build getBuild(String buildName, String buildNumber, String buildStarted) {
        boolean buildStartedSupplied = StringUtils.isNotBlank(buildStarted);
        try {
            Build build;
            if (buildStartedSupplied) {
                build = buildService.getBuild(buildName, buildNumber, buildStarted);
            } else {
                //Take the latest build of the specified number
                build = buildService.getLatestBuildByNameAndNumber(buildName, buildNumber);
            }
            if (build == null) {
                StringBuilder builder = new StringBuilder().append("Could not find build '").append(buildName).
                        append("' #").append(buildNumber);
                if (buildStartedSupplied) {
                    builder.append(" that started at ").append(buildStarted);
                }
                throwNotFoundError(builder.toString());
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
     * Returns the build name page parameter
     *
     * @return Build name
     */
    protected String getBuildName() {
        return getStringParameter(BUILD_NAME);
    }

    /**
     * Returns the build number page parameter
     *
     * @return Build number
     */
    protected String getBuildNumber() {
        return getStringParameter(BUILD_NUMBER);
    }

    /**
     * Validates that the given key exists as a parameter key
     *
     * @param key Key to validate
     * @throws org.apache.wicket.protocol.http.servlet.AbortWithWebErrorCodeException
     *          If the key was not found
     */
    protected void validateKey(String key) {
        if (!pageParameters.containsKey(key)) {
            String errorMessage = new StringBuilder().append("Could not find parameter '").append(key).append("'").
                    toString();
            throwNotFoundError(errorMessage);
        }
    }

    /**
     * Returns the string value for the given key
     *
     * @param key Key of parameter to find
     * @return String value
     */
    protected String getStringParameter(String key) {
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
     * Throws a 404 AbortWithWebErrorCodeException with the given message
     *
     * @param errorMessage Message to display in the error
     */
    protected void throwNotFoundError(String errorMessage) throws AbortWithWebErrorCodeException {
        throwError(HttpStatus.SC_NOT_FOUND, errorMessage);
    }

    /**
     * Throws a 500 AbortWithWebErrorCodeException with the given message
     *
     * @param errorMessage Message to display in the error
     */
    protected void throwInternalError(String errorMessage) throws AbortWithWebErrorCodeException {
        throwError(HttpStatus.SC_INTERNAL_SERVER_ERROR, errorMessage);
    }

    /**
     * Throws an AbortWithWebErrorCodeException with the given status and message
     *
     * @param status       Status to set for error
     * @param errorMessage Message to display in the error
     */
    private void throwError(int status, String errorMessage) {
        log.error("An error occurred during the browsing of build info: {}", errorMessage);
        throw new AbortWithWebErrorCodeException(status, errorMessage);
    }
}