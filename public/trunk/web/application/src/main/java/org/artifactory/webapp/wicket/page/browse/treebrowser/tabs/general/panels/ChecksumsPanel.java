/*
 * Artifactory is a binaries repository manager.
 * Copyright (C) 2010 JFrog Ltd.
 *
 * Artifactory is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Artifactory is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Artifactory.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.artifactory.webapp.wicket.page.browse.treebrowser.tabs.general.panels;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.spring.injection.annot.SpringBean;
import org.artifactory.api.repo.RepositoryService;
import org.artifactory.api.security.AuthorizationService;
import org.artifactory.checksum.ChecksumInfo;
import org.artifactory.checksum.ChecksumType;
import org.artifactory.common.wicket.component.border.fieldset.FieldSetBorder;
import org.artifactory.common.wicket.component.links.TitledAjaxLink;
import org.artifactory.common.wicket.util.AjaxUtils;
import org.artifactory.fs.FileInfo;

/**
 * A panel to display MD5 and SHA1 checksums on the GeneralTabPanel
 *
 * @author Noam Tenne
 */
public class ChecksumsPanel extends Panel {

    @SpringBean
    private RepositoryService repoService;

    @SpringBean
    private AuthorizationService authService;

    public ChecksumsPanel(String id, final org.artifactory.fs.FileInfo file) {
        super(id);
        setOutputMarkupId(true);

        FieldSetBorder border = new FieldSetBorder("border");
        add(border);

        boolean isLocalRepo = isLocal(file.getRepoKey());
        boolean checksumsMatch = true;

        String md5 = "";
        ChecksumInfo md5Info = getChecksumOfType(file, ChecksumType.md5);
        if (md5Info != null) {
            checksumsMatch &= md5Info.checksumsMatch();
            md5 = buildChecksumString(md5Info, isLocalRepo);
        }

        String sha1 = "";
        ChecksumInfo sha1Info = getChecksumOfType(file, ChecksumType.sha1);
        if (sha1Info != null) {
            checksumsMatch &= sha1Info.checksumsMatch();
            sha1 = buildChecksumString(sha1Info, isLocalRepo);
        }

        border.add(new Label("md5", md5).setEscapeModelStrings(false));
        border.add(new Label("sha1", sha1).setEscapeModelStrings(false));
        WebMarkupContainer checksumMismatchContainer = new WebMarkupContainer("mismatch");
        border.add(checksumMismatchContainer);
        if (checksumsMatch) {
            checksumMismatchContainer.setVisible(false);
        } else {
            boolean canFixChecksum = authService.canDeploy(file.getRepoPath()) && !authService.isAnonymous();
            // if one is missing but the other is broken display the following
            StringBuilder message = new StringBuilder();
            String repoClass = isLocalRepo ? "Uploaded" : "Remote";
            if ((sha1Info == null || sha1Info.getOriginal() == null) && (md5Info == null || md5Info.getOriginal() == null)) {
                message.append(" Remote checksum doesn't exist. <br/>");
            } // if one is missing and the other is ok.
            else if (((sha1Info != null && sha1Info.checksumsMatch()) && (md5Info == null || md5Info.getOriginal() == null))
                    || ((md5Info != null && md5Info.checksumsMatch()) && (sha1Info == null || sha1Info.getOriginal() == null))) {
                message = new StringBuilder().append(repoClass).append(" Remote checksum doesn't exist. <br/>");
            } else if ((sha1Info.checksumsMatch() && !md5Info.checksumsMatch()) || (md5Info.checksumsMatch() && !sha1Info.checksumsMatch())) {
                // one is ok, the other is broken (not missing)
                message = new StringBuilder().append(repoClass).append(" checksum doesn't match the actual checksum. ")
                        .append("Please redeploy the artifact with a correct checksum.<br/>");
                // one is missing and the other is broken
            } else if (((sha1Info != null && !sha1Info.checksumsMatch()) && (md5Info == null || md5Info.getOriginal() == null))
                    || ((md5Info != null && !md5Info.checksumsMatch()) && (sha1Info == null || sha1Info.getOriginal() == null))) {
                message = new StringBuilder().append(repoClass).append(" checksum doesn't match the actual checksum. ")
                        .append("Please redeploy the artifact with a correct checksum.<br/>");
                // both are legally broken
            } else if (sha1Info != null && !sha1Info.checksumsMatch() && md5Info != null && !md5Info.checksumsMatch()) {
                message = new StringBuilder().append(repoClass).append(" checksum doesn't match the actual checksum. ")
                        .append("Please redeploy the artifact with a correct checksum.<br/>");
            }

            if (canFixChecksum) {
                message.append("If you trust the ").append(isLocalRepo ? "uploaded" : "remote")
                        .append(" artifact you can accept the actual checksum by clicking the 'Fix Checksum' button.");
            }

            checksumMismatchContainer
                    .add(new Label("mismatchMessage", message.toString()).setEscapeModelStrings(false));

            FixChecksumsButton fixChecksumsButton = new FixChecksumsButton(file);
            fixChecksumsButton.setVisible(canFixChecksum);
            checksumMismatchContainer.add(fixChecksumsButton);
        }
    }

    private ChecksumInfo getChecksumOfType(FileInfo file, ChecksumType checksumType) {
        if (file != null) {
            return file.getChecksumsInfo().getChecksumInfo(checksumType);
        }
        return null;
    }

    private String buildChecksumString(ChecksumInfo checksumInfo, boolean isLocalRepo) {
        StringBuilder sb = new StringBuilder()
                .append(checksumInfo.getType()).append(": ")
                .append(checksumInfo.getActual()).append(" (")
                .append(isLocalRepo ? "Uploaded" : "Remote").append(": ")
                .append(checksumInfo.checksumsMatch() ? "" : "<span style=\"color:red\"}>");

        if (checksumInfo.getOriginal() != null) {
            if (checksumInfo.checksumsMatch()) {
                sb.append("Identical");
            } else {
                sb.append(checksumInfo.getOriginal());
            }
        } else {
            sb.append("None");
        }

        sb.append(checksumInfo.checksumsMatch() ? "" : "</span>")
                .append(")");
        return sb.toString();
    }

    private boolean isLocal(String repoKey) {
        return repoService.localRepoDescriptorByKey(repoKey) != null;
    }

    private class FixChecksumsButton extends TitledAjaxLink {
        private final org.artifactory.fs.FileInfo file;

        public FixChecksumsButton(FileInfo file) {
            super("fix", "Fix Checksum");
            this.file = file;
        }

        public void onClick(AjaxRequestTarget target) {
            try {
                repoService.fixChecksums(file.getRepoPath());
                info("Fixed checksums inconsistency");
                // refresh the panel's content
                ChecksumsPanel currentPanel = ChecksumsPanel.this;
                ChecksumsPanel newPanel = new ChecksumsPanel(
                        currentPanel.getId(), repoService.getFileInfo(file.getRepoPath()));
                currentPanel.replaceWith(newPanel);
                target.addComponent(newPanel);
            } catch (Exception e) {
                error("Failed to fix checksum inconsistency");
            }
            AjaxUtils.refreshFeedback(target);
        }
    }
}
