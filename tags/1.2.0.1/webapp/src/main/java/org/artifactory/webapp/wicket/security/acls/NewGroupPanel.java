package org.artifactory.webapp.wicket.security.acls;

import org.artifactory.security.ExtendedJdbcAclDao;
import org.artifactory.security.SecurityHelper;
import org.artifactory.security.StringObjectIdentity;
import org.artifactory.webapp.wicket.ArtifactoryPage;
import org.artifactory.webapp.wicket.components.DojoButton;
import org.artifactory.webapp.wicket.window.WindowPanel;
import org.springframework.dao.DataIntegrityViolationException;
import wicket.Component;
import wicket.ajax.AjaxRequestTarget;
import wicket.markup.html.WebMarkupContainer;
import wicket.markup.html.form.Form;
import wicket.markup.html.form.RequiredTextField;
import wicket.markup.html.panel.FeedbackPanel;
import wicket.model.CompoundPropertyModel;
import wicket.model.Model;

/**
 * @author Yoav Aharoni
 */
public class NewGroupPanel extends WindowPanel {

    public NewGroupPanel(String string, final Component groupsTable, final WebMarkupContainer groupRecipientsPanel) {
        super(string);
        //Add a feedback panel
        final FeedbackPanel feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);
        //Add the add group function
        Form form = new Form("newGroupForm", newEmptyGroup());
        form.setOutputMarkupId(true);
        //GroupId
        final RequiredTextField groupIdTf = new RequiredTextField("groupId");
        form.add(groupIdTf);
        //Cancel
        DojoButton cancel = new DojoButton("cancel", form, "Cancel") {
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                form.setModel(newEmptyGroup());
                target.addComponent(form);
                target.addComponent(feedback);
                groupRecipientsPanel.setVisible(false);
                target.addComponent(groupRecipientsPanel);
            }
        };
        cancel.setDefaultFormProcessing(false);
        form.add(cancel);
        //Submit
        DojoButton submit = new DojoButton("submit", form, "Create") {
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                super.onSubmit(target, form);
                ArtifactoryPage page = (ArtifactoryPage) getPage();
                SecurityHelper security = page.getContext().getSecurity();
                ExtendedJdbcAclDao aclDao = security.getAclDao();
                String group = (String) groupIdTf.getModelObject();
                //TODO: [by yl] Do we want to compute a parent group?
                try {
                    aclDao.createAclObjectIdentity(new StringObjectIdentity(group), null);
                } catch (DataIntegrityViolationException e) {
                    error("Group '" + group + "' already exists.");
                    target.addComponent(feedback);
                    return;
                } catch (Exception e) {
                    error("Failed to create group: " + e.getMessage());
                    target.addComponent(feedback);
                    return;
                }
                //Rerender the table
                groupsTable.modelChanged();
                target.addComponent(groupsTable);
                form.setModel(newEmptyGroup());
                target.addComponent(form);
                info("Group '" + group + "' created successfully.");
                groupRecipientsPanel.setVisible(false);
                target.addComponent(groupRecipientsPanel);
                target.addComponent(feedback);
            }

            protected void onError(AjaxRequestTarget target, Form form) {
                target.addComponent(feedback);
            }
        };
        form.add(submit);
        add(form);
    }

    private static CompoundPropertyModel newEmptyGroup() {
        return new CompoundPropertyModel(new Model(new Group()));
    }
}
