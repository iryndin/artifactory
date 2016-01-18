package org.artifactory.addon.docker;

import com.google.common.collect.Lists;

import java.util.List;

/**
 * @author Shay Yaakov
 */
public class DockerV2InfoModel {

    public DockerTagInfoModel tagInfo = new DockerTagInfoModel();
    public List<DockerBlobInfoModel> blobsInfo = Lists.newArrayList();
}
