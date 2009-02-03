package org.artifactory.security;

import org.acegisecurity.Authentication;
import org.acegisecurity.ui.WebAuthenticationDetails;
import org.apache.log4j.Logger;

/**
 * Created by IntelliJ IDEA. User: yoav
 */
public abstract class AccessLogger {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(AccessLogger.class);

    public enum RepoPathAction {
        DOWNLOAD, DEPLOY, DELETE
    }

    public static void downloaded(RepoPath repoPath) {
        downloaded(repoPath, false, SecurityHelper.getAuthentication());
    }

    public static void downloadDenied(RepoPath repoPath) {
        downloaded(repoPath, true, SecurityHelper.getAuthentication());
    }

    public static void downloaded(RepoPath repoPath, boolean denied,
            Authentication authentication) {
        repoPathAction(repoPath, RepoPathAction.DOWNLOAD, denied, authentication);
    }

    public static void deployed(RepoPath repoPath) {
        deployed(repoPath, false, SecurityHelper.getAuthentication());
    }

    public static void deployDenied(RepoPath repoPath) {
        deployed(repoPath, true, SecurityHelper.getAuthentication());
    }

    public static void deployed(RepoPath repoPath, boolean denied, Authentication authentication) {
        repoPathAction(repoPath, RepoPathAction.DEPLOY, denied, authentication);
    }

    public static void deleted(RepoPath repoPath) {
        deleted(repoPath, false, SecurityHelper.getAuthentication());
    }

    public static void deleteDenied(RepoPath repoPath) {
        deleted(repoPath, true, SecurityHelper.getAuthentication());
    }

    public static void deleted(RepoPath repoPath, boolean denied, Authentication authentication) {
        repoPathAction(repoPath, RepoPathAction.DELETE, denied, authentication);
    }

    public static void repoPathAction(RepoPath repoPath, RepoPathAction action, boolean denied,
            Authentication authentication) {
        if (authentication != null) {
            Object details = authentication.getDetails();
            String address = null;
            if (details != null && details instanceof WebAuthenticationDetails) {
                address = ((WebAuthenticationDetails) details).getRemoteAddress();
            }
            LOGGER.info(
                    (denied ? "[DENIED " : "[ACCEPTED ") + action.name() + "] " + repoPath +
                            " for " + authentication.getName() + (
                            address != null ? "/" + address : "") + ".");
        }
    }
}
