package org.artifactory.addon.docker;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Set;

/**
 * @author Shay Yaakov
 */
public class DockerTagInfoModel {

    public String title;
    public String digest;
    public String totalSize;
    public Set<String> ports = Sets.newHashSet();
    public Set<String> volumes = Sets.newHashSet();
    public List<DockerLabel> labels = Lists.newArrayList();
}