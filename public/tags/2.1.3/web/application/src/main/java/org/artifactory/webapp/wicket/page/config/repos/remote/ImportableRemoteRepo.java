package org.artifactory.webapp.wicket.page.config.repos.remote;

import org.artifactory.descriptor.repo.RemoteRepoDescriptor;

import java.io.Serializable;

/**
 * Contains the list row data of an importable repository
 *
 * @author Noam Y. Tenne
 */
public class ImportableRemoteRepo implements Serializable {

    private RemoteRepoDescriptor repoDescriptor;
    private boolean existsAsLocal = false;
    private boolean existsAsRemote = false;
    private boolean existsAsVirtual = false;
    private boolean selected = false;

    /**
     * Main constructor
     *
     * @param repoDescriptor Remote repository descriptor
     */
    public ImportableRemoteRepo(RemoteRepoDescriptor repoDescriptor) {
        this.repoDescriptor = repoDescriptor;
    }

    /**
     * Returns the repository descriptor
     *
     * @return RemoteRepoDescriptor
     */
    public RemoteRepoDescriptor getRepoDescriptor() {
        return repoDescriptor;
    }

    /**
     * Returns the repository key
     *
     * @return The key of the repository
     */
    public String getRepoKey() {
        return repoDescriptor.getKey();
    }

    /**
     * Sets the repository key
     *
     * @param key Key to set for repository
     */
    public void setRepoKey(String key) {
        repoDescriptor.setKey(key);
    }

    /**
     * Returns the repository URL
     *
     * @return The URL of the repository
     */
    public String getRepoUrl() {
        return repoDescriptor.getUrl();
    }

    /**
     * Returns the repository description
     *
     * @return The description of the repository
     */
    public String getRepoDescription() {
        return repoDescriptor.getDescription();
    }

    /**
     * Indicates whether a local repository with an identical key already exists
     *
     * @return True if a local repository with an identical key already exists
     */
    public boolean isExistsAsLocal() {
        return existsAsLocal;
    }

    /**
     * Indicate whether a local repository with an identical key already exists
     *
     * @param existsAsLocal True if a local repository with an identical key already exists
     */
    public void setExistsAsLocal(boolean existsAsLocal) {
        this.existsAsLocal = existsAsLocal;
    }

    /**
     * Indicates whether a remote repository with an identical key already exists
     *
     * @return True if a remote repository with an identical key already exists
     */
    public boolean isExistsAsRemote() {
        return existsAsRemote;
    }

    /**
     * Indicate whether a remote repository with an identical key already exists
     *
     * @param existsAsRemote True if a remote repository with an identical key already exists
     */
    public void setExistsAsRemote(boolean existsAsRemote) {
        this.existsAsRemote = existsAsRemote;
    }

    /**
     * Indicates whether a virtual repository with an identical key already exists
     *
     * @return True if a virtual repository with an identical key already exists
     */
    public boolean isExistsAsVirtual() {
        return existsAsVirtual;
    }

    /**
     * Indicate whether a virtual repository with an identical key already exists
     *
     * @param existsAsVirtual True if a virtual repository with an identical key already exists
     */
    public void setExistsAsVirtual(boolean existsAsVirtual) {
        this.existsAsVirtual = existsAsVirtual;
    }

    /**
     * Indicates whether the current row object is selected
     *
     * @return Row selection status
     */
    public boolean isSelected() {
        return selected;
    }

    /**
     * Sets the row selection status
     *
     * @param selected True if selected
     */
    public void setSelected(boolean selected) {
        this.selected = selected;
    }
}