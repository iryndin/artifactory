/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.artifactory.update.security.v130beta1;

import org.apache.jackrabbit.util.Text;
import org.artifactory.api.repo.RepoPath;
import org.artifactory.api.security.ArtifactoryPermisssion;
import org.artifactory.api.security.PermissionTargetInfo;
import org.artifactory.version.converter.XmlConverter;
import org.jdom.Document;
import org.jdom.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Renames the tags named after class names to the shorter notation (for example
 * org.acegisecurity.acl.basic.SimpleAclEntry will be renamed to acl).
 * Will convert the permission masks and identifiers.
 *
 * @author freds
 */
public class RepoPathAclConverter implements XmlConverter {
    private static final Logger log = LoggerFactory.getLogger(RepoPathAclConverter.class);
    private static final String IDENTIFIER = "identifier";
    private static final String ACES = "aces";
    private static final String MASK = "mask";
    private static final String PRINCIPAL = "principal";

    @SuppressWarnings({"unchecked"})
    public void convert(Document doc) {
        Element aclsTag = doc.getRootElement().getChild("acls");
        List<Element> acls = aclsTag.getChildren();
        for (Element acl : acls) {
            if (acl.getName().contains("RepoPathAcl")) {
                acl.setName("acl");
                convertIdentifierToPermissionTarget(acl);
                Element acesTag = acl.getChild(ACES);
                Element aceListTag = acesTag.getChild("list");
                List<Element> aces = aceListTag.getChildren("org.artifactory.security.RepoPathAce");
                Element newAces = new Element(ACES);
                Element aceTemplate = new Element("ace");
                Element groupEl = new Element("group");
                groupEl.setText("false");
                aceTemplate.addContent(new Element(PRINCIPAL)).addContent(groupEl).addContent(new Element(MASK));
                for (Element ace : aces) {
                    Element newAce = (Element) aceTemplate.clone();
                    newAce.getChild(PRINCIPAL).setText(ace.getChildText(PRINCIPAL));
                    Element maskEl = ace.getChild(MASK);
                    int mask = Integer.parseInt(maskEl.getText());
                    if ((mask & (ArtifactoryPermisssion.ADMIN.getMask() |
                            ArtifactoryPermisssion.DEPLOY.getMask())) > 0) {
                        mask |= ArtifactoryPermisssion.DELETE.getMask();
                    }
                    newAce.getChild(MASK).setText("" + mask);
                    newAces.addContent(newAce);
                }
                acl.removeChild(ACES);
                acl.addContent(newAces);
            } else {
                log.warn("Acl tag " + acl + " under acls is not a RepoPAthAcl!");
            }
        }
    }

    private void convertIdentifierToPermissionTarget(Element acl) {
        String identifier = Text.unescape(acl.getChildText(IDENTIFIER));
        RepoPath repoPath = new RepoPath(identifier);
        acl.removeChild(IDENTIFIER);
        Element permissionTarget = new Element("permissionTarget");

        Element nameEl = new Element("name");
        if (repoPath.getRepoKey().equalsIgnoreCase(PermissionTargetInfo.ANY_REPO) &&
                repoPath.getPath().equalsIgnoreCase(PermissionTargetInfo.ANY_REPO)) {
            nameEl.setText(PermissionTargetInfo.ANY_PERMISSION_TARGET_NAME);
        } else {
            nameEl.setText(repoPath.getId());
        }
        permissionTarget.addContent(nameEl);

        Element repoKeyEl = new Element("repoKey");
        repoKeyEl.setText(repoPath.getRepoKey());
        permissionTarget.addContent(repoKeyEl);

        Element includesEl = new Element("includes");
        Element includeEl = new Element("string");
        if (repoPath.getPath().equalsIgnoreCase(PermissionTargetInfo.ANY_REPO)) {
            includeEl.setText(PermissionTargetInfo.ANY_PATH);
        } else {
            includeEl.setText(repoPath.getPath() + "/" + PermissionTargetInfo.ANY_PATH);
        }
        includesEl.addContent(includeEl);
        permissionTarget.addContent(includesEl);
        permissionTarget.addContent(new Element("excludes"));

        acl.addContent(permissionTarget);
    }
}
