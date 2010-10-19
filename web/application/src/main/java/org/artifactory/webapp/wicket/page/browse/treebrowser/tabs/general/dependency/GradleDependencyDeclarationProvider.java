/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
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

package org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.general.dependency;

import org.apache.commons.lang.StringUtils;
import org.artifactory.api.maven.MavenArtifactInfo;
import org.artifactory.common.wicket.component.label.highlighter.Syntax;

/**
 * The Gradle dependency declaration generator
 *
 * @author Noam Y. Tenne
 */
public class GradleDependencyDeclarationProvider implements DependencyDeclarationProvider {

    public Syntax getSyntaxType() {
        return Syntax.groovy;
    }

    public String getDependencyDeclaration(MavenArtifactInfo artifactInfo) {
        StringBuilder sb = new StringBuilder("compile(group: '").append(artifactInfo.getGroupId()).append("', name: '").
                append(artifactInfo.getArtifactId()).append("', version: '").append(artifactInfo.getVersion()).
                append("'");

        String classifier = artifactInfo.getClassifier();
        if (StringUtils.isNotBlank(classifier)) {
            sb.append(", classifier: '").append(classifier).append("'");
        }

        String type = artifactInfo.getType();
        if (StringUtils.isNotBlank(type) && !"jar".equalsIgnoreCase(type)) {
            sb.append(", ext: '").append(type).append("'");
        }
        return sb.append(")").toString();
    }
}
