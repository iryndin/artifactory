/*
 * Copyright 2009 JFrog Ltd. All rights reserved.
 * JFROG PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package org.artifactory.update.security.v4;

import org.artifactory.api.security.ArtifactoryPermission;
import org.artifactory.api.security.UserInfo;
import org.artifactory.version.converter.XmlConverter;
import org.jdom.Document;
import org.jdom.Element;

import java.util.List;

/**
 * An xml converter that locates any ACE (except anonymous) with admin and deploy permissions, and grants them an
 * annotate permission
 *
 * @author Noam Y. Tenne
 */
public class AnnotatePermissionXmlConverter implements XmlConverter {

    private static final String ACES = "aces";
    private static final String MASK = "mask";
    private static final String PRINCIPAL = "principal";
    private static final String ACE = "ace";

    @SuppressWarnings({"unchecked"})
    public void convert(Document doc) {
        Element aclsTag = doc.getRootElement().getChild("acls");
        List<Element> acls = aclsTag.getChildren();
        for (Element acl : acls) {
            Element acesTag = acl.getChild(ACES);
            List<Element> aces = acesTag.getChildren(ACE);
            Element newAces = new Element(ACES);
            Element aceTemplate = new Element(ACE);
            Element groupEl = new Element("group");
            groupEl.setText("false");
            aceTemplate.addContent(new Element(PRINCIPAL)).addContent(groupEl).addContent(new Element(MASK));
            for (Element ace : aces) {
                Element child = ace.getChild("principal");
                Element newAce = (Element) aceTemplate.clone();
                newAce.getChild(PRINCIPAL).setText(ace.getChildText(PRINCIPAL));

                Element maskEl = ace.getChild(MASK);
                int mask = Integer.parseInt(maskEl.getText());
                if (!child.getText().equals(UserInfo.ANONYMOUS)) {
                    if ((mask & (ArtifactoryPermission.ADMIN.getMask() |
                            ArtifactoryPermission.DEPLOY.getMask())) > 0) {
                        mask |= ArtifactoryPermission.ANNOTATE.getMask();
                    }
                }
                newAce.getChild(MASK).setText("" + mask);
                newAces.addContent(newAce);
            }
            acl.removeChild(ACES);
            acl.addContent(newAces);
        }
    }
}