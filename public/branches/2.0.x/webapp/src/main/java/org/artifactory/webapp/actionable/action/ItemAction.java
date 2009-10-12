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

package org.artifactory.webapp.actionable.action;

import org.artifactory.webapp.actionable.ActionableItem;
import org.artifactory.webapp.actionable.event.ItemEvent;

import javax.swing.*;
import java.awt.event.ActionEvent;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public abstract class ItemAction extends AbstractAction {
    private final String name;
    private final CharSequence confirmationMessage;

    public ItemAction(String name, CharSequence confirmationMessage) {
        super(name);
        this.name = name;
        this.confirmationMessage = confirmationMessage;
    }

    public String getName() {
        return name;
    }

    public String getCssClass() {
        return getClass().getSimpleName();
    }

    public CharSequence getConfirmationMessage() {
        return confirmationMessage;
    }

    public void actionPerformed(ActionEvent e) {
        ItemEvent event = (ItemEvent) e;
        onAction(event);
        //Fire the event to listeners
        ActionableItem actionableItem = event.getSource();
        actionableItem.fireActionEvent(event);
    }

    public abstract void onAction(ItemEvent e);

    public CharSequence getConfirmationCallbackScript(ActionableItem actionableItem,
                                                      CharSequence orig) {
        if (confirmationMessage != null) {
            return "if (confirm('" + confirmationMessage + " " +
                    actionableItem.getDisplayName() + "?')) {" +
                    orig + "} else { return false; }";
        }
        return orig;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof ItemAction)) {
            return false;
        }
        ItemAction action = (ItemAction) o;
        return name.equals(action.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public String toString() {
        return name;
    }
}
