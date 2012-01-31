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

package org.artifactory.addon.plugin;

import java.io.Serializable;
import java.util.Map;

/**
 * Basic user plugin info
 *
 * @author Noam Y. Tenne
 */
public class PluginInfo implements Serializable {

    private String name;
    private String version = "undefined";
    private String description;

    /**
     * @param name                Name of plugin (applicable to executions and jobs)
     * @param pluginClosureParams Parameters given to the closure constructor
     */
    public PluginInfo(String name, Map pluginClosureParams) {
        this.name = name;
        if ((pluginClosureParams != null) && !pluginClosureParams.isEmpty()) {
            if (pluginClosureParams.containsKey("version")) {
                version = pluginClosureParams.get("version").toString();
            }
            if (pluginClosureParams.containsKey("description")) {
                description = pluginClosureParams.get("description").toString();
            }
        }
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public String getDescription() {
        return description;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof PluginInfo)) {
            return false;
        }

        PluginInfo that = (PluginInfo) o;

        if (description != null ? !description.equals(that.description) : that.description != null) {
            return false;
        }
        if (name != null ? !name.equals(that.name) : that.name != null) {
            return false;
        }
        if (version != null ? !version.equals(that.version) : that.version != null) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = name != null ? name.hashCode() : 0;
        result = 31 * result + (version != null ? version.hashCode() : 0);
        result = 31 * result + (description != null ? description.hashCode() : 0);
        return result;
    }
}
