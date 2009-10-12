package org.artifactory.webapp.wicket.common.component.panel.shortcutkey;

import java.io.Serializable;

/**
 * @author Yoav Aharoni
 */
public interface KeyListener extends Serializable {
    void keyReleased(KeyReleasedEvent e);
}
