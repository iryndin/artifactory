package org.artifactory.webapp.wicket.common.component.navigation;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.navigation.paging.IPageable;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.artifactory.webapp.wicket.common.component.links.TitledAjaxLink;

import static java.lang.String.format;
import java.util.ArrayList;
import java.util.List;

/**
 * User: yevgenys
 * Date: Oct 16, 2008
 * Time: 4:22:45 PM
 */

public class PagingNavigatorWithDropDown extends Panel {
    private IPageable pageable;

    private Integer currentPageIndex = 0;
    private static final String PREV_PAGE = "prevPage";
    private static final String NEXT_PAGE = "nextPage";

    public PagingNavigatorWithDropDown(String id, IPageable pageable) {
        super(id);
        this.pageable = pageable;
        setOutputMarkupId(true);
    }

    @Override
    protected void onBeforeRender() {
        // already added ?
        if (get(PREV_PAGE) != null) {
            super.onBeforeRender();
            return;
        }

        TitledAjaxLink link;

        // prev link
        link = new TitledAjaxLink(PREV_PAGE) {
            public void onClick(AjaxRequestTarget target) {
                // no more prev pages ?
                if (pageable.getCurrentPage() < 1) {
                    // update the buttons state
                    updateButtons(target);
                    return;
                }

                currentPageIndex--;

                // changing the page index
                pageable.setCurrentPage(pageable.getCurrentPage() - 1);

                // render it
                target.addComponent((Component) pageable);

                // update the buttons state
                updateButtons(target);
            }
        };
        link.setEnabled(!isPrevButtonDisabled());
        add(link);

        // pageable drop down
        DropDownChoice pdd = new DropDownChoice("pageableDropDown", new Model() {
            @Override
            public Object getObject() {
                return currentPageIndex;
            }

            @Override
            public void setObject(Object object) {
                if (object == null) {
                    return;
                }
                currentPageIndex = (Integer) object;
            }
        }, getPages());
        pdd.setOutputMarkupId(true);
        pdd.setChoiceRenderer(new ChoiceRenderer() {
            @Override
            public Object getDisplayValue(Object object) {
                if (object == null) {
                    return null;
                }

                // displaying the "Page X of Y"
                return format("Page %d of %d", (Integer) object + 1, pageable.getPageCount());
            }

        });

        // adding ajax
        pdd.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            @Override
            protected void onUpdate(AjaxRequestTarget target) {
                if (currentPageIndex == null) {
                    return;
                }

                // changing the page
                pageable.setCurrentPage(currentPageIndex);

                // render it !
                target.addComponent(((Component) pageable).getParent());

                updateButtons(target);

            }
        });
        add(pdd);

        // prev link
        link = new TitledAjaxLink(NEXT_PAGE) {
            public void onClick(AjaxRequestTarget target) {
                // no more next pages ?
                if (pageable.getCurrentPage() > pageable.getPageCount() - 2) {
                    // update the buttons state
                    updateButtons(target);
                    return;
                }

                // inc the index
                currentPageIndex++;

                // changing the page index
                pageable.setCurrentPage(pageable.getCurrentPage() + 1);

                // render it !!!
                target.addComponent((Component) pageable);

                // update the buttons state
                updateButtons(target);
            }
        };
        link.setEnabled(!isNextButtonDisabled());
        add(link);

        super.onBeforeRender();

    }


    private void updateButtons(AjaxRequestTarget target) {
        updateButton(target, PREV_PAGE, !isPrevButtonDisabled());
        updateButton(target, NEXT_PAGE, !isNextButtonDisabled());
    }

    private boolean isNextButtonDisabled() {
        return pageable.getCurrentPage() > pageable.getPageCount() - 2;
    }

    private boolean isPrevButtonDisabled() {
        return pageable.getCurrentPage() < 1;
    }

    private void updateButton(AjaxRequestTarget target, String buttonId, boolean state) {
        Component c = get(buttonId);

        // rendering the button ONLY if the state was changed
        if (c.isEnabled() != state) {
            c.setEnabled(state);
            target.addComponent(c);
        }
    }

    private List<Integer> getPages() {
        List<Integer> pages = new ArrayList<Integer>(2);

        // filling the list
        for (int i = 0; i < pageable.getPageCount(); i++) {
            pages.add(i);
        }

        return pages;
    }
}
