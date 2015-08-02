package org.artifactory.ui.rest.model.artifacts.browse.treebrowser.nodes;

import java.util.Collection;

import org.artifactory.api.security.AuthorizationService;
import org.artifactory.md.Properties;
import org.artifactory.rest.common.model.RestModel;
import org.codehaus.jackson.annotate.JsonSubTypes;
import org.codehaus.jackson.annotate.JsonTypeInfo;

/**
 * @author Chen Keinan
 */
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
@JsonSubTypes({@JsonSubTypes.Type(value = JunctionNode.class, name = "junction"),
        @JsonSubTypes.Type(value = RootNode.class, name = "root"),
        @JsonSubTypes.Type(value = FolderNode.class, name = "folder"),
        @JsonSubTypes.Type(value = RepositoryNode.class, name = "repository"),
        @JsonSubTypes.Type(value = ZipFileNode.class, name = "archive"),
        @JsonSubTypes.Type(value = FileNode.class, name = "file")})
public interface RestTreeNode extends RestModel {

    /**
     * update additional tree data
     */
    Collection<? extends RestModel> fetchItemTypeData(AuthorizationService authService, boolean isCompact, Properties props);

    /**
     * get node child's by authorization service service
     *
     * @param authService - authorization service
     * @return list for tee nodes
     */
    Collection<? extends RestTreeNode> getChildren(AuthorizationService authService, boolean isCompact);


}
