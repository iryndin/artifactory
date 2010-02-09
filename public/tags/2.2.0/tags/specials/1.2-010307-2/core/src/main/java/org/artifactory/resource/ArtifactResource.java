package org.artifactory.resource;

import org.apache.log4j.Logger;
import org.apache.maven.artifact.Artifact;
import org.apache.maven.artifact.ArtifactUtils;
import org.artifactory.jcr.JcrFile;
import org.artifactory.maven.MavenUtils;
import org.artifactory.repo.LocalRepo;
import org.artifactory.repo.Repo;

import java.io.File;
import java.util.LinkedList;
import java.util.StringTokenizer;

/**
 * Created by IntelliJ IDEA. User: yoavl
 */
public class ArtifactResource extends SimpleRepoResource {
    @SuppressWarnings({"UNUSED_SYMBOL", "UnusedDeclaration"})
    private final static Logger LOGGER = Logger.getLogger(ArtifactResource.class);

    private String groupId;
    private String artifactId;
    private String version;
    private String classifier;
    private String type;

    public ArtifactResource(
            String groupId, String artifactId, String version, String packagingType,
            String classifier, LocalRepo repo) {
        super(getRelativePath(groupId, artifactId, version, classifier, packagingType), repo);
        this.groupId = groupId;
        this.artifactId = artifactId;
        this.version = version;
        this.type = packagingType;
        this.classifier = classifier;
    }

    public ArtifactResource(String relPath, Repo repo) {
        super(relPath, repo);
        init();
    }

    public ArtifactResource(JcrFile file) {
        super(file);
        init();
    }

    private void init() {
        groupId = artifactId = version = type = classifier = NA;
        String name = getName();
        String relPath = getRelPath();
        //The format of the relative path in maven is a/b/c/artifactId/version where
        //groupId="a.b.c". We split the path to elements and analyze the needed fields.
        LinkedList<String> pathElements = new LinkedList<String>();
        StringTokenizer tokenizer = new StringTokenizer(relPath, "/");
        while (tokenizer.hasMoreTokens()) {
            pathElements.add(tokenizer.nextToken());
        }
        //Extract the type
        //The type is always the extension of the file name
        int lastDotIdx = name.lastIndexOf('.');
        if (lastDotIdx < 0 || lastDotIdx + 1 >= name.length()) {
            setError("Artifact resource type is unreadable.");
            return;
        }
        boolean metaData = MavenUtils.isMetaData(name);
        boolean hash = MavenUtils.isChecksum(name);
        //Do not calculate type or classifier for hashes and metadata files
        if (!metaData && !hash) {
            type = name.substring(lastDotIdx + 1);
        }
        //Sanity check, we need groupId, artifactId and version
        if (pathElements.size() < 3) {
            setError("The groupId, artifactId and version are unreadable.");
            return;
        }
        //Extract the version, artifactId and groupId
        int pos = pathElements.size() - 2;
        version = pathElements.get(pos--);
        artifactId = pathElements.get(pos--);
        StringBuffer groupIdBuff = new StringBuffer();
        for (; pos >= 0; pos--) {
            if (groupIdBuff.length() != 0) {
                groupIdBuff.insert(0, '.');
            }
            groupIdBuff.insert(0, pathElements.get(pos));
        }
        groupId = groupIdBuff.toString();
        //Extract the classifier
        if (!metaData && !hash) {
            //If resource is not a metadata the name should respect the full maven format and the
            //classifier is the delta between the actual name and the base name:
            //[artifactId]-[version].[type]
            String baseName = artifactId + "-" + version;
            boolean snapshot = MavenUtils.isVersionSnapshot(version);
            if (!snapshot) {
                if (!name.startsWith(baseName)) {
                    setError(getStandardLayoutMessage());
                    return;
                }
                if (name.length() != (baseName.length() + 1 + type.length())) {
                    //There should be a '-' sign between the default artifact name and the classifier
                    if (name.charAt(baseName.length()) != '-') {
                        setError(getStandardLayoutMessage());
                        return;
                    }
                    classifier = name.substring(
                            baseName.length() + 1, //After the '-'
                            name.length() - 1 - type.length()//Remove the .[type]
                    );
                }
            } else {
                //Check if the next character after the last '-' is not a digit
                int idx = name.lastIndexOf('-');
                if (!Character.isDigit(name.charAt(idx + 1))) {
                    classifier = name.substring(
                            idx + 1, //After the '-'
                            name.length() - 1 - type.length()//Remove the .[type]
                    );
                }
            }
        }
    }

    private String getStandardLayoutMessage() {
        return "The path does not respect Maven's standard layout requirements of " +
                "[artifactId]-[version]-[classifier].[type], which in this case should yield [" +
                artifactId + "-" + version + "." + type + "].";
    }

    private void setError(String message) {
        LOGGER.warn("Failed to build a ArtifactResource from '" + getRelPath() + "'. " +
                message + ".");
    }

    public String getGroupId() {
        return groupId;
    }

    public String getArtifactId() {
        return artifactId;
    }

    public String getVersion() {
        return version;
    }

    public String getClassifier() {
        if (NA.equals(classifier)) {
            return null;
        }
        return classifier;
    }

    public String getType() {
        return type;
    }

    public boolean isSnapshot() {
        return MavenUtils.isVersionSnapshot(version);
    }

    //TODO: [by yl] This doesn't belong here...
    public String getActualArtifactXml() {
        return (NA.equals(groupId) ? "" : wrapInTag(groupId, "groupId") + "\n") +
                (NA.equals(artifactId) ? "" : wrapInTag(artifactId, "artifactId") + "\n") +
                (NA.equals(version) ? "" : wrapInTag(version, "version") + "\n") +
                (NA.equals(classifier) || classifier == null ?
                        "" : wrapInTag(classifier, "classifier") + "\n");
    }

    public boolean isValid() {
        return !NA.equals(groupId) && !NA.equals(artifactId) &&
                !NA.equals(version) && !NA.equals(type);
    }

    public static String getRelativePath(Artifact artifact) {
        String groupId = artifact.getGroupId();
        String artifactId = artifact.getArtifactId();
        String version = artifact.getVersion();
        String classifier = artifact.getClassifier();
        String type = artifact.getType();
        return getRelativePath(groupId, artifactId, version, classifier, type);
    }

    @SuppressWarnings("UnnecessaryLocalVariable")
    public static String getRelativePath(
            String groupId, String artifactId, String version, String classifier, String type) {
        String relativePath = groupId.replace('.', '/') +
                "/" + artifactId + "/" + version + "/" +
                artifactId + "-" + version +
                (classifier != null ? "-" + classifier : "") +
                "." + type;
        return relativePath;
    }

    public String getId(String groupId, String artifactId, String classifier, String version,
            String repoKey) {
        String artifactKey =
                ArtifactUtils.artifactId(groupId, artifactId, type, classifier, version);
        return repoKey + "@" + artifactKey;
    }

    public boolean isStandardPackaging() {
        return isStandardPackaging(getRelPath());
    }

    /**
     * Checks whether the file path denotes a jar or a pom
     *
     * @param file The file in question
     * @return true if fits one of the standard packaging types
     */
    public static boolean isStandardPackaging(File file) {
        return isStandardPackaging(file.getPath());
    }

    public static boolean isStandardPackaging(String path) {
        for (PackagingType ext : PackagingType.LIST) {
            if (path.endsWith(ext.name())) {
                return true;
            }
        }
        return false;
    }

    private static String wrapInTag(String content, String tag) {
        return "<" + tag + ">" + content + "</" + tag + ">";
    }
}
