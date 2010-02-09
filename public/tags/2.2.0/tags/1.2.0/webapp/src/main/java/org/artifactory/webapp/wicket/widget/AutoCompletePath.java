package org.artifactory.webapp.wicket.widget;

import org.apache.log4j.Logger;
import wicket.extensions.ajax.markup.html.autocomplete.AutoCompleteTextField;
import wicket.model.IModel;
import wicket.util.file.File;
import wicket.util.file.Folder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class AutoCompletePath extends AutoCompleteTextField {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(AutoCompletePath.class);

    public AutoCompletePath(String id, IModel object) {
        super(id, object);
    }

    protected Iterator<String> getChoices(String input) {
        int idx = input.lastIndexOf('/');
        String folderName;
        if (idx < 0) {
            folderName = "/";
        } else {
            folderName = input.substring(0, idx + 1);
        }
        Folder folder = new Folder(folderName);
        if (folder.exists() && folder.isDirectory()) {
            Folder[] folders = folder.getFolders();
            File[] files = folder.getFiles();
            List<String> paths = new ArrayList<String>(folders.length + files.length);
            for (Folder file : folders) {
                String path = file.getPath().replace('\\', '/');
                paths.add(path);
            }
            for (File file : files) {
                String path = file.getPath().replace('\\', '/');
                paths.add(path);
            }
            return paths.iterator();
        } else {
            return Collections.<String>emptyList().iterator();
        }
    }
}
