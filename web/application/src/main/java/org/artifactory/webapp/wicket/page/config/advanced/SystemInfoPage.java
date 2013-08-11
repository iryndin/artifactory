/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2012 JFrog Ltd.
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

package org.artifactory.webapp.wicket.page.config.advanced;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.wicket.authroles.authorization.strategies.role.annotations.AuthorizeInstantiation;
import org.artifactory.api.context.ContextHelper;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.common.ConstantValues;
import org.artifactory.common.wicket.component.border.titled.TitledBorder;
import org.artifactory.common.wicket.component.label.highlighter.Syntax;
import org.artifactory.common.wicket.component.label.highlighter.SyntaxHighlighter;
import org.artifactory.info.InfoWriter;
import org.artifactory.storage.StorageProperties;
import org.artifactory.util.Strings;
import org.artifactory.webapp.wicket.page.base.AuthenticatedPage;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.RuntimeMXBean;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;

/**
 * Displays a list of all the system properties and their values as well as memory information and JVM arguments
 *
 * @author Noam Y. Tenne
 */
@AuthorizeInstantiation(AuthorizationService.ROLE_ADMIN)
public class SystemInfoPage extends AuthenticatedPage {

    private static String listFormat = "%1$-50s| %2$s%n";

    /**
     * Default Constructor
     */
    public SystemInfoPage() {
        TitledBorder border = new TitledBorder("border");
        add(border);

        SyntaxHighlighter infoPanel = new SyntaxHighlighter("sysInfo", collectSystemInfo(), Syntax.plain);
        infoPanel.setEscapeModelStrings(true);
        border.add(infoPanel);
    }

    /**
     * Return a formatted string of the system info to display
     *
     * @return
     */
    private String collectSystemInfo() {
        StringBuilder infoBuilder = new StringBuilder();

        StorageProperties storageProperties = ContextHelper.get().beanForType(StorageProperties.class);
        infoBuilder.append("Storage Info:").append("\n");
        addInfo(infoBuilder, "Database Type", storageProperties.getDbType().toString());
        addInfo(infoBuilder, "Storage Type", storageProperties.getBinariesStorageType().toString());

        infoBuilder.append("\n").append("System Properties:").append("\n");
        Properties properties = System.getProperties();
        //// add Artifactory version to the properties, will be alphabetically sorted later.
        properties.setProperty(ConstantValues.artifactoryVersion.getPropertyName(),
                ConstantValues.artifactoryVersion.getString());
        TreeSet sortedSystemPropKeys = new TreeSet<>(properties.keySet());
        for (Object key : sortedSystemPropKeys) {
            addInfo(infoBuilder, String.valueOf(key), String.valueOf(properties.get(key)));
        }
        infoBuilder.append("\n").append("General JVM Info:").append("\n");
        OperatingSystemMXBean systemBean = ManagementFactory.getOperatingSystemMXBean();
        addInfo(infoBuilder, "Available Processors", Integer.toString(systemBean.getAvailableProcessors()));

        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        MemoryUsage heapMemoryUsage = memoryBean.getHeapMemoryUsage();

        addInfo(infoBuilder, "Heap Memory Usage-Committed", Long.toString(heapMemoryUsage.getCommitted()));
        addInfo(infoBuilder, "Heap Memory Usage-Init", Long.toString(heapMemoryUsage.getInit()));
        addInfo(infoBuilder, "Heap Memory Usage-Max", Long.toString(heapMemoryUsage.getMax()));
        addInfo(infoBuilder, "Heap Memory Usage-Used", Long.toString(heapMemoryUsage.getUsed()));

        MemoryUsage nonHeapMemoryUsage = memoryBean.getNonHeapMemoryUsage();
        addInfo(infoBuilder, "Non-Heap Memory Usage-Committed", Long.toString(nonHeapMemoryUsage.getCommitted()));
        addInfo(infoBuilder, "Non-Heap Memory Usage-Init", Long.toString(nonHeapMemoryUsage.getInit()));
        addInfo(infoBuilder, "Non-Heap Memory Usage-Max", Long.toString(nonHeapMemoryUsage.getMax()));
        addInfo(infoBuilder, "Non-Heap Memory Usage-Used", Long.toString(nonHeapMemoryUsage.getUsed()));

        RuntimeMXBean RuntimemxBean = ManagementFactory.getRuntimeMXBean();
        StringBuilder vmArgumentBuilder = new StringBuilder();
        List<String> vmArguments = RuntimemxBean.getInputArguments();
        if (vmArguments != null) {
            for (String vmArgument : vmArguments) {
                if (InfoWriter.shouldMaskValue(vmArgument)) {
                    vmArgument = Strings.maskKeyValue(vmArgument);
                }
                vmArgumentBuilder.append(vmArgument);
                if (vmArguments.indexOf(vmArgument) != (vmArguments.size() - 1)) {
                    vmArgumentBuilder.append("\n");
                }
            }
        }

        infoBuilder.append("\nJVM Arguments:\n").append(vmArgumentBuilder.toString());

        return StringUtils.removeEnd(infoBuilder.toString(), "\n");
    }

    /**
     * Append a property key and value to the info builder
     *
     * @param infoBuilder   Target builder
     * @param propertyKey   Key of property to display
     * @param propertyValue Value of property to display
     */
    private void addInfo(StringBuilder infoBuilder, String propertyKey, String propertyValue) {
        if (InfoWriter.shouldMaskValue(propertyKey)) {
            propertyValue = Strings.mask(propertyValue);
        } else {
            propertyValue = StringEscapeUtils.escapeJava(propertyValue);
        }
        infoBuilder.append(String.format(listFormat, propertyKey, propertyValue));
    }

    @Override
    public String getPageName() {
        return "System Info";
    }
}
