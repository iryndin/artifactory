package org.artifactory.addon.docker;

/**
 * @author Shay Yaakov
 */
public class DockerBlobInfoModel {

    public String id;
    public String shortId;
    public String digest;
    public String size;
    public String created;
    public String command;
    public String commandText;

    public DockerBlobInfoModel(String id, String digest, String size, String created) {
        this.id = id;
        this.shortId = id.substring(0, 12);
        this.digest = digest;
        this.size = size;
        this.created = created;
    }
}
