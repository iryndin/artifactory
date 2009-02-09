package org.artifactory.webapp.wicket.common.component.dnd.select.sorted;

import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.behavior.IBehavior;
import org.apache.wicket.markup.html.form.IChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.artifactory.webapp.wicket.common.behavior.JavascriptEvent;
import org.artifactory.webapp.wicket.common.component.dnd.select.DragDropSelection;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * @author Yoav Aharoni
 */
public class SortedDragDropSelection<T> extends DragDropSelection<T> {
    public SortedDragDropSelection(String id) {
        super(id);
    }

    public SortedDragDropSelection(String id, List<T> choices) {
        super(id, choices);
    }

    public SortedDragDropSelection(String id, List<T> choices, IChoiceRenderer renderer) {
        super(id, choices, renderer);
    }

    public SortedDragDropSelection(String id, IModel model, List<T> choices) {
        super(id, model, choices);
    }

    public SortedDragDropSelection(String id, IModel model, List<T> choices, IChoiceRenderer renderer) {
        super(id, model, choices, renderer);
    }

    public SortedDragDropSelection(String id, IModel choicesModel) {
        super(id, choicesModel);
    }

    public SortedDragDropSelection(String id, IModel model, IModel choicesModel) {
        super(id, model, choicesModel);
    }

    public SortedDragDropSelection(String id, IModel choicesModel, IChoiceRenderer renderer) {
        super(id, choicesModel, renderer);
    }

    public SortedDragDropSelection(String id, IModel model, IModel choicesModel, IChoiceRenderer renderer) {
        super(id, model, choicesModel, renderer);
    }

    {
        add(HeaderContributor.forJavaScript(SortedDragDropSelection.class, "SortedDragDropSelection.js"));
    }

    @Override
    protected String getWidgetClassName() {
        return "artifactory.SortedDragDropSelection";
    }

    @Override
    protected void onBeforeRender() {
        super.onBeforeRender();
        sortModelObject(getChoices());
        sortModelObject(getModel());
    }

    @Override
    protected IBehavior newOnOrderChangeEventBehavior(String event) {
        // no ajax notification
        return new JavascriptEvent(event, "");
    }

    @SuppressWarnings({"unchecked"})
    private void sortModelObject(IModel listModel) {
        if (listModel != null) {
            List<T> list = (List<T>) listModel.getObject();
            if (list != null) {
                Collections.sort(list, new ItemComparator());
            }
        }
    }

    public class ItemComparator implements Comparator {
        public int compare(Object o1, Object o2) {
            IChoiceRenderer renderer = getChoiceRenderer();
            return renderer.getDisplayValue(o1).toString().compareTo(renderer.getDisplayValue(o2).toString());
        }
    }
}
