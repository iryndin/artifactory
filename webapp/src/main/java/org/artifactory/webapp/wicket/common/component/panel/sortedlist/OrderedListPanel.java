package org.artifactory.webapp.wicket.common.component.panel.sortedlist;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.HeaderContributor;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.HiddenField;
import org.apache.wicket.markup.html.link.AbstractLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.artifactory.webapp.wicket.common.behavior.CssClass;
import org.artifactory.webapp.wicket.common.behavior.JavascriptEvent;
import org.artifactory.webapp.wicket.common.component.links.SimpleTitledLink;
import org.artifactory.webapp.wicket.common.component.modal.links.ModalShowLink;
import org.artifactory.webapp.wicket.common.component.modal.panel.BaseModalPanel;
import org.artifactory.webapp.wicket.common.component.panel.titled.TitledPanel;
import org.artifactory.webapp.wicket.common.component.table.columns.panel.LinksColumnPanel;
import org.artifactory.webapp.wicket.common.component.template.HtmlTemplate;

import java.io.Serializable;
import static java.lang.String.format;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * @author Yoav Aharoni
 */
public abstract class OrderedListPanel<T> extends TitledPanel {
    protected OrderedListPanel(String id, Collection<T> collection) {
        this(id, new ArrayList<T>(collection));
    }

    protected OrderedListPanel(String id, List<T> list) {
        this(id, new Model((Serializable) list));
    }

    protected OrderedListPanel(String id, IModel listModel) {
        super(id, listModel);
        init();
    }

    protected void init() {
        add(HeaderContributor.forJavaScript(OrderedListPanel.class, "OrderedListPanel.js"));
        add(new CssClass("list-panel ordered-list-panel"));

        // add moveup/down links
        SimpleTitledLink upLink = new SimpleTitledLink("moveUpLink", "Up");
        add(upLink);

        SimpleTitledLink downLink = new SimpleTitledLink("moveDownLink", "Down");
        add(downLink);

        // add new item link
        add(new ModalShowLink("newItemLink", "New") {
            @Override
            protected BaseModalPanel getModelPanel() {
                return newCreateItemPanel();
            }
        });

        // add items container
        WebMarkupContainer container = new WebMarkupContainer("items");
        container.setOutputMarkupId(true);
        container.add(new SimpleAttributeModifier("dojoType", "dojo.dnd.Source"));
        container.add(new AttributeAppender("accept", true, new DndTypeModel(), ","));
        add(container);

        // add ListView
        ListView listView = new ListView("item", new DelegeteModel()) {
            @Override
            protected void populateItem(ListItem item) {
                OrderedListPanel.this.populateItem(item);
                item.add(new CssClass(item.getIndex() % 2 == 0 ? "even" : "odd"));
                item.add(new JavascriptEvent("onclick", ""));
            }
        };
        container.add(listView);

        // add hidden text field
        HiddenField textField = new HiddenField("listIndices", new IndicesModel());
        textField.setOutputMarkupId(true);
        textField.add(new OnOrderChangedEventBehavior());
        add(textField);

        // add init script
        HtmlTemplate template = new HtmlTemplate("initScript");
        template.setParameter("listId", new PropertyModel(container, "markupId"));
        template.setParameter("textFieldId", new PropertyModel(textField, "markupId"));
        template.setParameter("upLinkId", new PropertyModel(upLink, "markupId"));
        template.setParameter("downLinkId", new PropertyModel(downLink, "markupId"));
        add(template);
    }

    @SuppressWarnings({"RefusedBequest"})
    @Override
    public String getTitle() {
        return getString(getId() + ".title", null, getId() + ".title");
    }

    protected abstract String getItemDisplayValue(T itemObject);

    protected abstract BaseModalPanel newCreateItemPanel();

    protected abstract List<? extends AbstractLink> getItemActions(T itemObject, String linkId);

    @SuppressWarnings({"unchecked"})
    protected void populateItem(ListItem item) {
        item.add(new AttributeModifier("dndType", true, new DndTypeModel()));
        item.add(new CssClass("dojoDndItem"));

        T itemObject = (T) item.getModelObject();
        item.add(new Label("name", getItemDisplayValue(itemObject)));

        LinksColumnPanel linksPanel = new LinksColumnPanel("actions");
        item.add(linksPanel);
        List<? extends AbstractLink> links = getItemActions(itemObject, "link");
        for (AbstractLink link : links) {
            linksPanel.addLink(link);
        }
    }

    @SuppressWarnings({"unchecked"})
    public List<T> getList() {
        return (List<T>) getModelObject();
    }

    protected void onOrderChanged(AjaxRequestTarget target) {
        target.appendJavascript(format("dojo.byId('%s')._panel.resetIndices();", get("items").getMarkupId()));
    }

    private class DndTypeModel extends AbstractReadOnlyModel {
        @Override
        public Object getObject() {
            return "dnd-" + getMarkupId();
        }
    }

    private class DelegeteModel extends AbstractReadOnlyModel {
        @Override
        public Object getObject() {
            return getModelObject();
        }
    }

    private class IndicesModel extends Model {
        @Override
        public void setObject(Object object) {
            if (StringUtils.equals((String) object, (String) getObject())) {
                return;
            }
            super.setObject(object);

            if (object == null) {
                setModelObject(Collections.emptyList());
                return;
            }

            String indicesString = object.toString();
            String[] indices = indicesString.split(",");
            List<T> newList = new ArrayList<T>(indices.length);
            List<T> choices = getList();
            for (String index : indices) {
                Integer intIndex = Integer.valueOf(index);
                newList.add(choices.get(intIndex));
            }
            setModelObject(newList);
        }

    }

    public class OnOrderChangedEventBehavior extends AjaxFormComponentUpdatingBehavior {
        private OnOrderChangedEventBehavior() {
            super("onOrderChanged");
        }

        @Override
        protected void onUpdate(AjaxRequestTarget target) {
            onOrderChanged(target);
        }
    }
}
