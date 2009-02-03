/**
 *  Artifactory by jfrog [http://artifactory.jfrog.org]
 *  Copyright (C) 2000-2008 jfrog Ltd.
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Affero General Public License as
 *  published by the Free Software Foundation, either version 3 of the
 *  License, or (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 *  GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/> or write to
 *  the Free Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston,
 *  MA 02110-1301 USA.
 *
 *  You can also contact jfrog Ltd. at info@jfrog.org.
 *
 *  The interactive user interfaces in modified source and object code versions
 *  of this program must display Appropriate Legal Notices, as required under
 *  Section 5 of the GNU Affero General Public License version 3.
 *
 *  In accordance with Section 7(b) of the GNU Affero General Public License
 *  version 3, these Appropriate Legal Notices must retain the display of the
 *  "Powered by Artifactory" logo. If the display of the logo is not reasonably
 *  feasible for technical reasons, the Appropriate Legal Notices must display
 *  the words "Powered by Artifactory".
 */

package org.artifactory.webapp.actionable;

import org.apache.log4j.Logger;
import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.artifactory.webapp.actionable.model.ActionDescriptor;
import org.artifactory.webapp.actionable.model.ActionableItem;

import java.awt.event.ActionEvent;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public class ItemActionEvent extends ActionEvent {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ItemActionEvent.class);

    private final ActionDescriptor descriptor;
    private final List<Component> targetComponents;
    private final AjaxRequestTarget target;

    public ItemActionEvent(ActionableItem source, ActionDescriptor descriptor,
                           List<Component> targetComponents,
                           AjaxRequestTarget target) {
        super(source, 0, descriptor.getName());
        this.descriptor = descriptor;
        this.targetComponents = targetComponents;
        this.target = target;
    }

    @Override
    public String getActionCommand() {
        return descriptor.getName();
    }

    @Override
    public ActionableItem getSource() {
        return (ActionableItem) super.getSource();
    }

    public AjaxRequestTarget getTarget() {
        return target;
    }

    public List<Component> getTargetComponents() {
        return targetComponents;
    }

    public ActionDescriptor getDescriptor() {
        return descriptor;
    }
}
