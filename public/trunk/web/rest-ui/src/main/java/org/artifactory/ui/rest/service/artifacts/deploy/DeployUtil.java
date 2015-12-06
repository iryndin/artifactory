package org.artifactory.ui.rest.service.artifacts.deploy;

import org.artifactory.api.repo.exception.RepoRejectException;
import org.artifactory.exception.CancelException;

/**
 * @author Dan Feldman
 */
public class DeployUtil {

    public static String getDeployError(String fileName, String repoKey, Exception e) {
        String err = "Failed to deploy file: '" + fileName + "' to Repository: " + repoKey +
                ". Please check the log file for more details.";
        //Unfortunately Spring really messes up the exception that's thrown by the UploadService (was supposed to be
        //RepoRejectException with CancelException as the cause, instead we get RepoRejectException as the cause
        //And a concatenated string of both messages. the return code is also missing.
        if (e instanceof CancelException) {
            err = "Failed to deploy file: '" + fileName + "' to Repository: " + repoKey + ": " + e.getMessage();
        } else if (e instanceof RepoRejectException) {
            if (e.getMessage().contains(CancelException.class.getName())) {
                err = e.getMessage().replace(". " + CancelException.class.getName(), "");
            }
        }
        return err;
    }
}
