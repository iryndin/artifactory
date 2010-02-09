package org.artifactory.webapp.wicket.page.config.general;

import org.apache.commons.io.FileUtils;
import org.apache.wicket.MarkupContainer;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.addon.AddonsManager;
import org.artifactory.api.config.CentralConfigService;
import org.artifactory.common.wicket.WicketProperty;
import org.artifactory.common.wicket.behavior.CssClass;
import org.artifactory.common.wicket.component.help.HelpBubble;
import org.artifactory.common.wicket.component.links.TitledAjaxSubmitLink;
import org.artifactory.common.wicket.component.panel.titled.TitledPanel;
import org.artifactory.common.wicket.panel.upload.UploadListener;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.descriptor.config.MutableCentralConfigDescriptor;
import org.artifactory.webapp.wicket.components.container.LogoLinkContainer;
import org.artifactory.webapp.wicket.page.config.SchemaHelpBubble;
import org.artifactory.webapp.wicket.panel.upload.DefaultFileUploadForm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

/**
 * @author Tomer Cohen
 */
public class CustomizingPanel extends TitledPanel implements UploadListener {
    private static final Logger log = LoggerFactory.getLogger(CustomizingPanel.class);

    @SpringBean
    private AddonsManager addonsManager;

    @SpringBean
    private CentralConfigService centralConfigService;


    @WicketProperty
    private String footer;

    private DefaultFileUploadForm fileUploadLogo;
    private String logoPath;
    private TextField urlLogo;
    private boolean deleteFile = false;


    public CustomizingPanel(String id) {
        super(id);
        getModelObject();
        add(new CssClass("general-settings-panel"));
        urlLogo = new TextField("logo");
        urlLogo.setOutputMarkupId(true);
        fileUploadLogo = new DefaultFileUploadForm("logoPath", this);
        fileUploadLogo.add(urlLogo);
        Label label = new Label("orLabel", "or");
        fileUploadLogo.add(label);
        add(fileUploadLogo);
        TextField footer = new TextField("footer");
        TitledAjaxSubmitLink resetButton = new TitledAjaxSubmitLink("reset", "Reset", fileUploadLogo) {

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                deleteFile = true;
                urlLogo.setModelObject("");
                MutableCentralConfigDescriptor mutableDescriptor = centralConfigService.getMutableDescriptor();
                mutableDescriptor.setLogo(null);
                //centralConfigService.saveEditedDescriptorAndReload(mutableDescriptor);
                File tmpFile = fileUploadLogo.getUploadedFile();
                if (tmpFile != null && tmpFile.exists()) {
                    FileUtils.deleteQuietly(tmpFile);
                }
                getPage().info("Custom logo has been reset.");
                LogoLinkContainer logoLink = new LogoLinkContainer("artifactoryLink");
                target.addComponent(urlLogo);
                target.addComponent(fileUploadLogo);
                MarkupContainer logoContainer =
                        ((WebMarkupContainer) getPage().get("logoWrapper")).addOrReplace(logoLink);
                target.addComponent(logoContainer);
                AjaxUtils.refreshFeedback(target);
                deleteFile = false;
            }
        };
        fileUploadLogo.add(resetButton);
        footer.setOutputMarkupId(true);
        add(footer);
        fileUploadLogo.add(new SchemaHelpBubble(("logo.help")));
        fileUploadLogo.add(new HelpBubble("logoFile.help", "Upload a logo image file."));
        add(new SchemaHelpBubble("footer.help"));
    }

    @Override
    public String getTitle() {
        return "Look and Feel Settings";
    }

    public void onException() {
        throw new RuntimeException("An error occurred during logo upload");
    }

    public void onFileSaved(File file) {
        File uploadedFile = fileUploadLogo.getUploadedFile();
        logoPath = uploadedFile.getAbsolutePath();
        log.debug("Getting uploaded file from {}", logoPath);
        if (uploadedFile != null && uploadedFile.exists()) {
            deleteFile = false;
            info("File Uploaded Successfully.");
        } else {
            log.error("Error in uploading file from the following location {}", logoPath);
            error(errorMessage(uploadedFile));
            throw new RuntimeException(errorMessage(uploadedFile));
        }
    }

    private String errorMessage(File uploadedFile) {
        return "Error in uploading file from " + uploadedFile.getAbsolutePath() + ".";
    }

    public String getLogoPath() {
        return logoPath;
    }

    public boolean isDeleteFile() {
        return deleteFile;
    }
}
