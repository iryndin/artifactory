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

package org.artifactory.webapp.wicket.page.home.settings;

import org.artifactory.common.wicket.component.label.highlighter.Syntax;
import org.artifactory.common.wicket.component.label.highlighter.SyntaxHighlighter;
import org.artifactory.common.wicket.component.modal.panel.bordered.CodeModalPanel;

/**
 * A custom modal display panel for the generated settings. Created for the need of customizing the modal window
 * content panel, so an export link could be added
 *
 * @author Noam Y. Tenne
 */
public class SettingsModalPanel extends CodeModalPanel {
    public SettingsModalPanel(final String settings, Syntax syntax) {
        super(new SyntaxHighlighter("content", settings, syntax));
        setWidth(700);
    }

    @Override
    public String getCookieName() {
        return null;
    }
}