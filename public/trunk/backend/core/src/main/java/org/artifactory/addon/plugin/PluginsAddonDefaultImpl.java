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

package org.artifactory.addon.plugin;

import org.artifactory.sapi.common.ExportSettings;
import org.artifactory.sapi.common.ImportSettings;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class PluginsAddonDefaultImpl implements PluginsAddon {

    public <C> void execPluginActions(Class<? extends PluginAction> type, C context, Object... args) {
        //Nothing to do in the default impl
    }

    public ResponseCtx execute(String executionName, Map params, boolean async) {
        throw new UnsupportedOperationException("Executing plugin actions requires Artifactory Pro.");
    }

    public boolean isDefault() {
        return true;
    }

    public void exportTo(ExportSettings settings) {

    }

    public void importFrom(ImportSettings settings) {

    }
}