/**
 * User: Dror Bereznitsky
 * Date: 01/04/2007
 * Time: 01:28:24
 */
package org.jfrog.maven.viewer.ui.event;

import org.springframework.context.ApplicationEvent;
import java.io.File;

public class SaveAsEvent extends ApplicationEvent {
    private final File file;
    private final String type;

    /**
     * Create a new ApplicationEvent.
     *
     * @param source the component that published the event
     */
    public SaveAsEvent(Object source, File file, String type) {
        super(source);
        this.file = file;
        this.type = type;
    }


    public File getFile() {
        return file;
    }

    public String getType() {
        return type;
    }
}
