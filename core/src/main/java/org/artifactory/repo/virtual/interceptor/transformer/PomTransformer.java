package org.artifactory.repo.virtual.interceptor.transformer;

import org.apache.maven.model.Activation;
import org.apache.maven.model.Model;
import org.apache.maven.model.Profile;
import org.artifactory.descriptor.repo.PomCleanupPolicy;
import org.artifactory.maven.MavenModelUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * @author Eli Givoni
 */
public class PomTransformer {
    private static final Logger log = LoggerFactory.getLogger(PomTransformer.class);

    private final String pomAsString;
    private final PomCleanupPolicy pomCleanupPolicy;

    public PomTransformer(String pomAsString, PomCleanupPolicy pomCleanupPolicy) {
        if (pomAsString == null) {
            throw new IllegalArgumentException("Null pom content is not allowed");
        }
        this.pomCleanupPolicy = pomCleanupPolicy;
        this.pomAsString = pomAsString;
    }

    public String transform() {
        if (pomCleanupPolicy.equals(PomCleanupPolicy.nothing)) {
            return pomAsString;
        }

        Model pom = null;
        try {
            pom = MavenModelUtils.stringToMavenModel(pomAsString);
        } catch (Exception e) {
            log.warn("Failed to parse pom '{}': ", e.getMessage(), e);
            return pomAsString;
        }
        //delete repositories and pluginsRepositories
        pom.setRepositories(null);
        pom.setPluginRepositories(null);
        boolean onlyActiveDefault = pomCleanupPolicy.equals(PomCleanupPolicy.discard_active_reference);

        //delete repositories and pluginsRepositories in profiles
        List profiles = pom.getProfiles();
        for (Object itemProfile : profiles) {
            Profile profile = (Profile) itemProfile;
            if (onlyActiveDefault) {
                boolean activeByDefault = false;
                Activation activation = profile.getActivation();
                if (activation != null) {
                    activeByDefault = activation.isActiveByDefault();
                }
                if (activeByDefault) {
                    deleteProfileRepositories(profile);
                }
            } else {
                deleteProfileRepositories(profile);
            }
        }

        return MavenModelUtils.mavenModelToString(pom);
    }

    private void deleteProfileRepositories(Profile profile) {
        profile.setRepositories(null);
        profile.setPluginRepositories(null);
    }
}
