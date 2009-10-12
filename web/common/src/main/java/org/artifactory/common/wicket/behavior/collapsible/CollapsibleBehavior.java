/*
 * This file is part of Artifactory.
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

package org.artifactory.common.wicket.behavior.collapsible;

import org.artifactory.common.wicket.behavior.template.TemplateBehavior;

/**
 * @author Yoav Aharoni
 */
public class CollapsibleBehavior extends TemplateBehavior {
    private boolean expanded;
    private boolean resize;

    public CollapsibleBehavior() {
        this(false, false);
    }

    public CollapsibleBehavior(boolean expanded, boolean resize) {
        this.expanded = expanded;
        this.resize = resize;
        getResourcePackage().addJavaScript();
    }


    public boolean isExpanded() {
        return expanded;
    }

    public void setExpanded(boolean expanded) {
        this.expanded = expanded;
    }

    public String getCssClass() {
        return expanded ? "expanded" : "collapsed";
    }

    public boolean isResize() {
        return resize;
    }
}
