package org.artifactory.webapp.wicket.page.config.services.cron;

import org.apache.commons.lang.StringUtils;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.AbstractReadOnlyModel;
import org.artifactory.webapp.wicket.common.behavior.JavascriptEvent;
import org.artifactory.webapp.wicket.common.component.links.BaseTitledLink;
import org.artifactory.webapp.wicket.utils.CronUtils;

import java.util.Date;

/**
 * @author Yoav Aharoni
 */
public class CronNextDatePanel extends Panel {
    public CronNextDatePanel(String id, final FormComponent cronExpField) {
        super(id);

        String nextRun = getNextRunTime(cronExpField.getValue());
        final Label nextRunLabel = new Label("cronExpNextRun", nextRun);
        nextRunLabel.setOutputMarkupId(true);
        add(nextRunLabel);

        // send ajax request with only cronExpField
        BaseTitledLink calculateButton = new BaseTitledLink("calculate", "Refresh");
        calculateButton.add(new JavascriptEvent("onclick", new AbstractReadOnlyModel() {
            public Object getObject() {
                return "dojo.byId('" + cronExpField.getMarkupId() + "').onchange();";
            }
        }));
        add(calculateButton);

        // update nextRunLabel on cronExpField change
        cronExpField.setOutputMarkupId(true);
        cronExpField.add(new AjaxFormComponentUpdatingBehavior("onchange") {
            protected void onUpdate(AjaxRequestTarget target) {
                nextRunLabel.setModelObject(getNextRunTime(cronExpField.getValue()));
                target.addComponent(nextRunLabel);
            }

            @Override
            protected void onError(AjaxRequestTarget target, RuntimeException e) {
                super.onError(target, e);
                nextRunLabel.setModelObject(getNextRunTime(cronExpField.getValue()));
                target.addComponent(nextRunLabel);
            }
        });
    }

    private String getNextRunTime(String cronExpression) {
        if (StringUtils.isEmpty(cronExpression)) {
            return "The cron expression is blank.";
        }
        if (CronUtils.isValid(cronExpression)) {
            Date nextExecution = CronUtils.getNextExecution(cronExpression);
            return formatDate(nextExecution);
        }
        return "The cron expression is invalid.";
    }


    private String formatDate(Date nextRunDate) {
        return nextRunDate.toString();
    }
}
