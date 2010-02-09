package org.artifactory.webapp.wicket.page.logs;

import org.apache.commons.io.FileUtils;
import org.apache.wicket.ajax.AbstractAjaxTimerBehavior;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.form.AjaxFormComponentUpdatingBehavior;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.link.DownloadLink;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.util.time.Duration;
import org.artifactory.common.ArtifactoryHome;
import org.artifactory.common.ConstantsValue;
import org.artifactory.webapp.wicket.common.component.template.HtmlTemplate;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import static java.util.Arrays.asList;
import java.util.Date;
import java.util.List;

/**
 * This panel serves as as auto display of the information and content in the system log file, with an option of
 * downloading the file
 *
 * @author Noam Tenne
 */
public class SystemLogsViewPanel extends Panel {
    private static final int FIRST_READ_BLOCK_SIZE = 100 * 1024;

    /**
     * File object of the system log
     */
    private File systemLogFile = new File(ArtifactoryHome.getLogDir(), "artifactory.log");

    /**
     * Label to display the path of the currently tailed log file
     */
    Label pathLabel = new Label("systemLogsPath", getLogPathCaption());

    /**
     * Link object for downloading the system log file
     */
    private DownloadLink downloadLink =
            new DownloadLink("systemLogsLink", systemLogFile, "artifactory.log");

    /**
     * Label to display the dash between the download path and link
     */
    Label dashLabel = new Label("dashLabel", " - ");

    /**
     * Label to represent the download link caption
     */
    Label linkLabel = new Label("linkLabel", "Download");

    /**
     * Label to display the size of the system log file
     */
    private Label sizeLabel = new Label("systemLogsSize", "");

    /**
     * Label to display the content of the system log file (100K at a time)
     */
    private Label contentLabel = new Label("systemLogsContent", "");

    /**
     * Label to display file last modified and view last updated times
     */
    private Label lastUpdateLabel = new Label("lastUpdate", "");

    /**
     * Pointer to indicate the last position the log file was read from
     */
    private long lastPointer;

    /**
     * List containing log file names
     */
    private static final List<String> LOGS = asList("artifactory.log", "access.log", "import.export.log");

    /**
     * Main constructor
     *
     * @param id The verbal ID of the panel
     */
    public SystemLogsViewPanel(String id) {
        super(id);
        addLogComboBox();
        addSystemLogPath();
        addSystemLogsSize();
        addSystemLogsLink();
        addSystemLogsContent();
        addLastUpdate();

        // add the timer behavior to the page and make it update both components
        add(new AbstractAjaxTimerBehavior(Duration.seconds(ConstantsValue.logsRefreshRateSecs.getInt())) {
            @Override
            protected void onTimer(AjaxRequestTarget target) {
                updateComponents(target, (!systemLogFile.exists()));
            }
        });
    }

    /**
     * Adds a combo box with log file choices
     */
    private void addLogComboBox() {
        final DropDownChoice logsDropDownChoice = new DropDownChoice("logs", new Model(LOGS.get(0)), LOGS) {

            @Override
            protected boolean wantOnSelectionChangedNotifications() {
                return true;
            }
        };
        /**
         * Add behavior for when the combo box selection is changed
         */
        logsDropDownChoice.add(new AjaxFormComponentUpdatingBehavior("onChange") {

            protected void onUpdate(AjaxRequestTarget target) {
                int choice = Integer.parseInt(logsDropDownChoice.getValue());
                List choices = logsDropDownChoice.getChoices();
                String selectedLog = choices.get(choice).toString();
                systemLogFile = new File(ArtifactoryHome.getLogDir(), selectedLog);
                updateComponents(target, true);
            }
        });

        add(logsDropDownChoice);
    }

    /**
     * Update the different display components
     *
     * @param target     The AjaxRequestTarget from our action
     * @param cleanPanel True if the text container should be cleaned of content. false if not
     */
    private void updateComponents(AjaxRequestTarget target, boolean cleanPanel) {
        //Make sure the botton of the text area will be displayed after every update
        target.appendJavascript(
                "ArtifactoryLog.log('"
                        + contentLabel.getMarkupId()
                        + "', '"
                        + readLogAndUpdateSize(cleanPanel).replaceAll("[']", "\\\\'")
                        + "', '"
                        + cleanPanel
                        + "');");
        target.addComponent(dashLabel);
        target.addComponent(downloadLink);
        target.addComponent(pathLabel);
        target.addComponent(sizeLabel);
        target.addComponent(linkLabel);
        target.addComponent(lastUpdateLabel);
    }

