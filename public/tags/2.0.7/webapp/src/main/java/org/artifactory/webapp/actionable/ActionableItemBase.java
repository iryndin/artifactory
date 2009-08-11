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

import org.apache.wicket.model.AbstractReadOnlyModel;
import org.artifactory.webapp.actionable.action.ItemAction;
import org.artifactory.webapp.actionable.action.ItemActionListener;
import org.artifactory.webapp.actionable.event.ItemEvent;
import org.artifactory.webapp.actionable.event.ItemEventTargetComponents;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public abstract class ActionableItemBase extends AbstractReadOnlyModel implements ActionableItem {

    private LinkedHashSet<ItemAction> actions = new LinkedHashSet<ItemAction>();
    private final List<ItemActionListener> listeners;
    private ItemEventTargetComponents eventTargetComponents;

    public ActionableItemBase() {
        this.listeners = Collections.synchronizedList(new ArrayList<ItemActionListener>());
    }

    public Set<ItemAction> getActions() {
        return actions;
    }

    public Set<ItemAction> getContextMenuActions() {
        return actions;
    }

    public List<ItemActionListener> getListeners() {
        return listeners;
    }

    public void addActionListener(ItemActionListener listener) {
        if (listener == null) {
            return;
        }
        listeners.add(listener);
    }

    public void removeActionListener(ItemActionListener listener) {
        if (listener == null) {
            return;
        }
        listeners.remove(listener);
    }

    public void fireActionEvent(ItemEvent e) {
        //Fire the event to listeners
        synchronized (listeners) {
            for (ItemActionListener listener : listeners) {
                listener.actionPerformed(e);
            }
        }
    }

    public ItemEventTargetComponents getEventTargetComponents() {
        return eventTargetComponents;
    }

    public void setEventTargetComponents(ItemEventTargetComponents eventTargetComponents) {
        this.eventTargetComponents = eventTargetComponents;
    }

    @Override
    public ActionableItem getObject() {
        return this;
    }

    @Override
    public String toString() {
        return getDisplayName();
    }
}
