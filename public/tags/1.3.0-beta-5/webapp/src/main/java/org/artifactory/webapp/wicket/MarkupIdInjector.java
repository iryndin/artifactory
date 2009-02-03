/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.webapp.wicket;

import org.apache.wicket.Component;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.Page;
import org.apache.wicket.RequestCycle;
import org.apache.wicket.application.IComponentOnBeforeRenderListener;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * User: freds Date: Aug 4, 2008 Time: 7:37:29 PM
 */
public class MarkupIdInjector implements IComponentOnBeforeRenderListener {
    private static final String MARKUP_ID_PREFIX = "_sel_";

    public void setInAjaxRequest() {
        ArtifactoryRequestCycle cycle = (ArtifactoryRequestCycle) RequestCycle.get();
        cycle.setAjaxData(new AjaxCycleData());
    }

    public static class AjaxCycleData {
        private transient boolean cleanedComponentsChanged = false;
        private transient Set<MarkupContainer> componentsChanged = null;

        public void endRequest() {
            componentsChanged = null;
            cleanedComponentsChanged = false;
        }

        private void cleanComponentsChanged() {
            if (!cleanedComponentsChanged) {
                // First remove all component that have a parent
                Iterator<MarkupContainer> componentIterator = componentsChanged.iterator();
                while (componentIterator.hasNext()) {
                    MarkupContainer changed = componentIterator.next();
                    if (changed.getParent() != null) {
                        componentIterator.remove();
                    }
                }
                cleanedComponentsChanged = true;
            }
        }

        public void componentChanged(Component component, MarkupContainer parent) {
            // Try to find out if I'm in remove method ??
            if (componentsChanged == null) {
                componentsChanged = new HashSet<MarkupContainer>();
                cleanedComponentsChanged = false;
            }
            if (component instanceof MarkupContainer) {
                componentsChanged.add((MarkupContainer) component);
            }
        }
    }

    public void onBeforeRender(Component component) {
        Page page = component.getPage();
        if (page instanceof BasePage) {
            ArtifactoryRequestCycle cycle = (ArtifactoryRequestCycle) RequestCycle.get();
            setComponentMarkupId(component, ((BasePage) page).getIds(), cycle.getAjaxData());
        }
    }

    private void setComponentMarkupId(Component component, Map<String, Integer> ids,
            AjaxCycleData data) {
        // If already has our markupId don't bother
        if (hasMarkupId(component)) {
            return;
        }
        // In Ajax request check if a parent has good MarkupId
        if (data != null) {
            final Component[] goodParent = new Component[1];
            component.visitParents(Component.class, new Component.IVisitor() {
                public Object component(Component component) {
                    if (hasGoodMarkupId(component)) {
                        goodParent[0] = component;
                        return Component.IVisitor.STOP_TRAVERSAL;
                    }
                    return Component.IVisitor.CONTINUE_TRAVERSAL;
                }
            });
            if (goodParent[0] != null) {
                // It's a replaced Parent?
                data.cleanComponentsChanged();
                if (goodParent[0] instanceof MarkupContainer) {
                    changedMarkupIdsDeep((MarkupContainer) goodParent[0], data);
                }
                if (hasGoodMarkupId(component)) {
                    return;
                }
            }
        }
        setSelMarkupId(component, ids);
    }

    private void setSelMarkupId(Component component, Map<String, Integer> ids) {
        component.setOutputMarkupId(true);
        String key = MARKUP_ID_PREFIX + component.getId();
        Integer index = ids.get(key);
        if (index == null) {
            ids.put(key, 2);
        } else {
            ids.put(key, index + 1);
            key += index;
        }
        component.setMarkupId(key);
    }

    private boolean hasMarkupId(Component component) {
        String markupId = component.getMarkupId(false);
        return hasMarkupId(markupId);
    }

    private boolean hasMarkupId(String markupId) {
        return (markupId != null);
    }

    private boolean hasGoodMarkupId(Component component) {
        String markupId = component.getMarkupId(false);
        return hasGoodMarkupId(markupId);
    }

    private boolean hasGoodMarkupId(String markupId) {
        return (markupId != null && markupId.startsWith(MARKUP_ID_PREFIX));
    }

    private void changedMarkupIdsDeep(MarkupContainer newComponent,
            AjaxCycleData data) {
        String currentMarkupId = newComponent.getMarkupId(false);
        Iterator<MarkupContainer> componentIterator = data.componentsChanged.iterator();
        while (componentIterator.hasNext()) {
            MarkupContainer changed = componentIterator.next();
            String changedMarkupId = changed.getMarkupId(false);
            if (changedMarkupId != null && changedMarkupId.equals(currentMarkupId)) {
                // We have a replace, go through the childrens to steal markupids
                final Map<String, String> idsToMarkupIds = new HashMap<String, String>();
                changed.visitChildren(new Component.IVisitor() {
                    public Object component(Component component) {
                        String compMarkupId = component.getMarkupId(false);
                        if (hasGoodMarkupId(compMarkupId)) {
                            idsToMarkupIds.put(component.getId(), compMarkupId);
                        }
                        return Component.IVisitor.CONTINUE_TRAVERSAL;
                    }
                });
                newComponent.visitChildren(new Component.IVisitor() {
                    public Object component(Component component) {
                        if (hasGoodMarkupId(component)) {
                            System.err.println("Already done???");
                        } else {
                            String newMarkupId = idsToMarkupIds.get(component.getId());
                            if (newMarkupId != null) {
                                component.setOutputMarkupId(true);
                                component.setMarkupId(newMarkupId);
                            }
                        }
                        return Component.IVisitor.CONTINUE_TRAVERSAL;
                    }
                });
                // Don't go here again
                componentIterator.remove();
                break;
            }
        }
    }


}