    /**
     * Add a label with the path to the system log file
     */
    private void addSystemLogPath() {
        add(pathLabel);
        pathLabel.setOutputMarkupId(true);
    }

    /**
     * Add a label with the size of the system log file
     */
    private void addSystemLogsSize() {
        add(sizeLabel);
        sizeLabel.setOutputMarkupId(true);
    }

    /**
     * Add a link to enable the download of the system log file
     */
    private void addSystemLogsLink() {
        add(dashLabel);
        add(downloadLink);
        downloadLink.add(linkLabel);
        downloadLink.setOutputMarkupId(true);
        linkLabel.setOutputMarkupId(true);
        dashLabel.setOutputMarkupId(true);
    }

    /**
     * Add a label with the log file and view update times
     */
    private void addLastUpdate() {
        add(lastUpdateLabel);
        lastUpdateLabel.setOutputMarkupId(true);
    }

    /**
     * A a label to display the content of the system log file (last 100K)
     */
    private void addSystemLogsContent() {
        add(contentLabel);
        contentLabel.setOutputMarkupId(true);
        contentLabel.setEscapeModelStrings(false);
        contentLabel.setModelObject(readLogAndUpdateSize(false));

        HtmlTemplate initScript = new HtmlTemplate("initScript");
        initScript.setParameter("logDivId", new PropertyModel(contentLabel, "markupId"));
        add(initScript);
    }

    /**
     * Attemps to continue reading the log file from the last position, and the updates the log path, size and link
     * According to the outcome.
     *
     * @param cleanPanel True if the text container should be cleaned of content. false if not
     * @return String - The newly read content
     */
    protected String readLogAndUpdateSize(boolean cleanPanel) {
        if ((lastPointer > systemLogFile.length()) || cleanPanel) {
            lastPointer = 0;
        }
        pathLabel.setModelObject(getLogPathCaption());
        long size = systemLogFile.length();
        setLogInfo();
        if (lastPointer == size) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        RandomAccessFile logRandomAccessFile = null;
        try {
            logRandomAccessFile = new RandomAccessFile(systemLogFile, "r");

            //If the log file is larger than 100K
            if (lastPointer == 0 && logRandomAccessFile.length() > FIRST_READ_BLOCK_SIZE) {
                //Point to the begining of the last 100K
                lastPointer = logRandomAccessFile.length() - FIRST_READ_BLOCK_SIZE;
            }
            logRandomAccessFile.seek(lastPointer);

            String line;
            while ((line = logRandomAccessFile.readLine()) != null) {
                sb.append("<div>").append(line).append("<br/></div>");
            }
            lastPointer = logRandomAccessFile.getFilePointer();

        } catch (IOException e) {
            throw new RuntimeException(e.getMessage());
        }
        finally {
            try {
                if (logRandomAccessFile != null) {
                    logRandomAccessFile.close();
                }
            } catch (IOException ignore) {
            }
        }
        return sb.toString();
    }

    /**
     * Returns the caption of the log path label
     *
     * @return String Caption of the log path
     */
    private String getLogPathCaption() {
        if (systemLogFile.exists()) {
            return "Viewing log file at: " + systemLogFile.getAbsolutePath();
        } else {
            return "Cannot find log file at expected location: " + systemLogFile.getAbsolutePath();
        }
    }

    /**
     * Sets the attributes of the log size, download links and update times according to the log availability
     */
    private void setLogInfo() {
        if (!systemLogFile.exists()) {
            dashLabel.setModelObject("");
            sizeLabel.setModelObject("");
            linkLabel.setModelObject("");
            lastUpdateLabel.setModelObject("");
        } else {
            dashLabel.setModelObject(" - ");
            sizeLabel.setModelObject("(" + FileUtils.byteCountToDisplaySize(systemLogFile.length()) + ")");
            linkLabel.setModelObject("Download");
            StringBuilder sb = new StringBuilder();
            Date logLastModified = new Date(systemLogFile.lastModified());
            Date viewLastUpdate = new Date(System.currentTimeMillis());
            sb.append("File last modified: ").append(logLastModified).append(". ");
            sb.append("View last updated: ").append(viewLastUpdate).append(".");
            lastUpdateLabel.setModelObject(sb.toString());
        }
    }
}
