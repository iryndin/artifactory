package org.artifactory.webapp.wicket.page.build;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * Build browser path and variable constants
 *
 * @author Noam Y. Tenne
 */
public interface BuildBrowserConstants {
    String BUILDS = "builds";
    String BUILD_NAME = "buildName";
    String BUILD_NUMBER = "buildNumber";
    String MODULE_ID = "moduleName";
    List<String> PATH_CONSTANTS = Lists.newArrayList(BUILD_NAME, BUILD_NUMBER, MODULE_ID);
}