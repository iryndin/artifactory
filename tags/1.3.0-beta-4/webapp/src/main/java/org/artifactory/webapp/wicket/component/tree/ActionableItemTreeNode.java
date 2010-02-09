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

package org.artifactory.webapp.wicket.component.tree;

import org.apache.log4j.Logger;
import org.artifactory.webapp.actionable.model.ActionableItem;
import org.artifactory.webapp.actionable.model.HierarchicActionableItem;

import javax.swing.tree.DefaultMutableTreeNode;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public class ActionableItemTreeNode<T extends ActionableItem> extends DefaultMutableTreeNode {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ActionableItemTreeNode.class);

    private boolean leaf;

    public ActionableItemTreeNode(Object userObject) {
        super(userObject);
        typeCheck(userObject);
        leaf = ((ActionableItem) userObject) instanceof HierarchicActionableItem;
        setAllowsChildren(leaf);
    }

    @Override
    public ActionableItemTreeNode getParent() {
        return (ActionableItemTreeNode) super.getParent();
    }

    @SuppressWarnings({"unchecked"})
    @Override
    public T getUserObject() {
        return (T) super.getUserObject();
    }

    @Override
    public void setUserObject(Object userObject) {
        try {
            typeCheck(userObject);
            super.setUserObject(userObject);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(
                    "Using a non ActionableItem user object is not allowed with " +
                            "ActionableItemTreeNode.", e);
        }
    }

    @Override
    public boolean isLeaf() {
        return leaf;
    }

    void setLeaf(boolean leaf) {
        this.leaf = leaf;
    }

    @SuppressWarnings({"UnusedDeclaration", "unchecked"})
    private void typeCheck(Object o) {
        try {
            T t = (T) o;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(
                    "Using a non ActionableItem user object is not allowed with " +
                            "ActionableItemTreeNode.", e);
        }
    }
}
