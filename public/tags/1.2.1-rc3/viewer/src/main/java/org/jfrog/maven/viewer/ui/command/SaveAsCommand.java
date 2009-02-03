/**
 * User: Dror Bereznitsky
 * Date: 01/04/2007
 * Time: 01:25:00
 */
package org.jfrog.maven.viewer.ui.command;

import org.jfrog.maven.viewer.ui.event.SaveAsEvent;
import org.jfrog.maven.viewer.ui.event.NewGraphEvent;
import org.jfrog.maven.viewer.common.Config;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.context.ApplicationEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.richclient.command.ActionCommand;
import org.springframework.richclient.filechooser.DefaultFileFilter;
import javax.swing.*;
import java.io.File;

public class SaveAsCommand extends ActionCommand implements ApplicationContextAware, ApplicationListener {
    private ApplicationContext applicationContext;
    
    private final static String[] supportedFileExtensions = {"jpg", "png"};
    
    protected void doExecuteCommand() {
        // Get last folder used for saving graph image
        File lastSaveFolder = new File(Config.getLastSaveFolder());
        if (!lastSaveFolder.exists() || !lastSaveFolder.isDirectory()) {
            lastSaveFolder = new File(System.getProperty("use.dir"));
        }

        JFileChooser fc = new JFileChooser(lastSaveFolder);        

        // Add filters for all supported file types
        for (String ext : supportedFileExtensions) {
            DefaultFileFilter fileFilter = new DefaultFileFilter(ext, ext);
            fc.addChoosableFileFilter(fileFilter);
        }
        fc.setAcceptAllFileFilterUsed(false);
        fc.setDialogType(JFileChooser.SAVE_DIALOG);

        int returnVal = fc.showSaveDialog(null);

        if (returnVal == JFileChooser.APPROVE_OPTION) {
            File file = fc.getSelectedFile();

            Config.setLastSaveFolder(file.getParentFile().getAbsolutePath());
            
            DefaultFileFilter fileFilter = (DefaultFileFilter) fc.getFileFilter();
            String type = (String) fileFilter.getExtensions().get(0);
            // Add file extension if missing
            if (getExtension(file) == null) {
                file = new File(file.getAbsolutePath() + "." + type);
            }
            applicationContext.getParent().publishEvent(new SaveAsEvent(this, file, type));
        }
    }

    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }

    private String getExtension(File f) {
        if (f != null) {
            String filename = f.getName();
            int i = filename.lastIndexOf('.');
            if (i > 0 && i < filename.length() - 1) {
                return filename.substring(i + 1).toLowerCase();
            }
        }
        return null;
    }

    // TODO - find a better way to implement this (PropertyChangeListener?)
    public void onApplicationEvent(ApplicationEvent event) {
        if (event instanceof NewGraphEvent) {
            setEnabled(true);
        }
    }
}
