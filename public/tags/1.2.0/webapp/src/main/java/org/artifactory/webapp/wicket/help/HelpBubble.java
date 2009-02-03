package org.artifactory.webapp.wicket.help;

import wicket.markup.html.basic.Label;

/**
 * @author Yoav Aharoni
 */

// todo >> yoava: replace help bubble with js
public class HelpBubble extends Label {

    public HelpBubble(String string, String helpMessage) {
        super(string, "<span class='help_bubble' title='" + helpMessage + "'>&nbsp;&nbsp;&nbsp;</span>");
        setEscapeModelStrings(false);
        setRenderBodyOnly(true);
    }
}
