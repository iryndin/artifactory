package org.artifactory.webapp.wicket.security.acls;

import org.acegisecurity.acl.basic.BasicAclEntry;
import org.acegisecurity.acl.basic.SimpleAclEntry;
import org.acegisecurity.userdetails.User;
import org.acegisecurity.userdetails.UserDetails;
import org.apache.log4j.Logger;
import org.artifactory.security.ExtendedJdbcAclDao;
import org.artifactory.security.SecurityHelper;
import org.artifactory.security.SimpleUser;
import org.artifactory.security.StringObjectIdentity;
import org.artifactory.spring.ArtifactoryContext;
import org.artifactory.spring.ContextUtils;
import org.artifactory.webapp.wicket.components.DojoButton;
import wicket.ajax.AjaxRequestTarget;
import wicket.markup.html.WebMarkupContainer;
import wicket.markup.html.basic.Label;
import wicket.markup.html.form.Form;
import wicket.markup.html.form.IChoiceRenderer;
import wicket.markup.html.form.ListMultipleChoice;
import wicket.markup.html.panel.FeedbackPanel;
import wicket.markup.html.panel.Panel;
import wicket.model.CompoundPropertyModel;
import wicket.model.Model;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class GroupRecipientsPanel extends Panel {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(GroupRecipientsPanel.class);

    private List<UserDetails> admins = new ArrayList<UserDetails>();
    private List<UserDetails> deployers = new ArrayList<UserDetails>();
    private List<UserDetails> readers = new ArrayList<UserDetails>();
    private FeedbackPanel feedback;

    public GroupRecipientsPanel(String id) {
        super(id);
        setOutputMarkupId(true);
        setModel(new CompoundPropertyModel(this));
        //Feedback
        feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        add(feedback);

        Form form = new Form("groupRecipientsForm");
        add(form);
    }

    //A dropdown for the users - the ones already chosen + add/remove button
    public void updateModelObject(
            final Group group, final WebMarkupContainer container, AjaxRequestTarget target) {
        setVisible(true);

        //Restore the selections
        ArtifactoryContext context = ContextUtils.getArtifactoryContext();
        SecurityHelper security = context.getSecurity();
        final StringObjectIdentity identity = new StringObjectIdentity(group.getGroupId());
        BasicAclEntry[] entries = security.getAclDao().getAcls(identity);
        admins.clear();
        deployers.clear();
        readers.clear();
        for (BasicAclEntry entry : entries) {
            int mask = entry.getMask();
            String recipient = (String) entry.getRecipient();
            User user = new SimpleUser(recipient);
            if ((mask & SimpleAclEntry.ADMINISTRATION) > 0) {
                admins.add(user);
            }
            if ((mask & SimpleAclEntry.WRITE) > 0) {
                deployers.add(user);
            }
            if ((mask & SimpleAclEntry.READ) > 0) {
                readers.add(user);
            }
        }

        Form form = new Form("groupRecipientsForm");
        //Form header
        final Label groupRecipientsFormHeader = new Label("groupRecipientsFormHeader");
        groupRecipientsFormHeader.setModel(new Model("\"" + group.getGroupId() + "\" Permissions"));
        form.add(groupRecipientsFormHeader);
        //List of recipients except for admins that are filtered out from acl management
        final List<UserDetails> availableRecipients =
                security.getUserDetailsService().getAllUsers(false);
        final ListMultipleChoice adminsChoice =
                new ListMultipleChoice(
                        "admins", availableRecipients,
                        new IChoiceRenderer() {
                            public Object getDisplayValue(Object object) {
                                UserDetails userDetails = (UserDetails) object;
                                return userDetails.getUsername();
                            }

                            public String getIdValue(Object object, int index) {
                                UserDetails userDetails = (UserDetails) object;
                                return userDetails.getUsername();
                            }
                        });
        form.add(adminsChoice);
        final ListMultipleChoice deployersChoice =
                new ListMultipleChoice(
                        "deployers", availableRecipients,
                        new IChoiceRenderer() {
                            public Object getDisplayValue(Object object) {
                                UserDetails userDetails = (UserDetails) object;
                                return userDetails.getUsername();
                            }

                            public String getIdValue(Object object, int index) {
                                UserDetails userDetails = (UserDetails) object;
                                return userDetails.getUsername();
                            }
                        });
        form.add(deployersChoice);
        final ListMultipleChoice readersChoice =
                new ListMultipleChoice(
                        "readers", availableRecipients,
                        new IChoiceRenderer() {
                            public Object getDisplayValue(Object object) {
                                UserDetails userDetails = (UserDetails) object;
                                return userDetails.getUsername();
                            }

                            public String getIdValue(Object object, int index) {
                                UserDetails userDetails = (UserDetails) object;
                                return userDetails.getUsername();
                            }
                        });
        form.add(readersChoice);
        //Cancel
        DojoButton cancel = new DojoButton("cancel", form, "Cancel") {
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                //Hide the panel
                GroupRecipientsPanel.this.setVisible(false);
                target.addComponent(container);
            }
        };
        cancel.setDefaultFormProcessing(false);
        form.add(cancel);
        //Submit
        DojoButton submit = new DojoButton("submit", form, "Apply") {
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                super.onSubmit(target, form);
                //Update the acls
                ArtifactoryContext context = ContextUtils.getArtifactoryContext();
                SecurityHelper security = context.getSecurity();
                ExtendedJdbcAclDao aclDao = security.getAclDao();
                //Clear any existing acls
                aclDao.deleteAcls(identity);
                for (UserDetails recipient : availableRecipients) {
                    int mask = 0;
                    if (admins.contains(recipient)) {
                        mask |= SimpleAclEntry.ADMINISTRATION;
                    }
                    if (deployers.contains(recipient)) {
                        mask |= SimpleAclEntry.WRITE;
                    }
                    if (readers.contains(recipient)) {
                        mask |= SimpleAclEntry.READ;
                    }
                    SimpleAclEntry aclEntry = new SimpleAclEntry(
                            new SimpleUser(recipient), identity, null, mask);
                    aclDao.create(aclEntry);
                }
            }

            protected void onError(AjaxRequestTarget target, Form form) {
                target.addComponent(feedback);
            }
        };
        form.add(submit);

        replace(form);
        target.addComponent(container);
    }


    public List<UserDetails> getAdmins() {
        return admins;
    }

    public void setAdmins(List<UserDetails> admins) {
        this.admins = admins;
    }


    public List<UserDetails> getDeployers() {
        return deployers;
    }

    public void setDeployers(List<UserDetails> deployers) {
        this.deployers = deployers;
    }

    public List<UserDetails> getReaders() {
        return readers;
    }

    public void setReaders(List<UserDetails> readers) {
        this.readers = readers;
    }
}
